// This file is part of www.nand2tetris.org
// and the book "The Elements of Computing Systems"
// by Nisan and Schocken, MIT Press.
// File name: projects/04/Mult.asm

// Multiplies R0 and R1 and stores the result in R2.
// (R0, R1, R2 refer to RAM[0], RAM[1], and RAM[2], respectively.)

// Put your code here.

// Initialize the result (R2) to zero.
@R2
M=0

// Check if one of the numbers is zero, if so jump to END
@R0
D=M
@END
D;JEQ

@R1
D=M
@END
D;JEQ

// Initialize the loop counter 'i' to 0.
@i
M=0

// Set 'n' to the value of R1, determining the number of loop iterations.
@R1
D=M
@n
M=D

// If R1 is positive, jump to the loop. If negative, convert 'n' to positive.
@LOOP
D;JGT
@0
D=A
@n
M=D-M


(LOOP)
    // Compare 'i' with 'n'. If equal, all additions are done; jump to sign adjustment.
    @i
    D=M
    @n
    D=M-D
    @END_MULT_SELECT_SIGN
    D;JEQ

    // Add the value in R0 to the running total in R2.
    @R0
    D=M
    @R2
    M=D+M

    // Increment the loop counter 'i'.
    @i
    M=M+1

    // Repeat the loop until 'i' equals 'n'.
    @LOOP
    0;JMP


(END_MULT_SELECT_SIGN)
// If R1 is positive, multiplication is complete. jump to END.
@R1
D=M
@END
D;JGT
 // If R1 is negative, negate the result in R2.
@0
D=A
@R2
M=D-M

(END)
@END
0;JMP