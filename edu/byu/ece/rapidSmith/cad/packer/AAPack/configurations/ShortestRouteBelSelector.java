package edu.byu.ece.rapidSmith.cad.packer.AAPack.configurations;

import edu.byu.ece.rapidSmith.cad.clusters.Cluster;
import edu.byu.ece.rapidSmith.cad.clusters.ClusterConnection;
import edu.byu.ece.rapidSmith.cad.clusters.ClusterTemplate;
import edu.byu.ece.rapidSmith.cad.clusters.PackCell;
import edu.byu.ece.rapidSmith.cad.packer.AAPack.BelSelector;
import edu.byu.ece.rapidSmith.design.subsite.Cell;
import edu.byu.ece.rapidSmith.design.subsite.CellNet;
import edu.byu.ece.rapidSmith.design.subsite.CellPin;
import edu.byu.ece.rapidSmith.device.Bel;
import edu.byu.ece.rapidSmith.device.BelId;
import edu.byu.ece.rapidSmith.device.BelPin;
import edu.byu.ece.rapidSmith.device.PrimitiveType;
import edu.byu.ece.rapidSmith.device.helper.HashPool;
import edu.byu.ece.rapidSmith.util.StackedHashMap;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

/**
 *
 */
public class ShortestRouteBelSelector implements BelSelector {
	private static final double MUST_LEAVE_CLUSTER_COST = 2.0;
	// Slight penalty for BELs near other used BELs. This is a slight
	// offset to preserve nearby BELs
	private static final double RESERVE_BEL_PENALTY = 0.02;

	private static final double LEAVE_SITE_PENALTY = 0.5;
	private static final double PIP_PENALTY = 0.9;
	private static final double[] PIP_PENALTIES = new double[10];

	private Cluster<?, ?> cluster;

	private final Map<String, Map<BelId, Double>> baseBelCostMap;
	private StackedHashMap<Bel, Double> reserveBelCostMap;
	private PriorityQueue<BelCandidate> pq;
	private Deque<PriorityQueue<BelCandidate>> pqStack;

	private boolean clusterInitialized = false;
	private boolean cellInitialized = false;
	private int numCommits;

	public ShortestRouteBelSelector(Path belCostsFiles) throws IOException, JDOMException {
		baseBelCostMap = new HashMap<>();

		double penalty = 1.0;
		for (int i = 0; i < 10; i++) {
			PIP_PENALTIES[i] = penalty;
			penalty *= PIP_PENALTY;
		}

		loadBelCosts(belCostsFiles);
	}

	private void loadBelCosts(Path belCostsFiles) throws IOException, JDOMException {
		HashPool<Double> doublePool = new HashPool<>();
		SAXBuilder builder = new SAXBuilder();
		Document doc = builder.build(belCostsFiles.toFile());

		Element rootEl = doc.getRootElement();
		for (Element cellEl : rootEl.getChildren("cell")) {
			HashMap<BelId, Double> belsMap = new HashMap<>();
			baseBelCostMap.put(cellEl.getChildText("type"), belsMap);
			for (Element belEl : cellEl.getChildren("bel")) {
				Element idEl = belEl.getChild("id");
				PrimitiveType type = PrimitiveType.valueOf(idEl.getChildText("primitive_type"));
				String name = idEl.getChildText("name");
				BelId id = new BelId(type, name);
				double cost = Double.parseDouble(belEl.getChildText("cost"));
				belsMap.put(id, doublePool.add(cost));
			}
		}
	}

	@Override
	public void initCluster(Cluster<?, ?> cluster) {
		assert numCommits == 0;
		assert !clusterInitialized;

		this.cluster = cluster;

		// acts as the tie breaker, choose a less complex block
		int maxNumPins = 0;
		for (Bel bel : cluster.getTemplate().getBels()) {
			maxNumPins = Integer.max(maxNumPins , getNumPinsOnBel(bel));
		}

		reserveBelCostMap = new StackedHashMap<>();
		for (Bel bel : cluster.getTemplate().getBels()) {
			reserveBelCostMap.put(bel, 0.0);
		}

		pqStack = new ArrayDeque<>();
		pq = null;
		clusterInitialized = true;
	}

	private int getNumPinsOnBel(Bel bel) {
		return bel.getSources().size() + bel.getSinks().size();
	}

	@Override
	public void initCell(PackCell cell, Collection<Bel> forcedAnchors) {
		assert clusterInitialized;
		assert !cellInitialized;

		pq = new PriorityQueue<>();
		List<Bel> candidateRoots = getPossibleCellRoots(cluster, cell, forcedAnchors);
		for (Bel candidateRoot : candidateRoots) {
			double cost = calcCost(cell, cluster, candidateRoot);
			if (cost < Double.MAX_VALUE)
				pq.add(new BelCandidate(candidateRoot, cost));
		}

		cellInitialized = true;
	}

	private List<Bel> getPossibleCellRoots(
			Cluster<?, ?> cluster, PackCell cell, Collection<Bel> bels
	) {
		Collection<Bel> candidates = cell.getPossibleAnchors(cluster.getTemplate());
		if (bels != null)
			candidates.retainAll(bels);

		return new ArrayList<>(candidates);
	}

	private double calcCost(PackCell cell, Cluster<?, ?> cluster, Bel anchor) {
		double cost = 0.0;
		List<Bel> bels = cell.getRequiredBels(anchor);
		for (Bel bel : bels) {
			if (cluster.isBelOccupied(bel))
				return Double.MAX_VALUE;

			cost += getBaseCostOfBel(cell, bel) + getReserveCostOfBel(bel) +
					getRoutingCostOfBel(cluster, cell, bel);
		}
		return cost;
	}

	private double getBaseCostOfBel(PackCell cell, Bel bel) {
		Map<BelId, Double> belMap = baseBelCostMap.get(cell.getLibCell().getName());
		return belMap.get(bel.getId());
	}

	private double getReserveCostOfBel(Bel bel) {
		return reserveBelCostMap.get(bel);
	}

	private double getRoutingCostOfBel(Cluster<?, ?> cluster, Cell cell, Bel bel) {
		double cost = 0.0;
		// Iterate over every connection on this cell
		for (CellPin pin : cell.getPins()) {
			if (!pin.isConnectedToNet())
				continue;
			boolean isInpin = pin.isInpin();

			// test if there is a direct connection
			boolean mustLeaveCluster = false;
			CellNet net = pin.getNet();
			if (net.isClkNet())
				continue;
			for (CellPin connPin : net.getPins()) {
				if (connPin == pin || (isInpin && connPin.isInpin()))
					continue;
				PackCell connCell = (PackCell) connPin.getCell();
				assert cluster.hasCell(connCell) ? connCell.getCluster() == cluster : true;
				if (connCell.getCluster() == cluster) {
					Collection<ClusterConnection> conns = getConnections(cluster, pin, bel, connPin);
					// choose the best connection
					double connCost = Double.MAX_VALUE;
					for (ClusterConnection cc : conns) {
						double tmpCost = calcConnectionCost(cc);
						if (tmpCost < connCost)
							connCost = tmpCost;
					}
					if (connCost == Double.MAX_VALUE)
						return Double.MAX_VALUE;
					cost += connCost;
				} else {
					if (connCell.isValid()) {
						mustLeaveCluster = true;
					}
				}
			}

			if (mustLeaveCluster)
				cost += MUST_LEAVE_CLUSTER_COST;

		}
		return cost;
	}

	private Collection<ClusterConnection> getConnections(
			Cluster<?, ?> cluster, CellPin candidatePin, Bel loc, CellPin placedPin
	) {
		ClusterTemplate<?> template = cluster.getTemplate();
		PackCell placedCell = (PackCell) placedPin.getCell();
		Bel placedBel = placedCell.getLocationInCluster();
		if (placedPin.isOutpin()) {
			BelPin sourcePin = getBelPinOfCellPin(placedPin, placedBel);
			Collection<BelPin> sinkPins = getBelPinsOfCellPin(candidatePin, loc);

			return template.getSinksOfSource(sourcePin).stream()
					.filter(cc -> sinkPins.contains(cc.getPin()))
					.collect(Collectors.toList());
		} else {
			Collection<BelPin> sinkPins = getBelPinsOfCellPin(placedPin, placedBel);
			BelPin sourcePin = getBelPinOfCellPin(candidatePin, loc);

			return template.getSinksOfSource(sourcePin).stream()
					.filter(cc -> sinkPins.contains(cc.getPin()))
					.collect(Collectors.toList());
		}
	}

	private Collection<BelPin> getBelPinsOfCellPin(CellPin pin, Bel bel) {
		return pin.getPossibleBelPinNames(bel.getId()).stream()
				.map(bel::getBelPin)
				.collect(Collectors.toList());
	}

	private BelPin getBelPinOfCellPin(CellPin pin, Bel bel) {
		List<String> pinNames = pin.getPossibleBelPinNames(bel.getId());
		assert pinNames.size() == 1;
		String pinName = pinNames.get(0);
		return bel.getBelPin(pinName);
	}

	private double calcConnectionCost(ClusterConnection cc) {
		double cost = -1.0;
		if (!cc.isWithinSite())
			cost *= LEAVE_SITE_PENALTY;

		int distance = Integer.min(cc.getDistance(), 9);
		cost *= PIP_PENALTIES[distance];
		return cost;
	}

	@Override
	public Bel nextBel() {
		assert cellInitialized;

		if (pq.isEmpty())
			return null;
		return pq.poll().bel;
	}

	@Override
	public void commitBels(Collection<Bel> bels) {
		assert cellInitialized;

		checkpoint();

		ClusterTemplate<?> template = cluster.getTemplate();

		for (Bel bel : bels) {
			updateSinksOfSourcesCosts(bel, template);
			updateSourceOfSinksCosts(bel, template);
		}

		pq = null;

		cellInitialized = false;
		numCommits++;
	}

	private void updateSinksOfSourcesCosts(Bel bel, ClusterTemplate<?> template) {
		for (BelPin pin : bel.getSources()) {
			for (ClusterConnection cc : template.getSinksOfSource(pin)) {
				if (cc.isWithinSite())
					reserveBelCostMap.compute(
							cc.getPin().getBel(), (k, v) -> v + RESERVE_BEL_PENALTY);
			}
		}
	}

	private void updateSourceOfSinksCosts(Bel bel, ClusterTemplate<?> template) {
		for (BelPin pin : bel.getSinks()) {
			template.getSourcesOfSink(pin).stream()
					.filter(ClusterConnection::isWithinSite)
					.forEach(cc -> reserveBelCostMap.compute(
							cc.getPin().getBel(), (k, v) -> v + RESERVE_BEL_PENALTY));
		}
	}

	private void checkpoint() {
		assert pq != null;
		assert pqStack != null;
		assert reserveBelCostMap != null;

		PriorityQueue<BelCandidate> pqCopy = new PriorityQueue<>();
		pq.forEach(bc -> pqCopy.add(new BelCandidate(bc)));
		pqStack.push(pqCopy);

		reserveBelCostMap.checkPoint();
	}

	@Override
	public void cleanupCluster() {
		pq = null;
		pqStack = null;
		reserveBelCostMap = null;

		cellInitialized = false;
		clusterInitialized = false;
		numCommits = 0;

		cluster = null;
	}

	@Override
	public void revertToLastCommit() {
		assert cellInitialized;
		assert numCommits >= 0;

		pq = null;

		cellInitialized = false;
	}

	@Override
	public void rollBackLastCommit() {
		assert !cellInitialized;
		assert numCommits > 0;

		pq = pqStack.pop();
		reserveBelCostMap.rollBack();

		cellInitialized = true;
		numCommits--;
	}

	private static final class BelCandidate implements Comparable<BelCandidate> {
		public Bel bel;
		public double cost;

		public BelCandidate(Bel bel, double cost) {
			this.bel = bel;
			this.cost = cost;
		}

		public BelCandidate(BelCandidate o) {
			this.bel = o.bel;
			this.cost = o.cost;
		}

		public Bel getBel() {
			return bel;
		}

		public double getCost() {
			return cost;
		}

		@Override
		public int compareTo(BelCandidate o) {
			return Double.compare(cost, o.getCost());
		}
	}
}
