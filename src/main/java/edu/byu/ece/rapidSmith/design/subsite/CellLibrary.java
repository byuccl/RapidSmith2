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
import java.nio.file.Path;
import java.util.*;

/**
 *  Contains a set of cells for a design.
 */
public class CellLibrary implements Iterable<LibraryCell> {
	private final Map<String, LibraryCell> library;
	private LibraryCell vccSource;
	private LibraryCell gndSource;
	private FamilyType familyType; 

	public CellLibrary() {
		this.library = new HashMap<>();
	}

	public CellLibrary(Path filePath) throws IOException, JDOMException {
		this.library = new HashMap<>();
		loadFromFile(filePath);
	}

	private void loadFromFile(Path filePath) throws IOException, JDOMException {
		SAXBuilder builder = new SAXBuilder();
		Document doc;
		doc = builder.build(filePath.toFile());

		// get the family of the cell library.
		readFamilyType(doc.getRootElement().getChild("family"));
		
		Element cellsEl = doc.getRootElement().getChild("cells");
		Map<SiteType, Map<String, SiteProperty>> sitePropertiesMap = new HashMap<>();
		for (Element cellEl : cellsEl.getChildren("cell")) {
			loadCellFromXml(cellEl, sitePropertiesMap);
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

		loadPinsFromXml(cellEl, libCell);

		loadPossibleBelsFromXml(libCell, cellEl, sitePropertiesMap);
		add(libCell);
	}

	private void loadPinsFromXml(Element cellEl, SimpleLibraryCell libCell) {
		List<LibraryPin> pins = new ArrayList<>();
		Element pinsEl = cellEl.getChild("pins");
		for (Element pinEl : pinsEl.getChildren("pin")) {
			LibraryPin pin = new LibraryPin();
			pin.setLibraryCell(libCell);
			pin.setName(pinEl.getChildText("name"));
			String pinDirection = pinEl.getChildText("direction");
			switch (pinDirection) {
				case "input": pin.setDirection(PinDirection.IN); break;
				case "output": pin.setDirection(PinDirection.OUT); break;
				case "inout": pin.setDirection(PinDirection.INOUT); break;
				default: assert false : "unrecognized pin direction";
			}
			String pinType = pinEl.getChildText("type");
			pin.setPinType(pinType == null ? CellPinType.DATA : CellPinType.valueOf(pinType));
			
			pins.add(pin);
		}
		libCell.setLibraryPins(pins);
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

	public LibraryCell getVccSource() {
		return vccSource;
	}

	public LibraryCell getGndSource() {
		return gndSource;
	}

	public boolean contains(String cellName) {
		return library.containsKey(cellName);
	}

	public LibraryCell get(String cellName) {
		return library.get(cellName);
	}

	public LibraryCell add(LibraryCell libraryCell) {
		return library.put(libraryCell.getName(), libraryCell);
	}

	public void addAll(Collection<LibraryCell> libraryCells) {
		for (LibraryCell cell : libraryCells) {
			library.put(cell.getName(), cell);
		}
	}

	public void remove(String cellName) {
		library.remove(cellName);
	}

	public int size() {
		return library.size();
	}

	public Collection<LibraryCell> getAll() {
		return library.values();
	}

	@Override
	public Iterator<LibraryCell> iterator() {
		return library.values().iterator();
	}
}
