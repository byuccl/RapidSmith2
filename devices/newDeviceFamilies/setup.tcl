package require tincr 0.0

proc createBlankDesign { } {
    createBlankDesignByPart xc7a100t-csg324-3
}

proc is_my_placement_legal { c b } {
    unplace_cell $c
    if {[catch {place_cell $c $b} fid] == 0} {
#	puts "$b  $c [get_property LOC $c]  [get_property BEL $c]"
	if { [suffix $b "/"] == [suffix [get_property BEL $c] "."] } then {
	    unplace_cell $c
#	    puts "Success, returning"
	    return 1
	} else {
#	    puts "Failure"
	    unplace_cell $c
	    return 0
	}
    }	
    return 0
}

proc placeCell { libcellname } {
    global s
    global b
    global c
    set mydes [tincr::designs new mydes xc7a100t-csg324-3]
    set libcell [get_lib_cells $libcellname]
    set s [get_sites SLICE_X2Y199]
    set b [lindex [get_bels -of $s] 14]
    puts "Placing $libcellname onto $b"
    set c [create_cell -reference $libcell "xyz"]
    set st [is_my_placement_legal $c $b]
    if { $st == 1 } then {
	puts "Success!"
	place_cell $c $b
	puts "$b  $c [get_property LOC $c]  [get_property BEL $c]"
	if { [suffix $b "/"] != [suffix [get_property BEL $c] "."] } then {
	    puts "Actually, failure... [suffix $b "/"] [suffix [get_property BEL $c] "."]"
	}
    } else {
	puts "Failure..."
    }
    puts ""
    report_property $c
    return $c
}

proc printList { l } {
    set cnt 0
    foreach itm $l {
	puts "$cnt $itm"
	incr cnt
    }
}

proc resetsites { } {
    reset_property MANUAL_ROUTING [get_sites]
}

proc suffix { s p } {
    set t [split $s $p]
    return [lindex $t [llength $t]-3]
}

# Get a dictionary of primary sites, one for each type
proc buildPrimarySiteList { } {
    set dict { }
    foreach site [get_sites] {
	if {![dict exists $dict [get_property SITE_TYPE $site]]} {
	    dict set dict [get_property SITE_TYPE $site] $site
	}
    }
    return $dict
}

proc lastName { s } {
    return [suffix $s "/"]
}

proc createBlankDesignByPart { part } {
    return [tincr::designs new mydes $part]
}



proc dumpsitepins { s { fo ""} } {
    set fname "sitepins/$fo-site_pins.txt"
#    puts "Fname is: $fname"
    if { $fo != "" } then {
	set fo [open $fname w]
    }

    set sps [get_site_pins -of $s]
    foreach sp $sps {
	if {[catch { set n  [get_node -of $sp ] }     errorstring]} {
	    set t [lastName $sp]
	    if { $fo != ""} then {
		puts $fo "[lastName $t],"
	    } else {
		puts "[lastName $t],"
	    }
	} else {    
	    set t [lastName $sp]
	    if { $fo != ""} then {
		puts $fo "[lastName $t], [lastName $n]"
	    } else {
		puts "[lastName $t], [lastName $n]"
	    }
	}
    }
    close $fo
}

proc dumpAllSitePins {  } {
    createBlankDesignByPart xc7a100t-csg324-3
    set dict [buildPrimarySiteList]
    set keys [dict keys $dict]
    foreach k $keys {
	set s [dict get $dict $k]
	dumpsitepins $s $k
    }
    return $dict
}

proc ssite { typ } {
    global dict
    set site [dict get $dict $typ]
    puts $site
    tincr::sites::set_type $site $typ
    start_gui
    unselect_objects
    select_objects $site
    return $site
}

# Make a list of all the sites of a specific type
proc getsites_oftype {typ} {
    global sites
    global indx
#    set sites [get_sites -filter {site_type == $typ}]
    set sites {}
    foreach site [get_sites] {
	if {[tincr::sites::get_type $site] == $typ} then {
	    lappend sites $site
	}
    }
    set indx -1
    puts "Found [llength $sites] of type $typ"
    puts $sites
    start_gui
}

# Select next item from $sites from getsites_oftype
proc nextsite_oftype {  } {
    global sites
    global indx
    global site
    incr indx
    puts "Acccessing element $indx of [llength $sites]"
    set site [lindex $sites $indx]
    puts "Base type:alternative types = [tincr::sites::get_type $site] : [tincr::sites::get_alternate_types $site]"
    unselect_objects
    select_objects $site
    return $site
}

proc myunique {} {
	set sites {}
	
#	foreach site [get_sites] {
#		dict set sites [get_property SITE_TYPE $site] $site
#	}
	
	# TODO Change this so that sites with the type as a default are chosen over sites with type as alter
	
	# TODO This will not return all unique sites if a site has more alternate types than instances
	foreach site [get_sites] {
		if {![dict exists $sites [get_property SITE_TYPE $site]]} {
			dict set sites [get_property SITE_TYPE $site] $site
			continue
		}
		foreach type [get_types $site] {
			if {![dict exists $sites $type]} {
				dict set sites $type $site
				break
			}
		}
	}
	
	return $sites
}


