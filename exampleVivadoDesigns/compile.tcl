# Script to compile a set of HDL files, create the corresponding netlist in Vivado, and create a Tincr Checkpoint

# The Tincr package from github is required
package require tincr

# Convert a Vivado checkpoint to a Tincr checkpoint
proc dcp2tcp {top} {
    close_design -quiet
    open_checkpoint $top.dcp
    tincr::write_rscp $top
}

# Compile the specified directory of HDL files and create a Tincr
# checkpoint from that.  They do not remove the files or close the
# design in case you want to do further work with them.
proc compile_hdl_to_checkpoint_files {top} {
    link_design -part xc7a100t-csg324-3
    
    if {[glob -nocomplain $top/*.sv] != ""} {
	puts "Reading SV files..."
	read_verilog -sv [glob $top/*.sv]
    }
    if {[glob -nocomplain $top/*.v] != ""} {
	puts "Reading Verilog files..."
	read_verilog  [glob $top/*.v]
    }
    if {[glob -nocomplain $top/*.vhd] != ""} {
	puts "Reading VHDL files..."
	read_vhdl [glob $top/*.vhd]
    }

    puts "Synthesizing design..."
    synth_design -top $top -flatten_hierarchy full 
    
    puts "Placing Design..."
    place_design
    
    puts "Routing Design..."
    route_design

    #	remove files
#    file delete {*}[glob *.log]
#    file delete {*}[glob *.dmp]

    puts "Writing checkpoint"
    write_checkpoint -force $top.dcp
    puts "Writing rscp"
    tincr::write_rscp $top
    puts "All done..."
    #	close_design

    }

# Print the size statistics of an open Vivado design to the specified file.
# Specifically, the number of Cells, number of nets, number of sites, and 
# the cell type distribution is printed.
# This may be useful to understand the size of designs being used for
# benchmark purposes.
# NOTE: the filename does not need to have an extension. 
#	 	".txt" will automatically be appended to the filename 
proc print_cell_statistics { {filename "cell_stats.txt"} } {
    
    set filename [::tincr::add_extension ".txt" $filename]
    set fp [open $filename w]
    
    puts $fp "Benchmark: [get_property TOP [get_design]]"
    puts $fp "----------------------"
    #print size statistics
    puts $fp "\tCell Count: [llength [get_cells -hierarchical -quiet]]"
    puts $fp "\tNet Count: [llength [get_nets -hierarchical -quiet]]"
    puts $fp "\tSite Count: [llength [get_sites -filter IS_USED -quiet]]\n"

    #print distribution statistics
    puts $fp "\tCell Distribution: " 
    set cell_distribution_map [generate_cell_distribution_dictionary]
    
    dict for {group cell_list} $cell_distribution_map {
	puts $fp "\t\t$group: [llength $cell_list]"					
    }
    
    close $fp
}	

# Helper function used in print_cell_statistics
proc generate_cell_distribution_dictionary { } {
    set cell_dictionary [dict create] 
    
    foreach cell [get_cells -hierarchical]  {
	dict lappend cell_dictionary [get_property PRIMITIVE_GROUP $cell] $cell
    }
    
    return $cell_dictionary
}	
