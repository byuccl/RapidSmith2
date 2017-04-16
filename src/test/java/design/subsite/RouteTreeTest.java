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
import java.util.concurrent.SynchronousQueue;

import edu.byu.ece.rapidSmith.device.*;
import edu.byu.ece.rapidSmith.util.RapidSmithDebug;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.runner.RunWith;
import org.junit.platform.runner.JUnitPlatform;
import static org.junit.jupiter.api.Assertions.*;
import edu.byu.ece.rapidSmith.design.subsite.RouteTree;

/**
 * jUnit test for the Cell class in RapidSmith2
 * @author Mark Crossen
 */
@RunWith(JUnitPlatform.class)
public class RouteTreeTest {

    private Site dummy_site;
    private SiteWire dummy_wire;
    private RouteTree root;
    private RouteTree branch;
    private RouteTree leaf;

    @BeforeEach
    protected void setUp() {
        dummy_site = new Site();
        dummy_wire = new SiteWire(dummy_site, 0);
        root = newDummyTree();
        branch = root.addConnection(newDummyConnection(false));
        leaf = branch.addConnection(newDummyConnection(true));
    }

    // helper function to create a new dummy route tree
    private RouteTree newDummyTree() {
        return new RouteTree(dummy_wire);
    }

    // helper functionn to create a new dummy connection
    public Connection newDummyConnection(boolean isPip) {
        WireConnection wc = new WireConnection();
        wc.setPIP(isPip);
        return Connection.getSiteWireConnection(dummy_wire, wc);
    }


    @Test
    @DisplayName("test RouteTree method 'isSourced'")
    public void testIsSourced() {
        /* everything but the root tree should be sourced */
        assertFalse(root.isSourced());
        assertTrue(branch.isSourced());
        assertTrue(leaf.isSourced());
    }

    @Test
    @DisplayName("test RouteTree method 'getFirstSource'")
    public void testGetFirstSource() {
        assertEquals(root, root.getFirstSource());
        assertEquals(root, branch.getFirstSource());
        assertEquals(root, leaf.getFirstSource());
    }

    @Test
    @DisplayName("test RouteTree method 'isLeaf'")
    public void testIsLeaf() {
        assertFalse(root.isLeaf());
        assertFalse(branch.isLeaf());
        assertTrue(leaf.isLeaf());
    }

    @Test
    @DisplayName("test RouteTree method 'getConnectingSitePin'")
    public void testGetConnectingSitePin() {
        //assertNull(root.getConnectingSitePin());//TODO: blank wires give null exception. Bug or feature?
        //TODO: connect a SitePin and test if it gets returned
    }

    @Test
    @DisplayName("test RouteTree method 'getConnectingBelPin'")
    public void testGetConnectingBelPin() {
        //assertNull(root.getConnectingBelPin());//TODO: blank wires give null exception. Bug or feature?
        //TODO: connect a BelPin and test if it gets returned
    }

    @Test
    @DisplayName("test RouteTree method 'removeConnection'")
    public void testRemoveConnection() {
        branch.removeConnection(leaf.getConnection());
        assertTrue(branch.isLeaf());
        leaf = branch.addConnection(leaf.getConnection());
    }

    @Test
    @DisplayName("test RouteTree method 'getAllPips")
    public void testGetAllPips() {
        assertEquals(1, root.getAllPips().size());
        assertEquals(leaf.getConnection().getPip(), root.getAllPips().iterator().next());
    }

    @Test
    @DisplayName("test RouteTree method 'deepCopy'")
    public void testDeepCopy() {
        RouteTree copy = leaf.deepCopy();
        Iterator<RouteTree> main_index = leaf.iterator();
        Iterator<RouteTree> copy_index = copy.iterator();
        while (main_index.hasNext() && copy_index.hasNext()) {
            RouteTree main_next = main_index.next();
            RouteTree copy_next = copy_index.next();
            assertEquals(main_next.getConnection(), copy_next.getConnection(), "Connection doesn't match in copied RouteTree");
            assertEquals(main_next.getWire(), copy_next.getWire(), "Wire doesn't match in copied RouteTree");
            assertTrue(main_next != copy_next, "Copied RouteTree shouldn't have same reference");
            if (main_next.isSourced())
                assertTrue(main_next.getSourceTree() != copy_next.getSourceTree(), "Copied RouteTree should have different source tree");
            else
                assertNull(copy_next.getSourceTree(), "Copied RouteTree should have null source.");
        }
        assertFalse(main_index.hasNext(), "Copied RouteTree has less children.");
        assertFalse(copy_index.hasNext(), "Copied RouteTree has more children.");
    }

    @Test
    @DisplayName("test RouteTree method 'prune'")
    public void testPrune() {
        root.prune(branch);
        System.out.println("ending test");
        assertNotNull(root.getSinkTrees(), "The root RouteTree should still have sink trees after pruning");
        assertNotNull(branch.getSinkTrees(), "The terminal RouteTree should still have a collection of children after pruning");
        assertEquals(1, root.getSinkTrees().size(), "The root RouteTree should still have a reference to the terminal RouteTree after pruning");
        assertEquals(0, branch.getSinkTrees().size(), "The terminal RouteTree shouldn't have any children after pruning");
        leaf = branch.addConnection(leaf.getConnection());
    }
}