/*
* Copyright (c) 2016 Brigham Young University
*
* This file is part of the BYU RapidSmith Tools.
*
* BYU RapidSmith Tools is free software: you may redistribute it
* and/or modify it under the terms of the GNU General Public License
* as published by the Free Software Foundation, either version 3 of
* the License, or (at your option) any later version.
*
* BYU RapidSmith Tools is distributed in the hope that it will be
* useful, but WITHOUT ANY WARRANTY; without even the implied warranty
* of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
* GNU General Public License for more details.
*
* A copy of the GNU General Public License is included with the BYU
* RapidSmith Tools. It can be found at doc/LICENSE.GPL3.TXT. You may
* also get a copy of the license at <http://www.gnu.org/licenses/>.
*/

package design.subsite;
import edu.byu.ece.rapidSmith.design.subsite.RouteTree;
import edu.byu.ece.rapidSmith.device.*;
import edu.byu.ece.rapidSmith.util.Exceptions.DesignAssemblyException;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * jUnit test for the RouteTree class in RapidSmith2
 * @author Mark Crossen
 */
class RouteTreeTest {

	/** A three-level structure will be tested. This is level 1 of 3 */
	private RouteTree root;
	/** A three-level structure will be tested. This is level 2 of 3 */
	private RouteTree branch;
	/** A three-level structure will be tested. This is level 3 of 3 */
	private RouteTree leaf;

	private static Device device;

	@BeforeAll
	static void beforeAll() {
		makeDummyDevice();
	}

	private static void makeDummyDevice() {

		FamilyType dummyFamilyType = FamilyType.valueOf("DUMMY_FAMILY");
		SiteType dummySiteType = SiteType.valueOf(dummyFamilyType, "DUMMY_SITE");

		device = new Device();
		device.setFamily(dummyFamilyType);
		device.setPartName("rsdummy01");
		device.setWireEnumerator(makeWireEnumerator());
		SiteTemplate dummySiteTemplate = makeSiteTemplate(dummySiteType);
		Map<SiteType, SiteTemplate> siteTemplates = new HashMap<>();
		siteTemplates.put(dummySiteType, dummySiteTemplate);
		device.setSiteTemplates(siteTemplates);
		device.setTileArray(new Tile[][] {{new Tile()}});
		Tile dummyTile = device.getTile(0);
		dummyTile.setDevice(device);
		makeDummyTile(dummyTile, dummyFamilyType, dummySiteType);
	}

	@NotNull
	private static SiteTemplate makeSiteTemplate(SiteType dummySiteType) {
		SiteTemplate dummySiteTemplate = new SiteTemplate();
		dummySiteTemplate.setType(dummySiteType);

		HashMap<String, SitePinTemplate> sinks = new HashMap<>();
		SitePinTemplate dummySink = new SitePinTemplate("DUMMY_SINK", dummySiteType);
		dummySink.setDirection(PinDirection.IN);
		dummySink.setInternalWire(4);
		sinks.put("DUMMY_SINK", dummySink);
		dummySiteTemplate.setSinks(sinks);

		HashMap<String, SitePinTemplate> sources = new HashMap<>();
		SitePinTemplate dummySource = new SitePinTemplate("DUMMY_SOURCE", dummySiteType);
		dummySource.setDirection(PinDirection.OUT);
		dummySource.setInternalWire(5);
		sources.put("DUMMY_SOURCE", dummySource);
		dummySiteTemplate.setSources(sources);
		return dummySiteTemplate;
	}


	private static WireEnumerator makeWireEnumerator() {
		String[] wires = new String[5];
		Map<String, Integer> wireMap = new HashMap<>(10);
		for (int i = 0; i < wires.length; i++) {
			String wireName = "dummy_wire_" + i;
			wires[i] = wireName;
			wireMap.put(wireName, i);
		}
		WireEnumerator we = new WireEnumerator();
		we.setWireMap(wireMap);
		we.setWires(wires);
		return we;
	}

	private static void makeDummyTile(
		Tile dummyTile, FamilyType dummyFamilyType, SiteType dummySiteType
	) {
		dummyTile.setName("dummy_tile");
		dummyTile.setType(TileType.valueOf(dummyFamilyType, "DUMMY_TILE"));
		dummyTile.setRow(0);
		dummyTile.setColumn(0);
		Site dummySite = new Site();
		dummySite.setTile(dummyTile);
		dummyTile.setSites(new Site[] {dummySite});
		makeDummySite(dummySite, dummySiteType);
	}

	private static void makeDummySite(Site dummySite, SiteType dummySiteType) {
		dummySite.setName("dummy_site");
		dummySite.setIndex(0);
		dummySite.setType(dummySiteType);
		dummySite.setPossibleTypes(new SiteType[] {dummySiteType});

		SiteTemplate siteTemplate = device.getSiteTemplate(dummySiteType);
		SitePinTemplate dummySinkTemplate = siteTemplate.getSitePin("DUMMY_SINK");
		SitePinTemplate dummySourceTemplate = siteTemplate.getSitePin("DUMMY_SOURCE");

		Map<SiteType, Map<String, Integer>> externalWires = new HashMap<>();
		Map<String, Integer> eWires = new HashMap<>();
		eWires.put("DUMMY_SITE_PIN", 4);
		eWires.put("DUMMY_SOURCE_PIN", 5);
		externalWires.put(dummySiteType, eWires);
		dummySite.setExternalWires(externalWires);

		Map<SiteType, Map<Integer, SitePinTemplate>> e2pMap = new HashMap<>();
		Map<Integer, SitePinTemplate> e2pinMap = new HashMap<>();
		e2pinMap.put(4, dummySinkTemplate);
		e2pinMap.put(5, dummySourceTemplate);
		e2pMap.put(dummySiteType, e2pinMap);
		dummySite.setExternalWireToPinMap(e2pMap);
	}

	/**
	 * This method is ran between tests to repair and initialize the three tested RouteTrees.
	 */
	@BeforeEach
	void setUp() {
		// each RouteTree has a unique value so that they hash differently
		root = newDummyTree(); // unique value = 0
		branch = root.addConnection(newDummyConnection(root.getWire(), 1, false)); // unique value = 1
		leaf = branch.addConnection(newDummyConnection(root.getWire(), 2, true)); // unique value = 2
	}

	/**
	 * A helper function to create a new RouteTree.
	 *
	 * @return a new RouteTree containing a simple connection
	 */
	private RouteTree newDummyTree() {
		return new RouteTree(new TileWire(device.getTile(0), 0));
	}

	/**
	 * A helper function to create a simple connection
	 *
	 * @param wireEnum this can be any number. It is used to make each Connection have a different hashCode()
	 * @param isPip boolean to determine wether or not the Connection is a PIP
	 * @return the built connection
	 */
	private Connection newDummyConnection(Wire source, int wireEnum, boolean isPip) {
		WireConnection wc = new WireConnection(wireEnum, 0, 0, isPip);
		return new Connection.TileWireConnection((TileWire) source, wc);
	}

	/**
	 * test whether each of the three sample trees has a source tree.
	 * Because each sample tree is a child of another sample tree, only the first (root) tree shouldn't be sourced.
	 */
	@Test
	@DisplayName("test RouteTree method 'isSourced'")
	void testIsSourced() {
		// everything but the root tree should be sourced
		assertFalse(root.isSourced(), "the 'root' RouteTree should have no source tree.");
		assertTrue(branch.isSourced(), "the 'branch' RouteTree should have a source ('root')");
		assertTrue(leaf.isSourced(), "the 'leaf' RouteTree should have a source ('branch')");
	}

	/**
	 * When called, the getFirstSource() method should return the root tree in the overall RouteTree structure.
	 * For the three tested RouteTrees, this means that each one should return the 'root' RouteTree because each
	 * Tree is a child or grandchild of 'root'.
	 */
	@Test
	@DisplayName("test RouteTree method 'getFirstSource'")
	void testGetFirstSource() {
		// every RouteTree should return the root tree
		assertEquals(root, root.getFirstSource(), "the first source tree of 'root' should be itself");
		assertEquals(root, branch.getFirstSource(), "the first source tree of 'branch' should be 'root'");
		assertEquals(root, leaf.getFirstSource(), "the first source tree of 'leaf' should be 'root'");
	}

	/**
	 * A leaf RouteTree doesn't have any sink RouteTrees. In this test, only the 'leaf' RouteTree should return
	 * true because the other trees are parents and grandparents of 'leaf'. Remember, the order is:
	 * root -> branch -> leaf
	 * where the right side of -> is a sink (child) tree of the left side.
	 */
	@Test
	@DisplayName("test RouteTree method 'isLeaf'")
	void testIsLeaf() {
		// only the leaf RouteTree should return true
		assertFalse(root.isLeaf(), "the 'root' RouteTree shouldn't be a leaf node");
		assertFalse(branch.isLeaf(), "the 'branch' RouteTree shouldn't be a leaf node");
		assertTrue(leaf.isLeaf(), "the 'leaf' RouteTree should be a leaf node");
	}

	/**
	 * When removing a RouteTree from the structure, the orphaned RouteTree shouldn't contain a reference to the old
	 * tree, and the old tree shouldn't contain a reference to the orphaned RouteTree.
	 */
	@Test
	@DisplayName("test RouteTree method 'removeConnection'")
	void testRemoveConnection() {
		// remove the leaf connection and verify that the branch RouteTree is now a leaf
		branch.removeConnection(leaf.getConnection());
		assertTrue(branch.isLeaf(), "The 'branch' should be a leaf after removing the 'leaf' RouteTree");
		// verify that the orphaned RouteTree doesn't reference the main structure
		assertNull(leaf.getSourceTree(), "After removing the 'leaf', its source tree should be set to null");
	}

	/**
	 * the getAllPips() method should return a list of all PIP connections in the RouteTree structure.
	 * Because only one connection (the leaf) was initialized as a PIP, only this connection should be returned.
	 */
	@Test
	@DisplayName("test RouteTree method 'getAllPips")
	void testGetAllPips() {
		// only one of the same RouteTrees has a Pip connection (the leaf)
		assertEquals(1, root.getAllPips().size(), "Only one of the sample connections was a PIP");
		assertEquals(leaf.getConnection().getPip(), root.getAllPips().iterator().next(), "The leaf RouteTree contains the only PIP connection");
	}

	/**
	 * The values of a deeply copied object should be the same, but the references should be different. In the RouteTree
	 * structure, every RouteTree needs to be copied into a new RouteTree while maintaining the Connection and Wire
	 * values.
	 */
	@Test
	@DisplayName("test RouteTree method 'deepCopy'")
	void testDeepCopy() {
		// copy the RouteTree
		RouteTree copy = leaf.deepCopy();
		// begin to iterate over each child RouteTree in both copies
		Iterator<RouteTree> main_index = leaf.prefixIterator();
		Iterator<RouteTree> copy_index = copy.prefixIterator();
		while (main_index.hasNext() && copy_index.hasNext()) {
			RouteTree main_next = main_index.next();
			RouteTree copy_next = copy_index.next();
			// verify that each sub-RouteTree matches the original
			assertEquals(main_next.getConnection(), copy_next.getConnection(), "Connection doesn't match in copied RouteTree");
			assertEquals(main_next.getWire(), copy_next.getWire(), "Wire doesn't match in copied RouteTree");
			// verify that each copied RouteTree is a separate object from the original (not a shallow reference copy)
			assertTrue(main_next != copy_next, "Copied RouteTree shouldn't have same reference");
			if (main_next.isSourced())
				assertTrue(main_next.getSourceTree() != copy_next.getSourceTree(), "Copied RouteTree should have different source tree");
			else
				assertNull(copy_next.getSourceTree(), "Copied RouteTree should have null source.");
		}
		// verify that the copies have the same number of sub-RouteTrees
		assertFalse(main_index.hasNext(), "Copied RouteTree has less children");
		assertFalse(copy_index.hasNext(), "Copied RouteTree has more children");
	}

	/**
	 * Pruning a RouteTree removes unused RouteTrees from the structure. To do this, a Set of terminal trees is passed
	 * to the prune() method. Any RouteTree that is not a parent of these trees is removed.
	 * Because the Set of terminal trees is a HashSet, this means that each RouteTree should have a different hashcode.
	 */
	@Test
	@DisplayName("test RouteTree method 'prune'")
	void testPrune() {
		// after pruning the RouteTree, only the root and the branch (terminal) should be left
		boolean result = root.prune(Collections.singleton(branch));
		assertAll(
			() -> assertTrue(result, "Pruning should return 'true' to indicate that at least one terminal was found"),
			() -> assertNotNull(root.getSinkTrees(), "The root RouteTree should still have sink trees after pruning"),
			() -> assertNotNull(branch.getSinkTrees(), "The terminal RouteTree should still have a collection of children after pruning"),
			() -> assertEquals(1, root.getSinkTrees().size(), "The root RouteTree should still have a reference to the terminal RouteTree after pruning"),
			() -> assertEquals(0, branch.getSinkTrees().size(), "The terminal RouteTree shouldn't have any children after pruning")
		);
	}

	@Test
	@DisplayName("test getConnectingSitePin method")
	void testGetConnectingSitePin() {
		Site site = device.getTile(0).getSite(0);
		SitePin pin = site.getSinkPin("DUMMY_SITE_PIN");
		RouteTree t = leaf.addConnection(newDummyConnection(leaf.getWire(), 4, false));
		assertEquals(pin, t.getConnectedSitePin());
	}

	@Test
	@DisplayName("getConnectingSitePin is unidirectional")
	void testGetConnectingSitePin2() {
		RouteTree t = leaf.addConnection(newDummyConnection(leaf.getWire(), 5, false));
		assertNull(t.getConnectedSitePin());
	}

	@Test
	@DisplayName("connect different route trees")
	void testConnectTwoRouteTrees() {
		RouteTree tree2 = new RouteTree(new TileWire(device.getTile(0), 5));
		Connection c = newDummyConnection(tree2.getWire(), 0, true);
		tree2.addConnection(c, root);
		assertAll(
			() -> assertTrue(tree2.getSinkTrees().contains(root)),
			() -> assertEquals(c, root.getConnection()),
			() -> assertEquals(tree2, root.getSourceTree())
		);
	}

	@Test
	@DisplayName("cannot connect already sourced tree")
	void testSourcingAlreadySourcedTree() {
		RouteTree tree2 = new RouteTree(new TileWire(device.getTile(0), 5));
		Connection c = newDummyConnection(tree2.getWire(), 1, true);
		assertThrows(DesignAssemblyException.class,
			() -> tree2.addConnection(c, branch));
	}

	@Test
	@DisplayName("error thrown when mismatch occurs between connection and tree wire")
	void testMismatchedWireWhenConnectingTrees() {
		RouteTree tree2 = new RouteTree(new TileWire(device.getTile(0), 5));
		Connection c = newDummyConnection(tree2.getWire(), 1, true);
		assertThrows(DesignAssemblyException.class,
			() -> tree2.addConnection(c, leaf));
	}

	@Test
	@DisplayName("test prefix iterator")
	void testPrefixIterator() {
		// adds an extra branch to the tree
		Connection c = newDummyConnection(root.getWire(), 4, false);
		RouteTree branch2 = root.addConnection(c);
		Iterator<RouteTree> it = root.prefixIterator();
		assertTrue(it.hasNext());
		assertEquals(root, it.next());

		// in order, branch visited, branch2 visited, leaf visited
		boolean b1 = false, b2 = false, lv = false;
		while (it.hasNext()) {
			RouteTree t = it.next();
			if (t == branch) {
				assertFalse(b1);
				b1 = true;
			} else if (t == branch2) {
				assertFalse(b2);
				b2 = true;
			} else if (t == leaf) {
				assertTrue(b1, "leaf visited before branch");
				assertFalse(lv);
				lv = true;
			}
		}

		// just to make variables "effectively final"
		boolean b1f = b1, b2f = b2, lvf = lv;
		assertAll(
			() -> assertTrue(b1f, "branch not visited"),
			() -> assertTrue(b2f, "branch2 not visited"),
			() -> assertTrue(lvf, "leaf not visited")
		);
	}

	@Test
	@DisplayName("tests iterator method")
	void testIterator() {
		// adds an extra branch to the tree
		Connection c = newDummyConnection(root.getWire(), 4, false);
		RouteTree branch2 = root.addConnection(c);

		Set<RouteTree> nodes = new HashSet<>();
		for (RouteTree aRoot : root)
			nodes.add(aRoot);

		Set<RouteTree> expected = new HashSet<>();
		expected.add(root);
		expected.add(branch);
		expected.add(branch2);
		expected.add(leaf);

		assertEquals(expected, nodes);
	}

	@Test
	@DisplayName("tests iterator does not include sources method")
	void testIteratorMiddle() {
		// adds an extra branch to the tree
		Connection c = newDummyConnection(root.getWire(), 4, false);
		root.addConnection(c);

		Set<RouteTree> nodes = new HashSet<>();
		for (RouteTree aBranch : branch)
			nodes.add(aBranch);

		Set<RouteTree> expected = new HashSet<>();
		expected.add(branch);
		expected.add(leaf);

		assertEquals(expected, nodes);
	}

	@Test
	@DisplayName("tests iterator does not include sources method")
	void testPrefixIteratorMiddle() {
		// adds an extra branch to the tree
		Connection c = newDummyConnection(root.getWire(), 4, false);
		root.addConnection(c);

		List<RouteTree> nodes = new ArrayList<>();
		for (RouteTree aBranch : branch)
			nodes.add(aBranch);

		List<RouteTree> expected = new ArrayList<>();
		expected.add(branch);
		expected.add(leaf);

		assertEquals(expected, nodes);
	}
}
