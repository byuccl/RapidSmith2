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

package edu.byu.ece.rapidSmith.util;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.comments.BlockComment;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.printer.PrettyPrinter;
import com.github.javaparser.printer.PrettyPrinterConfiguration;
import edu.byu.ece.rapidSmith.RSEnvironment;
import edu.byu.ece.rapidSmith.device.*;
import edu.byu.ece.rapidSmith.device.families.FamilyInfos;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import static edu.byu.ece.rapidSmith.util.Exceptions.*;

/**
 *
 */
public class FamilyBuilders {
	private static final Path pkgPath = Paths.get("edu").resolve("byu").resolve("ece")
			.resolve("rapidSmith").resolve("device").resolve("families");

	private FamilyBuilders() { }

	private void build(FamilyType family, List<String> parts, RSEnvironment env) throws IOException {
		String className = capFirstLetter(family.name());
		Path fileLocation = env.getJavaPath().resolve(pkgPath).resolve(className + ".java");
		CompilationUnit cu;
		if (Files.exists(fileLocation))
			cu = loadFamilyInfoClass(fileLocation, className);
		else
			cu = createFamilyInfoClass(className);

		FamilyInfoData familyInfoData = parseInfo(cu, className);
		parts.forEach(p -> addPartData(p, familyInfoData, env));
		updateCompilationUnit(cu.getClassByName(className), familyInfoData);

		PrettyPrinterConfiguration cfg = new PrettyPrinterConfiguration();
		cfg.setPrintComments(true);
		PrettyPrinter pp = new PrettyPrinter(cfg);
		String output = pp.print(cu);
		try (BufferedWriter bw = Files.newBufferedWriter(fileLocation, Charset.defaultCharset())) {
			bw.write(output);
		}
	}

	private void addPartData(String p, FamilyInfoData familyInfoData, RSEnvironment env) {
		if (!familyInfoData.supportedParts.contains(p)) {
			Device device = env.getDevice(p);
			for (Tile tile : device.getTileMap().values()) {
				familyInfoData.tileTypes.add(tile.getType().name());
				Site[] sites = tile.getSites();
				if (sites != null) {
					for (Site site : sites) {
						familyInfoData.siteTypes.add(site.getDefaultType().name());
						SiteType[] possibleTypes = site.getPossibleTypes();
						if (possibleTypes != null) {
							for (SiteType siteType : possibleTypes) {
								familyInfoData.siteTypes.add(siteType.name());
							}
						}
					}
				}
			}
			familyInfoData.supportedParts.add(p);
		}
	}

	private CompilationUnit loadFamilyInfoClass(Path fileLocation, String className) throws IOException {
		return JavaParser.parse(fileLocation);
	}

	private CompilationUnit createFamilyInfoClass(String className) {
		CompilationUnit cu = new CompilationUnit();
		cu.setPackageName("edu.byu.ece.rapidSmith.device.families");
		cu.addImport("edu.byu.ece.rapidSmith.device.FamilyType");
		cu.addImport("edu.byu.ece.rapidSmith.device.TileType");
		cu.addImport("edu.byu.ece.rapidSmith.device.SiteType");
		cu.addImport("java.util.*");

		ClassOrInterfaceDeclaration clazz = cu.addClass(className, Modifier.PUBLIC, Modifier.FINAL);
		clazz.addImplements("FamilyInfo");

		addFamilyTypeField(clazz, className);
		addListField(clazz, "GENERATED_FROM", "String");
		addListField(clazz, "TILE_TYPES", "TileType");
		addListField(clazz, "SITE_TYPES", "SiteType");
		addSetField(clazz, "CLB_TILES", "TileType");
		addSetField(clazz, "SWITCHBOX_TILES", "TileType");
		addSetField(clazz, "BRAM_TILES", "TileType");
		addSetField(clazz, "DSP_TILES", "TileType");
		addSetField(clazz, "IO_TILES", "TileType");
        addSetField(clazz, "SLICE_SITES", "SiteType");
		addSetField(clazz, "BRAM_SITES", "SiteType");
		addSetField(clazz, "DSP_SITES", "SiteType");
		addSetField(clazz, "IO_SITES", "SiteType");
		addSetField(clazz, "FIFO_SITES", "SiteType");

		addTilesClass(clazz);
		addSitesClass(clazz);
		BlockStmt blockStmt = clazz.addStaticInitializer();

		BlockStmt userStatements = clazz.addStaticInitializer();
		Node staticBlock = userStatements.getParentNode().get();
		staticBlock.setComment(new BlockComment(" ------ AUTO-GENERATED --- DO NOT EDIT ABOVE ------ "));
		userStatements.addOrphanComment(new BlockComment(" ------ CLASSIFICATIONS GO HERE ------ "));

		return cu;
	}

	private void addFamilyTypeField(ClassOrInterfaceDeclaration clazz, String className) {
		FieldDeclaration field = clazz.addField("FamilyType", "FAMILY_TYPE",
				Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL);
		VariableDeclarator variable = field.getVariable(0);
		Expression familyType = new NameExpr("FamilyType");
		NameExpr value = new NameExpr('"' + className.toUpperCase() + '"');
		NodeList<Expression> args = NodeList.nodeList(value);
		Expression init = new MethodCallExpr(familyType, new SimpleName("valueOf"), args);
		variable.setInitializer(init);
		BlockComment comment = new BlockComment(" ------ AUTO-GENERATED --- DO NOT EDIT BELOW ------ ");
		field.setComment(comment);
	}

	private void addListField(
			ClassOrInterfaceDeclaration clazz,
			String variable, String subType
	) {
		addField(clazz, variable, subType, "ArrayList", "List");
	}

	private void addSetField(
			ClassOrInterfaceDeclaration clazz,
			String variable, String subType
	) {
		addField(clazz, variable, subType, "HashSet", "Set");
	}


	private void addField(
			ClassOrInterfaceDeclaration clazz, String variable,
			String subType, String privateType, String publicType
	) {
		String privateVarName = '_' + variable;
		ClassOrInterfaceType subTypeType = new ClassOrInterfaceType(subType);
		ClassOrInterfaceType publicTypeType = new ClassOrInterfaceType(publicType);
		publicTypeType.setTypeArguments(NodeList.nodeList(subTypeType));


		addPrivateField(clazz, privateType, privateVarName, subTypeType);
		addPublicField(clazz, variable, publicType, publicTypeType, privateVarName);
		addGetterForField(clazz, variable, publicTypeType);
	}

	private void addPrivateField(
			ClassOrInterfaceDeclaration clazz, String privateType,
			String privateVarName, ClassOrInterfaceType subTypeType
	) {
		ClassOrInterfaceType arrayListType = new ClassOrInterfaceType(privateType);
		ClassOrInterfaceType publicDeclType = (ClassOrInterfaceType) arrayListType.clone();
		publicDeclType.setTypeArguments(subTypeType);

		FieldDeclaration privateField = clazz.addField(publicDeclType, privateVarName,
				Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL);
		VariableDeclarator privateVariable = privateField.getVariable(0);

		ObjectCreationExpr privateInit = new ObjectCreationExpr();
		ClassOrInterfaceType publicInitType = (ClassOrInterfaceType) arrayListType.clone();
		publicInitType.setDiamondOperator();
		privateInit.setType(publicInitType);
		privateVariable.setInitializer(privateInit);
	}

	private void addPublicField(
			ClassOrInterfaceDeclaration clazz, String variable, String collectionType,
			ClassOrInterfaceType publicType, String privateVarName
	) {
		FieldDeclaration publicField = clazz.addField(publicType, variable,
				Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL);

		Expression collections = new NameExpr("Collections");
		NodeList<Expression> args = NodeList.nodeList(new NameExpr(privateVarName));
		MethodCallExpr publicInit = new MethodCallExpr(collections, new SimpleName("unmodifiable" + collectionType), args);

		VariableDeclarator publicVariable = publicField.getVariable(0);
		publicVariable.setInitializer(publicInit);
	}

	private void addGetterForField(
			ClassOrInterfaceDeclaration clazz, String fieldName,
			ClassOrInterfaceType publicType
	) {
		MethodDeclaration method = clazz.addMethod(toCamelCase(fieldName), Modifier.PUBLIC);
		method.setType(publicType);
		method.setAnnotations(NodeList.nodeList(new MarkerAnnotationExpr(new Name("Override"))));

		BlockStmt body = new BlockStmt();
		ReturnStmt returnStmt = new ReturnStmt(fieldName);
		body.addStatement(returnStmt);

		method.setBody(body);
	}

	private void addTilesClass(ClassOrInterfaceDeclaration clazz) {
		ClassOrInterfaceDeclaration tilesClass = new ClassOrInterfaceDeclaration();
		tilesClass.setName("TileTypes");
		tilesClass.addModifier(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL);
		clazz.addMember(tilesClass);
	}

	private void addSitesClass(ClassOrInterfaceDeclaration clazz) {
		ClassOrInterfaceDeclaration tilesClass = new ClassOrInterfaceDeclaration();
		tilesClass.setName("SiteTypes");
		tilesClass.addModifier(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL);
		clazz.addMember(tilesClass);
	}

	private FamilyInfoData parseInfo(CompilationUnit cu, String className) {
		FamilyInfoData familyInfoData = new FamilyInfoData();

		ClassOrInterfaceDeclaration clazz = cu.getClassByName(className);
		BlockStmt block = getFirstStaticBlock(clazz);
		for (Statement statement : block.getStatements()) {
			if (statement instanceof ExpressionStmt) {
				ExpressionStmt expressionStmt = (ExpressionStmt) statement;
				Expression expression = expressionStmt.getExpression();
				if (expression instanceof MethodCallExpr) {
					MethodCallExpr methodCall = (MethodCallExpr) expression;
					parseMethodCall(familyInfoData, methodCall);
				}
			}
		}
		return familyInfoData;
	}

	private void parseMethodCall(FamilyInfoData familyInfoData, MethodCallExpr methodCall) {
		if (methodCall.getName().getIdentifier().equals("add")) {
			String scope = getScope(methodCall);
			if (scope != null) {
				Expression argument = methodCall.getArgument(0);
				switch (scope) {
					case "_GENERATED_FROM":
						familyInfoData.supportedParts.add(getArgumentString(argument));
						break;
					case "_TILE_TYPES":
						familyInfoData.tileTypes.add(getArgumentMember(argument, "TileTypes"));
						break;
					case "_SITE_TYPES":
						familyInfoData.siteTypes.add(getArgumentMember(argument, "SiteTypes"));
						break;
				}
			}
		}
	}

	private static String getScope(MethodCallExpr methodCall) {
		String scope = null;
		Expression scopeExpr = methodCall.getScope();
		if (scopeExpr instanceof NameExpr) {
			NameExpr nameExpr = (NameExpr) scopeExpr;
			String structure = nameExpr.getName().getIdentifier();
			switch (structure) {
				case "_GENERATED_FROM":
					scope = "_GENERATED_FROM";
					break;
				case "_TILE_TYPES":
					scope = "_TILE_TYPES";
					break;
				case "_SITE_TYPES":
					scope = "_SITE_TYPES";
					break;
				default:
					scope = null;
			}
		}
		return scope;
	}

	private static String getArgumentString(Expression expr) {
		if (!(expr instanceof StringLiteralExpr))
			throw new EnvironmentException("Poorly formatted FamilyInfo file");
		StringLiteralExpr litExpr = (StringLiteralExpr) expr;
		return litExpr.getValue();
	}

	private static String getArgumentMember(Expression expr, String parent) {
		if (!(expr instanceof FieldAccessExpr))
			throw new EnvironmentException("Poorly formatted FamilyInfo file");
		FieldAccessExpr fieldAccessExpr = (FieldAccessExpr) expr;
		Optional<Expression> scope = fieldAccessExpr.getScope();
		if (!scope.isPresent())
			throw new EnvironmentException("Poorly formatted FamilyInfo file");
		Expression scopeExpr = scope.get();
		if (!(scopeExpr instanceof NameExpr))
			throw new EnvironmentException("Poorly formatted FamilyInfo file");
		String scopeName = ((NameExpr) scopeExpr).getNameAsString();
		if (!scopeName.equals(parent))
			throw new EnvironmentException("Poorly formatted FamilyInfo file");
		return fieldAccessExpr.getField().getIdentifier();
	}

	private BlockStmt getFirstStaticBlock(ClassOrInterfaceDeclaration clazz) {
		NodeList<BodyDeclaration<?>> bodyDeclarations = clazz.getMembers();
		for (BodyDeclaration<?> bodyDeclaration : bodyDeclarations) {
			if (bodyDeclaration instanceof InitializerDeclaration) {
				InitializerDeclaration init = (InitializerDeclaration) bodyDeclaration;
				if (init.isStatic())
					return init.getBlock();
			}
		}
		throw new EnvironmentException("Illegal FamilyInfo class");
	}

	private void updateCompilationUnit(
			ClassOrInterfaceDeclaration clazz,
			FamilyInfoData familyInfoData
	) {
		ClassOrInterfaceDeclaration tileTypes = getSubclass(clazz, "TileTypes");
		tileTypes.setMembers(new NodeList<>());
		familyInfoData.tileTypes.forEach(tt -> addTileTypeToClass(tileTypes, tt));

		ClassOrInterfaceDeclaration siteTypes = getSubclass(clazz, "SiteTypes");
		siteTypes.setMembers(new NodeList<>());
		familyInfoData.siteTypes.forEach(tt -> addSiteTypeToClass(siteTypes, tt));

		BlockStmt staticBlock = getFirstStaticBlock(clazz);
		staticBlock.setStatements(new NodeList<>());
		familyInfoData.supportedParts.forEach(p -> addSupportedPart(staticBlock, p));
		familyInfoData.tileTypes.forEach(tt -> addTileTypeToBlock(staticBlock, tt));
		familyInfoData.siteTypes.forEach(st -> addSiteTypeToBlock(staticBlock, st));
	}

	private void addTileTypeToClass(ClassOrInterfaceDeclaration clazz, String tileType) {
		FieldDeclaration field = clazz.addField("TileType", tileType,
				Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL);
		VariableDeclarator variable = field.getVariable(0);
		NameExpr TileType = new NameExpr("TileType");
		SimpleName valueOf = new SimpleName("valueOf");
		NameExpr family_type = new NameExpr("FAMILY_TYPE");
		NameExpr typeString = new NameExpr('"' + tileType + '"');
		NodeList<Expression> argList = NodeList.nodeList(family_type, typeString);
		Expression init = new MethodCallExpr(TileType, valueOf, argList);
		variable.setInitializer(init);
	}

	private void addSiteTypeToClass(ClassOrInterfaceDeclaration clazz, String siteType) {
		FieldDeclaration field = clazz.addField("SiteType", siteType,
				Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL);
		VariableDeclarator variable = field.getVariable(0);
		NameExpr SiteType = new NameExpr("SiteType");
		SimpleName valueOf = new SimpleName("valueOf");
		NameExpr family_type = new NameExpr("FAMILY_TYPE");
		NameExpr typeString = new NameExpr('"' + siteType + '"');
		NodeList<Expression> argList = NodeList.nodeList(family_type, typeString);
		Expression init = new MethodCallExpr(SiteType, valueOf, argList);
		variable.setInitializer(init);
	}

	private void addSupportedPart(BlockStmt block, String part) {
		Expression _GENERATED_FROM = new NameExpr("_GENERATED_FROM");
		MethodCallExpr methodCallExpr = new MethodCallExpr(_GENERATED_FROM, "add");
		methodCallExpr.addArgument('"' + part + '"');
		block.addStatement(methodCallExpr);
	}

	private void addTileTypeToBlock(BlockStmt block, String tt) {
		Expression _TILE_TYPES = new NameExpr("_TILE_TYPES");
		MethodCallExpr methodCallExpr = new MethodCallExpr(_TILE_TYPES, "add");
		methodCallExpr.addArgument(new FieldAccessExpr(new NameExpr("TileTypes"), tt));
		block.addStatement(methodCallExpr);
	}

	private void addSiteTypeToBlock(BlockStmt block, String st) {
		Expression _SITE_TYPES = new NameExpr("_SITE_TYPES");
		MethodCallExpr methodCallExpr = new MethodCallExpr(_SITE_TYPES, "add");
		methodCallExpr.addArgument(new FieldAccessExpr(new NameExpr("SiteTypes"), st));
		block.addStatement(methodCallExpr);
	}

	private static ClassOrInterfaceDeclaration getSubclass(
			ClassOrInterfaceDeclaration clazz, String className
	) {
		for (BodyDeclaration<?> bodyDeclaration : clazz.getMembers()) {
			if (bodyDeclaration instanceof ClassOrInterfaceDeclaration) {
				ClassOrInterfaceDeclaration decl = (ClassOrInterfaceDeclaration) bodyDeclaration;
				if (decl.getName().getIdentifier().equals(className))
					return decl;
			}
		}
		throw new EnvironmentException("Poorly formatted FamilyInfo class");
	}

	private static String toCamelCase(String varName) {
		boolean upper = false;
		StringBuilder sb = new StringBuilder(varName.length());
		for (char ch : varName.toCharArray()) {
			if (ch == '_') {
				upper = true;
			} else {
				if (upper)
					sb.append(Character.toUpperCase(ch));
				else
					sb.append(Character.toLowerCase(ch));
				upper = false;
			}
		}
		return sb.toString();
	}

	private static String capFirstLetter(String string) {
		return Character.toUpperCase(string.charAt(0)) + string.substring(1).toLowerCase();
	}

	private static class FamilyInfoData {
		final LinkedHashSet<String> supportedParts = new LinkedHashSet<>();
		final LinkedHashSet<String> tileTypes = new LinkedHashSet<>();
		final LinkedHashSet<String> siteTypes = new LinkedHashSet<>();
	}

	private void addFamiliesToInfosClass(Set<FamilyType> families, RSEnvironment env) throws IOException {
		Path fileLocation = env.getJavaPath().resolve(pkgPath).resolve("FamilyInfos.java");
		Set<FamilyType> supported = getSupportedFamilies();
		// check if we need to update anything
		if (supported.containsAll(families))
			return;

		Set<FamilyType> updatedFamilies = new LinkedHashSet<>();
		updatedFamilies.addAll(supported);
		updatedFamilies.addAll(families);
		CompilationUnit cu = JavaParser.parse(fileLocation);
		updateFamiliesInFamilyInfo(cu, updatedFamilies);
		PrettyPrinter pp = new PrettyPrinter(new PrettyPrinterConfiguration());
		String output = pp.print(cu);
		
		try (BufferedWriter bw = Files.newBufferedWriter(fileLocation, Charset.defaultCharset())) {
			bw.write(output);
		}
	}

	private void updateFamiliesInFamilyInfo(CompilationUnit cu, Set<FamilyType> updatedFamilies) {
		ClassOrInterfaceDeclaration clazz = cu.getClassByName("FamilyInfos");
		BlockStmt firstStaticBlock = getFirstStaticBlock(clazz);
		firstStaticBlock.setStatements(NodeList.nodeList());
		for (FamilyType family : updatedFamilies) {
			String className = capFirstLetter(family.name());
			FieldAccessExpr firstArg = new FieldAccessExpr(new NameExpr(className), "FAMILY_TYPE");
			ObjectCreationExpr secondArg = new ObjectCreationExpr();
			secondArg.setType(className);
			NodeList<Expression> args = NodeList.nodeList(firstArg, secondArg);
			NameExpr familyInfoMap = new NameExpr("familyInfoMap");
			MethodCallExpr methodCall = new MethodCallExpr(familyInfoMap, new SimpleName("put"), args);
			firstStaticBlock.addStatement(methodCall);
		}
	}

	private Set<FamilyType> getSupportedFamilies() {
		return FamilyInfos.supportedFamilies();
	}

	public static void main(String[] args) throws IOException {
		if (args.length != 1) {
			System.err.println("Example Usage: FamilyBuilders xc7a100tcsg324");
			System.err.println("Terminating...");
			System.exit(1);
		}
		FamilyBuilders fb = new FamilyBuilders();
		RSEnvironment env = RSEnvironment.defaultEnv();
		Map<FamilyType, List<String>> partsForFamily = new HashMap<>();
		for (String part : args) {
			FamilyType family = env.getFamilyTypeFromPart(part);
			System.out.println("Processing Family: " + family + " based on part name: " + part);
			partsForFamily.computeIfAbsent(family, k -> new ArrayList<>()).add(part);
		}
		for (Map.Entry<FamilyType, List<String>> e : partsForFamily.entrySet()) {
			fb.build(e.getKey(), e.getValue(), RSEnvironment.defaultEnv());
		}
		fb.addFamiliesToInfosClass(partsForFamily.keySet(), RSEnvironment.defaultEnv());
	}
}
