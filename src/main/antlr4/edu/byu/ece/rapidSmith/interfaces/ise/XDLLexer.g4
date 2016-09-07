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
