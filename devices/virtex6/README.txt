Notes on what goes into familyInfo.xml:

- the switch_matrix_types entries need editing to match technology.
- Example: INT for V6, INT_L and INT_R for V7

Marking tags:
   slices ==> <is_slice/>
   fifos ==> <is_fifo/>
   iobs ==> <is_iob/>
   brams ==> <is_bram/>
   dsps ==> <is_dsp/>

- Some, but not all, of the items in primitive_defs which are not in
  primitive_sites have compatible types.  There is no other info other
  than the name of the compatible type.

Compatible types:
  BUFG ==> BUFGCTRL
  FIFO36E1 ==> RAMBFIFO36E1
  IOB ==> IOBM
  IOB ==> IOBS
  IPAD ==> IOBM
  IPAD ==> IOBS
  ISERDESE1 ==> ILOGICE1
  OSERDESE1 ==> OLOGICE1
  RAMB18E1 ==> FIFO18E1
  RAMB36E1 ==> RAMBFIFO36E1
  SLICEL ==> SLICEM

- Some have alternative types.  

Alternative types:
  BUFGCTRL ==> BUFG
  FIFO18E1 ==> RAMB18E1
  ILOGICE1 ==> ISERDESE1
  IOBM ==> IOB
  IOBM ==> IPAD
  IOBS ==> IOB
  IOBS ==> IPAD
  OLOGICE1 ==> OSERDESE1
  RAMBFIFO36E1 ==> FIFO36E1
  RAMBFIFO36E1 ==> RAMB36E1
  SLICEM ==> SLICEL

- Alternatives need a pinmaps section, even if empty.  The ones that
  are not empty: 

  IOBM ==> IPAD
    <pin>
      <name>O</name>
      <map>PADOUT</map>
  IOBS ==> IPAD
    <pin>
      <name>O</name>
      <map>PADOUT</map>
  RAMGFIFOE36E1 ==> FIFO36E1
    <pin>
      tons of pins remapped
  RAMGFIFOE36E1 ==> RAMB36E1
    <pin>
      tons of pins remapped
  OLOGICE1 ==> OSERDESE1
    <pin>
      <name>RST</name>
      <map>SR</map>
  FIFO18E1 ==> RAMB18E1
    <pin>
      tons of pins remapped
  ILOGICE1 ==> ISERDESE1
    <pin>
      <name>RST</name>
      <map>SR</map>

- I took the above compatibles and alternatives and created a DOT file
  so I could visualize the relationships using something that can
  visualize DOT files. 


- There are lots of corrections:
  polarity_selector -> just contains a name
  new_element -> I think always contains a CFG
  modify_element -> contains a name and a type
  pin_direction -> used to change a pin to inout
  
  
