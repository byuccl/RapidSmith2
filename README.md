RapidSmith2 - the Vivado successor to RapidSmith. Released Jan 4, 2017.

RapidSmith2 provides an CAD tool framework which allows users to manipulate Xilinx designs outside the standard Xilinx-provided CAD tool flow.

RapidSmith2 supports doing the following tasks:

1. Pull a design from Vivado into the RapidSmith2 data structure using functionality found in the Tincr tool set (https://github.com/byuccl/tincr).
2. Modify or analyze those designs using the RapidSmith2-provided Java API's.
3. Push the resulting design back into Vivado using Tincr.

RapidSmith2 and Tincr together provide support for creating device files for Xilinx device from Series 7 forward.  These device files are the equivalent of ISE-provided XDLRC files and therefore provide full visibility into the resources availabe within the FPGA.

RapidSmith andnow RapidSmith2 have been used in a variety of settings where control over the logical and/or physical characteristics of an FPGA design implementation are required.  One example is Post-PAR logic and layout modifications to address reliability issues resulting from how the Xilinx tool flow implements designs.  Another is the creation of a physical template on the FPGA into which circuit modules may be placed using partial reconfiguration.
