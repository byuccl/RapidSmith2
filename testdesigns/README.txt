This directory contains the original source and design checkpoints for
some simple designs used with the RS2 example programs found in the
edu.byu.ece.rapidSmith.examples2 package.

For each design the original Verilog, SystemVerilog, or VHDL source is
given.  A Vivado design checkpoint is also provided.

A file of Tcl commands is also provided.  Commands are given which:

1. Show how to load a Vivado design checkpoint into Vivado.  These
design checkpoints were created using Vivado 2016.2 and so you must be
using a 2016.2 or later version of Vivado to load them.

2. Synthesize, place, and route the original HDL source in Vivado.

3. Create a Tincr checkpoint (TCP) from an open Vivado design.  TCP's
are the mechanism that  RS2 uses to import/export designs from/to
Vivado. 
