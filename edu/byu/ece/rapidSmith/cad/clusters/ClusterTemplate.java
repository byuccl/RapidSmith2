package edu.byu.ece.rapidSmith.cad.clusters;

import edu.byu.ece.rapidSmith.design.subsite.Wire;
import edu.byu.ece.rapidSmith.device.Bel;
import edu.byu.ece.rapidSmith.device.BelPin;
import edu.byu.ece.rapidSmith.device.PrimitiveSite;

import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 *
 */
public interface ClusterTemplate<CTYPE extends ClusterType> {
	CTYPE getType();
	int getNumBelsAvailable();

	Set<BelPin> getVccSources();

	Set<BelPin> getGndSources();

	List<DirectConnection> getDirectSourcesOfCluster();

	List<DirectConnection> getDirectSinksOfCluster();

	Collection<ClusterConnection> getSinksOfSource(BelPin belPin);

	Collection<ClusterConnection> getSourcesOfSink(BelPin sinkBelPin);

	Collection<Bel> getBels();

	List<Wire> getOutputs();

	List<Wire> getInputsOfSink(BelPin sinkPin);

	Set<Wire> getInputs();

	PinGroup getPinGroup(BelPin pin);

	Collection<PinGroup> getPinGroups();

	Wire getWire(PrimitiveSite site, String name);
}
