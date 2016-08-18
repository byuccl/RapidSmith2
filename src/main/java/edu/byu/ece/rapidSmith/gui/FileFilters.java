/*
 * Copyright (c) 2010-2011 Brigham Young University
 * 
 * This file is part of the BYU RapidSmith Tools.
 * 
 * BYU RapidSmith Tools is free software: you may redistribute it 
 * and/or modify it under the terms of the GNU General Public License 
 * as published by the Free Software Foundation, either version 2 of 
 * the License, or (at your option) any later version.
 * 
 * BYU RapidSmith Tools is distributed in the hope that it will be 
 * useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU 
 * General Public License for more details.
 * 
 * A copy of the GNU General Public License is included with the BYU 
 * RapidSmith Tools. It can be found at doc/gpl2.txt. You may also 
 * get a copy of the license at <http://www.gnu.org/licenses/>.
 * 
 */
package edu.byu.ece.rapidSmith.gui;

import com.trolltech.qt.gui.QFileDialog.Filter;

public class FileFilters {
	/** Xilinx Design Language File Filter */
	public static Filter xdlFilter = new Filter("Xilinx Design Language Files (*.xdl)");
	/** Native Circuit Description File Filter */
	public static Filter ncdFilter = new Filter("Design Files (*.ncd)");
	/** Hard Macro File Filter */
	public static Filter nmcFilter = new Filter("Hard Macro Files (*.nmc)");
	/** Portable Document Format File Filter */
	public static Filter pdfFilter = new Filter("Portable Document Format Files (*.pdf)");
	/** Xilinx Trace Report File Filter */
	public static Filter twrFilter = new Filter("Xilinx Trace Report Files (*.twr)");
	/** EDK Microprocessor Hardware Specification File Filter */
	public static Filter mhsFilter = new Filter("Microprocessor Hardware Specification Files (*.mhs)");
}
