/*
 * Copyright (c) 2010 Brigham Young University
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
package edu.byu.ece.rapidSmith.design.parser;

public enum ParserState{
	BEGIN_DESIGN,
	DESIGN_NAME,
	PART_NAME,
	NCD_VERSION,
	CFG_STRING,
	ATTRIBUTE,
	XDL_STATEMENT,
	INSTANCE_NAME,
	INSTANCE_TYPE,
	INSTANCE_PLACED,
	INSTANCE_TILE,
	INSTANCE_SITE,
	INSTANCE_BONDED,
	MODULE_INSTANCE_TOKEN,
	MODULE_INSTANCE_NAME,
	MODULE_TEMPLATE_NAME,
	MODULE_TEMPLATE_INSTANCE_NAME,
	NET_NAME,
	NET_TYPE,
	NET_STATEMENT,
	PIN_INSTANCE_NAME,
	PIN_NAME,
	PIP_TILE,
	PIP_WIRE0,
	PIP_CONN_TYPE,
	PIP_WIRE1,
	MODULE_NAME,
	MODULE_ANCHOR_NAME,
	MODULE_STATEMENT,
	PORT_NAME,
	PORT_INSTANCE_NAME,
	PORT_PIN_NAME,
	END_PORT,
	END_MODULE_NAME,
	END_MODULE
}
