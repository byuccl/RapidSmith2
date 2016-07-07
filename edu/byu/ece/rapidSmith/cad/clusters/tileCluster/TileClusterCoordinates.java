package edu.byu.ece.rapidSmith.cad.clusters.tileCluster;

import edu.byu.ece.rapidSmith.cad.placer.annealer.TypeSiteCoordinates;

import java.awt.*;
import java.util.*;
import java.util.List;

/**
 *
 */
public class TileClusterCoordinates extends TypeSiteCoordinates<TileClusterType, TileClusterSite> {
	/**
	 * A two dimensional representation of the coordinate system. This is
	 * handy for getting coordinates based on the x,y locations.
	 */
	protected TileClusterSite[][] coordinates;

	TileClusterTemplate template;
	List<TileClusterSite> locations;
	private final Map<Integer, Integer> xRelocationMap;
	private final Map<Integer, Integer> yRelocationMap;

	public TileClusterCoordinates(
			TileClusterTemplate template, List<TileClusterSite> locations
	) {
		super(template.getType());
		this.template = template;
		this.locations = locations;

		// Create a HashSet of these sites and keep track of the maximum height and width
		Set<Integer> xLocations = new HashSet<>();
		Set<Integer> yLocations = new HashSet<>();
		for (TileClusterSite tile : locations) {
			Point loc = tile.getLocation();
			xLocations.add(loc.x);
			yLocations.add(loc.y);
		}

		List<Integer> sortedX = new ArrayList<>(xLocations);
		Collections.sort(sortedX);
		List<Integer> sortedY = new ArrayList<>(yLocations);
		Collections.sort(sortedY);

		xRelocationMap = new HashMap<>();
		for (int i = 0; i < sortedX.size(); i++)
			xRelocationMap.put(sortedX.get(i), i);
		yRelocationMap = new HashMap<>();
		for (int i = 0; i < sortedY.size(); i++)
			yRelocationMap.put(sortedY.get(i), i);

		// Populate the coordinate system with the sites
		coordinateDimensions = new Point(xLocations.size(), yLocations.size());
		coordinates = new TileClusterSite[coordinateDimensions.x][coordinateDimensions.y];
		for (TileClusterSite tile : locations) {
			Point p = getSiteCoordinates(tile);
			coordinates[p.x][p.y] = tile;
		}
	}

	public List<TileClusterSite> getValidSites() {
		return locations;
	}

	@Override
	public TileClusterSite getSite(int column, int row) {
		if (column < 0 || column >= coordinateDimensions.x) {
			//System.out.println("Warning: bad column "+column);
			return null;
		}
		TileClusterSite[] columnSites = coordinates[column];
		if (columnSites != null) {
			if (row < 0 || row >= coordinateDimensions.y)
				return null;
			return columnSites[row];
		}
		System.out.println("Bad Site "+column+","+row+" type "+type);
		return null;
	}

	@Override
	public TileClusterSite getSiteOffset(TileClusterSite site, int xOffset, int yOffset) {
		Point p = getSiteCoordinates(site);
		if (p == null)
			return null;
		int newX = p.x + xOffset;
		int newY = p.y + yOffset;

		if (newX >= 0 && newX < coordinateDimensions.x && newY >=0 && newY < coordinateDimensions.y)
			return getSite(newX, newY);

		// Bad coordinates - return null
		return null;
	}

	@Override
	public Point getSiteOffset(TileClusterSite aSite, TileClusterSite bSite) {
		Point bLoc = bSite.getLocation();
		Point aLoc = aSite.getLocation();
		int xOffset = bLoc.x - aLoc.x;
		int yOffset = bLoc.y - aLoc.y;
		return new Point(xOffset, yOffset);
	}

	@Override
	public Point getSiteCoordinates(TileClusterSite site) {
		Point loc = site.getLocation();
		Integer x = xRelocationMap.get(loc.x);
		Integer y = yRelocationMap.get(loc.y);
		if (x == null || y == null) {
			return null;
		}
		return new Point(x, y);
	}
}
