package emulator.cpu;

import emulator.bus.IBus;
import emulator.interrupts.InterruptController;
import emulator.cpu.instruction.Instruction;
import emulator.cpu.register.*;

public class CPU {
    // Order of registers; B,C,D,E,H,L,(HL),A
    private IBus bus;
    private InterruptController interruptController;
    private InstructionDecoder instructionDecoder;
    private InstructionFetcher instructionFetcher;
    private GameboyRegisters registers;


    public CPU(IBus bus, GameboyRegisters registers, InterruptController interruptController, InstructionDecoder instructionDecoder, InstructionFetcher instructionFetcher) {
        this.bus = bus;
        this.interruptController = interruptController;
        this.instructionDecoder = instructionDecoder;
        this.instructionFetcher = instructionFetcher;
        this.registers = registers;
        
        // initial values after boot
        initMemory();
    }

    private void initMemory() {
        bus.writeByteAt(0xFF05, (byte) 0x00); // ; TIMA
        bus.writeByteAt(0xFF06, (byte) 0x00); // ; TMA
        bus.writeByteAt(0xFF07, (byte) 0x00); // ; TAC
        bus.writeByteAt(0xFF10, (byte) 0x80); // ; NR10
        bus.writeByteAt(0xFF11, (byte) 0xBF); // ; NR11
        bus.writeByteAt(0xFF12, (byte) 0xF3); // ; NR12
        bus.writeByteAt(0xFF14, (byte) 0xBF); // ; NR14
        bus.writeByteAt(0xFF16, (byte) 0x3F); // ; NR21
        bus.writeByteAt(0xFF17, (byte) 0x00); // ; NR22
        bus.writeByteAt(0xFF19, (byte) 0xBF); // ; NR24
        bus.writeByteAt(0xFF1A, (byte) 0x7F); // ; NR30
        bus.writeByteAt(0xFF1B, (byte) 0xFF); // ; NR31
        bus.writeByteAt(0xFF1C, (byte) 0x9F); // ; NR32
        bus.writeByteAt(0xFF1E, (byte) 0xBF); // ; NR33
        bus.writeByteAt(0xFF20, (byte) 0xFF); // ; NR41
        bus.writeByteAt(0xFF21, (byte) 0x00); // ; NR42
        bus.writeByteAt(0xFF22, (byte) 0x00); // ; NR43
        bus.writeByteAt(0xFF23, (byte) 0xBF); // ; NR30
        bus.writeByteAt(0xFF24, (byte) 0x77); // ; NR50
        bus.writeByteAt(0xFF25, (byte) 0xF3); // ; NR51
        bus.writeByteAt(0xFF26, (byte) 0xF1); // ; NR52
        bus.writeByteAt(0xFF40, (byte) 0x91); // ; LCDC
        bus.writeByteAt(0xFF42, (byte) 0x00); // ; SCY
        bus.writeByteAt(0xFF43, (byte) 0x00); // ; SCX
        bus.writeByteAt(0xFF45, (byte) 0x00); // ; LYC
        bus.writeByteAt(0xFF47, (byte) 0xFC); // ; BGP
        bus.writeByteAt(0xFF48, (byte) 0xFF); // ; OBP0
        bus.writeByteAt(0xFF49, (byte) 0xFF); // ; OBP1
        bus.writeByteAt(0xFF4A, (byte) 0x00); // ; WY
        bus.writeByteAt(0xFF4B, (byte) 0x00); // ; WX
        bus.writeByteAt(0xFFFF, (byte) 0x00); // ; IE
        System.out.println("Setup memory!\n");
    }

    public int executeNext() {
        if (interruptController.isHalted()) {
            if (!interruptController.getInterruptMasterEnable() && (interruptController.getInterruptFlag() & interruptController.getInterruptEnable()) != 0) {
                interruptController.setHalted(false);
            } else {
                return 1;
            }
        }

//        InstructionLogger.logInstruction(A, F, B, C, D, E, H, L, SP, PC, bus);
        // fetch
        byte nextInstruction = instructionFetcher.fetchNextByte();

        // decode
        Instruction decodedInstruction = instructionDecoder.decode(nextInstruction);
        
        return decodedInstruction.execute(nextInstruction);
    }

    public int checkInterrupts() {
        if (interruptController.interruptReady()) {
            int jumpAddress = interruptController.getHighestPriorityInterruptAddress();
//            System.out.printf("Servicing interrupt: 0x%02X\n", jumpAddress);

            //  Reset the IME flag and prevent all interrupts.
            interruptController.setInterruptMasterEnable(false);

            //  The PC (program counter) is pushed onto the stack.
            byte lowerPC = registers.getPC().getLowerRegister().getValue();
            byte higherPC = registers.getPC().getHigherRegister().getValue();
            registers.getSP().setValue(registers.getSP().getValue() - 1);
            bus.writeByteAt(registers.getSP().getValue(), higherPC);
            registers.getSP().setValue(registers.getSP().getValue() - 1);
            bus.writeByteAt(registers.getSP().getValue(), lowerPC);

            //  Jump to the starting address of the interrupt.
            registers.getPC().setValue(jumpAddress);

            interruptController.setHalted(false);

            return 5;
        }
        return 0;
    }
}
