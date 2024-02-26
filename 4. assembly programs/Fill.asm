// This file is part of www.nand2tetris.org
// and the book "The Elements of Computing Systems"
// by Nisan and Schocken, MIT Press.
// File name: projects/04/Fill.asm

// Runs an infinite loop that listens to the keyboard input.
// When a key is pressed (any key), the program blackens the screen
// by writing 'black' in every pixel;
// the screen should remain fully black as long as the key is pressed. 
// When no key is pressed, the program clears the screen by writing
// 'white' in every pixel;
// the screen should remain fully clear as long as no key is pressed.

//// Replace this comment with your code.

// Indicates the previous keyboard state: 0 for no keys pressed, 1 for a key pressed.
@keypressStatus
M=0

(CHECK_STATE_CHANGE)
    // Read the current state of the keyboard.
    @KBD
    D=M
    
    @KEY_PRESSED
    D;JGT
    
    @KEY_NOT_PRESSED
    D;JMP

    (KEY_PRESSED)
        // Continue if a key remains pressed
        @keypressStatus
        D=M
        @SCREEN_LOOP
        D;JGT

        // Transition detected from a state with no key pressed to a state with a key pressed
        // Mark keypressStatus as 1 to indicate an active key press
        @keypressStatus
        M=1
        @INITIALIZE_SCREEN_LOOP
        0;JMP
    
    (KEY_NOT_PRESSED)
        // Confirming the continuation of no key press
        @keypressStatus
        D=M
        @SCREEN_LOOP
        D;JEQ

        // Transition from a pressed state to no key press
        // Mark keypressStatus as 0 to indicate of no key press
        @keypressStatus
        M=0
        @INITIALIZE_SCREEN_LOOP
        0;JMP



(INITIALIZE_SCREEN_LOOP)    
    // Initialize pointer to SCREEN address
    @SCREEN
    D=A
    @pointer
    M=D

    // Set count to 8192 (total number of words to set)
    @8192
    D=A
    @count
    M=D

(SCREEN_LOOP)
    // Check if the count of remaning words to paint is zero
    // Jump out of SCREEN_LOOP if the screen is already painted the correct way
    @count
    D=M
    @CHECK_STATE_CHANGE
    D;JEQ

    // Determine the color to paint based on the keyboard input.
    // If a key is pressed, paint the screen black, else white.
    @KBD
    D=M
    @PAINT_BLACK
    D;JGT

    (PAINT_WHITE)
    @pointer
    A=M
    M=0
    @CONTINUE_SCREEN_LOOP
    0;JMP

    (PAINT_BLACK)
    @pointer
    A=M
    M=-1

    (CONTINUE_SCREEN_LOOP)

    // Increase the pointer by 1 to point to the next adress
    @pointer
    M=M+1
    // Decrase the remaning address count by one
    @count
    M=M-1

    // Jump to the start of the loop
    @CHECK_STATE_CHANGE
    0;JMP
