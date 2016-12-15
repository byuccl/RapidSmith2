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

grammar LutEquation;

// Entry rules
config_string : '#' op_mode ':' output_pin '=' lut_value EOF ;
equation_only : equation EOF ;

op_mode : 'LUT' | 'RAM' | 'ROM' ;
output_pin : INPUT | OUTPUT_PIN | CONST_VALUE ;
lut_value : init_string | equation_value ;
static_value : '0' | '1' ;
init_string : CONST_VALUE ;
equation_value : equation ;
equation : binary_eqn ;
binary_eqn : ( input | static_value | '(' left_eqn=binary_eqn ')' ) (binary_op right_eqn=binary_eqn)? ;
binary_op : AND | OR | XOR ;
input : INV? INPUT ;

INV : '~' ;

AND : '*' ;
OR : '+' ;
XOR : '@' ;

INPUT : [a-zA-Z] [0-9] ;
CONST_VALUE : '0x' [a-fA-F0-9]+ ;
OUTPUT_PIN : [a-zA-Z0-9]+ ;
