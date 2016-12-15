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

parser grammar XDLParser;

options {tokenVocab=XDLLexer;}

design: DESIGN name=STRING part=NAME (hm_design | std_design) ;
hm_design:  (COMMA cfg)? SEMICOLON module ;
std_design: version=VERSION
	(COMMA cfg)? SEMICOLON
	module*
    inst*
    net*
    ;

cfg: CFG START_QUOTE attribute* END_QUOTE ;
attribute: physical=CFG_NAME COLON logical=logical_value COLON value=attribute_value ;
logical_value: CFG_NAME? ;
attribute_value: (CFG_NAME (COLON CFG_NAME?)*)? ;

module: MODULE name=STRING anchor=STRING (COMMA cfg)? SEMICOLON
	port* inst* net* ENDMODULE close_name=STRING;
port: PORT name=STRING inst_name=STRING inst_pin=STRING SEMICOLON ;

inst: INST name=STRING type=STRING
	COMMA placement
	(COMMA module_info)?
	(COMMA cfg)? SEMICOLON
	;
placement: UNPLACED bonded=(BONDED | UNBONDED)? | PLACED tile=NAME site=NAME ;
module_info: MODULE mi=STRING module_name=STRING instance=STRING ;

net: NET name=STRING type=(VCC | GND | WIRE)? (COMMA cfg)? COMMA pin* pip* SEMICOLON ;
pin: direction=(OUTPIN | INPIN) instance=STRING name=NAME COMMA ;
pip: PIP tile=NAME source=NAME
	direction=(BIDI_UNBUF | BIDI_ONEBUF | BIDI_TWOBUF | MONO_BUF) sink=NAME COMMA ;




