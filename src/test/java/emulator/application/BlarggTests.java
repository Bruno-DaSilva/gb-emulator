package emulator.application;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import emulator.bus.GameboyBus;
import emulator.cartridge.Cartridge;
import emulator.cpu.CPU;
import emulator.cpu.GameboyRegisters;
import emulator.cpu.InstructionDecoder;
import emulator.cpu.InstructionFetcher;
import emulator.interrupts.InterruptController;
import emulator.interrupts.Timer;

public class BlarggTests {
    public static void main(String[] args) throws IOException {

//        File romFile = new File("src/emulator/application/tests/individual/01-special.gb");

//        File romFile = new File("src/emulator/application/tests/individual/02-interrupts.gb");

//        File romFile = new File("src/emulator/application/tests/individual/03-op sp,hl.gb");
//        File romFile = new File("src/emulator/application/tests/individual/04-op r,imm.gb");
//        File romFile = new File("src/emulator/application/tests/individual/05-op rp.gb");
//        File romFile = new File("src/emulator/application/tests/individual/06-ld r,r.gb");
//        File romFile = new File("src/emulator/application/tests/individual/07-jr,jp,call,ret,rst.gb");
//        File romFile = new File("src/emulator/application/tests/individual/08-misc instrs.gb");
//        File romFile = new File("src/emulator/application/tests/individual/09-op r,r.gb");
//        File romFile = new File("src/emulator/application/tests/individual/10-bit ops.gb");
//        File romFile = new File("src/emulator/application/tests/individual/11-op a,(hl).gb");
        File romFile = new File("src/test/java/emulator/application/tests/cpu_instrs.gb");
        // File romFile = new File("src/test/java/emulator/application/tests/source/test.gb");
        byte[] romBytes = Files.readAllBytes(romFile.toPath());

        Cartridge rom = new Cartridge(romBytes);
        InterruptController interruptController = new InterruptController();
        Timer timer = new Timer(interruptController);
        GameboyBus bus = new GameboyBus(rom, interruptController, timer);
        GameboyRegisters registers = new GameboyRegisters(bus);
        InstructionFetcher instructionFetcher = new InstructionFetcher(registers, bus);
        InstructionDecoder instructionDecoder = new InstructionDecoder(instructionFetcher, registers, bus, interruptController);
        CPU cpu = new CPU(bus, registers, interruptController, instructionDecoder, instructionFetcher);

        int maxCycles = Integer.MAX_VALUE;
        int currCycle = 0;
        while(currCycle++ < maxCycles) {
            int numCycles = cpu.executeNext();
            numCycles += cpu.checkInterrupts();
            timer.addCycles(numCycles*4);
        }
    }
}
