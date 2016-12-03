This directory contains the original source and design checkpoints for
some simple designs used with the RS2 example programs found in the
edu.byu.ece.rapidSmith.examples2 package.

For each design the original Verilog, SystemVerilog, or VHDL source is
given.  A Vivado design checkpoint is also provided for each.  These
design checkpoints were created using Vivado 2016.2 and so you must be
using a 2016.2 or later version of Vivado to use them.

A file of Tcl commands is also provided (compile.tcl).  Commands are given which:

1. Convert a Vivado design checkpoint into a Tincr checkpoint (dcp2tcp).  

2. Synthesize, place, and route the original HDL source in Vivado and
then write out both a Vivado and a Tincr checkpoint (a TCP). TCP's
are the mechanism that  RS2 uses to import/export designs from/to
Vivado. 

3. Print out some statistics regarding the currently open design such
as cell count, net count, etc.


