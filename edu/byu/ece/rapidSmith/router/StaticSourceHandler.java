/*
 * Copyright (c) 2010 Brigham Young University
 * 
 * This file is part of the BYU RapidSmith Tools.
 * 
 * BYU RapidSmith Tools is free software: you may redistribute it 
 * and/or modify it under the terms of the GNU General Public License 
 * as published by the Free Software Foundation, either version 2 of 
 * the License, or (at your option) any later version.
 * 
 * BYU RapidSmith Tools is distributed in the hope that it will be 
 * useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU 
 * General Public License for more details.
 * 
 * A copy of the GNU General Public License is included with the BYU 
 * RapidSmith Tools. It can be found at doc/gpl2.txt. You may also 
 * get a copy of the license at <http://www.gnu.org/licenses/>.
 * 
 */
package edu.byu.ece.rapidSmith.router;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;

import edu.byu.ece.rapidSmith.design.Attribute;
import edu.byu.ece.rapidSmith.design.Instance;
import edu.byu.ece.rapidSmith.design.Net;
import edu.byu.ece.rapidSmith.design.NetType;
import edu.byu.ece.rapidSmith.design.PIP;
import edu.byu.ece.rapidSmith.design.Pin;
import edu.byu.ece.rapidSmith.device.*;
import edu.byu.ece.rapidSmith.router.PinSorter.StaticSink;
import edu.byu.ece.rapidSmith.util.FamilyType;
import edu.byu.ece.rapidSmith.util.MessageGenerator;

/**
 * This class separates the static nets in to separate nets so they can be routed
 * more easily by the router.  This same process happens in Xilinx PAR during the routing
 * phase.
 * @author Chris Lavin
 * Created on: Jun 10, 2010
 */
public class StaticSourceHandler{

	/** Current Router */
	private AbstractRouter router;
	/** Current Device */
	private Device dev;
	/** Current WireEnumerator */
	private WireEnumerator we;
	/** Number of nets created and makes them unique by name */
	private int netCount;
	/** Final set of static nets */
	private ArrayList<Net> finalStaticNets;
	/** Default pin on SLICE to be used as static output source */
	String slicePin;
	/** Just a node that is used so it doesn't have to be created over and over */
	private Node tempNode;
	/** Map to help determine when to use a SLICE for GND connections */
	private static HashMap<String, String> v4BounceMap;
	/** Map to help retrieve the FAN and BOUNCE connections in Virtex 5 */
	private static HashMap<String,String[]> fanBounceMap;
	/** List of OMUX Top Wires in Virtex 4 Switch Box */
	private static String[] v4TopOmuxs = {"OMUX14","OMUX9","OMUX8","OMUX13","OMUX11","OMUX15","OMUX12","OMUX10"};
	/** List of OMUX Bottom Wires in Virtex 4 Switch Box */
	private static String[] v4BottomOmuxs = {"OMUX0","OMUX4","OMUX3","OMUX5","OMUX2","OMUX7","OMUX6","OMUX1"};
	
	private static String[] v5ctrl = {"CTRL_B0", "CTRL_B1", "CTRL_B2", "CTRL_B3"};
	private static int[] v5ctrlWires = new int[4];
	
	private Pin currStaticSourcePin = null;
	
	private HashMap<Node, Pin> reservedGNDVCCResources;
	
	// Attributes used in creating TIEOFFs
	private Attribute noUserLogicAttr;
	private Attribute hard1Attr;
	private Attribute keep1Attr;
	private Attribute keep0Attr;
	
	FamilyType familyType;
	
	/**
	 * Constructor
	 * @param router The router to be used with this static source handler.
	 */
	public StaticSourceHandler(AbstractRouter router){
		this.router = router;
		dev = router.dev;
		we = dev.getWireEnumerator();
		familyType = dev.getFamilyType();
		netCount = 0;
		finalStaticNets = new ArrayList<>();
		tempNode = new Node();
		reservedGNDVCCResources = new HashMap<>();
		if(dev.getFamilyType().equals(FamilyType.VIRTEX5)){
			slicePin = "B";
		}
		else if(dev.getFamilyType().equals(FamilyType.VIRTEX4)){
			slicePin = "Y";
		}
		else{
			System.out.println("Sorry, this architecture is not supported.");
			System.exit(1);
		}
		
		// Initialize attributes
		noUserLogicAttr = new Attribute("_NO_USER_LOGIC","","");
		hard1Attr = new Attribute("_VCC_SOURCE","","HARD1");
		keep1Attr = new Attribute("_VCC_SOURCE","","KEEP1");
		keep0Attr = new Attribute("_GND_SOURCE","","KEEP0");
		
		for (int i = 0; i < v5ctrl.length; i++) {
			v5ctrlWires[i] = we.getWireEnum(v5ctrl[i]);
		}
	}
	
	/**
	 * Reserves a node for a ground or vcc inpin that has
	 * not been assigned a final net yet.
	 * @param node The node to reserve.
	 * @param pin The pin for which to reserve the routing resource.
	 */
	private boolean addReservedGNDVCCNode(Node node, Pin pin){
		if(router.usedNodes.contains(node)){
			LinkedList<Net> nets = router.usedNodesMap.get(node);
			if(nets == null){
				Pin p = reservedGNDVCCResources.get(node);
				if(p == null){
					return false;
				}
				else if(!p.getNet().getType().equals(pin.getNet().getType())){
					return false;
				}
			}
			else if(!nets.get(0).getType().equals(pin.getNet().getType())){
				return false;
			}
		}
		
		LinkedList<Net> nets = router.usedNodesMap.get(node);
		if(nets == null){
			nets = new LinkedList<>();
			router.usedNodesMap.put(node, nets);
		}
		nets.add(pin.getNet());
		
		// We will update the net reserved list later,
		// after the pin has been assigned its final net
		reservedGNDVCCResources.put(node, pin);
		router.usedNodes.add(node);
		return true;
	}
	
	private void addReservedNode(Node node, Net net){
		ArrayList<Node> nodes = router.reservedNodes.get(net);
		if(nodes == null){
			nodes = new ArrayList<>();
			router.reservedNodes.put(net, nodes);
		}
		nodes.add(node);
		router.usedNodes.add(node);
		LinkedList<Net> nets = router.usedNodesMap.get(node);
		if(nets == null){
			nets = new LinkedList<>();
			router.usedNodesMap.put(node, nets);
		}
		nets.add(net);
	}
	
	public Node getSwitchBoxWire(Net net){
		Pin source = net.getSource();
		if(source.getName().contains("COUT") && net.getPins().size() == 2){
			Pin sink = net.getPins().get(1).equals(source) ? net.getPins().get(0) : net.getPins().get(1);
			if(sink.getName().contains("CIN")){
				return null;
			}
		}
		
		Node curr = new Node(source.getTile(), dev.getPrimitiveExternalPin(source).getWireEnum(), null, 0);
		while(!we.getWireDirection(curr.getWire()).equals(WireDirection.CLK) && !we.getWireType(curr.getWire()).equals(WireType.INT_SOURCE)){
			WireConnection[] wires = curr.getConnections();
			if(wires == null) return null;
			WireConnection w = wires[0];
			if(we.getWireName(w.getWire()).contains("COUT") && wires.length > 1 ){
				
				w = wires[1];
			}
			curr = new Node(w.getTile(curr.tile),w.getWire(), null, 0);
		}
		
		return curr;
	}
	
	/**
	 * Some designs may already have some routed nets.  This method makes sure
	 * that the router notes the resources already used in the routed net.
	 * @param net The routed net to mark used nodes 
	 */
	private void accomodateRoutedNet(Net net){
		for(PIP p : net.getPIPs()){
			router.setWireAsUsed(p.getTile(), p.getStartWire(), net);
			router.setWireAsUsed(p.getTile(), p.getEndWire(), net);
			router.markIntermediateNodesAsUsed(p, net);
		}
	}
	
	/**
	 * Determines the critical resource for a given wire.  
	 * @param wire The wire to determine if it is critical
	 * in order to route to the underlying pin.
	 * @return The critical wire resource (which could be different from 
	 * the given wire) or -1 if the resource is not critical.
	 */
	private int getCriticalResource(int wire){
		String name = we.getWireName(wire);
		if(name.startsWith("BYP_INT_B")) // VIRTEX4
			return wire;
		else if(name.startsWith("BYP_B")) // VIRTEX5
			return we.getWireEnum("BYP" + name.charAt(name.length()-1));
		else if(name.startsWith("CTRL_B")) // VIRTEX5
			return we.getWireEnum("CTRL" + name.charAt(name.length()-1));
		else if(name.startsWith("FAN_B")) // VIRTEX5
			return we.getWireEnum("FAN" + name.charAt(name.length()-1));
		return -1;
	}
	
	private void unRouteNetForCriticalNode(Node n){
		LinkedList<Net> previous = router.usedNodesMap.get(n);
		if(previous == null){
			MessageGenerator.briefError("ERROR: Failure to unroute net for node: " + n.toString());
			return;
		}
		LinkedList<Net> nets = new LinkedList<>(previous);
		for(Net net : nets){
			for(PIP p : net.getPIPs()){
				router.setWireAsUnused(p.getTile(), p.getStartWire(), net);
				router.setWireAsUnused(p.getTile(), p.getEndWire(), net);
				router.markIntermediateNodesAsUnused(p, net);
			}
			net.unroute();
			MessageGenerator.briefError("Unrouting Net: " + net.getName());
			MessageGenerator.briefError("  For Critical Resource: " + n.toString());
			ArrayList<Node> reservedNodes = reserveCriticalNodes(net);
			if(!reservedNodes.isEmpty()){
				ArrayList<Node> nodes = router.reservedNodes.get(net);
				if(nodes == null){
					nodes = reservedNodes;
					router.reservedNodes.put(net, nodes);
				}else {
					nodes.addAll(reservedNodes);						
				}
				router.usedNodes.addAll(reservedNodes);
			}
		}
	}
	
	/**
	 * Reserves resources for nets which require specific routing resources
	 * in order to complete a net.
	 * @param net the net to examine.
	 * @return A List of critical resources that should be reserved for the 
	 * net.
	 */
	private ArrayList<Node> reserveCriticalNodes(Net net){
		ArrayList<Node> reservedNodes = new ArrayList<>();
		
		for(Pin p : net.getPins()){
			if(p.isOutPin()) continue; // Skip outpins
			int extPin = dev.getPrimitiveExternalPin(p).getWireEnum();
			tempNode.setTileAndWire(p.getInstance().getTile(), extPin);
			Node reserved = tempNode.getSwitchBoxSink(dev);
			if(reserved.wire == -1) continue;
			if(router.usedNodes.contains(reserved)){
				unRouteNetForCriticalNode(reserved);
				addReservedNode(reserved, net);
				continue;
			}
			int criticalResource = getCriticalResource(reserved.wire);
			if(criticalResource != -1){
				reserved.setWire(criticalResource);
				if(router.usedNodes.contains(reserved)){
					unRouteNetForCriticalNode(reserved);
					addReservedNode(reserved, net);
				}
				else{
					reservedNodes.add(reserved);		
				}

			}
		}
		return reservedNodes;
	}
	
	/**
	 * This is for Virtex 4 designs only.  It reserves some of the OMUX
	 * wires for heavily congested switch matrices.
	 */
	private void reserveVirtex4SpecificResources(ArrayList<Net> netList){
		HashMap<Tile, ArrayList<Net>> sourceCount = new HashMap<>();
		for(Net net : netList){
			Pin p = net.getSource();
			if(p == null) continue;
			ArrayList<Net> nets = sourceCount.get(p.getTile());
			if(nets == null){
				nets = new ArrayList<>();
				sourceCount.put(p.getTile(), nets);
			}
			nets.add(net);
		}
		HashMap<Tile, ArrayList<Net>> switchMatrixSources = new HashMap<>();
		for(Tile t : sourceCount.keySet()){
			ArrayList<Net> nets = sourceCount.get(t);
			for(Net n : nets){
				Node node = getSwitchBoxWire(n);
				if(node == null) continue;
				ArrayList<Net> tmp = switchMatrixSources.get(node.tile);
				if(tmp == null){
					tmp = new ArrayList<>();
					switchMatrixSources.put(node.tile,tmp);
				}
				tmp.add(n);
			}
		}
		for(Tile t : switchMatrixSources.keySet()){
			ArrayList<Net> nets = switchMatrixSources.get(t);
			
			if(nets.size() == 0) continue;
			int reservedTop = 0;
			int reservedBot = 0;				
			ArrayList<Net> secondaryNets = new ArrayList<>();
			for(Net n : nets){
				if(n.getPIPs().size() > 0) continue;
				Node node = getSwitchBoxWire(n);
				if(node == null) continue;
				String wireName = we.getWireName(node.getWire());
				if(wireName.startsWith("HALF")){
					if(wireName.contains("TOP")){
						if(reservedTop > 7){
							break;
						}
						Node newNode = new Node(node.tile, we.getWireEnum(v4TopOmuxs[reservedTop]), null, 0);
						while(router.usedNodes.contains(newNode)){

							reservedTop++;
							if(reservedTop > 7) {
								break;
							}
							newNode = new Node(node.tile, we.getWireEnum(v4TopOmuxs[reservedTop]), null, 0);
						}
						if(!router.usedNodes.contains(newNode)){
							addReservedNode(newNode, n);
							reservedTop++;																
						}
					}
					else if(wireName.contains("BOT")){
						if(reservedBot > 7){
							break;
						}
						Node newNode = new Node(node.tile, we.getWireEnum(v4BottomOmuxs[reservedBot]), null, 0);
						while(router.usedNodes.contains(newNode)){
							reservedBot++;
							if(reservedBot > 7){
								break;
							}
							newNode = new Node(node.tile, we.getWireEnum(v4BottomOmuxs[reservedBot]), null, 0);
						}
						if(!router.usedNodes.contains(newNode)){
							addReservedNode(newNode, n);
							reservedBot++;
						}
					}
				}
				else if(wireName.startsWith("SECONDARY")){
					secondaryNets.add(n);
				}
			}
			for(Net n : secondaryNets){
				Node node = getSwitchBoxWire(n);
				if(node == null) continue;
				if(reservedTop < 8){
					Node newNode = new Node(node.tile, we.getWireEnum(v4TopOmuxs[reservedTop]), null, 0);
					while(router.usedNodes.contains(newNode)){
						reservedTop++;
						if(reservedTop > 7) break;
						newNode = new Node(node.tile, we.getWireEnum(v4TopOmuxs[reservedTop]), null, 0);
					}
					if(!router.usedNodes.contains(newNode)){
						addReservedNode(newNode, n);
						reservedTop++;						
					}
				}
				else if(reservedBot < 8){
					Node newNode = new Node(node.tile, we.getWireEnum(v4BottomOmuxs[reservedBot]), null, 0);
					while(router.usedNodes.contains(newNode)){
						reservedBot++;
						if(reservedBot > 7) {
							break;
						}
						newNode = new Node(node.tile, we.getWireEnum(v4BottomOmuxs[reservedBot]), null, 0);
					}
					if(!router.usedNodes.contains(newNode)){
						addReservedNode(newNode, n);							
						reservedBot++;							
					}
				}
				else{
					break;
				}
			}
		}			
	}	
	
	private HashMap<Tile, PinSorter> sortPinsVirtex4(ArrayList<Net> staticSourcedNets){
		HashMap<Tile, PinSorter> pinSwitchMatrixMap = new HashMap<>();
		Node bounce0 = new Node(); bounce0.wire = we.getWireEnum("BOUNCE0");
		Node bounce1 = new Node(); bounce1.wire = we.getWireEnum("BOUNCE1");
		Node bounce2 = new Node(); bounce2.wire = we.getWireEnum("BOUNCE2");
		Node bounce3 = new Node(); bounce3.wire = we.getWireEnum("BOUNCE3");
		
		for(Net net : staticSourcedNets){
			for(Pin pin : net.getPins()){
				// Switch matrix sink, where the route has to connect through
				Node switchMatrixSink  = dev.getSwitchMatrixSink(pin);
				PinSorter tmp = pinSwitchMatrixMap.get(switchMatrixSink.tile);
				if(tmp == null){
					tmp = PinSorter.createPinSorter(dev);
					assert tmp != null;
					pinSwitchMatrixMap.put(switchMatrixSink.tile, tmp);
				}
				
				String wireName = we.getWireName(switchMatrixSink.wire);
				String bounce = v4BounceMap.get(wireName);
				if(bounce != null && net.getType().equals(NetType.GND) && 
				   router.isNodeUsed(switchMatrixSink.tile, we.getWireEnum(bounce))){
					bounce0.setTile(switchMatrixSink.tile);
					bounce1.setTile(switchMatrixSink.tile);
					bounce2.setTile(switchMatrixSink.tile);
					bounce3.setTile(switchMatrixSink.tile);
					if(wireName.startsWith("CE") || wireName.startsWith("SR")){
						
						if(router.isNodeUsed(bounce0) && router.isNodeUsed(bounce1) &&
						   router.isNodeUsed(bounce2) && router.isNodeUsed(bounce3)){
							tmp.addPinToSliceList(switchMatrixSink, pin);
						}
						else{
							tmp.addPin(switchMatrixSink, pin);
						}
					}
					else{
						tmp.addPinToSliceList(switchMatrixSink, pin);
					}
				}
				else{
					tmp.addPin(switchMatrixSink, pin);
				}
			}
		}			

		return pinSwitchMatrixMap;
	}
	
	private HashMap<Tile, PinSorter> sortPinsVirtex5(ArrayList<Net> staticSourcedNets){
		HashMap<Tile, PinSorter> pinSwitchMatrixMap = new HashMap<>();
		for(Net net : staticSourcedNets){
			for(Pin pin : net.getPins()){
				Node switchMatrixSink = dev.getSwitchMatrixSink(pin);
				int wire = getCriticalResource(switchMatrixSink.wire);
				
				if(wire != -1){
					// This pin requires a critical resource, let's try to reserve it
					Node n = new Node(switchMatrixSink.tile, wire, null, 0);
					if(!addReservedGNDVCCNode(n, pin)){
						MessageGenerator.briefError("ERROR: This pin requires the critical resource " + n.toString() + " which has already been used.");
					}
				}
				
				PinSorter ps = pinSwitchMatrixMap.get(switchMatrixSink.tile);
				if(ps == null) {
					ps = PinSorter.createPinSorter(dev);
					assert ps != null;
					pinSwitchMatrixMap.put(switchMatrixSink.tile, ps);
				}
				ps.addPin(switchMatrixSink, pin);
			}
		}
		return pinSwitchMatrixMap;
	}
	
	
	/**
	 * This function makes sure that all the GND and VCC sources are grouped together
	 * @param inputList The input static source net list
	 * @return The grouped net list with GND nets first
	 */
	public ArrayList<Net> orderGNDNetsFirst(ArrayList<Net> inputList){
		ArrayList<Net> gndNets = new ArrayList<>();
		ArrayList<Net> vccNets = new ArrayList<>();
		
		for(Net net : inputList){
			if(net.getType().equals(NetType.GND)){
				gndNets.add(net);
			}
			else if(net.getType().equals(NetType.VCC)){
				vccNets.add(net);
			}
			else{
				MessageGenerator.briefErrorAndExit("Error: found non-static net in static netlist.");
			}
		}
		gndNets.addAll(vccNets);
		return gndNets;
	}
	
	private Tile getNeighboringSwitchBox(int yOffset, Tile currTile){
		String newTileName = "INT_X" + currTile.getTileXCoordinate() + "Y" + (currTile.getTileYCoordinate()+yOffset);
		return dev.getTile(newTileName);
	}
	
	/**
	 * This method will separate out static sourced nets, partitioning them into localized
	 * nets
	 */
	public void separateStaticSourceNets(){
		ArrayList<Net> netList = router.netList;
		ArrayList<Net> staticSourcedNets = new ArrayList<>();
		
		//===================================================================//
		// Step 1: Separate static sourced nets, re-entrant routing stuff, 
		//         and reserve nodes for critical inpins on nets
		//===================================================================//
		for(Net net : netList){
			if(net.getPIPs().size() > 0){
				//===========================================================//
				// Do re-entrant routing, if a net already has PIPs, 
				// assume its routed, GND/VCC nets are also included
				//===========================================================//	
				accomodateRoutedNet(net);
			}
			else if(net.isStaticNet()){
				//===========================================================//
				// Separate out static sourced nets
				//===========================================================//	
				if(net.getSource() != null){
					MessageGenerator.briefError("Error: GND/VCC net already " +
						"has a source pin: " +net.getName() + 
						net.getSource().toString() +", it will be removed.");
					net.removePin(net.getSource());
				}					
				staticSourcedNets.add(net);
			}
		}
		
		for(Net net : netList){
			if(!net.hasPIPs() && !net.isStaticNet()){
				//===========================================================//
				// Reserve Nodes for Critical Input Pins on Nets
				//===========================================================//				
				ArrayList<Node> reservedNodes = reserveCriticalNodes(net);

				if(!reservedNodes.isEmpty()){
					ArrayList<Node> nodes = router.reservedNodes.get(net);
					if(nodes == null){
						nodes = reservedNodes;
						router.reservedNodes.put(net, nodes);
					}else {
						nodes.addAll(reservedNodes);						
					}
					router.usedNodes.addAll(reservedNodes);
				}
			}
		}
		
		if(familyType.equals(FamilyType.VIRTEX4)){
			reserveVirtex4SpecificResources(netList);
		}
		
		// Remove all static sourced nets from original netlist, to be 
		// recombined later
		netList.removeAll(staticSourcedNets);

		//===================================================================//
		// Step 2: Find which static sourced inpins go to which switch 
		//         matrices, sort pins to go into categories: useTIEOFF, 
		//         attemptTIEOFF, useSLICE
		//===================================================================//
		HashMap<Tile, PinSorter> pinSwitchMatrixMap = null;
		// Iterate through all static nets and create mapping of all sinks to their
		// respective switch matrix tile, each pin is separated into groups of how their
		// sources should be created. There are 3 groups:
		// 1. High priority TIEOFF sinks - Do the best you can to attach these sinks to the TIEOFF
		// 2. Attempt TIEOFF sinks - Attempt to connect them to a TIEOFF, but not critical
		// 3. SLICE Source - Instance a nearby slice to supply GND/VCC
		switch(familyType){
			case VIRTEX4:
				pinSwitchMatrixMap = sortPinsVirtex4(staticSourcedNets);
				break;
			case VIRTEX5:
				pinSwitchMatrixMap = sortPinsVirtex5(staticSourcedNets);
				break;
			default:
				MessageGenerator.briefErrorAndExit("ERROR: " + familyType +
					" is unsupported.");
		}
		assert pinSwitchMatrixMap != null;

		//===================================================================//
		// Step 3: Make more adjustments to pin categories and create final
		//         partitioned nets
		//===================================================================//
		HashSet<Tile> contentionTiles = new HashSet<>();
		for(Tile tile : pinSwitchMatrixMap.keySet()){
			PinSorter ps = pinSwitchMatrixMap.get(tile);
			boolean foundVCC = false;
			boolean foundGND = false;
			for(StaticSink ss : ps.useTIEOFF){
				if(ss.pin.getNet().getType().equals(NetType.GND)) foundGND = true;
				if(ss.pin.getNet().getType().equals(NetType.VCC)) foundVCC = true;
			}
			if(foundVCC && foundGND){
				contentionTiles.add(tile);
			}
		}
		
		// For every switch matrix tile we have found that requires static sink connections
		for(Tile tile : pinSwitchMatrixMap.keySet()){
			PinSorter ps = pinSwitchMatrixMap.get(tile);
			ArrayList<StaticSink> removeThese = new ArrayList<>();
			
			// Virtex 5 has some special pins that we should reserve
			if(dev.getFamilyType().equals(FamilyType.VIRTEX5)){
				for(StaticSink ss : ps.useTIEOFF){
					String ssWireName = we.getWireName(ss.switchMatrixSink.wire);
					String[] fans = fanBounceMap.get(ssWireName);
					Node newNode = null;

					for(String fan : fans){
						tempNode.setTile(tile);
						tempNode.setWire(we.getWireEnum(fan));
						
						/*if(ssWireName.startsWith("CLK_") && router.usedNodes.contains(tempNode)){
							LinkedList<Net> net = router.usedNodesMap.get(tempNode);
							if(net.size() == 1){
								unRouteNetForCriticalNode(tempNode);
								break;
							}
						}*/
						
						boolean ableToReserveResource = (!router.usedNodes.contains(tempNode)) || 
												(reservedGNDVCCResources.get(tempNode) != null && 
												reservedGNDVCCResources.get(tempNode).equals(ss.pin)); 

						// Add this to reserved
						if(ableToReserveResource){
							newNode = new Node(tile, we.getWireEnum(fan), null, 0);
							String wireName = we.getWireName(newNode.wire);
							
							if(wireName.equals("FAN0") && !we.getWireName(ss.switchMatrixSink.wire).equals("FAN_B0")){
								ss.switchMatrixSink.tile = getNeighboringSwitchBox(1, ss.switchMatrixSink.tile);
								newNode.tile = ss.switchMatrixSink.tile;
								
								// Special case when neighboring resources are used (hard macros)
								tempNode.tile = ss.switchMatrixSink.tile;
								tempNode.wire = we.getWireEnum("FAN0");
								if(tempNode.tile == null || router.usedNodes.contains(tempNode)){
									newNode = null;
								}
							}
							else if(wireName.equals("FAN7") && !we.getWireName(ss.switchMatrixSink.wire).equals("FAN_B7")){
								ss.switchMatrixSink.tile = getNeighboringSwitchBox(-1, ss.switchMatrixSink.tile);
								newNode.tile = ss.switchMatrixSink.tile;
								
								// Special case when neighboring resources are used (hard macros)
								tempNode.tile = ss.switchMatrixSink.tile;
								tempNode.wire = we.getWireEnum("FAN7");
								if(tempNode.tile == null || router.usedNodes.contains(tempNode)){
									newNode = null;
								}
							}
							
							if(newNode != null && ss.pin.getNet().getType().equals(NetType.GND)){
								if(contentionTiles.contains(tile)){
									newNode = null;									
								}
								else if(ss.pin.getName().equals("SSRBU")){
									Node n = new Node(ss.switchMatrixSink.tile, we.getWireEnum(we.getWireName(ss.switchMatrixSink.wire).replace("_B", "")),null, 0);
									if(!addReservedGNDVCCNode(n, ss.pin)){
										MessageGenerator.briefError("ERROR: Possible problem routing pin: " + ss.pin.toString());
									}

								}
							}
													
							break;
						}
					}
					if(newNode == null){
						removeThese.add(ss);
						ps.useSLICE.add(ss);
					}
				}
				ps.useTIEOFF.removeAll(removeThese);				
			
				removeThese = new ArrayList<>();
				for(StaticSink ss : ps.attemptTIEOFF){
					if(ss.pin.getNet().getType().equals(NetType.GND)){
						String[] fans = fanBounceMap.get(we.getWireName(ss.switchMatrixSink.wire));
						boolean useSLICE = true;
						for(String fan : fans){
							tempNode.setWire(we.getWireEnum(fan));
							// Add this to reserved
							if(!router.usedNodes.contains(tempNode)){
								useSLICE = false;
								break;
							}
						}
						if(useSLICE){
							ps.useSLICE.add(ss);
							removeThese.add(ss);
						}
					}
				}
				ps.attemptTIEOFF.removeAll(removeThese);
			}
		}
				
		// Handle each group of sinks separately, allocating TIEOFF to those sinks according
		// to priority
		for(PinSorter ps : pinSwitchMatrixMap.values()){
			ssLoop : for(StaticSink ss : ps.useTIEOFF){
				Instance inst = updateTIEOFF(ss.switchMatrixSink.tile, ss.pin.getNet(), true);
				if(dev.getFamilyType().equals(FamilyType.VIRTEX5)){				
					String[] fanWireNames;
					if((fanWireNames = fanBounceMap.get(we.getWireName(ss.switchMatrixSink.wire))) != null){
						Node nn = new Node(inst.getTile(), we.getWireEnum(fanWireNames[0]), null, 0);
						for(String fanWireName : fanWireNames){
							nn.setWire(we.getWireEnum(fanWireName));
							boolean reservedWire = addReservedGNDVCCNode(nn, ss.pin);
							if(reservedWire){
								break;
							}
							if(fanWireName.equals(fanWireNames[fanWireNames.length - 1])){
								ps.useSLICE.add(ss);
								continue ssLoop;
							}
						}
					}
				}
				
				// Find the correct net corresponding to this TIEOFF if it exists
				Net matchingNet = null;
				for(Net net : inst.getNetList()){
					if(net.getType().equals(ss.pin.getNet().getType()) && !net.getSource().getName().equals("KEEP1")){
						matchingNet = net;
						break;
					}
				}
				if(matchingNet == null){
					matchingNet = createNewNet(ss.pin.getNet(), ss.pin);
					finalStaticNets.add(matchingNet);
					inst.addToNetList(matchingNet);
					createAndAddPin(matchingNet, inst, true);
				}
				else{
					matchingNet.addPin(ss.pin);
					ss.pin.getInstance().addToNetList(matchingNet);
				}
			}
			
			for(StaticSink ss : ps.attemptTIEOFF){
				Instance inst = updateTIEOFF(ss.switchMatrixSink.tile, ss.pin.getNet(), false);
				// Special case with CLK pins BRAMs on Virtex5 devices, when competing for FANs against GND Nets
				if(dev.getFamilyType().equals(FamilyType.VIRTEX5)){
					int switchBoxSink = ss.switchMatrixSink.wire;			
					
					if(we.getWireName(ss.switchMatrixSink.getWire()).startsWith("BYP_B")){
						Node nn = new Node(inst.getTile(), we.getWireEnum(we.getWireName(switchBoxSink).replace("_B","")), null, 0);
						if(!addReservedGNDVCCNode(nn, ss.pin)){
							// we need to use a SLICE 
							ps.useSLICE.add(ss);
							continue;
						}
					}
					else if(switchBoxSink == v5ctrlWires[0] || switchBoxSink == v5ctrlWires[1] || switchBoxSink == v5ctrlWires[2] || switchBoxSink == v5ctrlWires[3]){
						Node nn = new Node(inst.getTile(), we.getWireEnum(we.getWireName(switchBoxSink).replace("_B","")), null, 0);
						if(!addReservedGNDVCCNode(nn, ss.pin)){
							// we need to use a SLICE 
							ps.useSLICE.add(ss);
							continue;
						}
					}else if(ss.pin.getInstance().getPrimitiveSite().getType().equals(PrimitiveType.DSP48E) && ss.pin.getName().contains("CEP")){
						Node nn = new Node(inst.getTile(), we.getWireEnum("CTRL1"), null, 0);
						if(!addReservedGNDVCCNode(nn, ss.pin)){
							// we need to use a SLICE 
							ps.useSLICE.add(ss);
							continue;
						}
					}
					else if(ss.pin.getName().contains("ENBL")){
						Node nn = new Node(inst.getTile(), we.getWireEnum("CTRL2"), null, 0);
						if(!addReservedGNDVCCNode(nn, ss.pin)){
							// we need to use a SLICE 
							ps.useSLICE.add(ss);
							continue;
						}
					}
				}
				
				Net matchingNet = null;
				
				// Find the correct net corresponding to this TIEOFF if it exists
				for(Net net : inst.getNetList()){
					if(net.getType().equals(ss.pin.getNet().getType()) && !net.getSource().getName().equals("HARD1")){
						matchingNet = net;
						break;
					}
				}
				if(matchingNet == null){
					matchingNet = createNewNet(ss.pin.getNet(), ss.pin);
					finalStaticNets.add(matchingNet);
					inst.addToNetList(matchingNet);
					createAndAddPin(matchingNet, inst, false);
				}
				else{
					matchingNet.addPin(ss.pin);
					ss.pin.getInstance().addToNetList(matchingNet);
				}
			}
			
			if(ps.useSLICE.size() > 0){
				ArrayList<Pin> gnds = new ArrayList<>();
				ArrayList<Pin> vccs = new ArrayList<>();
				for(StaticSink ss : ps.useSLICE){
					if(ss.pin.getNet().getType().equals(NetType.GND)){
						gnds.add(ss.pin);
					}
					else if(ss.pin.getNet().getType().equals(NetType.VCC)){
						vccs.add(ss.pin);
					}
				}		
				
				if(gnds.size() > 0){
					// Create the new net
					Net newNet = createNewNet(NetType.GND, gnds);
					finalStaticNets.add(newNet);

					// Create new instance of SLICE primitive to get source
					Instance currInst = findClosestAvailableSLICE(ps.useSLICE.get(0).switchMatrixSink.tile, NetType.GND);
					assert currInst != null;
					if(currStaticSourcePin != null){
						currInst.addToNetList(newNet);
						newNet.addPin(currStaticSourcePin);
					}
					else{
						router.design.addInstance(currInst);
						currInst.addToNetList(newNet);
						Pin source = new Pin(true, slicePin, currInst); 
						newNet.addPin(source);					
					}
				}
				if(vccs.size() > 0){
					// Create the new net
					Net newNet = createNewNet(NetType.VCC, vccs);
					finalStaticNets.add(newNet);

					// Create new instance of SLICE primitive to get source
					Instance currInst = findClosestAvailableSLICE(ps.useSLICE.get(0).switchMatrixSink.tile, NetType.VCC);
					assert currInst != null;

					if(currStaticSourcePin != null){
						currInst.addToNetList(newNet);
						newNet.addPin(currStaticSourcePin);
					}
					else{
						router.design.addInstance(currInst);
						currInst.addToNetList(newNet);
						Pin source = new Pin(true, slicePin, currInst); 
						newNet.addPin(source);
					}
				}
			}
		}
		
		
		//===================================================================//
		// Step 4: Finalize node reservations and re-order and assemble nets 
		//         for router
		//===================================================================//
		for(Node n : reservedGNDVCCResources.keySet()){
			Pin pinToReserve = reservedGNDVCCResources.get(n);
			addReservedNode(n, pinToReserve.getNet());
		}	
		
		finalStaticNets = orderGNDNetsFirst(finalStaticNets);
		finalStaticNets.addAll(netList);
		router.netList = finalStaticNets;
	}
	
	/**
	 * Creates a new net based on the staticNet and will contain the newPinList
	 * @param type NetType to create new net from
	 * @param newPinList The new set of pins for the new net
	 * @return The newly created net
	 */
	private Net createNewNet(NetType type, ArrayList<Pin> newPinList){
		Net newNet = type.equals(NetType.VCC) ? new Net("GLOBAL_LOGIC1_" + netCount, type) : new Net("GLOBAL_LOGIC0_" + netCount, type) ;
		newNet.setPins(newPinList);
		netCount++;
		return newNet;
	}

	/**
	 * Creates a new net based on the staticNet and will contain the newPin
	 * @param staticNet Parent net to create new net from
	 * @param newPin The new pin of the new net
	 * @return The newly created net
	 */
	private Net createNewNet(Net staticNet, Pin newPin){
		Net newNet = new Net();
		newNet.addPin(newPin);
		newPin.getInstance().addToNetList(newNet);
		newNet.setName(staticNet.getName() + "_" + netCount);
		newNet.setType(staticNet.getType());
		netCount++;
		return newNet;
	}
	
	/**
	 * Helper method that creates and adds a pin to a net
	 * @param net The net to add the pin to.
	 * @param inst The instance the pin belongs to.
	 */
	private void createAndAddPin(Net net, Instance inst, boolean needHard1){
		String pinName;
		if(net.getType().equals(NetType.GND)){
			pinName = "HARD0";
		}
		else if(needHard1){
			pinName = "HARD1";
		}
		else{
			pinName = "KEEP1";
		}
		Pin source = new Pin(true,pinName, inst); 
		net.addPin(source);
		inst.addToNetList(net);
	}
		
	enum Direction{UP, DOWN, LEFT, RIGHT}
	
	/**
	 * Finds an available SLICE to be used as a static source.  
	 * @param tile The tile where the sink to be driven is located
	 * @return The newly created instance of the SLICE to source the sink
	 */
	private Instance findClosestAvailableSLICE(Tile tile, NetType sourceType){
		Direction dir = Direction.DOWN;
		int column = tile.getColumn();
		int row = tile.getRow();
		int maxColumn = column+1;
		int maxRow = row+1;
		int minColumn = column-1;
		int minRow = row;
		String srcTypeString = sourceType.equals(NetType.VCC) ? "_VCC_SOURCE" : "_GND_SOURCE";
		boolean isVirtex5 = dev.getFamilyType().equals(FamilyType.VIRTEX5);
		Tile currentTile;
		while(true){
			switch(dir){
				case UP:
					if(row == minRow){
						dir = Direction.RIGHT;
						minRow--;
						column++;
					}
					else{
						row--;
					}
					break;
				case DOWN:
					if(row == maxRow){
						dir = Direction.LEFT;
						maxRow++;
						column--;
					}
					else{
						row++;
					}
					break;
				case LEFT:
					if(column == minColumn){
						dir = Direction.UP;
						minColumn--;
						row--;
					}
					else{
						column--;
					}
					break;
				case RIGHT:
					if(column == maxColumn){
						dir = Direction.DOWN;
						maxColumn++;
						row++;
					}
					else{
						column++;
					}
					break;
			}
			currentTile = dev.getTile(row, column);
			if(currentTile != null && currentTile.getPrimitiveSites() != null){
				for(PrimitiveSite site : currentTile.getPrimitiveSites()){
					if(site.getType().equals(PrimitiveType.SLICEL) || site.getType().equals(PrimitiveType.SLICEM)){
						if(!router.design.getUsedPrimitiveSites().contains(site)){
							Instance returnMe = new Instance();
							HashMap<String, Attribute> attributeMap = new HashMap<>();
							attributeMap.put("_NO_USER_LOGIC", new Attribute("_NO_USER_LOGIC","",""));
							
							attributeMap.put(srcTypeString, new Attribute(srcTypeString,"",slicePin));
							
							returnMe.place(site);
							returnMe.setType(PrimitiveType.SLICEL);
							returnMe.setAttributes(attributeMap);
							returnMe.setName("XDL_DUMMY_" + returnMe.getTile() + "_" + site.getName());
							currStaticSourcePin = null;
							return returnMe;							
						}
						else if(isVirtex5){
							// Check all the LUTs in this slice
							Instance i = router.design.getInstanceAtPrimitiveSite(site);
							String[] letters = {"A","B","C","D"};
							for(String letter : letters){
								if(i.testAttributeValue(letter+"5LUT", "#OFF") &&
								   i.testAttributeValue(letter+"6LUT", "#OFF") &&
								   !i.hasAttribute("_GND_SOURCE") &&
								   !i.hasAttribute("_VCC_SOURCE")){
									i.addAttribute(new Attribute(new Attribute(srcTypeString,"",letter)));
									currStaticSourcePin = new Pin(true, letter, i);
									i.addPin(currStaticSourcePin);
									return i;
								}
							}
						}
					}	
				}
			}
		}
	}
	
	/**
	 * Creates or updates the appropriate TIEOFF to act as a source for a given net.
	 * @param net The net driven by the TIEOFF.
	 * @param needHard1 Determines if the source should be a HARD1.
	 * @return The created/updated TIEOFF.
	 */
	private Instance updateTIEOFF(Tile tile, Net net, boolean needHard1){
		String tileSuffix = tile.getTileNameSuffix();
		String instName = "XDL_DUMMY_INT" + tileSuffix + "_TIEOFF" + tileSuffix;
		Instance currInst = router.design.getInstance(instName);
		Attribute vccAttr = needHard1 ? hard1Attr : keep1Attr;
		// Add the appropriate attribute if instance already exists
		if(currInst != null){
			if(net.getType().equals(NetType.VCC)){
				// Add HARD1
				if(!currInst.hasAttribute(vccAttr.getPhysicalName())){
					currInst.addAttribute(vccAttr);
				}
			}
			else if(net.getType().equals(NetType.GND)){
				if(!currInst.hasAttribute(keep0Attr.getPhysicalName())){
					currInst.addAttribute(keep0Attr);
				}
			}
		}
		// Add the instance (it doesn't exist yet)
		else{
			currInst = new Instance();
			currInst.place(router.dev.getPrimitiveSite("TIEOFF" + tileSuffix));
			currInst.setType(PrimitiveType.TIEOFF);
			currInst.setName(instName);
			currInst.addAttribute(noUserLogicAttr);
			if(net.getType().equals(NetType.VCC)){
				// Add HARD1
				currInst.addAttribute(vccAttr);
			}
			else if(net.getType().equals(NetType.GND)){
				currInst.addAttribute(keep0Attr);
			}
			router.design.addInstance(currInst);
		}

		return currInst;
	}	
	// This static information is used to help remove routing conflicts
	static{
		fanBounceMap = new HashMap<>();
		String[] array0 = {"FAN2", "FAN7"};
		fanBounceMap.put("BYP_B0", array0);
		String[] array1 = {"FAN2", "FAN7"};
		fanBounceMap.put("BYP_B1", array1);
		String[] array2 = {"FAN5", "FAN0"};
		fanBounceMap.put("BYP_B2", array2);
		String[] array3 = {"FAN5", "FAN0"};
		fanBounceMap.put("BYP_B3", array3);
		String[] array4 = {"FAN2", "FAN7"};
		fanBounceMap.put("BYP_B4", array4);
		String[] array5 = {"FAN2", "FAN7"};
		fanBounceMap.put("BYP_B5", array5);
		String[] array6 = {"FAN5", "FAN0"};
		fanBounceMap.put("BYP_B6", array6);
		String[] array7 = {"FAN5", "FAN0"};
		fanBounceMap.put("BYP_B7", array7);
		String[] array8 = {"FAN4", "FAN1"};
		fanBounceMap.put("CLK_B0", array8);
		String[] array9 = {"FAN6", "FAN3"};
		fanBounceMap.put("CLK_B1", array9);
		String[] array10 = {"FAN4", "FAN1"};
		fanBounceMap.put("CTRL_B0", array10);
		String[] array11 = {"FAN4", "FAN1"};
		fanBounceMap.put("CTRL_B1", array11);
		String[] array12 = {"FAN6", "FAN3"};
		fanBounceMap.put("CTRL_B2", array12);
		String[] array13 = {"FAN6", "FAN3"};
		fanBounceMap.put("CTRL_B3", array13);
		String[] array14 = {"FAN4", "FAN1"};
		fanBounceMap.put("GFAN0", array14);
		String[] array15 = {"FAN6", "FAN3"};
		fanBounceMap.put("GFAN1", array15);
		String[] array16 = {"FAN2", "FAN7"};
		fanBounceMap.put("IMUX_B0", array16);
		String[] array17 = {"FAN4", "FAN2", "FAN7"};
		fanBounceMap.put("IMUX_B1", array17);
		String[] array18 = {"FAN5", "FAN3", "FAN0"};
		fanBounceMap.put("IMUX_B10", array18);
		String[] array19 = {"FAN5", "FAN0"};
		fanBounceMap.put("IMUX_B11", array19);
		String[] array20 = {"FAN2", "FAN7"};
		fanBounceMap.put("IMUX_B12", array20);
		String[] array21 = {"FAN4", "FAN2", "FAN7"};
		fanBounceMap.put("IMUX_B13", array21);
		String[] array22 = {"FAN6", "FAN4", "FAN1"};
		fanBounceMap.put("IMUX_B14", array22);
		String[] array23 = {"FAN6", "FAN3", "FAN1"};
		fanBounceMap.put("IMUX_B15", array23);
		String[] array24 = {"FAN5", "FAN3", "FAN0"};
		fanBounceMap.put("IMUX_B16", array24);
		String[] array25 = {"FAN5", "FAN0"};
		fanBounceMap.put("IMUX_B17", array25);
		String[] array26 = {"FAN2", "FAN7"};
		fanBounceMap.put("IMUX_B18", array26);
		String[] array27 = {"FAN4", "FAN2", "FAN7"};
		fanBounceMap.put("IMUX_B19", array27);
		String[] array28 = {"FAN6", "FAN4", "FAN1"};
		fanBounceMap.put("IMUX_B2", array28);
		String[] array29 = {"FAN6", "FAN4", "FAN1"};
		fanBounceMap.put("IMUX_B20", array29);
		String[] array30 = {"FAN6", "FAN3", "FAN1"};
		fanBounceMap.put("IMUX_B21", array30);
		String[] array31 = {"FAN5", "FAN3", "FAN0"};
		fanBounceMap.put("IMUX_B22", array31);
		String[] array32 = {"FAN5", "FAN0"};
		fanBounceMap.put("IMUX_B23", array32);
		String[] array33 = {"FAN2", "FAN7"};
		fanBounceMap.put("IMUX_B24", array33);
		String[] array34 = {"FAN4", "FAN2", "FAN7"};
		fanBounceMap.put("IMUX_B25", array34);
		String[] array35 = {"FAN6", "FAN4", "FAN1"};
		fanBounceMap.put("IMUX_B26", array35);
		String[] array36 = {"FAN6", "FAN3", "FAN1"};
		fanBounceMap.put("IMUX_B27", array36);
		String[] array37 = {"FAN5", "FAN3", "FAN0"};
		fanBounceMap.put("IMUX_B28", array37);
		String[] array38 = {"FAN5", "FAN0"};
		fanBounceMap.put("IMUX_B29", array38);
		String[] array39 = {"FAN6", "FAN3", "FAN1"};
		fanBounceMap.put("IMUX_B3", array39);
		String[] array40 = {"FAN2", "FAN7"};
		fanBounceMap.put("IMUX_B30", array40);
		String[] array41 = {"FAN4", "FAN2", "FAN7"};
		fanBounceMap.put("IMUX_B31", array41);
		String[] array42 = {"FAN6", "FAN4", "FAN1"};
		fanBounceMap.put("IMUX_B32", array42);
		String[] array43 = {"FAN6", "FAN3", "FAN1"};
		fanBounceMap.put("IMUX_B33", array43);
		String[] array44 = {"FAN5", "FAN3", "FAN0"};
		fanBounceMap.put("IMUX_B34", array44);
		String[] array45 = {"FAN5", "FAN0"};
		fanBounceMap.put("IMUX_B35", array45);
		String[] array46 = {"FAN2", "FAN7"};
		fanBounceMap.put("IMUX_B36", array46);
		String[] array47 = {"FAN4", "FAN2", "FAN7"};
		fanBounceMap.put("IMUX_B37", array47);
		String[] array48 = {"FAN6", "FAN4", "FAN1"};
		fanBounceMap.put("IMUX_B38", array48);
		String[] array49 = {"FAN6", "FAN3", "FAN1"};
		fanBounceMap.put("IMUX_B39", array49);
		String[] array50 = {"FAN5", "FAN3", "FAN0"};
		fanBounceMap.put("IMUX_B4", array50);
		String[] array51 = {"FAN5", "FAN3", "FAN0"};
		fanBounceMap.put("IMUX_B40", array51);
		String[] array52 = {"FAN5", "FAN0"};
		fanBounceMap.put("IMUX_B41", array52);
		String[] array53 = {"FAN2", "FAN7"};
		fanBounceMap.put("IMUX_B42", array53);
		String[] array54 = {"FAN4", "FAN2", "FAN7"};
		fanBounceMap.put("IMUX_B43", array54);
		String[] array55 = {"FAN6", "FAN4", "FAN1"};
		fanBounceMap.put("IMUX_B44", array55);
		String[] array56 = {"FAN6", "FAN3", "FAN1"};
		fanBounceMap.put("IMUX_B45", array56);
		String[] array57 = {"FAN5", "FAN3", "FAN0"};
		fanBounceMap.put("IMUX_B46", array57);
		String[] array58 = {"FAN5", "FAN0"};
		fanBounceMap.put("IMUX_B47", array58);
		String[] array59 = {"FAN5", "FAN0"};
		fanBounceMap.put("IMUX_B5", array59);
		String[] array60 = {"FAN2", "FAN7"};
		fanBounceMap.put("IMUX_B6", array60);
		String[] array61 = {"FAN4", "FAN2", "FAN7"};
		fanBounceMap.put("IMUX_B7", array61);
		String[] array62 = {"FAN6", "FAN4", "FAN1"};
		fanBounceMap.put("IMUX_B8", array62);
		String[] array63 = {"FAN6", "FAN3", "FAN1"};
		fanBounceMap.put("IMUX_B9", array63);
		String[] array64 = {"FAN0"};
		fanBounceMap.put("FAN_B0", array64);
		String[] array65 = {"FAN1"};
		fanBounceMap.put("FAN_B1", array65);
		String[] array66 = {"FAN2"};
		fanBounceMap.put("FAN_B2", array66);
		String[] array67 = {"FAN3"};
		fanBounceMap.put("FAN_B3", array67);
		String[] array68 = {"FAN4"};
		fanBounceMap.put("FAN_B4", array68);
		String[] array69 = {"FAN5"};
		fanBounceMap.put("FAN_B5", array69);
		String[] array70 = {"FAN6"};
		fanBounceMap.put("FAN_B6", array70);
		String[] array71 = {"FAN7"};
		fanBounceMap.put("FAN_B7", array71);
		
		v4BounceMap = new HashMap<>();
		v4BounceMap.put("SR_B0", "BOUNCE0");
		v4BounceMap.put("SR_B1", "BOUNCE0");
		v4BounceMap.put("SR_B2", "BOUNCE0");
		v4BounceMap.put("SR_B3", "BOUNCE0");
		
		v4BounceMap.put("CE_B0", "BOUNCE0");
		v4BounceMap.put("CE_B1", "BOUNCE0");
		v4BounceMap.put("CE_B2", "BOUNCE0");
		v4BounceMap.put("CE_B3", "BOUNCE0");
		
		v4BounceMap.put("BYP_INT_B0", "BOUNCE0");
		v4BounceMap.put("BYP_INT_B1", "BOUNCE2");
		v4BounceMap.put("BYP_INT_B2", "BOUNCE0");
		v4BounceMap.put("BYP_INT_B3", "BOUNCE2");
		v4BounceMap.put("BYP_INT_B4", "BOUNCE1");
		v4BounceMap.put("BYP_INT_B5", "BOUNCE3");
		v4BounceMap.put("BYP_INT_B6", "BOUNCE1");
		v4BounceMap.put("BYP_INT_B7", "BOUNCE3");
		v4BounceMap.put("IMUX_B0", "BOUNCE0");
		v4BounceMap.put("IMUX_B1", "BOUNCE1");
		v4BounceMap.put("IMUX_B10", "BOUNCE2");
		v4BounceMap.put("IMUX_B11", "BOUNCE3");
		v4BounceMap.put("IMUX_B12", "BOUNCE0");
		v4BounceMap.put("IMUX_B13", "BOUNCE1");
		v4BounceMap.put("IMUX_B14", "BOUNCE2");
		v4BounceMap.put("IMUX_B15", "BOUNCE3");
		v4BounceMap.put("IMUX_B16", "BOUNCE0");
		v4BounceMap.put("IMUX_B17", "BOUNCE1");
		v4BounceMap.put("IMUX_B18", "BOUNCE2");
		v4BounceMap.put("IMUX_B19", "BOUNCE3");
		v4BounceMap.put("IMUX_B2", "BOUNCE2");
		v4BounceMap.put("IMUX_B20", "BOUNCE0");
		v4BounceMap.put("IMUX_B21", "BOUNCE1");
		v4BounceMap.put("IMUX_B22", "BOUNCE2");
		v4BounceMap.put("IMUX_B23", "BOUNCE3");
		v4BounceMap.put("IMUX_B24", "BOUNCE0");
		v4BounceMap.put("IMUX_B25", "BOUNCE1");
		v4BounceMap.put("IMUX_B26", "BOUNCE2");
		v4BounceMap.put("IMUX_B27", "BOUNCE3");
		v4BounceMap.put("IMUX_B28", "BOUNCE0");
		v4BounceMap.put("IMUX_B29", "BOUNCE1");
		v4BounceMap.put("IMUX_B3", "BOUNCE3");
		v4BounceMap.put("IMUX_B30", "BOUNCE2");
		v4BounceMap.put("IMUX_B31", "BOUNCE3");
		v4BounceMap.put("IMUX_B4", "BOUNCE0");
		v4BounceMap.put("IMUX_B5", "BOUNCE1");
		v4BounceMap.put("IMUX_B6", "BOUNCE2");
		v4BounceMap.put("IMUX_B7", "BOUNCE3");
		v4BounceMap.put("IMUX_B8", "BOUNCE0");
		v4BounceMap.put("IMUX_B9", "BOUNCE1");
	}
}
