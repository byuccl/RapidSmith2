package edu.byu.ece.rapidSmith.design.unpacker;

import edu.byu.ece.rapidSmith.design.subsite.CellLibrary;
import edu.byu.ece.rapidSmith.design.subsite.LibraryCell;
import edu.byu.ece.rapidSmith.device.BelId;
import edu.byu.ece.rapidSmith.device.SiteType;
import org.jdom2.Document;
import org.jdom2.Element;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

/**
 *
 */
public class CellCreatorFactoryFactory {
	public static Map<BelId, CellCreatorFactory> createCellCreators(
			Document doc, CellLibrary cellLibrary) {
		Map<BelId, CellCreatorFactory> cellCreatorFactories = new HashMap<>();

		// Make cell creator for each element in xml file
		Element unpackerEl = doc.getRootElement();
		for ( Element belEl : unpackerEl.getChildren("bel")) {
			Element idEl = belEl.getChild("id");
			BelId id = new BelId(
					SiteType.valueOf(idEl.getChildText("primitive_type")),
					idEl.getChildText("name"));

			String mode = belEl.getChildText("mode");
			CellCreatorFactory cellCreatorFactory = null;
			if (mode.equals("automatic")) {
				cellCreatorFactory = createAutomatedCellCreator(belEl, id, cellLibrary);
			} else if (mode.equals("manual")) {
				cellCreatorFactory = createManualCellCreator(belEl, id, cellLibrary);
			}
			cellCreatorFactories.put(id, cellCreatorFactory);
		}
		return cellCreatorFactories;
	}

	private static CellCreatorFactory createManualCellCreator(
			Element belEl, BelId belId, CellLibrary cellLibrary)
	{
		String creatorClassName = belEl.getChildText("class");
		CellCreatorFactory cellCreatorFactory = null;

		try {
			Class<?> clazz = Class.forName(creatorClassName);
			Constructor<?> ctor = clazz.getConstructors()[0];
			Object obj = ctor.newInstance(belId, cellLibrary);
			cellCreatorFactory = (CellCreatorFactory) obj;
		} catch (ClassNotFoundException | IllegalAccessException | InstantiationException | InvocationTargetException e) {
			e.printStackTrace();
			System.exit(-1);
		}
		return cellCreatorFactory;
	}

	private static CellCreatorFactory createAutomatedCellCreator(
			Element belEl, BelId id, CellLibrary cellLibrary)
	{
		LibraryCell libCell = cellLibrary.get(belEl.getChildText("cell_type"));
		DefaultCellCreatorFactory ccf = new DefaultCellCreatorFactory();
		ccf.setCellType(libCell);
		ccf.setBelId(id);
		Element attributesEl = belEl.getChild("attributes");
		for (Element attributeEl : attributesEl.getChildren()) {
			String name = attributeEl.getChildText("name");
			String rename = attributeEl.getChildText("rename");
			if (rename == null)
				rename = name;
			ccf.addAttribute(name, rename);
		}
		Element pinsEl = belEl.getChild("pins");
		for (Element pinEl : pinsEl.getChildren("pin")) {
			String pinName = pinEl.getChildText("name");
			String pinRename = pinEl.getChildText("rename");
			ccf.addPinMap(pinName, pinRename);
		}
		return ccf;
	}
}
