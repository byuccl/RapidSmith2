set config_dict [dict create WRITE_WIDTH_A 4 READ_WIDTH_A 4 WRITE_WIDTH_B 18 READ_WIDTH_B 18 RAM_MODE SDP]
set libcellname RAMB18E1
set belname RAMB18E1
set filename newMappings.xml
source create_nondefault_pin_mappings.tcl
