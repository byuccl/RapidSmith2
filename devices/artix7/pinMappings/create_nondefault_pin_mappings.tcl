
close_project -quiet
link_design -part xc7a100tcsg324

set libcell [get_lib_cells $libcellname]
set cell [create_cell -reference $libcell "tmpcell"]
set bel [lindex [get_bels *$belname] 0]

# Create dictionary of changes to apply
set config_dict [dict create]
dict set config_dict READ_WIDTH_A 2
dict set config_dict READ_WIDTH_B 4
dict set config_dict WRITE_WIDTH_A 2
dict set config_dict WRITE_WIDTH_B 4
dict set config_dict  RAM_MODE TDP

# Create the actual pin mappings
set pin_mappings [tincr::cells::create_nondefault_pin_mappings $cell $bel $config_dict] 
#dict for {pin mapping} $pin_mappings {
#  puts "Mapping = $pin - $mapping"
#}

# Write the pin mappings to an xml file
set pinMappingFileName "newPinMappings.xml"
tincr::cells::write_nondefault_pin_mappings $cell $bel $pin_mappings $config_dict $pinMappingFileName
