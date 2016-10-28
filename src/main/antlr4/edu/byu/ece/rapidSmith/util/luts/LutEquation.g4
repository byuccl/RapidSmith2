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
