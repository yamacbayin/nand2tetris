import java.io.*;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Assembler {

    private static final String COMMENT_INDICATOR = "//";
    private static final int BINARY_WORD_LENGTH = 16;
    private static final int A_INSTRUCTION_MAX_BINARY_VALUE_LENGTH = 15;
    private static final Logger LOGGER = Logger.getLogger(Assembler.class.getName());
    private HashMap<String, String> symbolsMap;

    private void setSymbolsMap() {
        symbolsMap = new HashMap<>();
        // Populate the symbols map using a loop for registers
        for (int i = 0; i <= 15; i++) {
            symbolsMap.put("R" + i, binaryStringWithLeadingZeros(Integer.toBinaryString(i)));
        }

        // Screen and keyboard constants
        symbolsMap.put("SCREEN", binaryStringWithLeadingZeros(Integer.toBinaryString(16384)));
        symbolsMap.put("KBD", binaryStringWithLeadingZeros(Integer.toBinaryString(24576)));

        // Predefined symbols
        symbolsMap.put("SP", binaryStringWithLeadingZeros(Integer.toBinaryString(0)));
        symbolsMap.put("LCL", binaryStringWithLeadingZeros(Integer.toBinaryString(1)));
        symbolsMap.put("ARG", binaryStringWithLeadingZeros(Integer.toBinaryString(2)));
        symbolsMap.put("THIS", binaryStringWithLeadingZeros(Integer.toBinaryString(3)));
        symbolsMap.put("THAT", binaryStringWithLeadingZeros(Integer.toBinaryString(4)));
    }

    public void translate(String filepath) {
        // Reset the map so previous symbols get deleted
        setSymbolsMap();
        try {
            String cleanAssemblyFile = readAndCleanAssemblyFile(filepath);
            String refactoredAssemblyFile = mapAndDeleteLabels(cleanAssemblyFile);
            String machineLanguage = toMachineLanguage(refactoredAssemblyFile);
            writeToFile(machineLanguage, filepath);

        } catch (IOException exception) {
            LOGGER.log(Level.SEVERE, "Error reading or writing file", exception);
        }
    }

    public void writeToFile(String translatedString, String filepath) throws IOException {
        String outputFilePath = filepath.replace(".asm", ".hack");
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFilePath))) {
            writer.write(translatedString);
        }
    }

    private String readAndCleanAssemblyFile(String filepath) throws IOException {
        StringBuilder cleanedAssemblyCode = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(filepath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String processedLine = cleanLine(line);
                if (!processedLine.isEmpty()) {
                    cleanedAssemblyCode.append(processedLine).append("\n");
                }
            }
        }
        return cleanedAssemblyCode.toString().trim();
    }

    private String cleanLine(String line) {
        line = line.trim();
        int commentIndex = line.indexOf(COMMENT_INDICATOR);
        if (commentIndex != -1) {
            line = line.substring(0, commentIndex).trim();
        }
        return line;
    }

    private String mapAndDeleteLabels(String cleanAssemblyFile) {
        String[] cleanedLines = cleanAssemblyFile.split("\n");
        StringBuilder refactoredAssemblyCode = new StringBuilder();
        int romIndex = 0;
        int ramIndex = 16;

        for (String line : cleanedLines) {
            if (line.startsWith("(") && line.endsWith(")")) {
                // Label found, map it to the current ROM address
                symbolsMap.put(line.substring(1, line.length() - 1),
                        binaryStringWithLeadingZeros(Integer.toBinaryString(romIndex)));
            } else {
                refactoredAssemblyCode.append(line).append("\n");
                romIndex++;
            }
        }

        String[] refactoredLines = refactoredAssemblyCode.toString().split("\n");
        for (String line : refactoredLines) {
            if (line.startsWith("@")) {
                String symbol = line.substring(1);
                if (!isNumeric(symbol) && !symbolsMap.containsKey(symbol)) {
                    // New variable found, assign next RAM address
                    symbolsMap.put(symbol, binaryStringWithLeadingZeros(Integer.toBinaryString(ramIndex++)));
                }
            }
        }

        return refactoredAssemblyCode.toString().trim();
    }

    private String toMachineLanguage(String assemblyProgram) {
        String[] lines = assemblyProgram.split("\n");
        StringBuilder machineCode = new StringBuilder();

        for (String line : lines) {
            if (line.startsWith("@")) {
                //A-Instruction
                machineCode.append(translateA(line));
            } else {
                //C-Instruction
                machineCode.append(translateC(line));
            }
            machineCode.append("\n");
        }

        return machineCode.toString().trim();
    }

    private String translateA(String instruction) {
        // Remove the @
        String value = instruction.substring(1);
        // Check if it's a number or a symbol
        if (isNumeric(value)) {
            int decimalValue = Integer.parseInt(value);
            String binaryValue = Integer.toBinaryString(decimalValue);

            if (binaryValue.length() > A_INSTRUCTION_MAX_BINARY_VALUE_LENGTH) {
                throw new IllegalArgumentException("Binary value exceeds 15 bits limit.");
            }

            return binaryStringWithLeadingZeros(binaryValue);
        }

        return symbolsMap.get(value);
    }

    private boolean isNumeric(String str) {
        return str.matches("\\d+");
    }

    private String binaryStringWithLeadingZeros(String binaryValue) {
        return String.format("%" + BINARY_WORD_LENGTH + "s", binaryValue).replace(' ', '0');
    }

    private String translateC(String instruction) {
        // 3 leftmost bits are 1 for a C instruction
        final String cInstructionSign = "111";
        StringBuilder cInstructionBuilder = new StringBuilder(cInstructionSign);

        //split the assembly instruction string by "=" and ";"
        String destination = "";
        String computation = "";
        String jump = "";

        int equalIndex = instruction.indexOf('=');
        int semiColonIndex = instruction.indexOf(';');

        if (equalIndex != -1) {
            destination = instruction.substring(0, equalIndex);
        }

        if (semiColonIndex != -1) {
            jump = instruction.substring(semiColonIndex + 1);
            computation = instruction.substring(equalIndex + 1, semiColonIndex);
        } else if (equalIndex != -1) {
            computation = instruction.substring(equalIndex + 1);
        } else {
            computation = instruction;
        }

        cInstructionBuilder.append(translateComputation(computation));
        cInstructionBuilder.append(translateDestination(destination));
        cInstructionBuilder.append(translateJump(jump));

        return cInstructionBuilder.toString();
    }

    private String translateDestination(String destination) {
        return switch (destination) {
            case "" -> "000";
            case "M" -> "001";
            case "D" -> "010";
            case "MD" -> "011";
            case "A" -> "100";
            case "AM" -> "101";
            case "AD" -> "110";
            case "AMD" -> "111";
            default -> throw new IllegalArgumentException("Invalid destination mnemonic: " + destination);
        };
    }

    private String translateJump(String jump) {
        return switch (jump) {
            case "" -> "000";
            case "JGT" -> "001";
            case "JEQ" -> "010";
            case "JGE" -> "011";
            case "JLT" -> "100";
            case "JNE" -> "101";
            case "JLE" -> "110";
            case "JMP" -> "111";
            default -> throw new IllegalArgumentException("Invalid jump mnemonic: " + jump);
        };
    }

    private String translateComputation(String computation) {
        return switch (computation) {
            case "0" -> "0101010";
            case "1" -> "0111111";
            case "-1" -> "0111010";
            case "D" -> "0001100";
            case "A" -> "0110000";
            case "!D" -> "0001101";
            case "!A" -> "0110001";
            case "-D" -> "0001111";
            case "-A" -> "0110011";
            case "D+1" -> "0011111";
            case "A+1" -> "0110111";
            case "D-1" -> "0001110";
            case "A-1" -> "0110010";
            case "D+A" -> "0000010";
            case "D-A" -> "0010011";
            case "A-D" -> "0000111";
            case "D&A" -> "0000000";
            case "D|A" -> "0010101";
            case "M" -> "1110000";
            case "!M" -> "1110001";
            case "-M" -> "1110011";
            case "M+1" -> "1110111";
            case "M-1" -> "1110010";
            case "D+M" -> "1000010";
            case "D-M" -> "1010011";
            case "M-D" -> "1000111";
            case "D&M" -> "1000000";
            case "D|M" -> "1010101";
            default -> throw new IllegalArgumentException("Invalid computation: " + computation);
        };
    }
}
