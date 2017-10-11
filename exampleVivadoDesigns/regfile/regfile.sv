module regfile(input logic         clk, 
               input logic         regWE,
               input logic [2:0]   DR, SR1, SR2,
               input logic [15:0]  din,
               output logic [15:0] Ra, Rb);
   
   logic [15:0] registers[8];

   always_ff @(posedge clk)
     if (regWE)
       registers[DR] <= din;
   assign Ra = registers[SR1];
   assign Rb = registers[SR2];
endmodule // regfile

