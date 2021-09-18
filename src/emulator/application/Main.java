package emulator.application;
import emulator.Bus;
import emulator.Cartridge;
import emulator.cpu.CPU;

import java.io.File;
import java.io.IOException;

public class Main {
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
        File romFile = new File("src/emulator/application/tests/cpu_instrs.gb");

        Cartridge rom = new Cartridge(romFile);
        Bus bus = new Bus(rom);
        CPU cpu = new CPU(bus);

        int maxCycles = Integer.MAX_VALUE;
        int currCycle = 0;
        while(currCycle++ < maxCycles) {
//            System.out.printf("%d: \n", currCycle);
            cpu.executeNext();
        }
    }
}
