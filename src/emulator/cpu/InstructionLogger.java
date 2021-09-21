package emulator.cpu;

import emulator.Bus;
import emulator.application.SimpleLogFormatter;

import java.io.IOException;
import java.util.logging.FileHandler;
import java.util.logging.Logger;

public class InstructionLogger {
    private static Logger staticLogger;

    public static void logInstruction(Register A, Register F, Register B, Register C, Register D, Register E, Register H, Register L, DoubleRegister SP, DoubleRegister PC, Bus bus) {

        Logger logger = getLogger();
        byte[] nextFourInstructions = new byte[4];
        nextFourInstructions[0] = bus.readByteAt(PC.getValue());
        nextFourInstructions[1] = bus.readByteAt(PC.getValue()+1);
        nextFourInstructions[2] = bus.readByteAt(PC.getValue()+2);
        nextFourInstructions[3] = bus.readByteAt(PC.getValue()+3);
        String logString = String.format(
                "A: %02X F: %02X B: %02X C: %02X D: %02X E: %02X H: %02X L: %02X SP: %04X PC: 00:%04X (%02X %02X %02X %02X)",
                A.getValue(), F.getValue(), B.getValue(), C.getValue(), D.getValue(), E.getValue(), H.getValue(), L.getValue(),
                SP.getValue(), PC.getValue(), nextFourInstructions[0], nextFourInstructions[1], nextFourInstructions[2], nextFourInstructions[3]
        );
//        String logString = String.format(
//                "%04X:  %02X  A:%02X B:%02X C:%02X D:%02X E:%02X F:%02X H:%02X L:%02X SP:%04X",
//                PC, nextFourInstructions[0],
//                A.getValue(), B.getValue(), C.getValue(), D.getValue(), E.getValue(), F, H.getValue(), L.getValue(),
//                SP.getValue()
//        );
        logger.info(logString);
    }

    private static Logger getLogger() {
        if (staticLogger == null) {
            staticLogger = Logger.getLogger(InstructionLogger.class.getName());
            FileHandler fileHandler = null;
            try {
                fileHandler = new FileHandler("inst-out.log", false);
                fileHandler.setFormatter(new SimpleLogFormatter());
            } catch (IOException e) {
                e.printStackTrace();
            }
            staticLogger.addHandler(fileHandler);
            staticLogger.setUseParentHandlers(false);
        }
        return staticLogger;
    }
}
