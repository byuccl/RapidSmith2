# Source-ing this file will create the basic familyInfo file called
# 'familyInfo_new.xml'.

# It will first output records for all primary types associated with
# physical sites.

# It will then output records for all the alternative types. Finally,
# it will output records for some that the regular logic doesn't
# catch.

set family "ARTIX7"

proc putis {nam} {
    global fo
    if {[string first SLICE $nam 0] == 0} then {
	puts $fo "      <is_slice/>"
    }
    if {[string first IOB $nam 0] == 0} then {
	puts $fo "      <is_iob/>"
    }
    if {[string first DSP $nam 0] == 0} then {
	puts $fo "      <is_dsp/>"
    }
    if {[string first RAMB $nam 0] == 0} then {
	puts $fo "      <is_bram/>"
    }
    if {[string first FIFO $nam 0] == 0} then {
	puts $fo "      <is_fifo/>"
    }
    if {[string first RAMBFIFO $nam 0] == 0} then {
	puts $fo "      <is_fifo/>"
    }
}

# This crashes on some types, IOB18M in Virtex7 for example
proc checkrbels { s k } {
    if {[catch { set rb [tincr::sites::get_routing_muxes $s] } errorstring]} {
#	puts "Site type $k has possible polarity_selectors or muxes (chkerr)"
        return 2
    } else {    
	if {[llength $rb] > 0} then {
#	    puts "Site type $k has possible polarity_selectors or muxes"
	    return $rb
	}
    }
    return 0
}

proc doprimary { fo s k } {
    puts $fo "    <primitive_type>"
    puts $fo "      <name>$k</name>"
    putis $k
    if {[tincr::sites::is_alternate_type $s] == 0} then {
	if {[tincr::sites::has_alternate_types $s] != 0} then {
	    puts $fo "      <alternatives>"
	    foreach alt [tincr::sites::get_alternate_types $s] {
		puts $fo "        <alternative>"
		puts $fo "          <name>$alt</name>"
		puts $fo "          <pinmaps>"
		puts "Compatible: $alt --> $k"
		puts $fo "          </pinmaps>"
		puts $fo "        </alternative>"
	    }
	    puts $fo "      </alternatives>"
	}
    }
    puts $fo "      <bels>"
    set bels [get_bels -of $s]
    foreach b $bels {
	set tmpnam [lastName $b]
	puts $fo "        <bel>"
	puts $fo "          <name>$tmpnam</name>"
	puts $fo "          <type>$tmpnam</type>"
	puts $fo "        </bel>"
    }
    puts $fo "      </bels>"


    puts $fo "      <corrections>"
    set rb [checkrbels $s $k]
    if {$rb == 2} then {
	puts $fo "<!-- Got error when checking for polarity_selectors and muxes, check GUI and add if needed -->"
    } elseif {$rb == 0} then {
    } else {
	foreach r [lsort $rb] {
	    if {[string range $r end-2 end] == "INV" } then {
		puts $fo "        <polarity_selector> <name>$r</name> </polarity_selector>"
	    } else {
		puts $fo "        <modify_element>    <name>$r</name> <type>mux</type> </modify_element>"
	    }
	}
    }
    puts $fo "      </corrections>"

    puts $fo "    </primitive_type>"
    puts $fo ""
}


proc doalt { fo s k} {
    puts "Alt: $s $k"
    tincr::sites::set_type $s $k
    puts $fo "    <primitive_type>"
    puts $fo "      <name>$k</name>"
    putis $k
    puts $fo "      <bels>"
    set bels [get_bels -of $s]
    foreach b $bels {
	set tmpnam [lastName $b]
	puts $fo "        <bel>"
	puts $fo "          <name>$tmpnam</name>"
	puts $fo "          <type>$tmpnam</type>"
	puts $fo "        </bel>"
    }
    puts $fo "      </bels>"

    puts $fo "      <corrections>"
    set rb [checkrbels $s $k]
    if {$rb == 2} then {
	puts $fo "<!-- Got error when checking for polarity_selectors and muxes, check GUI and add if needed -->"
    } elseif {$rb == 0} then {
    } else {
	foreach r [lsort $rb] {
	    if {[string range $r end-2 end] == "INV" } then {
		puts $fo "        <polarity_selector> <name>$r</name> </polarity_selector>"
	    } else {
		puts $fo "        <modify_element>    <name>$r</name> <type>mux</type> </modify_element>"
	    }
	}
    }
    puts $fo "      </corrections>"

    puts $fo "    </primitive_type>"
    puts $fo ""
}


####################### Actual Code Starts Here ###################################
set mydes [createBlankDesign xc7a100t-csg324-1 ]

set fo [open "familyInfo_new.xml" w]

set dict [buildPrimarySiteList]

# Get a list of the keys
set keys [dict keys $dict]

puts $fo {<?xml version="1.0" encoding="UTF-8"?>}
puts $fo {<device_description>}
puts $fo {  <family>ARTIX7</family>}
puts $fo {  <switch_matrix_types>}
puts $fo {    <type>INT_L</type>}
puts $fo {    <type>INT_R</type>}
puts $fo {  </switch_matrix_types>}
puts $fo {  <primitive_types>}

puts $fo ""
puts $fo "<!-- Do the base sites -->"
puts $fo ""

set keys [lsort $keys]
foreach k $keys {
    set s [dict get $dict $k]
    doprimary $fo $s $k
}

puts $fo ""
puts $fo "<!-- Do the alternative sites for the base sites -->"
puts $fo ""

set altdicttypes  { }
foreach k [lsort $keys] {
    set s [dict get $dict $k]
    set alts [tincr::sites::get_alternate_types $s]
    if {[llength $alts] > 0} then {
	foreach alt $alts {
	    if {![dict exists $dict $alt]} then {
		if {![dict exists $altdicttypes $alt]} then {
		    dict set altdicttypes $alt $alt
		    doalt $fo $s $alt
		}
	    }
	}
    }
}

#puts "Primary types: $keys"
#puts "Alternate types: $altdicttypes"

puts $fo " "
puts $fo " "

puts $fo ""
puts $fo "<!-- Do the site types that don't appear in the tincr::sites::unique list -->"
puts $fo ""

# Do the site types that don't appear in the unique list from above
# These are site types for which there are primitive_def's in the XDLRC but for which no cell was found of that type.
# It could be that we need to try different devices from the family to pick these up because they aren't in the one that
# we are using for this script?
foreach s [tincr::sites::get_types] {
    set tmp "$s"
    set d [dict exists $dict $tmp]
    if {$d == 0} then {
	puts $fo "    <primitive_type> <name>$tmp</name>"
        putis $tmp
	puts $fo "      <bels>"
	puts $fo "        <bel>"
	puts $fo "          <name>$tmp</name>"
	puts $fo "          <type>$tmp</type>"
	puts $fo "        </bel>"
	puts $fo "      </bels>"
	puts $fo "    </primitive_type>"

    }
}

puts $fo " "
puts $fo " "

puts $fo ""
puts $fo "<!-- Do the site types that appear in the XDLRC as primitive sites that this code doesn't pick up -->"
puts $fo ""

# Do the site types that appear in the XDLRC as primitive sites that this code doesn't pick up
set lst [list CFG_IO_ACCESS GCLK_TEST_BUF MTBF2 PMV PMV2_SVT PMVBRAM PMVIOB]
foreach s $lst {
    puts $fo "    <primitive_type> <name>$s</name>"
    puts $fo "      <bels>"
    puts $fo "        <bel>"
    puts $fo "          <name>$s</name>"
    puts $fo "          <type>$s</type>"
    puts $fo "        </bel>"
    puts $fo "      </bels>"
    puts $fo "    </primitive_type>"
}

puts $fo " "
puts $fo " "

puts $fo ""
puts $fo "<!-- Do the site types that are simply needed but show up nowhere - not sure where they would come from so I add them here -->"
puts $fo ""

# Do the site types that are simply needed but show up nowhere - not sure where they would
# come from so I add them here
set lst [list DCI ]
foreach s $lst {
    puts $fo "    <primitive_type> <name>$s</name>"
    puts $fo "      <bels>"
    puts $fo "        <bel>"
    puts $fo "          <name>$s</name>"
    puts $fo "          <type>$s</type>"
    puts $fo "        </bel>"
    puts $fo "      </bels>"
    puts $fo "    </primitive_type>"
}



puts $fo "  </primitive_types>"
puts $fo "</device_description>"

close $fo



