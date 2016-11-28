module count16 (output logic[15:0] q, input clk, clr, ld, input logic[15:0] din);
   always @(posedge clk)
     if (clr)
       q <= 0;
     else if (ld)
       q <= din;
     else
       q <= q + 1;
   
endmodule // count16
