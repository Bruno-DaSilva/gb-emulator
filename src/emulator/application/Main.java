package emulator.application;
import emulator.Bus;
import emulator.CPU;
import emulator.ROM;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public class Main {
    public static void main(String[] args) throws IOException {

        File romFile = new File("src/emulator/application/tests/04-op r,imm.gb");
        byte[] romBytes = Files.readAllBytes(romFile.toPath());

        ROM rom = new ROM(romBytes);
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
