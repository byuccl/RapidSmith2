package edu.byu.ece.rapidSmith.cad;

import com.caucho.hessian.io.Hessian2Input;
import edu.byu.ece.rapidSmith.RapidSmithEnv;
import edu.byu.ece.rapidSmith.cad.clusters.tileCluster.*;
import edu.byu.ece.rapidSmith.cad.packer.AAPack.AAPack;
import edu.byu.ece.rapidSmith.cad.packer.AAPack.BelSelector;
import edu.byu.ece.rapidSmith.cad.packer.AAPack.Virtex6TileAAPack;
import edu.byu.ece.rapidSmith.cad.packer.AAPack.configurations.ShortestRouteBelSelector;
import edu.byu.ece.rapidSmith.cad.placer.annealer.SimulatedAnnealingPlacer;
import edu.byu.ece.rapidSmith.design.ClusterDesign;
import edu.byu.ece.rapidSmith.design.Design;
import edu.byu.ece.rapidSmith.design.subsite.*;
import edu.byu.ece.rapidSmith.design.unpacker.XdlPacker;
import edu.byu.ece.rapidSmith.design.unpacker.XdlUnpacker;
import edu.byu.ece.rapidSmith.device.*;
import edu.byu.ece.rapidSmith.device.creation.ExtendedDeviceInfo;
import edu.byu.ece.rapidSmith.util.FileTools;
import org.jdom2.JDOMException;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.stream.Collectors;

/**
 *
 */
public class CADFlow {

	public static void main(String[] args) throws JDOMException, IOException {
//		AAPack.logger.setLevel(Level.FINER);
//		AAPack.logger.setUseParentHandlers(false);
//		ConsoleHandler handler = new ConsoleHandler();
//		handler.setLevel(Level.ALL);
//		AAPack.logger.addHandler(handler);
//		AAPack.logger.addHandler(new FileHandler("C:\\myouput.xml"));

		RapidSmithEnv env = RapidSmithEnv.getDefaultEnv();
		CellLibrary cellLibrary;
		try {
			cellLibrary = new CellLibrary(Paths.get(
					"devices", "virtex6", "defaultCellLibrary_noperm.xml"));
		} catch (IOException e) {
			return;
		}

		Design design = new Design(Paths.get(args[0]));
		long seed = 0x12345678BEEFFFFFL;
		seed *= Integer.parseInt(args[1]);

		design.unrouteDesign();
		Device device = design.getDevice();
		ExtendedDeviceInfo.loadExtendedInfo(device);
		Path partFolderPath = env.getPartFolderPath(device.getFamilyType());
		Path unpackerFile = partFolderPath.resolve("unpack.xml");
		XdlUnpacker unpacker = new XdlUnpacker(cellLibrary, unpackerFile);
		CellDesign cellDesign = unpacker.unpack(design, false);
		cellDesign.unplaceDesign();

		Path templatePath = partFolderPath;
		templatePath = templatePath.resolve(device.getPartName() + "_template.dev");
		TileClusterDevice clusterTemplates;
		try {
			Hessian2Input input = FileTools.getCompactReader(templatePath);
			Object obj = input.readObject();
			clusterTemplates = (TileClusterDevice) obj;
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}

		TileClusterFactory factory =
				new TileClusterFactory(device, clusterTemplates);
		BelSelector selector = new ShortestRouteBelSelector(Paths.get(
				"devices", "virtex6", "belCosts.xml"));
		AAPack<TileClusterType, TileClusterSite> packer =
				Virtex6TileAAPack.getPacker(device, clusterTemplates, selector, factory, cellLibrary);
		ClusterDesign<TileClusterType, TileClusterSite> clusterDesign = packer.pack(cellDesign);

//		new RandomPlacer<TileClusterType, TileClusterSite, TileCluster>().place(clusterDesign, device);
		new SimulatedAnnealingPlacer<>(new TileClusterCoordinatesFactory(factory), seed,
				null, SimulatedAnnealingPlacer.PlacerEffortLevel.HIGH).place(clusterDesign, device);
		CellDesign placed = clusterDesign.flatten();
		packAndPlaceIO(placed, device);
		randomlyPlaceRemainingCells(placed, device);

		Path packerFile = Paths.get("C:\\work\\rapidsmith2\\devices\\virtex6\\pack.xml");
		XdlPacker xdlPacker = new XdlPacker(packerFile);
		Design design2 = xdlPacker.pack(placed);
		design2.saveXDLFile(getOutputPath(args[0]));
		System.out.println(design2);
	}

	private static Path getOutputPath(String arg) {
		arg = arg.substring(0, arg.lastIndexOf(".xdl"));
		arg += "_packed.xdl";
		return Paths.get(arg);
	}

	private static PrimitiveSite getIOSite(CellDesign cellDesign, Device device) {
		for (PrimitiveSite site : device.getAllCompatibleSites(PrimitiveType.IOB)) {
			if (site.getBondedType() == BondedType.BONDED && !cellDesign.isSiteUsed(site))
				return site;
		}
		throw new AssertionError("No more IO available");
	}

	private static void packAndPlaceIO(CellDesign design, Device device) {
		List<Cell> padCells = design.getCells().stream()
				.filter(c -> c.getLibCell().getName().equals("PAD"))
				.collect(Collectors.toList());

		for (Cell padCell : padCells) {
			// get cell anchor
			PrimitiveSite ioSite = getIOSite(design, device);

			CellPin padPin = padCell.getPin("PAD");
			if (!padPin.isConnectedToNet()) {
				design.placeCell(padCell, ioSite.getBel("PAD"));
				continue;
			}

			IOGroup ioGroup = buildIOGroup(padCell, padPin);
			placeGroup(design, padCell, ioSite, padPin, ioGroup);
		}
	}

	private static IOGroup buildIOGroup(Cell padCell, CellPin padPin) {
		IOGroup ioGroup = new IOGroup();
		ioGroup.padCell = padCell;

		Queue<Cell> cellQueue = new ArrayDeque<>();
		cellQueue.add(padCell);

		while (!cellQueue.isEmpty()) {
			Cell cell = cellQueue.poll();

			if (cell == ioGroup.padCell) {
				CellNet padNet = padPin.getNet();
				for (CellPin pin : padNet.getPins()) {
					Cell oCell = pin.getCell();
					switch (oCell.getLibCell().getName()) {
						case "PAD":
							assert pin == padPin;
							assert pin.getName().equals("PAD");
							break;
						case "PULL_OR_KEEP1":
							assert ioGroup.pullCell == null;
							assert pin.getName().equals("PAD");
							ioGroup.pullCell = oCell;
							cellQueue.add(oCell);
							break;
						case "IOB_INBUF":
							assert ioGroup.inbufCell == null;
							assert pin.getName().equals("PAD");
							ioGroup.inbufCell = oCell;
							cellQueue.add(oCell);
							break;
						case "IOB_OUTBUF":
							assert ioGroup.outbufCell == null;
							assert pin.getName().equals("OUT");
							ioGroup.outbufCell = oCell;
							cellQueue.add(oCell);
							break;
						case "IOBM_OUTBUF":
							throw new AssertionError("Not supported");
					}
				}
			} else if (cell == ioGroup.inbufCell) {
				CellPin outPin = cell.getPin("OUT");
				if (outPin.isConnectedToNet()) {
					CellNet outNet = outPin.getNet();
					for (CellPin pin : outNet.getPins()) {
						Cell oCell = pin.getCell();
						switch(oCell.getLibCell().getName()) {
							case "IDDR_FF":
								assert ioGroup.iddrff == null;
								assert pin.getName().equals("D");
								ioGroup.iddrff = oCell;
								break;
						}
					}

				}
			} else if (cell == ioGroup.outbufCell) {
				CellPin outPin = cell.getPin("IN");
				if (outPin.isConnectedToNet()) {
					CellNet outNet = outPin.getNet();
					for (CellPin pin : outNet.getPins()) {
						Cell oCell = pin.getCell();
						switch(oCell.getLibCell().getName()) {
							case "ODDR_FF":
								assert ioGroup.oddrff == null;
								assert pin.getName().equals("Q");
								ioGroup.oddrff = oCell;
								break;
						}
					}
				}
			}
		}

		return ioGroup;
	}

	private static void placeGroup(
			CellDesign design, Cell padCell, PrimitiveSite ioSite,
			CellPin padPin, IOGroup ioGroup
	) {
		ioSite.setType(PrimitiveType.IOB);
		design.placeCell(padCell, ioSite.getBel("PAD"));
		padCell.getPin("PAD").setBelPin("PAD");
		if (ioGroup.pullCell != null) {
			design.placeCell(ioGroup.pullCell, ioSite.getBel("PULL"));
			ioGroup.pullCell.getPin("PAD").setBelPin("PAD");
		}
		if (ioGroup.outbufCell != null) {
			design.placeCell(ioGroup.outbufCell, ioSite.getBel("OUTBUF"));
			CellPin inpin = ioGroup.outbufCell.getPin("IN");
			if (inpin.isConnectedToNet()) {
				inpin.setBelPin("IN");
				RouteTree rt = directRouteToSinks(
						ioSite.getSitePin("O").getInternalWire(),
						Collections.singletonList(inpin.getBelPin()));
				inpin.getNet().addRouteTree(rt);
			}
			CellPin triPin = ioGroup.outbufCell.getPin("TRI");
			if (triPin.isConnectedToNet()) {
				triPin.setBelPin("TRI");
				RouteTree rt = directRouteToSinks(
						ioSite.getSitePin("T").getInternalWire(),
						Collections.singletonList(triPin.getBelPin()));
				triPin.getNet().addRouteTree(rt);
			}
			ioGroup.outbufCell.getPin("OUT").setBelPin("OUT");
		}
		if (ioGroup.inbufCell != null) {
			design.placeCell(ioGroup.inbufCell, ioSite.getBel("INBUF"));
			ioGroup.inbufCell.getPin("PAD").setBelPin("PAD");
			CellPin outPin = ioGroup.inbufCell.getPin("OUT");
			if (outPin.isConnectedToNet()) {
				outPin.setBelPin("OUT");
				RouteTree rt = directRouteToSinks(
						outPin.getBelPin().getWire(),
						Collections.singletonList(
								ioSite.getSitePin("I")));
				outPin.getNet().addRouteTree(rt);
			}
			CellPin diffiInPin = ioGroup.inbufCell.getPin("DIFFI_IN");
			if (diffiInPin.isConnectedToNet()) {
				diffiInPin.setBelPin("DIFFI_IN");
				RouteTree rt = directRouteToSinks(
						ioSite.getSitePin("DIFFI_IN").getInternalWire(),
						Collections.singletonList(diffiInPin.getBelPin()));
				diffiInPin.getNet().addRouteTree(rt);
			}
		}

		if (ioGroup.outbufCell != null) {
			List<Object> sinks = new ArrayList<>();
			sinks.add(padPin.getBelPin());
			if (ioGroup.inbufCell != null) sinks.add(ioGroup.inbufCell.getPin("PAD").getBelPin());
			if (ioGroup.pullCell != null) sinks.add(ioGroup.pullCell.getPin("PAD").getBelPin());
			CellPin sourcePin = ioGroup.outbufCell.getPin("OUT");
			RouteTree rt = directRouteToSinks(sourcePin.getBelPin().getWire(), sinks);
			sourcePin.getNet().addRouteTree(rt);
		} else {
			List<Object> sinks = new ArrayList<>();
			CellNet padNet = ioGroup.padCell.getPin("PAD").getNet();
			if (ioGroup.inbufCell != null) sinks.add(ioGroup.inbufCell.getPin("PAD").getBelPin());
			if (ioGroup.pullCell != null) sinks.add(ioGroup.pullCell.getPin("PAD").getBelPin());
			RouteTree rt = directRouteToSinks(padPin.getBelPin().getWire(), sinks);
			padNet.addRouteTree(rt);
		}

		if (ioGroup.iddrff != null) {
			PrimitiveSite ilogic = findILogic(ioSite);
			Bel iff = ilogic.getBel("IFF");
			design.placeCell(ioGroup.iddrff, iff);
			connectPin(ioGroup.iddrff, "D", "D", "D");
			connectPin(ioGroup.iddrff, "CE", "CE", "CE1");
			connectPin(ioGroup.iddrff, "CK", "CK", "CLK");
			connectPin(ioGroup.iddrff, "CKB", "CKB", "CLKB");
			connectPin(ioGroup.iddrff, "SR", "SR", "SR");
			connectPin(ioGroup.iddrff, "Q1", "Q1", "Q1");
			connectPin(ioGroup.iddrff, "Q2", "Q2", "Q2");
		}

		if (ioGroup.oddrff != null) {
			PrimitiveSite ologic = findOLogic(ioSite);
			Bel outff = ologic.getBel("OUTFF");
			design.placeCell(ioGroup.oddrff, outff);
			connectPin(ioGroup.oddrff, "D1", "D1", "D1");
			connectPin(ioGroup.oddrff, "D2", "D2", "D2");
			connectPin(ioGroup.oddrff, "CE", "CE", "OCE");
			connectPin(ioGroup.oddrff, "CK", "CK", "CLK");
			connectPin(ioGroup.oddrff, "SR", "SR", "SR");
			connectPin(ioGroup.oddrff, "Q", "Q", "OQ");
		}
	}

	private static class IOGroup {
		Cell padCell, pullCell, outbufCell, inbufCell, iddrff, oddrff;
	}

	private static void connectPin(Cell cell, String cPin, String bPin, String sPin) {
		CellPin pin = cell.getPin(cPin);
		if (pin.isConnectedToNet()) {
			pin.setBelPin(bPin);
			PrimitiveSite site = cell.getAnchorSite();
			assert site != null;
			SitePin sitePin = site.getSitePin(sPin);
			if (sitePin.isOutput()) {
				List<Object> sinks = Collections.singletonList(sitePin);
				SiteWire source = pin.getBelPin().getWire();
				RouteTree rt = directRouteToSinks(source, sinks);
				pin.getNet().addRouteTree(rt);
			} else {
				SiteWire source = sitePin.getInternalWire();
				BelPin sink = pin.getBelPin();
				List<Object> sinks = Collections.singletonList(sink);
				RouteTree rt = directRouteToSinks(source, sinks);
				pin.getNet().addRouteTree(rt);
			}
		}
	}

	private static PrimitiveSite findILogic(PrimitiveSite iob) {
		SitePin iPin = iob.getSitePin("I");
		Queue<Wire> queue = new ArrayDeque<>();
		queue.add(iPin.getExternalWire());

		while(!queue.isEmpty()) {
			Wire wire = queue.poll();
			for (Connection c : wire.getPinConnections()) {
				PrimitiveSite site = c.getSitePin().getSite();
				if (contains(site.getPossibleTypes(), PrimitiveType.ILOGICE1)) {
					site.setType(PrimitiveType.ILOGICE1);
					return site;
				}
			}

			for (Connection c : wire.getWireConnections()) {
				if (!c.isRouteThrough()) {
					queue.add(c.getSinkWire());
				}
			}
		}
		throw new AssertionError();
	}

	private static PrimitiveSite findOLogic(PrimitiveSite iob) {
		SitePin iPin = iob.getSitePin("O");
		Queue<Wire> queue = new ArrayDeque<>();
		queue.add(iPin.getExternalWire());

		while(!queue.isEmpty()) {
			Wire wire = queue.poll();
			for (Connection c : wire.getReversePinConnections()) {
				PrimitiveSite site = c.getSitePin().getSite();
				if (contains(site.getPossibleTypes(), PrimitiveType.OLOGICE1)) {
					site.setType(PrimitiveType.OLOGICE1);
					return site;
				}
			}

			for (Connection c : wire.getReverseWireConnections()) {
				if (!c.isRouteThrough()) {
					queue.add(c.getSinkWire());
				}
			}
		}
		throw new AssertionError();
	}

	private static boolean contains(Object[] array, Object obj) {
		for (Object o : array) {
			if (Objects.equals(o, obj))
				return true;
		}
		return false;
	}

	private static void randomlyPlaceRemainingCells(CellDesign design, Device device) {
		List<Cell> unplacedCells = design.getCells().stream()
				.filter(c -> !c.isPlaced())
				.collect(Collectors.toList());
		for (Cell unplaced : unplacedCells) {
			Bel location = getRandomLocation(unplaced, design, device);
			design.placeCell(unplaced, location);
			for (CellPin pin : unplaced.getPins()) {
				if (pin.isConnectedToNet()) {
					pin.setBelPin(pin.getPossibleBelPins().get(0));
				}
			}
		}
	}

	private static Bel getRandomLocation(Cell cell, CellDesign design, Device device) {
		List<BelId> anchors = cell.getPossibleAnchors();
		for (BelId anchor : anchors) {
			PrimitiveType siteType = anchor.getPrimitiveType();
			PrimitiveSite[] sites = device.getAllCompatibleSites(siteType);
			if (sites == null)
				continue;
			for (PrimitiveSite site : sites) {
				if (design.isSiteUsed(site) && site.getType() != siteType)
					continue;

				site.setType(siteType);
				Bel bel = site.getBel(anchor);
				if (!design.isBelUsed(bel)) {
					return bel;
				}
			}
		}
		throw new AssertionError("No remaining BELs for cell " + cell.getName());
	}

	private static RouteTree directRouteToSinks(Wire source, List<Object> sinks) {
		sinks = new ArrayList<>(sinks);

		RouteTree sourceTree = new RouteTree(source);
		Queue<RouteTree> q = new ArrayDeque<>();
		Set<Wire> visited = new HashSet<>();
		q.add(sourceTree);

		Set<RouteTree> terminals = new HashSet<>();

		while (!q.isEmpty() && !sinks.isEmpty()) {
			RouteTree rt = q.poll();
			Wire wire = rt.getWire();

			if (!visited.add(wire))
				continue;

			for (Connection c : wire.getWireConnections()) {
				q.add(rt.addConnection(c));
			}
			for (Connection c : wire.getPinConnections()) {
				if (sinks.contains(c.getSitePin())) {
					RouteTree sinkTree = rt.addConnection(c);
					terminals.add(sinkTree);
					sinks.remove(c.getSitePin());
				}
			}
			for (Connection c : wire.getTerminals()) {
				if (sinks.contains(c.getBelPin())) {
					RouteTree sinkTree = rt.addConnection(c);
					terminals.add(sinkTree);
					sinks.remove(c.getBelPin());
				}
			}
		}

		sourceTree.prune(terminals);
		return sourceTree;
	}
}
