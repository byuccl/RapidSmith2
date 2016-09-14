package edu.byu.ece.rapidSmith.interfaces.ise;

import edu.byu.ece.rapidSmith.RapidSmithEnv;
import edu.byu.ece.rapidSmith.design.*;
import edu.byu.ece.rapidSmith.design.xdl.*;
import edu.byu.ece.rapidSmith.device.*;
import edu.byu.ece.rapidSmith.util.Exceptions.ParseException;
import org.antlr.v4.runtime.ANTLRFileStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTreeWalker;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;

/**
 *
 */
public final class XDLReader {
	public Design readDesign(Path xdlFile) throws IOException {
		XDLLexer lexer = new XDLLexer(new ANTLRFileStream(xdlFile.toString()));
		XDLParser parser = new XDLParser(new CommonTokenStream(lexer));
		XDLParser.DesignContext design = parser.design();
		ParseTreeWalker walker = new ParseTreeWalker();
		DesignListener listener = new DesignListener();
		walker.walk(listener, design);
		return listener.design;
	}

	private static class DesignListener extends XDLParserBaseListener {
		Design design;
		private CfgState cfgState;
		private Device device;

		private Net currNet = null;
		private Instance currInstance = null;

		private Module currModule = null;
		private String currModuleAnchorName = null;
		private HashMap<String, Pin> modulePinMap = null;
		private ArrayList<String> portNames = null;
		private ArrayList<String> portInstanceNames = null;
		private ArrayList<String> portPinNames = null;

		@Override
		public void enterDesign(XDLParser.DesignContext ctx) {
			String name = stripQuotes(ctx.name.getText());

			String partName = ctx.part.getText();
			device = RapidSmithEnv.getDefaultEnv().getDevice(partName);
			if (device == null)
				throw new ParseException("unsupported device: " + partName);

			design = new Design(name, partName);
			cfgState = CfgState.DESIGN;
		}

		@Override
		public void exitDesign(XDLParser.DesignContext ctx) {
			cfgState = null;
		}

		@Override
		public void enterStd_design(XDLParser.Std_designContext ctx) {
			design.setNCDVersion(ctx.version.getText().intern()); // TODO replace with version
		}

		@Override
		public void enterInst(XDLParser.InstContext ctx) {
			String name = stripQuotes(ctx.name.getText()).intern();
			String typeName = stripQuotes(ctx.type.getText());
			SiteType type = SiteType.valueOf(typeName);
			currInstance = new Instance(name, type);
			currInstance.setDesign(design);
			if (currModule == null) {
				design.addInstance(currInstance);
			} else {
				currModule.addInstance(currInstance);
				currInstance.setModuleTemplate(currModule);
				if (currInstance.getName().equals(currModuleAnchorName))
					currModule.setAnchor(currInstance);
			}
			cfgState = CfgState.INSTANCE;
		}

		@Override
		public void exitInst(XDLParser.InstContext ctx) {
			currInstance = null;
			cfgState = CfgState.DESIGN;
		}

		@Override
		public void enterPlacement(XDLParser.PlacementContext ctx) {
			if (ctx.UNPLACED() != null) {
				if (ctx.bonded == null) {
					currInstance.setBonded(null); // TODO change bonded to internal
				} else if (ctx.bonded.getType() == XDLParser.BONDED) {
					currInstance.setBonded(true);
				} else {
					assert ctx.bonded.getType() == XDLParser.UNBONDED;
					currInstance.setBonded(false);
				}
			} else {
				assert ctx.PLACED() != null;
				String siteName = ctx.site.getText();
				Site site = device.getPrimitiveSite(siteName); // TODO rename getPrimtiveSite to getSite
				if (site == null)
					throw new ParseException("no such site on device: " + siteName);
				// TODO add check against tile
				site.setType(currInstance.getType());
				if (currModule == null) {
					currInstance.place(site);
					switch (site.getBondedType()) {
						case BONDED: currInstance.setBonded(true); break;
						case UNBONDED: currInstance.setBonded(false); break;
						case INTERNAL: currInstance.setBonded(null); break;
						default: throw new AssertionError("illegal enum value");
					}
				} else {
					currInstance.setSite(site);
				}
			}
		}

		@Override
		public void enterModule_info(XDLParser.Module_infoContext ctx) {
			String miName = stripQuotes(ctx.mi.getText()).intern();
			String moduleName = stripQuotes(ctx.module_name.getText());
			Module module = design.getModule(moduleName);
			if (module == null)
				throw new ParseException("unknown module: " + moduleName);
			currInstance.setModuleTemplate(module);

			String templateName = stripQuotes(ctx.instance.getText());
			Instance templateInstance = module.getInstance(templateName);
			currInstance.setModuleTemplateInstance(templateInstance);
			ModuleInstance mi = design.addInstanceToModuleInstances(
					currInstance, miName);

			if(templateInstance.equals(module.getAnchor())) {
				mi.setAnchor(currInstance);
			}
		}

		@Override
		public void enterNet(XDLParser.NetContext ctx) {
			String name = stripQuotes(ctx.name.getText()).intern();
			NetType type;
			if (ctx.type != null) {
				switch(ctx.type.getType()) {
					case XDLParser.VCC: type = NetType.VCC; break;
					case XDLParser.GND: type = NetType.GND; break;
					case XDLParser.WIRE: type = NetType.WIRE; break;
					default: throw new ParseException("illegal net type");
				}
			} else {
				type = NetType.WIRE;
			}
			currNet = new Net(name, type);
			if (currModule == null)
				design.addNet(currNet);
			else
				currModule.addNet(currNet);
		}

		@Override
		public void exitNet(XDLParser.NetContext ctx) {
			currNet = null;
			cfgState = CfgState.DESIGN;
		}

		@Override
		public void enterPin(XDLParser.PinContext ctx) {
			Pin pin;
			String instName = stripQuotes(ctx.instance.getText());
			Instance inst;
			inst = getPinInstance(instName);

			String name = ctx.name.getText().intern();
			switch (ctx.direction.getType()) {
				// Note: old code had an inout option, no special handling though
				case XDLParser.INPIN:
					pin = new Pin(false, name, inst);
					break;
				case XDLParser.OUTPIN:
					if (currNet.getSource() != null) {
						throw new ParseException("net " + currNet.getName()
								+ " has two or more output pins");
					}
					pin = new Pin(true, name, inst);
					break;
				default:
					throw new ParseException("illegal pin direction");
			}

			// save the pin name for later connecting it to a port
			if (currModule != null)
				modulePinMap.put(inst.getName() + name, pin);

			currNet.addPin(pin);
			inst.addToNetList(currNet);
		}

		private Instance getPinInstance(String instName) {
			Instance inst;
			if (currModule == null) {
				inst = design.getInstance(instName);
			} else {
				inst = currModule.getInstance(instName);
			}
			if (inst == null)
				throw new ParseException("unrecognized instance: " + instName);
			return inst;
		}

		@Override
		public void enterPip(XDLParser.PipContext ctx) {
			String tileName = ctx.tile.getText();
			Tile tile = device.getTile(tileName);
			if (tile == null)
				throw new ParseException("unrecognized tile: " + tileName);

			WireEnumerator we = device.getWireEnumerator();
			String sourceName = ctx.source.getText();
			Integer sourceEnum = we.getWireEnum(sourceName);
			if (sourceEnum == null)
				throw new ParseException("unrecognized wire: " + sourceName);

			String sinkName = ctx.sink.getText();
			Integer sinkEnum = we.getWireEnum(sinkName);
			if (sinkEnum == null)
				throw new ParseException("unrecognized wire: " + sinkName);

			PIP pip = new PIP(tile, sourceEnum, sinkEnum);
			currNet.addPIP(pip);
		}

		@Override
		public void enterAttribute(XDLParser.AttributeContext ctx) {
			Attribute attr = makeAttribute(ctx);

			switch (cfgState) {
				case DESIGN:
					design.addAttribute(attr);
					break;
				case INSTANCE:
					currInstance.addAttribute(attr);
					break;
				case NET:
					currNet.addAttribute(attr);
					if(attr.getPhysicalName().equals("_MACRO")){
						setNetModuleInstance(attr);
				    }
					break;
				case MODULE:
					currModule.addAttribute(attr);
			}
		}

		private static Attribute makeAttribute(XDLParser.AttributeContext ctx) {
			String physicalName = ctx.physical.getText().intern();
			String logicalName = ctx.logical.getText().intern();
			String value = ctx.attribute_value().getText().intern();
			return new Attribute(physicalName, logicalName, value);
		}

		private void setNetModuleInstance(Attribute attr) {
			ModuleInstance mi = design.getModuleInstance(attr.getValue());
			currNet.setModuleInstance(mi);
			mi.addNet(currNet);
			Module module = mi.getModule();
			currNet.setModuleTemplate(module);
			String moduleNetName = currNet.getName().replaceFirst(
					mi.getName() + "/", "");
			currNet.setModuleTemplateNet(module.getNet(moduleNetName));
		}

		@Override
		public void enterModule(XDLParser.ModuleContext ctx) {
			currModule = new Module();
			modulePinMap = new HashMap<>();
			portNames = new ArrayList<>();
			portInstanceNames = new ArrayList<>();
			portPinNames = new ArrayList<>();
			cfgState = CfgState.MODULE;

			currModule.setName(stripQuotes(ctx.name.getText()).intern());
			currModuleAnchorName = stripQuotes(ctx.anchor.getText());
		}

		@Override
		public void exitModule(XDLParser.ModuleContext ctx) {
			int numPorts = portNames.size();
			for (int i = 0; i < numPorts; i++) {
				String key = portInstanceNames.get(i) + portPinNames.get(i);
				Port port = new Port(portNames.get(i), modulePinMap.get(key));
				currModule.addPort(port);
			}

			currModule = null;
			modulePinMap = null;
			portNames = null;
			portInstanceNames = null;
			portPinNames = null;
			cfgState = CfgState.DESIGN;
		}

		@Override
		public void enterPort(XDLParser.PortContext ctx) {
			portNames.add(stripQuotes(ctx.name.getText()).intern());
			portInstanceNames.add(stripQuotes(ctx.inst_name.getText()).intern());
			portPinNames.add(stripQuotes(ctx.inst_pin.getText()).intern());
		}

		private static String stripQuotes(String str) {
			if (str.charAt(0) == '"' && str.charAt(str.length()-1) == '"')
				return str.substring(1, str.length()-1);
			return str;
		}

		private enum CfgState {
			DESIGN, INSTANCE, NET, MODULE
		}
	}
}
