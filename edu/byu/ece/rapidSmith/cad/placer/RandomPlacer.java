package edu.byu.ece.rapidSmith.cad.placer;

import com.caucho.hessian.io.Hessian2Input;
import edu.byu.ece.rapidSmith.RapidSmithEnv;
import edu.byu.ece.rapidSmith.cad.clusters.Cluster;
import edu.byu.ece.rapidSmith.cad.clusters.ClusterSite;
import edu.byu.ece.rapidSmith.cad.clusters.ClusterType;
import edu.byu.ece.rapidSmith.cad.clusters.tileCluster.*;
import edu.byu.ece.rapidSmith.cad.packer.AAPack.*;
import edu.byu.ece.rapidSmith.cad.placer.annealer.SimulatedAnnealingPlacer;
import edu.byu.ece.rapidSmith.design.ClusterDesign;
import edu.byu.ece.rapidSmith.design.Design;
import edu.byu.ece.rapidSmith.design.subsite.*;
import edu.byu.ece.rapidSmith.design.unpacker.XdlUnpacker;
import edu.byu.ece.rapidSmith.design.unpacker.XdlPacker;
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
public class RandomPlacer<CTYPE extends ClusterType, CSITE extends ClusterSite>
		implements Placer<CTYPE, CSITE>
{
	private Random rand = new Random();
	private ClusterDesign<CTYPE, CSITE> design;

	@Override
	public boolean place(ClusterDesign<CTYPE, CSITE> design, Device device) {
		this.design = design;
		for (Cluster<CTYPE, CSITE> cluster : design.getClusters()) {
 			LinkedList<CSITE> candidates = new LinkedList<>();

			while (!candidates.isEmpty()) {
				int i = rand.nextInt(candidates.size());
				CSITE site = candidates.remove(i);
				if (tryPlace(cluster, site))
					break;
			}
			if (cluster.getPlacement() == null)
				return false;
		}
		return true;
	}

	private boolean tryPlace(Cluster<CTYPE, CSITE> cluster, CSITE site) {
		System.out.println("cluster " + cluster.getName());
		if (design.isLocationUsed(site))
			return false;
		design.placeCluster(cluster, site);

		// Any other checking here
		return true;
	}


}
