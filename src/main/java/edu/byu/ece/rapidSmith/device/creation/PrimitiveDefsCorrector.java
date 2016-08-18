package edu.byu.ece.rapidSmith.device.creation;

import edu.byu.ece.rapidSmith.device.PinDirection;
import edu.byu.ece.rapidSmith.device.SiteType;
import edu.byu.ece.rapidSmith.primitiveDefs.*;
import org.jdom2.Document;
import org.jdom2.Element;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 *
 */
public class PrimitiveDefsCorrector {
	public static void makeCorrections(PrimitiveDefList defs, Document deviceInfo) {
		for (PrimitiveDef def : defs) {
			Element ptEl = getPrimitiveTypeEl(deviceInfo, def.getType());
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

	private static Element getPrimitiveTypeEl(Document deviceInfo, SiteType type) {
		Element primitiveTypesEl = deviceInfo.getRootElement().getChild("primitive_types");
		for (Element primitiveTypeEl : primitiveTypesEl.getChildren("primitive_type")) {
			if (primitiveTypeEl.getChildText("name").equals(type.name()))
				return primitiveTypeEl;
		}
		assert false;
		return null;
	}
}
