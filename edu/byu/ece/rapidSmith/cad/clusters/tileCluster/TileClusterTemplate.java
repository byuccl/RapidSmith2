package edu.byu.ece.rapidSmith.cad.clusters.tileCluster;

import edu.byu.ece.rapidSmith.cad.clusters.ClusterConnection;
import edu.byu.ece.rapidSmith.cad.clusters.ClusterTemplate;
import edu.byu.ece.rapidSmith.cad.clusters.DirectConnection;
import edu.byu.ece.rapidSmith.cad.clusters.PinGroup;
import edu.byu.ece.rapidSmith.design.subsite.Connection;
import edu.byu.ece.rapidSmith.design.subsite.SiteWire;
import edu.byu.ece.rapidSmith.design.subsite.TileWire;
import edu.byu.ece.rapidSmith.design.subsite.Wire;
import edu.byu.ece.rapidSmith.device.*;

import java.io.Serializable;
import java.util.*;

/**
 *
 */
public class TileClusterTemplate implements ClusterTemplate<TileClusterType>, Serializable {
	private TileClusterDevice device;
	private TileClusterType type;
	private List<Tile> tiles;
	private List<Bel> bels;

	private Set<BelPin> vccSources;
	private Set<BelPin> gndSources;

	private List<DirectConnection> directSourcesOfCluster = new ArrayList<>();
	private List<DirectConnection> directSinksOfCluster = new ArrayList<>();
	private Map<BelPin, List<ClusterConnection>> sourcePins = new HashMap<>();
	private Map<BelPin, List<ClusterConnection>> sinkPins = new HashMap<>();
	private Map<BelPin, PinGroup> pinGroups;

	private List<Wire> outputs;
	private Map<BelPin, List<Wire>> inputs;

	public void setType(TileClusterType type) {
		this.type = type;
	}

	public List<Tile> getTiles() {
		return tiles;
	}

	public void setTiles(List<Tile> tiles) {
		this.tiles = tiles;
	}

	public void setBels(List<Bel> bels) {
		this.bels = bels;
	}

	public Bel getAnchor() {
		return bels.get(0);
	}

	public void setVccSources(Set<BelPin> vccSources) {
		this.vccSources = vccSources;
	}

	public void setGndSources(Set<BelPin> gndSources) {
		this.gndSources = gndSources;
	}

	public void setDirectSourcesOfCluster(List<DirectConnection> directSourcesOfCluster) {
		this.directSourcesOfCluster = directSourcesOfCluster;
	}

	public void setDirectSinksOfCluster(List<DirectConnection> directSinksOfCluster) {
		this.directSinksOfCluster = directSinksOfCluster;
	}

	public void setSourcePins(Map<BelPin, List<ClusterConnection>> sourcePins) {
		this.sourcePins = sourcePins;
	}

	public void setSinkPins(Map<BelPin, List<ClusterConnection>> sinkPins) {
		this.sinkPins = sinkPins;
	}

	@Override
	public TileClusterType getType() {
		return type;
	}

	@Override
	public int getNumBelsAvailable() {
		return bels.size();
	}

	@Override
	public Set<BelPin> getVccSources() {
		return vccSources;
	}

	@Override
	public Set<BelPin> getGndSources() {
		return gndSources;
	}

	@Override
	public List<DirectConnection> getDirectSourcesOfCluster() {
		return directSourcesOfCluster;
	}

	@Override
	public List<DirectConnection> getDirectSinksOfCluster() {
		return directSinksOfCluster;
	}

	@Override
	public Collection<ClusterConnection> getSinksOfSource(BelPin belPin) {
		return sourcePins.get(belPin);
	}

	@Override
	public Collection<ClusterConnection> getSourcesOfSink(BelPin BelPin) {
		return sinkPins.get(BelPin);
	}

	@Override
	public Collection<Bel> getBels() {
		return bels;
	}

	public Bel getRelocatedBel(Bel curBel, Bel curAnchor, Bel newAnchor) {
		assert newAnchor.getSite().getIndex() == getAnchor().getSite().getIndex();
		assert newAnchor.getId().equals(getAnchor().getId());

		assert curAnchor.getSite().getIndex() == getAnchor().getSite().getIndex();
		assert curAnchor.getId().equals(getAnchor().getId());

		Tile newTile = relocateTile(curBel.getSite().getTile(), curAnchor, newAnchor);
		PrimitiveSite newSite = newTile.getPrimitiveSite(curBel.getSite().getIndex());
		return newSite.getBel(curBel.getId());
	}

	public BelPin getRelocatedBelPin(BelPin belPin, Bel curAnchor, Bel newAnchor) {
		assert newAnchor.getSite().getIndex() == getAnchor().getSite().getIndex();
		assert newAnchor.getId().equals(getAnchor().getId());

		assert curAnchor.getSite().getIndex() == getAnchor().getSite().getIndex();
		assert curAnchor.getId().equals(getAnchor().getId());

		Bel curBel = belPin.getBel();
		Tile rTile = relocateTile(curBel.getSite().getTile(), curAnchor, newAnchor);
		PrimitiveSite rSite = rTile.getPrimitiveSite(curBel.getSite().getIndex());
		Bel rBel = rSite.getBel(curBel.getId());
		return rBel.getBelPin(belPin.getName());
	}

	public Wire getRelocatedWire(Wire oldWire, Bel curAnchor, Bel newAnchor) {
		assert newAnchor.getSite().getIndex() == getAnchor().getSite().getIndex();
		assert newAnchor.getId().equals(getAnchor().getId());

		assert curAnchor.getSite().getIndex() == getAnchor().getSite().getIndex();
		assert curAnchor.getId().equals(getAnchor().getId());

		if (oldWire instanceof SiteWire) {
			SiteWire oldSiteWire = (SiteWire) oldWire;
			PrimitiveSite oldSite = oldSiteWire.getSite();
			Tile newTile = relocateTile(oldSite.getTile(), curAnchor, newAnchor);
			PrimitiveSite newSite = newTile.getPrimitiveSite(oldSite.getIndex());
			return new SiteWire(newSite, oldSiteWire.getWireEnum());
		} else if (oldWire instanceof TileWire) {
			throw new AssertionError("This doesn't support tile wires");
		} else {
			throw new AssertionError("Unknown wire class");
		}
	}

	public Connection getRelocatedConnection(Wire sourceWire, Connection conn, Bel curAnchor, Bel newAnchor) {
		assert newAnchor.getSite().getIndex() == getAnchor().getSite().getIndex();
		assert newAnchor.getId().equals(getAnchor().getId());

		assert curAnchor.getSite().getIndex() == getAnchor().getSite().getIndex();
		assert curAnchor.getId().equals(getAnchor().getId());

		if (conn.isWireConnection()) {
			for (Connection c : sourceWire.getWireConnections()) {
				if (c.getSinkWire().getWireEnum() == conn.getSinkWire().getWireEnum())
					return c;
			}
		} else if (conn.isPinConnection()) {
			for (Connection c : sourceWire.getPinConnections()) {
				if (c.getSitePin().getName().equals(conn.getSitePin().getName()))
					return c;
			}
		} else if (conn.isTerminal()) {
			for (Connection c : sourceWire.getTerminals()) {
				if (c.getBelPin().getName().equals(conn.getBelPin().getName()))
					return c;
			}
		} else {
			throw new AssertionError("Illegal connection type");
		}

		throw new AssertionError("Failed to find similar connection.");
	}

	private Offset getTileOffset(Bel newAnchor, Bel curAnchor) {
		Tile cAnchorTile = curAnchor.getSite().getTile();
		Tile nAnchorTile = newAnchor.getSite().getTile();
		Offset offset = new Offset();
		offset.row = nAnchorTile.getRow() - cAnchorTile.getRow();
		offset.col = nAnchorTile.getColumn() - cAnchorTile.getColumn();
		return offset;
	}

	public void setDevice(TileClusterDevice device) {
		this.device = device;
	}

	private static class Offset {
		public int row;
		public int col;
	}

	private Tile relocateTile(Tile oldTile, Bel curAnchor, Bel newAnchor) {
		Offset offset = getTileOffset(newAnchor, curAnchor);
		Device device = newAnchor.getSite().getTile().getDevice();
		return device.getTile(
				oldTile.getRow() + offset.row,
				oldTile.getColumn() + offset.col);
	}

	public void setOutputs(List<Wire> outputs) {
		this.outputs = outputs;
	}

	@Override
	public List<Wire> getOutputs() {
		return outputs;
	}

	public void setInputs(Map<BelPin, List<Wire>> inputs) {
		this.inputs = inputs;
	}

	@Override
	public List<Wire> getInputsOfSink(BelPin sinkPin) {
		return inputs.get(sinkPin);
	}

	@Override
	public Set<Wire> getInputs() {
		Set<Wire> inputs = new HashSet<>();
		for (List<Wire> wires : this.inputs.values()) {
			inputs.addAll(wires);
		}
		return inputs;
	}

	@Override
	public PinGroup getPinGroup(BelPin pin) {
		return pinGroups.get(pin);
	}

	public void setPinGroups(Map<BelPin, PinGroup> groups) {
		this.pinGroups = groups;
	}

	@Override
	public Collection<PinGroup> getPinGroups() {
		return new ArrayList<>(pinGroups.values());
	}

	@Override
	public Wire getWire(PrimitiveSite site, String wireName) {
		WireEnumerator we = device.getWireEnumerator();
		Integer wireEnum = we.getWireEnum(wireName);
		return new SiteWire(site, wireEnum);
	}
}
