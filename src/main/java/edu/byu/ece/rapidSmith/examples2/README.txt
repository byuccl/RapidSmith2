Example programs which specifically use RapidSmith 2 functionality are
in this directory.  They can be used to learn RS2 by example.  A
listing of the programs is provided below.  In each case the command
line usage is given. If using Eclipse, you should create a Run
Configuration with the appropriate parameters. 

CreateDesignExample.java
------------------------
This is a simple program which creates a design from scratch.  It
first creates a netlist for a simple circuit by creating cells and
nets and wiring everything together.  It then places the cells onto
BELs and then pretty prints everything.

Usage: java edu.byu.ece.rapidSmith.examples2.CreateDesignExample

ImportExportExample.java
------------------------
This shows how to import a Vivado design (using a Tincr checkpoint)
and then export the design back into Vivado.  It relies on (a) the
existence of a Tincr checkpoint (name hard-coded into the code) and
(b) the program being run in the directory that contains that
checkpoint.

Usage: java edu.byu.ece.rapidSmith.examples2.ImportExportExample TincrCheckPointDirectoryName

DotFilePrinterDemo.java
-----------------------
This loads a Tincr checkpoint and then creates a DOT file for it.  It
then calls a System command to start up an extern DOT file viewer
program.

Usage: java edu.byu.ece.rapidSmith.examples2.DotFilePrinterDemo tincrCheckPointName

DesignAnalyzer.java
-------------------
The loads a Tincr checkpoint and does a prettyprint of its contents,
demonsrating how to walk the RS2 data structures.  

Usage: java edu.byu.ece.rapidSmith.examples2.DesignAnalyzer tincrCheckPointName

PlacerTest.java (inside the placerTest subpackage)
--------------------------------------------------
This uses a simple simulated annealing placer to re-place a circuit.
It requires a Tincr checkpoint of a circuit which has been placed by
Vivado.  It reads in the checkpoint, randomizes the tile-level
placement, and then does a placement.

It runs in two modes:

1. In batch mode it simply does the placement, writes out the
resulting circuit, and terminates.

2. In interactive mode (supply the -I option), it will start
up an instance of Vivado and then periodically bring up its
gui to display its placement progress.  Follow the
instructions in terms of executing "stop_gui" in the Vivado
Tcl command line.  It will bring up the Vivado gui each time
it needs.  Important: just be patient - it may take over a
minute for the Vivado gui to initially load.

Batch mode usage: java edu.byu.ece.rapidSmith.examples2.placerTest.PlacerTest tincrCheckPointName
Interactive mode usage: java edu.byu.ece.rapidSmith.examples2.placerTest.PlacerTest -I tincrCheckPointName







