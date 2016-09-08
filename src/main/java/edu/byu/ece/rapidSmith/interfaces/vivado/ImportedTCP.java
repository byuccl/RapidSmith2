package edu.byu.ece.rapidSmith.interfaces.vivado;

import edu.byu.ece.rapidSmith.design.subsite.CellDesign;
import edu.byu.ece.rapidSmith.design.subsite.CellLibrary;
import edu.byu.ece.rapidSmith.device.Device;

public class ImportedTCP {
	
	private CellLibrary libCells;
	private Device device;
	private CellDesign design;
	private String partName;

	public ImportedTCP(String partName, CellDesign design, Device device, CellLibrary libCells) {
		this.partName = partName;
		this.design = design;
		this.device = device;
		this.libCells = libCells;
	}
	
	public Device getDevice() {
		return device;
	}
	public void setDevice(Device device) {
		this.device = device;
	}
	public CellLibrary getLibCells() {
		return libCells;
	}
	public void setLibCells(CellLibrary libCells) {
		this.libCells = libCells;
	}
	public CellDesign getDesign() {
		return design;
	}
	public void setDesign(CellDesign design) {
		this.design = design;
	}
	public String getPartName() {
		return partName;
	}
	public void setPartName(String partName) {
		this.partName = partName;
	}
}
