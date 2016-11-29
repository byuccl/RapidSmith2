package edu.byu.ece.rapidSmith.design;

import edu.byu.ece.rapidSmith.RSEnvironment;
import edu.byu.ece.rapidSmith.device.Device;
import edu.byu.ece.rapidSmith.device.SiteType;
import edu.byu.ece.rapidSmith.util.FamilyType;

import java.io.Serializable;
import java.util.*;

/**
 *
 */
public abstract class AbstractDesign implements Serializable {
	private static final long serialVersionUID = 6284690406230426968L;
	/**  Keeps track of all slice primitive types, initialized statically */
	public static Set<SiteType> sliceTypes;
	/**  Keeps track of all DSP48 primitive types, initialized statically */
	public static Set<SiteType> dspTypes;
	/**  Keeps track of all BRAM primitive types, initialized statically */
	public static Set<SiteType> bramTypes;
	/**  Keeps track of all IOB primitive types, initialized statically */
	public static Set<SiteType> iobTypes;
	/**  Name of the design */
	protected String name;
	// use partName instead of device here to allow speed grade to be specified
	/**  This is the Xilinx part, package and speed grade that this design targets */
	protected String partName;
	protected Device device;

	public AbstractDesign() {
		partName = null;
		name = null;
	}

	public AbstractDesign(String designName, String partName) {
		this();
		setName(designName);
		setPartName(partName);
	}

	/**
	 * Returns the name of this design.
	 *
	 * @return the name of this design
	 */
	public String getName() {
		return name;
	}

	/**
	 * Sets the name of this design
	 *
	 * @param name new name for this design
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * This will return the part name with speed grade of the part this design or
	 * hard macro targets (ex: xc4vsx35ff668-10).
	 *
	 * @return he part name with package and speed grade information
	 */
	public String getPartName() {
		return this.partName;
	}

	/**
	 * Sets the name of the part used for this design.
	 * The part name should include package and speed grade.  For example
	 * xc4vfx12ff668-10 is a valid part name.
	 *
	 * @param partName name of the FPGA part.
	 */
	public void setPartName(String partName) {
		this.partName = partName;
		this.device = RSEnvironment.defaultEnv().getDevice(partName);
	}

	public Device getDevice() {
		return device;
	}

	/**
	 * Returns the base family type this design targets.
	 * This ensures compatibility with all RapidSmith files. For differentiating
	 * family types (qvirtex4 rather than virtex4) use getExactFamilyType().
	 *
	 * @return the base family type of the part this design targets
	 */
	public FamilyType getFamily() {
		return getDevice().getFamily();
	}

	// TODO remove these, they should exist in the device now
	static {
		sliceTypes = new HashSet<>();
		sliceTypes.add(SiteType.SLICE);
		sliceTypes.add(SiteType.SLICEL);
		sliceTypes.add(SiteType.SLICEM);
		sliceTypes.add(SiteType.SLICEX);

		dspTypes = new HashSet<>();
		dspTypes.add(SiteType.DSP48);
		dspTypes.add(SiteType.DSP48A);
		dspTypes.add(SiteType.DSP48A1);
		dspTypes.add(SiteType.DSP48E);
		dspTypes.add(SiteType.DSP48E1);
		dspTypes.add(SiteType.MULT18X18);
		dspTypes.add(SiteType.MULT18X18SIO);

		bramTypes = new HashSet<>();
		bramTypes.add(SiteType.BLOCKRAM);
		bramTypes.add(SiteType.FIFO16);
		bramTypes.add(SiteType.FIFO18E1);
		bramTypes.add(SiteType.FIFO36_72_EXP);
		bramTypes.add(SiteType.FIFO36_EXP);
		bramTypes.add(SiteType.FIFO36E1);
		bramTypes.add(SiteType.RAMB16);
		bramTypes.add(SiteType.RAMB16BWE);
		bramTypes.add(SiteType.RAMB16BWER);
		bramTypes.add(SiteType.RAMB18E1);
		bramTypes.add(SiteType.RAMB18X2);
		bramTypes.add(SiteType.RAMB18X2SDP);
		bramTypes.add(SiteType.RAMB36_EXP);
		bramTypes.add(SiteType.RAMB36E1);
		bramTypes.add(SiteType.RAMB36SDP_EXP);
		bramTypes.add(SiteType.RAMB8BWER);
		bramTypes.add(SiteType.RAMBFIFO18);
		bramTypes.add(SiteType.RAMBFIFO18_36);
		bramTypes.add(SiteType.RAMBFIFO36);
		bramTypes.add(SiteType.RAMBFIFO36E1);

		iobTypes = new HashSet<>();
		iobTypes.add(SiteType.IOB);
		iobTypes.add(SiteType.IOB18);
		iobTypes.add(SiteType.IOB18M);
		iobTypes.add(SiteType.IOB18S);
		iobTypes.add(SiteType.IOB33);
		iobTypes.add(SiteType.IOB33M);
		iobTypes.add(SiteType.IOB33S);
		iobTypes.add(SiteType.IOB_USB);
		iobTypes.add(SiteType.IOBLR);
		iobTypes.add(SiteType.IOBM);
		iobTypes.add(SiteType.IOBS);
		iobTypes.add(SiteType.LOWCAPIOB);
	}

}
