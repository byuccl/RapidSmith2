set family [lindex $argv 0]
set filename "$family.xml"
set indnt ""


proc get_last_div {full_name} {
    regexp {[a-zA-Z0-9_/\[\]\.-]+/([a-zA-Z0-9_\[\]]+)} $full_name matched last_name
    return $last_name
}

proc starttag {name} {
    global fileId indnt
    puts $fileId "$indnt<$name>"
    set indnt "$indnt  "
#    flush $fileId
}

proc endtag {name} {
    global fileId indnt
    if {$indnt != ""} {
	set indnt [string range $indnt 2 end]
    }
    puts $fileId "$indnt</$name>"
#    flush $fileId
}

proc tag {name val} {
    global fileId indnt
    puts $fileId "$indnt<$name>$val</$name>"
#    flush $fileId
}

proc emptytag {name} {
	global fileId indnt
	puts $fileId "$indnt<$name/>"
}

create_project -force -quiet TEMPORARY_PROJECT

foreach part [get_parts -filter "ARCHITECTURE == $family"] {
    dict set parts [get_property DEVICE $part] $part
}

set processed_parts ""
set processed_types [dict create]
set bel_types [dict create]
set bels ""

puts "Writing to file: $filename"
puts "Part count = [dict size $parts]"

foreach part  [dict values $parts]  {
	regexp (.*)-.* $part matched part_less_sg
	if {[lsearch -exact $processed_parts $part_less_sg] != -1} continue
	lappend processed_parts $part_less_sg
	
	puts "processing part $part"
	link_design -part $part -quiet
	foreach site [get_sites] {
		set site_type [get_property SITE_TYPE $site]
		if {[dict exists $processed_types $site_type]} continue
		dict set processed_types $site_type 0
		puts "processing site $site"
		
		foreach bel [get_bels -of $site] {
			set bel_type [get_property TYPE $bel]
			if {$bel_type in $bel_types} continue

			set pins ""
			foreach pin [get_bel_pins -of $bel] {
				set pin_list [list [get_last_div [get_property NAME $pin]]]
				
                set pcnt 0
                if {[get_property IS_BIDIR $pin] == 1} {lappend pin_list BIDIR; set pcnt $pcnt+1}
                if {[get_property IS_INPUT $pin] == 1} {lappend pin_list INPUT; set pcnt $pcnt+1}
                if {[get_property IS_OUTPUT $pin] == 1} {lappend pin_list OUTPUT; set pcnt $pcnt+1}
                if {$pcnt == 0} {puts "Pin: $pin on bel: $bel has no direction..."}
                if {$pcnt > 1} {puts "Pin: $pin on bel: $bel multiple directions..."}

                lappend pins $pin_list
            }

			dict set bel_types $bel_type $pins
			
			# create the BEL info
			set bel_name [get_last_div [get_property NAME $bel]]
			set bels_in_site [llength [get_bels -of $site]]
			lappend bels [list $site_type $bel_name $bel_type $bels_in_site]
		}

	}
}

set fileId [open $filename "w"]

starttag root
starttag corrections
endtag corrections

starttag cell_types
dict for {type pins} $bel_types {
	starttag cell_type
	tag name $type
	starttag pins
	foreach pin $pins {
		starttag pin
		tag name [lindex $pin 0]
		tag direction [lindex $pin 1]
		endtag pin
	}
	endtag pins
	starttag compatible_bels
	foreach bel_info $bels {
		if {$type == [lindex $bel_info 2]} {
			starttag bel
			starttag identifier
			tag primitive_type [lindex $bel_info 0]
            tag name [lindex $bel_info 1]
			endtag identifier
			emptytag "pins mode=\"direct\""
			endtag bel
		}
	}
	endtag compatible_bels
	endtag cell_type
}
endtag cell_types

starttag bels
foreach bel_info $bels {
	starttag bel
	starttag identifier
	tag primitive_type [lindex $bel_info 0]
	tag name [lindex $bel_info 1]
	endtag identifier

	starttag "unpack mode=\"automatic\""
	tag type [lindex $bel_info 2]
	set attrmode [expr ([lindex $bel_info 3]==1)?"absorb_all":"specify"]
	emptytag "attributes mode=\"$attrmode\""
	emptytag "pins mode=\"direct\""
	endtag unpack

	starttag "pack mode=\"automatic\""
	tag type [lindex $bel_info 2]
	emptytag "attributes mode=\"direct\""
	endtag pack
	endtag bel
}
endtag bels
endtag root

close $fileId
