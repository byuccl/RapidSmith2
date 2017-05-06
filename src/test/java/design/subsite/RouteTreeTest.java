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
import java.util.*;

import edu.byu.ece.rapidSmith.device.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.runner.RunWith;
import org.junit.platform.runner.JUnitPlatform;
import static org.junit.jupiter.api.Assertions.*;
import edu.byu.ece.rapidSmith.design.subsite.RouteTree;

/**
 * jUnit test for the RouteTree class in RapidSmith2
 * @author Mark Crossen
 */
@RunWith(JUnitPlatform.class)
public class RouteTreeTest {

    /** the Site that all tested RouteTrees share through the SiteWire */
    private Site dummy_site;
    /** the SiteWire that all tested RouteTrees will share */
    private SiteWire dummy_wire;
    /** A three-level structure will be tested. This is level 1 of 3 */
    private RouteTree root;
    /** A three-level structure will be tested. This is level 2 of 3 */
    private RouteTree branch;
    /** A three-level structure will be tested. This is level 3 of 3 */
    private RouteTree leaf;

    /**
     * This method is ran between tests to repair and initialize the three tested RouteTrees.
     */
    @BeforeEach
    protected void setUp() {
        // Create a fake device, tile, and string array. Apparently these are needed when finding the hashCode() of a SiteWireConnection
        String[] wires = new String[1];
        wires[0] = "dummy_wire_enum";
        WireEnumerator dummy_wire_enumerator = new WireEnumerator();
        dummy_wire_enumerator.setWires(wires);
        Device dummy_device = new Device();
        dummy_device.setWireEnumerator(dummy_wire_enumerator);
        Tile dummy_tile = new Tile();
        dummy_tile.setDevice(dummy_device);
        // fill the variables that will be used for the test
        dummy_site = new Site();
        dummy_site.setName("dummy_site");
        dummy_site.setTile(dummy_tile);
        dummy_wire = new SiteWire(dummy_site, 0);
        // each RouteTree has a unique value so that they hash differently
        root = newDummyTree(); // unique value = 0
        branch = root.addConnection(newDummyConnection(1, false)); // unique value = 1
        leaf = branch.addConnection(newDummyConnection(2, true)); // unique value = 2
    }

    /**
     * A helper function to create a new RouteTree.
     *
     * @return a new RouteTree containing a simple connection
     */
    private RouteTree newDummyTree() {
        RouteTree rt = new RouteTree(dummy_wire);
        rt.setConnection(newDummyConnection(0, false));
        return rt;
    }

    /**
     * A helper function to create a simple connection
     *
     * @param unique this can be any number. It is used to make each Connection have a different hashCode()
     * @param isPip boolean to determine wether or not the Connection is a PIP
     * @return the built connection
     */
    public Connection newDummyConnection(int unique, boolean isPip) {
        WireConnection wc = new WireConnection(dummy_wire.getWireEnum(), unique, unique, isPip);
        return Connection.getSiteWireConnection(dummy_wire, wc);
    }

    /**
     * test whether each of the three sample trees has a source tree.
     * Because each sample tree is a child of another sample tree, only the first (root) tree shouldn't be sourced.
     */
    @Test
    @DisplayName("test RouteTree method 'isSourced'")
    public void testIsSourced() {
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
    public void testGetFirstSource() {
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
    public void testIsLeaf() {
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
    public void testRemoveConnection() {
        // remove the leaf connection and verify that the branch RouteTree is now a leaf
        branch.removeConnection(leaf.getConnection());
        assertTrue(branch.isLeaf(), "The 'branch' should be a leaf after removing the 'leaf' RouteTree");
        // verify that the orphaned RouteTree doesn't reference the main structure
        assertNull(leaf.getSourceTree(), "After removing the 'leaf', its source tree should be set to null");
        // repair the RouteTree so it can be used for other tests
        leaf = branch.addConnection(leaf.getConnection());
    }

    /**
     * the getAllPips() method should return a list of all PIP connections in the RouteTree structure.
     * Because only one connection (the leaf) was initialized as a PIP, only this connection should be returned.
     */
    @Test
    @DisplayName("test RouteTree method 'getAllPips")
    public void testGetAllPips() {
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
    public void testDeepCopy() {
        // copy the RouteTree
        RouteTree copy = leaf.deepCopy();
        // begin to iterate over each child RouteTree in both copies
        Iterator<RouteTree> main_index = leaf.iterator();
        Iterator<RouteTree> copy_index = copy.iterator();
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
    public void testPrune() {
        // after pruning the RouteTree, only the root and the branch (terminal) should be left
        assertTrue(root.prune(branch), "Pruning should return 'true' to indicate that at least one terminal was found");
        assertNotNull(root.getSinkTrees(), "The root RouteTree should still have sink trees after pruning");
        assertNotNull(branch.getSinkTrees(), "The terminal RouteTree should still have a collection of children after pruning");
        assertEquals(1, root.getSinkTrees().size(), "The root RouteTree should still have a reference to the terminal RouteTree after pruning");
        assertEquals(0, branch.getSinkTrees().size(), "The terminal RouteTree shouldn't have any children after pruning");
        // repair the RouteTree for future tests
        leaf = branch.addConnection(leaf.getConnection());
    }
}