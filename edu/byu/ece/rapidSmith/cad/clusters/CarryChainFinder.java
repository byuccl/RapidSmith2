package edu.byu.ece.rapidSmith.cad.clusters;

import edu.byu.ece.rapidSmith.cad.clusters.CarryChain;
import edu.byu.ece.rapidSmith.cad.clusters.ClusterDevice;
import edu.byu.ece.rapidSmith.cad.clusters.ClusterTemplate;
import edu.byu.ece.rapidSmith.cad.clusters.DirectConnection;
import edu.byu.ece.rapidSmith.design.subsite.Cell;
import edu.byu.ece.rapidSmith.design.subsite.CellNet;
import edu.byu.ece.rapidSmith.design.subsite.CellPin;
import edu.byu.ece.rapidSmith.device.*;

import java.util.*;

/**
 *
 */
public class CarryChainFinder {
	public void findCarryChains(ClusterDevice<?> device, Collection<CellNet> nets) {
		for (CellNet net : nets) {
			if (isSourced(net)) {
				CellPin sourcePin = net.getSourcePin();
				List<DirectConnection> dcs = getDirectSinks(device, sourcePin);

				for (CellPin sinkPin : net.getSinkPins()) {
					for (DirectConnection dc : dcs) {
						BelPinTemplate sinkPinTemplate = dc.endPin;
						List<String> pinNames = sinkPin.getPossibleBelPinNames(
								sinkPinTemplate.getId());
						if (pinNames.contains(sinkPinTemplate.getName())) {
							CarryChain.connect(sourcePin, sinkPin);
							break;
						}
					}
				}
			}
		}
	}

	private static boolean isSourced(CellNet net) {
		return net.getSourcePin() != null;
	}

	private static List<DirectConnection> getDirectSinks(
			ClusterDevice<?> device, CellPin sourcePin
	) {
		List<DirectConnection> retDcsList = new ArrayList<>();

		Cell cell = sourcePin.getCell();
		Set<BelId> possibleBels = new HashSet<>(cell.getPossibleAnchors());
		for (ClusterTemplate<?> template : device.getTemplates()) {
			for (DirectConnection dc : template.getDirectSinksOfCluster()) {
				BelPin sourceBelPin = dc.clusterPin;
				Bel sourceBel = sourceBelPin.getBel();
				if (possibleBels.contains(sourceBel.getId())) {
					Collection<BelPin> possiblePins = sourcePin.getPossibleBelPins(sourceBel);
					if (possiblePins.contains(sourceBelPin)) {
						retDcsList.add(dc);
					}
				}
			}
		}
		return retDcsList;
	}
}
