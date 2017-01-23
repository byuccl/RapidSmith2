# RapidSmith2

RapidSmith2, the Vivado successor to [RapidSmith](http://rapidsmith.sourceforge.net) is a research-based, open source FPGA CAD tool written in Java for modern Xilinx FPGAs. Its objective is to serve as a rapid prototyping platform for research ideas and algorithms relating to low level FPGA CAD tools.  Specifically, it allows users to manipulate Xilinx designs outside the standard Xilinx-provided CAD tool flow.

Unlike RapidSmith, which was based on XDL, RapidSmith2 exports/imports design data from/to Vivado using Vivado's built-in Tcl interface.  To do so, it relies on functionality found in the [Tincr project](https://github.com/byuccl/tincr).

A typical RapidSmith2 tool flow consists of:

1. Pull a design from Vivado into the RapidSmith2 data structure using functionality found in the [Tincr tool set](https://github.com/byuccl/tincr).
2. Modify or analyze those designs using the RapidSmith2-provided Java API's.
3. Push the resulting design back into Vivado using Tincr.
4. Complete the implementation task in Vivado by generating a bitstream from the resulting design.

RapidSmith2 and Tincr together also provide support for creating device files for Xilinx device from Series 7 forward.  These device files are the equivalent of ISE-provided XDLRC files and therefore provide full visibility into the resources availabe within the FPGA.  RapidSmith2 provides functionality to support future Xilinx devices into Ultrascale and beyond.

RapidSmith2 (and previously RapidSmith) have been used in a variety of settings where control over the logical and/or physical characteristics of an FPGA design implementation are desired.  One example is Post-PAR logic and layout modifications to address reliability issues resulting from how the Xilinx tool flow implements designs.  Another is the creation of a physical template on the FPGA into which circuit modules may be inserted using partial reconfiguration.

For the full documentation, see the [TechReport](doc/TechReport.pdf).
