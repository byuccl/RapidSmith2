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

package edu.byu.ece.rapidSmith.device.creation;

import edu.byu.ece.rapidSmith.device.PinDirection;
import edu.byu.ece.rapidSmith.device.SiteType;
import edu.byu.ece.rapidSmith.primitiveDefs.*;
import org.jdom2.Document;
import org.jdom2.Element;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import static edu.byu.ece.rapidSmith.util.Exceptions.FileFormatException;

/**
 *
 */
public class PrimitiveDefsCorrector {
	public static void makeCorrections(PrimitiveDefList defs, Document deviceInfo) {
		for (PrimitiveDef def : defs) {
			Element ptEl = getSiteTypeEl(deviceInfo, def.getType());
			Element correctionsEl = ptEl.getChild("corrections");
			if (correctionsEl == null) // no corrections for this primitive type
				continue;
			for (Element creationEl : correctionsEl.getChildren("new_element")) {
				createPrimitiveElement(def, creationEl);
			}
			for (Element modificationEl : correctionsEl.getChildren("modify_element")) {
				modifyPrimitiveElement(def, modificationEl);
			}
			for (Element psEl : correctionsEl.getChildren("polarity_selector")) {
				fixPolarityMux(def, psEl);
			}
			for (Element pinEl : correctionsEl.getChildren("pin_direction")) {
				modifyPinDirection(def, pinEl);
			}
		}
	}

	private static void fixPolarityMux(PrimitiveDef def, Element psEl) {
		PrimitiveElement el = def.getElement(psEl.getChildText("name"));
		if (el == null)
			assert false;

		PrimitiveElement sourceEl = null;
		String sourcePin = null;
		Map<PrimitiveElement, String> sinks = new HashMap<>();
		for (PrimitiveConnection c : el.getConnections()) {
			if (c.isForwardConnection()) {
				PrimitiveElement sinkEl = def.getElement(c.getElement1());
				String sinkPin = c.getPin1();
				assert !sinks.containsKey(sinkEl);
				sinks.put(sinkEl, sinkPin);
			} else {
				sourceEl = def.getElement(c.getElement1());
				sourcePin = c.getPin1();
			}
		}
		assert sinks.size() > 0;
		assert sourceEl != null;

		el.getPins().clear();
		el.setMux(false);
		el.setConfiguration(true);
		el.setConnections(null);

		String opinName = null;
		Iterator<PrimitiveConnection> it = sourceEl.getConnections().iterator();
		while(it.hasNext()) {
			PrimitiveConnection c = it.next();
			if (!c.isForwardConnection())
				continue;
			if (c.getElement1().equals(el.getName()) && !c.getPin1().endsWith("_B")) {
				opinName = c.getPin1();
				it.remove();
			}
		}
		assert opinName != null;
		for (Map.Entry<PrimitiveElement, String> sink : sinks.entrySet()) {
			PrimitiveConnection c = new PrimitiveConnection();
			c.setElement0(sourceEl.getName());
			c.setPin0(opinName);
			c.setForwardConnection(true);
			c.setElement1(sink.getKey().getName());
			c.setPin1(sink.getValue());
			sourceEl.addConnection(c);
		}

		for (PrimitiveElement sinkEl : sinks.keySet()) {
			it = sinkEl.getConnections().iterator();
			while(it.hasNext()) {
				PrimitiveConnection c = it.next();
				if (c.isForwardConnection())
					continue;
				if (c.getElement1().equals(el.getName())) {
					c.setElement1(sourceEl.getName());
					c.setPin1(sourcePin);
				}
			}
		}
	}

	private static void modifyPrimitiveElement(PrimitiveDef def, Element modificationEl) {
		PrimitiveElement element = def.getElement(modificationEl.getChildText("name"));
		if (element == null)
			assert false;

		Element typeEl = modificationEl.getChild("type");
		if (typeEl != null) {
			element.setMux(false);
			element.setBel(false);
			element.setConfiguration(false);
			element.setPin(false);

			switch (typeEl.getText()) {
				case "mux": element.setMux(true); break;
				case "bel": element.setBel(true); break;
				case "cfg": element.setConfiguration(true); break;
				case "pin": element.setPin(true); break;
				default: assert false;
			}
		}
	}

	private static void createPrimitiveElement(PrimitiveDef def, Element creationEl) {
		PrimitiveElement element = new PrimitiveElement();
		def.addElement(element);
		element.setName(creationEl.getChildText("name"));
		element.setMux(false);
		element.setBel(false);
		element.setConfiguration(false);
		element.setPin(false);

		switch (creationEl.getChildText("type")) {
			case "mux": element.setMux(true); break;
			case "bel": element.setBel(true); break;
			case "cfg": element.setConfiguration(true); break;
			case "pin": element.setPin(true); break;
			default: assert false;
		}
	}

	private static void modifyPinDirection(PrimitiveDef def, Element pinEl) {
		String elName = pinEl.getChildText("element");
		String pinName = pinEl.getChildText("pin");
		String dirName = pinEl.getChildText("direction");
		PinDirection direction;
		switch (dirName) {
			case "in" : direction = PinDirection.IN; break;
			case "out" : direction = PinDirection.OUT; break;
			case "inout" : direction = PinDirection.INOUT; break;
			default: throw new AssertionError("Invalid direction");
		}

		PrimitiveElement element = def.getElement(elName);
		for (PrimitiveDefPin pin : element.getPins()) {
			if (pin.getInternalName().equals(pinName)) {
				pin.setDirection(direction);
				break;
			}
		}

	}

	private static Element getSiteTypeEl(Document deviceInfo, SiteType type) {
		Element siteTypesEl = deviceInfo.getRootElement().getChild("site_types");
		for (Element siteTypeEl : siteTypesEl.getChildren("site_type")) {
			if (siteTypeEl.getChildText("name").equals(type.name()))
				return siteTypeEl;
		}
		throw new FileFormatException("no site type " + type.name() + " in familyInfo.xml");
	}
}
