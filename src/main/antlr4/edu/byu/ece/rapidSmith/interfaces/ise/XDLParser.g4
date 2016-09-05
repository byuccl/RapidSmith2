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




