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
package edu.byu.ece.rapidSmith.gui;

import javafx.scene.paint.Color;
import edu.byu.ece.rapidSmith.device.Device;
import edu.byu.ece.rapidSmith.device.Tile;
import edu.byu.ece.rapidSmith.device.TileType;
import edu.byu.ece.rapidSmith.device.families.FamilyInfo;
import edu.byu.ece.rapidSmith.device.families.FamilyInfos;

/**
 * This class is simply a suggested coloring of tile types for displaying
 * a grid of tiles.
 * @author Chris Lavin
 * Modified on: May 23, 2018
 * by Jesse Grigg for use with JavaFx
 * Created on: Jan 22, 2011
 */
public class TileColorsJavaFx {
    /**
     * Gets a suggested color based on the tile's tileType.
     * @param tile The tile for which to get the color suggestion.
     * @return A suggested color, or null if none exists.
     */
    public static Color getSuggestedTileColor(Tile tile){
        Device device = tile.getDevice();
        TileType type = tile.getType();
        FamilyInfo familyInfo = FamilyInfos.get(device.getFamily());
        if (familyInfo.clbTiles().contains(type)) {
            Color blue = Color.rgb(0,0,255);
            return blue;
        } else if (familyInfo.dspTiles().contains(type)) {
            return Color.CYAN.brighter();
        } else if (familyInfo.bramTiles().contains(type)) {
            return Color.MAGENTA.brighter();
        } else if (familyInfo.switchboxTiles().contains(type)) {
            return Color.GREEN.brighter();
        } else if (familyInfo.ioTiles().contains(type)) {
            return Color.DARKGOLDENROD;
        } else if (type.name().startsWith("EMPTY")) {
            return Color.color(240, 141, 45);
        } else if (type.name().contains("TERM")) {
            return Color.DARKGRAY;
        } else if (type.name().startsWith("HCLK")) {
            return Color.DARKCYAN;
        } else if (type.name().matches(".*(BRKH|VBRK|GCLK).*") || type.name().startsWith("REG")) {
            return Color.DARKBLUE;
        } else if (type.name().matches(".*(DCM|PLL).*")) {
            return Color.DARKRED;
        }

        return Color.BLACK;
    }
}
