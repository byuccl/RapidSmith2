package edu.byu.ece.rapidSmith.device.creation;

import edu.byu.ece.rapidSmith.device.PinDirection;
import edu.byu.ece.rapidSmith.device.SiteType;
import edu.byu.ece.rapidSmith.primitiveDefs.*;
import org.jdom2.Document;
import org.jdom2.Element;

import java.util.*;

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

	/**
	 * Converts the polarity mux element from a mux to a configuration.  The
	 * polarity muxes take a single input, and provides an option to either pass
	 * the signal or invert it.
	 *
	 *     |---- A  /-|   |-- O1
	 * S --|       |PS|---|-- O2
	 *     |-- A_B  \_|   |-- ...
	 *
	 * To convert this mux, we will change the polarity selector mux (PS) to a
	 * configuration, and then connect the source (S) to each output(O1, O2, ...).
	 * @param def the primitive def the polarity mux is in
	 * @param polarityMuxEl the corrections element describing the change to be made
	 */
	private static void fixPolarityMux(PrimitiveDef def, Element polarityMuxEl) {
		// get the polarity mux element
		PrimitiveElement muxElement = def.getElement(polarityMuxEl.getChildText("name"));
		assert muxElement != null;

		// find the source pin and all of the sinks of the polarity mux
		Pin sourcePin = null;
		Set<Pin> sinks = new HashSet<>();
		for (PrimitiveConnection c : muxElement.getConnections()) {
			if (c.isForwardConnection()) {
				PrimitiveElement sinkEl = def.getElement(c.getElement1());
				String sinkPin = c.getPin1();
				sinks.add(new Pin(sinkEl, sinkPin));
			} else {
				PrimitiveElement sourceEl = def.getElement(c.getElement1());
				sourcePin = new Pin(sourceEl, c.getPin1());
			}
		}
		// make sure we found something
		assert sinks.size() > 0;
		assert sourcePin != null;

		// disconnect the polarity mux and configure it as a configuration
		muxElement.getPins().clear();
		muxElement.setMux(false);
		muxElement.setConfiguration(true);
		muxElement.getConnections().clear();

		// remove all connections from the source to the polarity mux
		Iterator<PrimitiveConnection> it = sourcePin.element.getConnections().iterator();
		while(it.hasNext()) {
			PrimitiveConnection c = it.next();
			if (c.isForwardConnection()) {
				if (c.getElement1().equals(muxElement.getName())) {
					it.remove();
				}
			}
		}

		// remove all connections from the sinks to the polarity mux
		for (Pin sinkPin : sinks) {
			it = sinkPin.element.getConnections().iterator();
			while (it.hasNext()) {
				PrimitiveConnection c = it.next();
				if (!c.isForwardConnection()) {
					if (c.getElement1().equals(muxElement.getName())) {
						it.remove();
					}
				}
			}
		}

		// create connections from the source to every sink
		for (Pin sinkPin : sinks) {
			// create a forward connection from the source to the sink
			PrimitiveConnection sourceConn = new PrimitiveConnection();
			sourceConn.setElement0(sourcePin.element.getName());
			sourceConn.setPin0(sourcePin.pin);
			sourceConn.setForwardConnection(true);
			sourceConn.setElement1(sinkPin.element.getName());
			sourceConn.setPin1(sinkPin.pin);
			sourcePin.element.addConnection(sourceConn);

			// create a backward connection from the sink to the source
			PrimitiveConnection sinkConn = new PrimitiveConnection();
			sinkConn.setElement0(sinkPin.element.getName());
			sinkConn.setPin0(sinkPin.pin);
			sinkConn.setForwardConnection(false);
			sinkConn.setElement1(sourcePin.element.getName());
			sinkConn.setPin1(sourcePin.pin);
			sinkPin.element.addConnection(sinkConn);
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

	private static class Pin {
		PrimitiveElement element;
		String pin;

		public Pin(PrimitiveElement element, String pin) {
			this.element = element;
			this.pin = pin;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			Pin pin1 = (Pin) o;
			return Objects.equals(element, pin1.element) &&
					Objects.equals(pin, pin1.pin);
		}

		@Override
		public int hashCode() {
			return Objects.hash(element, pin);
		}
	}
}
