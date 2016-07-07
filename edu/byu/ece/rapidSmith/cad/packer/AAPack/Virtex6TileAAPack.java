package edu.byu.ece.rapidSmith.cad.packer.AAPack;

import edu.byu.ece.rapidSmith.cad.clusters.router.TableBasedRoutabilityCheckerFactory;
import edu.byu.ece.rapidSmith.cad.clusters.tileCluster.*;
import edu.byu.ece.rapidSmith.cad.packer.AAPack.packers.SingleCellPrepacker;
import edu.byu.ece.rapidSmith.cad.packer.AAPack.rules.*;
import edu.byu.ece.rapidSmith.design.Design;
import edu.byu.ece.rapidSmith.design.subsite.*;
import edu.byu.ece.rapidSmith.design.unpacker.XdlUnpacker;
import edu.byu.ece.rapidSmith.device.Device;
import edu.byu.ece.rapidSmith.device.creation.ExtendedDeviceInfo;
import edu.byu.ece.rapidSmith.cad.packer.AAPack.configurations.*;
import org.jdom2.JDOMException;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.Collections;

/**
 *
 */
public class Virtex6TileAAPack {
	public static AAPack<TileClusterType, TileClusterSite> getPacker(
			Device device, TileClusterDevice tcd, BelSelector belSelector,
			TileClusterFactory clusterFactory, CellLibrary cellLibrary
	) {
		AAPack<TileClusterType, TileClusterSite> packer = new AAPack<>(
				tcd, cellLibrary, clusterFactory,
				new TileClusterPackingCompletionUtils(cellLibrary, clusterFactory),
				Collections.singletonList(
						new SingleCellPrepacker()
				),
				new TileClusterCostCalculator(),
				new HighestInputPinCountSeedSelector(),
				new ExposedNetsCellSelector(),
				belSelector
		);
		packer.registerPackRule(new Mixing5And6LutsRuleFactory());
		packer.registerPackRule(new LutMemberConsistencyRuleFactory());
		packer.registerPackRule(new LUTRAMValidityChecksFactory(cellLibrary));
		packer.registerPackRule(new Carry4RequiredDI0LutSourcePackRuleFactory(cellLibrary));
		packer.registerPackRule(new MergeFFsWithSourceRuleFactory(cellLibrary));
		packer.registerPackRule(new CarryChainValidityRuleFactory());
		packer.registerPackRule(new CarryChainLookAheadRuleFactory());
		packer.registerPackRule(new SitePropertiesConsistencyRuleFactory());
		packer.registerPackRule(new RoutabilityCheckerPackRuleFactory(
				new TableBasedRoutabilityCheckerFactory(tcd), device));
//		packer.registerPackRule(new RoutabilityCheckerPackRuleFactory(
//				new PathFinderRoutabilityCheckerFactory(), device));
//		packer.registerPackRule(new RoutabilityComparingPackRuleFactory(
//				new TableBasedRoutabilityCheckerFactory(tcd),
//				new PathFinderRoutabilityCheckerFactory(),
//				device
//		));
		return packer;
	}

	public static void main(String[] args) throws JDOMException, IOException {
		CellLibrary cellLibrary;
		try {
			cellLibrary = new CellLibrary(Paths.get("devices", "virtex6", "defaultCellLibrary.xml"));
		} catch (IOException e) {
			return;
		}

		Design design = new Design(Paths.get("C:/ise_workspace/or64/or64_map.xdl"));
		Device device = design.getDevice();
		ExtendedDeviceInfo.loadExtendedInfo(device);
		XdlUnpacker unpacker = new XdlUnpacker(cellLibrary, Paths.get("devices", "virtex6", "unpack.xml"));
		CellDesign cellDesign = unpacker.unpack(design, false);
		cellDesign.unplaceDesign();

//		NonTimingDrivenTileAAPack.getPacker(design.getDevice(), cellLibrary).pack(device, cellDesign);
	}
}
