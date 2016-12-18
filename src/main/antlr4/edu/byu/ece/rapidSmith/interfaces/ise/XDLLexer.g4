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

lexer grammar XDLLexer;

COMMENT: '#' .*? '\r'? '\n' -> skip ;
WHITESPACE:  [ \t\n\r]+ -> skip ;
COMMA: ',';
SEMICOLON: ';';

BIDI_UNBUF: '==' ;
BIDI_ONEBUF: '=>' ;
BIDI_TWOBUF: '=-' ;
MONO_BUF: '->' ;

DESIGN: 'design' ;
MODULE: 'module' ;
PORT : 'port' ;
ENDMODULE: 'endmodule' ;
CFG: 'cfg' -> pushMode(START_CFG);
INST: 'inst' | 'instance' ;
PLACED: 'placed' ;
UNPLACED: 'unplaced' ;
BONDED: 'bonded' ;
UNBONDED: 'unbonded' ;
VCC: 'vcc' | 'power' | 'vdd' ;
GND: 'ground' | 'gnd' ;
WIRE: 'wire' ;
OUTPIN: 'outpin' ;
INPIN: 'inpin' ;
PIP: 'pip' ;
NET: 'net' ;

VERSION: 'v' DIGIT+ '.' DIGIT+ ;
NAME: LETTER_OR_DIGIT+ ('-' LETTER_OR_DIGIT+)*;
STRING: '"' ~["]* '"' ;

fragment WS: [ \t\n\r]+ ;
fragment LETTER_OR_DIGIT: [a-zA-Z0-9_] ;
fragment DIGIT: [0-9] ;

mode START_CFG;
START_QUOTE: '"' -> pushMode(IN_CFG);
START_CFG_WS: WS -> skip ;

mode IN_CFG;
END_QUOTE: '"' -> popMode, popMode ;
COLON: ':' ;
CFG_NAME: (~(':'| '"' | [ \t\n\r]) | ('\\' .))+ ;
CFG_WS: WS -> skip ;
