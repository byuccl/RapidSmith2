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

package edu.byu.ece.rapidSmith.design.subsite;

import edu.byu.ece.rapidSmith.device.BelId;
import edu.byu.ece.rapidSmith.device.FamilyType;
import edu.byu.ece.rapidSmith.device.PinDirection;
import edu.byu.ece.rapidSmith.device.SiteType;
import edu.byu.ece.rapidSmith.util.Exceptions;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 *  Contains a set of cells for a design.
 */
public class CellLibrary implements Iterable<LibraryCell> {
	private final Map<String, LibraryCell> library;
	private LibraryCell vccSource;
	private LibraryCell gndSource;
	private FamilyType familyType; 

	/**
	 * Creates a new Cell Library object
	 */
	public CellLibrary() {
		this.library = new HashMap<>();
	}

	/**
	 * Creates a new cell library object, and populates it with the
	 * XML cell library found at the specified path.
	 * 
	 * @param filePath Path to the cell library XML file.
	 * @throws IOException
	 */
	public CellLibrary(Path filePath) throws IOException {
		this.library = new HashMap<>();
		
		try {
			loadFromFile(filePath);
		} catch (JDOMException e) {
			// wrap the JDOMException in a generic parse exception
			throw new Exceptions.ParseException(e);
		}
	}

	/**
	 * Creates a new cell library object, and populates it with the
	 * XML cell library contents found in istream.
	 *
	 * @param istream {@link InputStream} containing the library XML contents.
	 * @throws IOException
	 */
	public CellLibrary(InputStream istream) throws IOException {
		this.library = new HashMap<>();

		try {
			loadFromStream(istream);
		} catch (JDOMException e) {
			// wrap the JDOMException in a generic parse exception
			throw new Exceptions.ParseException(e);
		}
	}

	/**
	 * Parses an XML file which represents MACRO cell objects, creates corresponding
	 * {@link LibraryMacro} library cells in RapidSmith, and adds them the current
	 * cell library. This function can be used to augment the default {@link CellLibrary}
	 * with additional cells.
	 * 
	 * @param macroXmlPath {@link Path} to the XML file
	 */
	public void loadMacroXML(Path macroXmlPath) throws IOException {
		SAXBuilder builder = new SAXBuilder();
		Document doc;
		try {
			doc = builder.build(macroXmlPath.toFile());
		} catch (JDOMException e) {
			throw new Exceptions.ParseException(e);
		}
		loadMacros(doc);
	}

	/**
	 * Parses an XML stream which represents MACRO cell objects, creates corresponding
	 * {@link LibraryMacro} library cells in RapidSmith, and adds them the current
	 * cell library. This function can be used to augment the default {@link CellLibrary}
	 * with additional cells.
	 *
	 * @param macroXmlStream {@link InputStream} to the XML file
	 */
	public void loadMacroXML(InputStream macroXmlStream) throws IOException {
		SAXBuilder builder = new SAXBuilder();
		Document doc;
		try {
			doc = builder.build(macroXmlStream);
		} catch (JDOMException e) {
			throw new Exceptions.ParseException(e);
		}
		loadMacros(doc);
	}

	private void loadMacros(Document doc) {
		// Load all macro library cells into the cell library
		Element macrosEl = doc.getRootElement().getChild("macros");

		List<Element> childrenMacros = macrosEl.getChildren("macro");

		if (childrenMacros != null) {
			for (Element macroEl : childrenMacros) {
				loadMacroFromXml(macroEl);
			}
		}
	}

	private void loadFromFile(Path filePath) throws IOException, JDOMException {
		SAXBuilder builder = new SAXBuilder();
		Document doc = builder.build(filePath.toFile());
		loadFromDoc(doc);
	}

	private void loadFromStream(InputStream is) throws JDOMException, IOException {
		SAXBuilder builder = new SAXBuilder();
		Document doc = builder.build(is);
		loadFromDoc(doc);
	}

	private void loadFromDoc(Document doc) {
		// get the family of the cell library.
		readFamilyType(doc.getRootElement().getChild("family"));

		Element cellsEl = doc.getRootElement().getChild("cells");
		Map<SiteType, Map<String, SiteProperty>> sitePropertiesMap = new HashMap<>();
		// first load the leaf cells
		for (Element cellEl : cellsEl.getChildren("cell")) {
			loadCellFromXml(cellEl, sitePropertiesMap);
		}
		// then load the macro cells if any exist
		Element macrosEl = doc.getRootElement().getChild("macros");

		if (macrosEl != null) {
			List<Element> childrenMacros = macrosEl.getChildren("macro");
			if (childrenMacros != null) {
				for (Element macroEl : childrenMacros) {
					loadMacroFromXml(macroEl);
				}
			}
		}
	}

	/**
	 * Reads the "family" tag from the cellLibrary.xml file and stores its value.
	 * This tag is necessary to get handles to the proper site type later on in the parsing
	 * process.
	 * @param familyEl family element in the cellLibray.xml
	 */
	private void readFamilyType(Element familyEl) {
		
		if (familyEl == null) {
			// TODO: replace this exception with the proper exception. 
			throw new Exceptions.FileFormatException("<family> tag not found in cellLibrary.xml file");
		}
		
		this.familyType = FamilyType.valueOf(familyEl.getValue()); 
	}
	

	private void loadCellFromXml(
			Element cellEl, Map<SiteType, Map<String, SiteProperty>> sitePropertiesMap
	) {
		String type = cellEl.getChildText("type");
		SimpleLibraryCell libCell = new SimpleLibraryCell(type);
		libCell.setVccSource(cellEl.getChild("vcc_source") != null);
		libCell.setGndSource(cellEl.getChild("gnd_source") != null);
		libCell.setIsPort(cellEl.getChild("is_port") != null); 
		Element lutType = cellEl.getChild("is_lut");
		if (lutType != null) {
			String strInputs = lutType.getChildText("num_inputs");
			libCell.setNumLutInputs(Integer.parseInt(strInputs));
		}

		if (libCell.isVccSource())
			vccSource = libCell;
		if (libCell.isGndSource())
			gndSource = libCell;

		loadConfigurationPropertiesFromXml(cellEl, libCell);
		loadPinsFromXml(cellEl, libCell);
		loadPossibleBelsFromXml(libCell, cellEl, sitePropertiesMap);
		add(libCell);
	}

	/*
	 * Loads the configuration properties found in the cell library XML and 
	 * applies them to the library cell. The properties look like the following: 
	 * 
	 * <libcellproperties>
	 *   <libcellproperty>
	 *	   <name>INIT</name>
	 *	   <default>0x8'h00</default>
	 *	   <max>0x8'hFF</max>
	 *	   <min>0x8'h00</min>
	 *	   <type>hex</type>
	 *	   <values>min=Ox8'h00, max=Ox8'hFF</values>
	 *   </libcellproperty>
	 * <libcellproperties>
	 */
	private void loadConfigurationPropertiesFromXml(Element cellEl, LibraryCell libCell) { 
		Element properties = cellEl.getChild("libcellproperties");
		
		if (properties != null) {
			for (Element propertyEl : properties.getChildren("libcellproperty")) {

				boolean isReadonly = propertyEl.getChild("readonly") != null;
				// for now, skip read only properties
				// TODO: revisit this
				if (isReadonly) {
					continue;
				}
				
				String name = propertyEl.getChildText("name");
				String deflt = propertyEl.getChildText("default");
				// TODO: integrate the min and max properties
				// String max = propertyEl.getChildText("max");
				// String min = propertyEl.getChildText("min");
				String type = propertyEl.getChildText("type");
				String valueString = propertyEl.getChildText("values");
				String[] values = valueString.isEmpty() ? new String[0] : valueString.split(", ");
				
				// add the configuration to the library cell
				libCell.addDefaultProperty(new Property(name, PropertyType.EDIF, deflt));						
				libCell.addConfigurableProperty(new LibraryCellProperty(name, type, values, isReadonly));
			}
		}
	}
	
	private void loadMacroFromXml(Element macroEl) {
		
		String type = macroEl.getChildText("type");
		LibraryMacro macroCell = new LibraryMacro(type);

		loadInternalCellsFromXml(macroEl, macroCell);
		loadPinsFromXml(macroEl, macroCell);
		loadInternalNetsFromXml(macroEl, macroCell);
		add(macroCell);
	}
	
	private void loadPinsFromXml(Element cellEl, LibraryCell libCell) {
		List<LibraryPin> pins = new ArrayList<>();
		Element pinsEl = cellEl.getChild("pins");
		
		Pattern pinNamePattern = Pattern.compile("(.*)\\[(.*)\\]");
		
		for (Element pinEl : pinsEl.getChildren("pin")) {
			LibraryPin pin = new LibraryPin();
			pin.setLibraryCell(libCell);
			pin.setName(pinEl.getChildText("name"));
			String pinDirection = pinEl.getChildText("direction");
			switch (pinDirection) {
				case "input": pin.setDirection(PinDirection.IN); break;
				case "output": pin.setDirection(PinDirection.OUT); break;
				case "inout": pin.setDirection(PinDirection.INOUT); break;
				default: throw new Exceptions.ParseException("Unrecognized pin direction while parsing the CellLibrary.xml file: " + pinDirection);
			}
			String pinType = pinEl.getChildText("type");
			pin.setPinType(pinType == null ? CellPinType.DATA : CellPinType.valueOf(pinType));
			pins.add(pin);
			
			// for macro cells, add the internal connection information
			if (libCell.isMacro()) {
				Matcher m = pinNamePattern.matcher(pin.getName());
				
				if (m.matches()) {
					((LibraryMacro)libCell).addPinOffset(m.group(1), Integer.parseInt(m.group(2)));
				}
				
				List<String> internalPinNames = pinEl.getChild("internalConnections")
													.getChildren("pinname")
													.stream().map(el -> el.getText())
													.collect(Collectors.toList());
				((LibraryMacro)libCell).addInternalPinConnections(pin, internalPinNames);
			}
		}
		libCell.setLibraryPins(pins);
	}
	
	private void loadInternalCellsFromXml(Element macroEl, LibraryMacro macroCell) {
		
		Element cellsEl = macroEl.getChild("cells");
		
		for (Element internalEl : cellsEl.getChildren("internal")) {
			LibraryCell libCell = library.get(internalEl.getChildText("type"));
			
			if (libCell == null) {
				throw new Exceptions.ParseException("Unable to find leaf library cell \"" + internalEl.getChildText("type") + 
												 "\" in macro cell: \"" + macroEl.getChildText("type") + "\"");
			}
			else if (libCell.isMacro()) {
				throw new Exceptions.ParseException("Nested hierarchy is not supported. Cell: \"" + macroEl.getChildText("type") + "\"");
			}
			
			macroCell.addInternalCell(internalEl.getChildText("name"), (SimpleLibraryCell)libCell);
		}
	}
	
	private void loadInternalNetsFromXml(Element macroEl, LibraryMacro macroCell) {
		Element internalNetsEl = macroEl.getChild("internalNets");
		
		// only add internal nets to the macro if they exist
		if (internalNetsEl != null) {
			
			for (Element internalNetEl : internalNetsEl.getChildren("internalNet")) {
				String name = internalNetEl.getChildText("name");
				List<String> pinNames = internalNetEl.getChild("pins")
													.getChildren("pinname")
													.stream().map(el -> el.getText())
													.collect(Collectors.toList());
				String type = internalNetEl.getChildText("type");
				if (type == null) {
					type = "WIRE";
				}
				
				macroCell.addInternalNet(name, type, pinNames);
			}
		}
	}

	private void loadPossibleBelsFromXml(
			SimpleLibraryCell libCell, Element cellEl,
			Map<SiteType, Map<String, SiteProperty>> sitePropertiesMap
	) {
		List<BelId> compatibleBels = new ArrayList<>();
		Map<BelId, Map<String, SiteProperty>> sharedSitePropertiesMap = new HashMap<>();
		Element belsEl = cellEl.getChild("bels");
		for (Element belEl : belsEl.getChildren("bel")) {
			Element id = belEl.getChild("id");
			
			String site_type = id.getChildText("site_type");
			BelId belId = new BelId(
					SiteType.valueOf(familyType, site_type),
					id.getChildText("name")
			);
			compatibleBels.add(belId);

			loadPinMapFromXml(libCell, belEl, belId);

			Map<String, SiteProperty> siteProperties = sitePropertiesMap.computeIfAbsent(
					belId.getSiteType(), k -> new HashMap<>());
			Map<String, SiteProperty> sharedSiteProperties = new HashMap<>();
			sharedSitePropertiesMap.put(belId, sharedSiteProperties);
			Element attrsEl = belEl.getChild("attributes");
			if (attrsEl != null) {
				for (Element attrEl : attrsEl.getChildren("attribute")) {
					if (attrEl.getChild("is_site_property") != null) {
						String attrName = attrEl.getChildText("name");
						String rename = attrEl.getChildText("rename");
						if (rename == null)
							rename = attrName;
						SiteProperty siteProperty = siteProperties.computeIfAbsent(
								rename, k -> new SiteProperty(belId.getSiteType(), k));
						sharedSiteProperties.put(attrName, siteProperty);
					}
				}
			}
		}
		libCell.setPossibleBels(compatibleBels);
		libCell.setSharedSiteProperties(sharedSitePropertiesMap);
	}

	private void loadPinMapFromXml(SimpleLibraryCell libCell, Element belEl, BelId belId) {
		Element belPinsEl = belEl.getChild("pins");
		if (belPinsEl == null) {
			for (LibraryPin pin : libCell.getLibraryPins()) {
				ArrayList<String> possPins = new ArrayList<>(1);
				possPins.add(pin.getName());
				pin.getPossibleBelPins().put(belId, possPins);
			}
		} else {
			Set<LibraryPin> unmappedPins = new HashSet<>();
			
			// Create the bel pin mappings for each cell pin
			for (Element belPinEl : belPinsEl.getChildren("pin")) {
				
				String pinName = belPinEl.getChildText("name");
				LibraryPin pin = libCell.getLibraryPin(pinName);
				
				// skip pins that have no default mapping
				if (belPinEl.getChild("no_map") != null) {
					unmappedPins.add(pin);
					continue;
				}
				
				ArrayList<String> possibles = new ArrayList<>();
				for (Element possibleEl : belPinEl.getChildren("possible")) {
					possibles.add(possibleEl.getText());
				}
				possibles.trimToSize();
				pin.getPossibleBelPins().put(belId, possibles);
			}
			
			// Look for cell pins without a bel pin mapping. For these
			// pins, assume a bel pin name that is identical to the cell pin name
			for (LibraryPin pin : libCell.getLibraryPins()) {
				if (pin.getPossibleBelPins(belId) != null || unmappedPins.contains(pin)) {
					continue;
				}
				ArrayList<String> possPins = new ArrayList<>(1);
				possPins.add(pin.getName());
				pin.getPossibleBelPins().put(belId, possPins);
			}
		}
	}

	/**
	 * Returns the library cell in the cell library that is a VCC source.
	 * Only one such cell should exist.
	 */
	public LibraryCell getVccSource() {
		return vccSource;
	}

	/**
	 * Returns the library cell in the cell library that is a GND source.
	 * Only one such cell should exist.
	 */
	public LibraryCell getGndSource() {
		return gndSource;
	}

	/**
	 * Returns {@code true} if the cell library contains a library cell
	 * with the specified name. Otherwise, {@code false} is returned.
	 * 
	 * @param cellName String name of a library cell (i.e. LUT6)
	 */
	public boolean contains(String cellName) {
		return library.containsKey(cellName);
	}

	/**
	 * Returns the {@link LibraryCell} in the cell library with the given
	 * name.
	 * 
	 * @param cellName String name of a library cell (i.e. LUT6)
	 */
	public LibraryCell get(String cellName) {
		return library.get(cellName);
	}

	/**
	 * Adds a new @link{LibraryCell} to the cell library.
	 * 
	 * @param libraryCell Library Cell to add (can be a macro cell or leaf cell)
	 * @return The previous library cell of the same name, or {@code null}
	 * 		if there is no previous library cell of the same name.
	 */
	public LibraryCell add(LibraryCell libraryCell) {
		return library.put(libraryCell.getName(), libraryCell);
	}

	/**
	 * Adds a collection of {@link LibraryCell}s to the cell library
	 * 
	 * @param libraryCells Collection of {@link LibraryCell}s
	 */
	public void addAll(Collection<LibraryCell> libraryCells) {
		for (LibraryCell cell : libraryCells) {
			library.put(cell.getName(), cell);
		}
	}
	
	/**
	 * Removes the {@link LibraryCell} with the specified name
	 * from the cell library.
	 * 
	 * @param cellName String name of a library cell.
	 */
	public void remove(String cellName) {
		library.remove(cellName);
	}

	/**
	 * Returns the number of {@link LibraryCell}s currently in the cell library.
	 */
	public int size() {
		return library.size();
	}

	/**
	 * Returns all {@link LibraryCell}s in the cell library as
	 * a generic collection.
	 */
	public Collection<LibraryCell> getAll() {
		return library.values();
	}

	/**
	 * Returns the Xilinx device family type that this cell library
	 * is valid for (i.e. Artix7, Ultrascale, etc.).
	 */
	public FamilyType getFamilyType() {
		return this.familyType;
	}
	
	/**
	 * Creates and returns an iterator for the cell library.
	 */
	@Override
	public Iterator<LibraryCell> iterator() {
		return library.values().iterator();
	}
}
