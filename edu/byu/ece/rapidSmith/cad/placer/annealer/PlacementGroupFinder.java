package edu.byu.ece.rapidSmith.cad.placer.annealer;

import edu.byu.ece.rapidSmith.cad.clusters.Cluster;
import edu.byu.ece.rapidSmith.cad.clusters.ClusterSite;
import edu.byu.ece.rapidSmith.cad.clusters.ClusterType;
import edu.byu.ece.rapidSmith.cad.clusters.PackCell;
import edu.byu.ece.rapidSmith.design.ClusterDesign;
import edu.byu.ece.rapidSmith.design.subsite.*;
import edu.byu.ece.rapidSmith.device.*;

import java.awt.*;
import java.util.*;
import java.util.List;

/**
 * This class contains routines for finding placement groups within a design based
 * on the topology.  This should create only multiple instance groups.
 *
 * @author wirthlin
 */
public class PlacementGroupFinder<CTYPE extends ClusterType, CSITE extends ClusterSite> {
	public Set<PlacementGroup<CTYPE, CSITE>> findPlacementGroups(
			ClusterDesign<CTYPE, CSITE> design
	) {
		Collection<Shape> shapes = findShapes(design);
		Set<PlacementGroup<CTYPE, CSITE>> groups = new HashSet<>();
		for (Shape shape : shapes) {
			groups.add(constructPlacementGroup(shape));
		}
		return groups;
	}

	private Collection<Shape> findShapes(ClusterDesign<CTYPE, CSITE> design) {
		Map<Cluster<CTYPE, CSITE>, Shape> shapes = new HashMap<>();

		for (Cluster<CTYPE, CSITE> cluster : design.getClusters()) {
			if (!cluster.isPlaceable())
				continue;
			for (Cell cell : cluster.getCells()) {
				for (CellPin sourcePin : cell.getOutputPins()) {
					CellNet net = sourcePin.getNet();
					if (net == null)
						continue;
					for (CellPin sinkPin : net.getSinkPins()) {
						if (isDirectConnection(cluster, sourcePin, sinkPin)) {
							PackCell sinkCell = (PackCell) sinkPin.getCell();
							Cluster<CTYPE, CSITE> sinkCluster = sinkCell.getCluster();
							mergeIntoShape(shapes, cluster, sinkCluster);
						}
					}
				}
			}
		}

		return new HashSet<>(shapes.values());
	}

	private void mergeIntoShape(
			Map<Cluster<CTYPE, CSITE>, Shape> shapes, Cluster<CTYPE, CSITE> sourceCluster,
			Cluster<CTYPE, CSITE> sinkCluster
	) {
		// WARNING: assumes that all shapes are vertical!
		Shape sourceShape = shapes.get(sourceCluster);
		Shape sinkShape = shapes.get(sinkCluster);

		if (sourceShape == null & sinkShape == null) {
			Shape shape = new Shape();
			shape.clusters.put(sourceCluster, new Point(0, 0));
			shape.clusters.put(sinkCluster, new Point(0, 1));
			shape.anchor = sourceCluster;
			shapes.put(sourceCluster, shape);
			shapes.put(sinkCluster, shape);
		} else if (sourceShape == null) {
			for (Point p : sinkShape.clusters.values())
				p.y = p.y + 1;
			sinkShape.clusters.put(sourceCluster, new Point(0, 0));
			sinkShape.anchor = sourceCluster;
			shapes.put(sourceCluster, sinkShape);
		} else if (sinkShape == null) {
			Point p = sourceShape.clusters.get(sourceCluster);
			sourceShape.clusters.put(sinkCluster, new Point(p.x, p.y + 1));
			shapes.put(sinkCluster, sourceShape);
		} else if (sourceShape != sinkShape) {
			Point p = sourceShape.clusters.get(sourceCluster);
			for (Cluster<CTYPE, CSITE> cluster : sinkShape.clusters.keySet()) {
				Point sp = sinkShape.clusters.get(cluster);
				sourceShape.clusters.put(cluster, new Point(p.x + sp.x, p.y + sp.y + 1));
				shapes.put(cluster, sourceShape);
			}
		}
	}

	private boolean isDirectConnection(
			Cluster<CTYPE, CSITE> cluster, CellPin sourcePin, CellPin sinkPin
	) {
		assert cluster.getPinMapping(sourcePin) != null;

		PackCell sinkCell = (PackCell) sinkPin.getCell();
		Cluster<?, ?> sinkCluster = sinkCell.getCluster();
		if (sinkCluster == cluster || !sinkCluster.isPlaceable())
			return false;

		BelPin sourceBelPin = cluster.getPinMapping(sourcePin);
		if (sourceBelPin == null)
			return false;
		if (!sourceBelPin.drivesGeneralFabric())
			return true;
		return getPossibleSinkPins(sinkPin).stream()
				.noneMatch(BelPinTemplate::isDrivenByGeneralFabric);
	}

	private List<BelPinTemplate> getPossibleSinkPins(CellPin sinkPin) {
		PackCell sinkCell = (PackCell) sinkPin.getCell();
		Cluster<?, ?> sinkCluster = sinkCell.getCluster();

		List<BelPinTemplate> possibleSinkPins = new ArrayList<>();
		Bel sinkBel = sinkCell.getLocationInCluster();
		List<String> pinNames = sinkPin.getPossibleBelPinNames(sinkBel.getId());
		pinNames.forEach(n -> {
			BelPin sinkBelPin = sinkBel.getBelPin(n);
			assert sinkBelPin != null;
			possibleSinkPins.add(sinkBelPin.getTemplate());
		});
		return possibleSinkPins;
	}

	private PlacementGroup<CTYPE, CSITE> constructPlacementGroup(Shape shape) {
		MultipleClusterPlacementGroup<CTYPE, CSITE> group = new MultipleClusterPlacementGroup<>();

		// Determine the anchor cluster of the shape.
		group.anchor = shape.anchor;

		// Determine the shape of the group. This involves searching through the
		// list and finding the largest x value and the largest y value.
		group.groupDimension = new Point();
		for (Cluster<CTYPE, CSITE> member : shape.clusters.keySet()) {
			Point offset = shape.clusters.get(member);
			if (offset.x > group.groupDimension.x)
				group.groupDimension.x = offset.x;
			if (offset.y > group.groupDimension.y)
				group.groupDimension.y = offset.y;
		}
		// the x and y indicate the largest x and y locations
		// within the group. Since the dimension has size, we need
		// to add one since the grid starts at 0. For example,
		// if the largest x = 0 and the largest y = 5, the
		// dimension will be 1,6
		group.groupDimension.x += 1;
		group.groupDimension.y += 1;

		// Add all of the clusters to the group
		//System.out.println("Group "+shape.getName());
		for (Cluster<CTYPE, CSITE> member : shape.clusters.keySet()) {
			Point offset = shape.clusters.get(member);
			group.clusterOffsetMap.put(member, offset);
		}

		// Determine the placeable sites of this shape based on its type.
		group.groupType = group.anchor.getType();
		boolean groupMembersHaveSameType = true;
		for (Cluster<CTYPE, CSITE> i : shape.clusters.keySet()) {
			if (i.getType() != group.groupType) {
				groupMembersHaveSameType = false;
				System.out.println("Warning: mixing types where anchor is of type "+ group.getAnchor().getType() +
						" and member is of type "+i.getType());
				if (group.groupType.toString().contains("CLBLL") &&
						i.getType().toString().contains("CLBLM")) {
					group.groupType = i.getType();
				}
				break;
			}
		}

		// Set type coordinates
//		if (!groupMembersHaveSameType) {
//			group.groupType = null; // TODO what should the type be
//		}
//
		return group;
	}

	class Shape {
		Map<Cluster<CTYPE, CSITE>, Point> clusters = new HashMap<>();
		Cluster<CTYPE, CSITE> anchor;
	}
}

