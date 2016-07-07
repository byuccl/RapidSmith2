package edu.byu.ece.rapidSmith.cad.clusters.router;

import edu.byu.ece.rapidSmith.cad.clusters.Cluster;
import edu.byu.ece.rapidSmith.cad.clusters.PackCell;
import edu.byu.ece.rapidSmith.cad.packer.AAPack.Routability;
import edu.byu.ece.rapidSmith.design.subsite.CellNet;
import edu.byu.ece.rapidSmith.design.subsite.CellPin;
import edu.byu.ece.rapidSmith.device.Device;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.Stack;

/**
 *
 */
public class PathFinderRoutabilityChecker implements RoutabilityChecker {
	IncrementalClusterRouter router;
	Set<CellNet> changedNets;
	Stack<Set<CellNet>> changedNetsStack = new Stack<>();

	public PathFinderRoutabilityChecker(Cluster<?, ?> cluster, Device device) {
		this.router = new IncrementalClusterRouter(cluster, device);
		changedNets = new HashSet<>();
	}

	@Override
	public RoutabilityResult check(Collection<PackCell> changed) {
		for (PackCell cell : changed) {
			for (CellPin pin : cell.getPins()) {
				if (pin.isConnectedToNet()) {
					CellNet net = pin.getNet();
					router.invalidateNet(net);
					changedNets.add(net);
				}
			}
		}

		Routability routeStatus = router.routeCluster();
		return new RoutabilityResult(routeStatus, router.conditionals);
	}

	@Override
	public void checkpoint() {
		changedNetsStack.push(changedNets);
		changedNets = new HashSet<>();
	}

	@Override
	public void rollback() {
		for (CellNet net : changedNets) {
			router.invalidateNet(net);
		}
		changedNets = changedNetsStack.pop();
	}
}
