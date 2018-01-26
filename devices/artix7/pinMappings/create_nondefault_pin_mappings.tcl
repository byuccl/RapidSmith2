
link_design -part $partname

set libcell [get_lib_cells $libcellname]
set cell [create_cell -reference $libcell "tmpcell"]
set bel [lindex [get_bels *$belname] 0]

# Create the actual pin mappings
set pin_mappings [tincr::cells::create_nondefault_pin_mappings $cell $bel $config_dict] 
#dict for {pin mapping} $pin_mappings {
#  puts "Mapping = $pin - $mapping"
#}

# Write the pin mappings to an xml file
tincr::cells::write_nondefault_pin_mappings $cell $bel $pin_mappings $config_dict $filename
