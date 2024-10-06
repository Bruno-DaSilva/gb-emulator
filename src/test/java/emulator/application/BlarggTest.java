package emulator.application;

import emulator.Gameboy;
import emulator.utils.SavingSerialHandler;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class BlarggTest {
    @Test
    public void mainTests() throws IOException {
        File romFile = new File("src/test/java/emulator/application/tests/cpu_instrs.gb");
        byte[] romBytes = Files.readAllBytes(romFile.toPath());

        int maxCycles = 24654872; // exact number of cycles before we get correct output
        SavingSerialHandler savingSerialHandler = new SavingSerialHandler();
        Gameboy gameboy = new Gameboy.BusBuilder().cartridge(romBytes).serialHandler(savingSerialHandler).buildBus().buildCpu().build();

        gameboy.runCpuFor(maxCycles);

        assertEquals("cpu_instrs\n\n01:ok  02:ok  03:ok  04:ok  05:ok  06:ok  07:ok  08:ok  09:ok  10:ok  11:ok  \n\nPassed all tests\n", savingSerialHandler.getSavedData());
    }

    @Test
    public void test01() throws IOException {
        File romFile = new File("src/test/java/emulator/application/tests/individual/01-special.gb");
        byte[] romBytes = Files.readAllBytes(romFile.toPath());

        int maxCycles = 2000000; // exact number of cycles before we get correct output
        SavingSerialHandler savingSerialHandler = new SavingSerialHandler();
        Gameboy gameboy = new Gameboy.BusBuilder().cartridge(romBytes).serialHandler(savingSerialHandler).buildBus().buildCpu().build();

        gameboy.runCpuFor(maxCycles);

        assertEquals("01-special\n\n\nPassed\n", savingSerialHandler.getSavedData());
    }

    @Test
    public void test02() throws IOException {
        File romFile = new File("src/test/java/emulator/application/tests/individual/02-interrupts.gb");
        byte[] romBytes = Files.readAllBytes(romFile.toPath());

        int maxCycles = 1000000; // exact number of cycles before we get correct output
        SavingSerialHandler savingSerialHandler = new SavingSerialHandler();
        Gameboy gameboy = new Gameboy.BusBuilder().cartridge(romBytes).serialHandler(savingSerialHandler).buildBus().buildCpu().build();

        gameboy.runCpuFor(maxCycles);

        assertEquals("02-interrupts\n\n\nPassed\n", savingSerialHandler.getSavedData());
    }

    @Test
    public void test03() throws IOException {
        File romFile = new File("src/test/java/emulator/application/tests/individual/03-op sp,hl.gb");
        byte[] romBytes = Files.readAllBytes(romFile.toPath());

        int maxCycles = 1067113; // exact number of cycles before we get correct output
        SavingSerialHandler savingSerialHandler = new SavingSerialHandler();
        Gameboy gameboy = new Gameboy.BusBuilder().cartridge(romBytes).serialHandler(savingSerialHandler).buildBus().buildCpu().build();

        gameboy.runCpuFor(maxCycles);

        assertEquals("03-op sp,hl\n\n\nPassed\n", savingSerialHandler.getSavedData());
    }

    @Test
    public void test04() throws IOException {
        File romFile = new File("src/test/java/emulator/application/tests/individual/04-op r,imm.gb");
        byte[] romBytes = Files.readAllBytes(romFile.toPath());

        int maxCycles = 2000000; // exact number of cycles before we get correct output
        SavingSerialHandler savingSerialHandler = new SavingSerialHandler();
        Gameboy gameboy = new Gameboy.BusBuilder().cartridge(romBytes).serialHandler(savingSerialHandler).buildBus().buildCpu().build();

        gameboy.runCpuFor(maxCycles);

        assertEquals("04-op r,imm\n\n\nPassed\n", savingSerialHandler.getSavedData());
    }

    @Test
    public void test05() throws IOException {
        File romFile = new File("src/test/java/emulator/application/tests/individual/05-op rp.gb");
        byte[] romBytes = Files.readAllBytes(romFile.toPath());

        int maxCycles = 2000000; // exact number of cycles before we get correct output
        SavingSerialHandler savingSerialHandler = new SavingSerialHandler();
        Gameboy gameboy = new Gameboy.BusBuilder().cartridge(romBytes).serialHandler(savingSerialHandler).buildBus().buildCpu().build();

        gameboy.runCpuFor(maxCycles);

        assertEquals("05-op rp\n\n\nPassed\n", savingSerialHandler.getSavedData());
    }

    @Test
    public void test06() throws IOException {
        File romFile = new File("src/test/java/emulator/application/tests/individual/06-ld r,r.gb");
        byte[] romBytes = Files.readAllBytes(romFile.toPath());

        int maxCycles = 2000000; // exact number of cycles before we get correct output
        SavingSerialHandler savingSerialHandler = new SavingSerialHandler();
        Gameboy gameboy = new Gameboy.BusBuilder().cartridge(romBytes).serialHandler(savingSerialHandler).buildBus().buildCpu().build();

        gameboy.runCpuFor(maxCycles);

        assertEquals("06-ld r,r\n\n\nPassed\n", savingSerialHandler.getSavedData());
    }

    @Test
    public void test07() throws IOException {
        File romFile = new File("src/test/java/emulator/application/tests/individual/07-jr,jp,call,ret,rst.gb");
        byte[] romBytes = Files.readAllBytes(romFile.toPath());

        int maxCycles = 2000000; // exact number of cycles before we get correct output
        SavingSerialHandler savingSerialHandler = new SavingSerialHandler();
        Gameboy gameboy = new Gameboy.BusBuilder().cartridge(romBytes).serialHandler(savingSerialHandler).buildBus().buildCpu().build();

        gameboy.runCpuFor(maxCycles);

        assertEquals("07-jr,jp,call,ret,rst\n\n\nPassed\n", savingSerialHandler.getSavedData());
    }

    @Test
    public void test08() throws IOException {
        File romFile = new File("src/test/java/emulator/application/tests/individual/08-misc instrs.gb");
        byte[] romBytes = Files.readAllBytes(romFile.toPath());

        int maxCycles = 2000000; // exact number of cycles before we get correct output
        SavingSerialHandler savingSerialHandler = new SavingSerialHandler();
        Gameboy gameboy = new Gameboy.BusBuilder().cartridge(romBytes).serialHandler(savingSerialHandler).buildBus().buildCpu().build();

        gameboy.runCpuFor(maxCycles);

        assertEquals("08-misc instrs\n\n\nPassed\n", savingSerialHandler.getSavedData());
    }

    // //        File romFile = new File("src/emulator/application/tests/individual/09-op r,r.gb");
// //        File romFile = new File("src/emulator/application/tests/individual/10-bit ops.gb");
// //        File romFile = new File("src/emulator/application/tests/individual/11-op a,(hl).gb");

    @Test
    public void test09() throws IOException {
        File romFile = new File("src/test/java/emulator/application/tests/individual/09-op r,r.gb");
        byte[] romBytes = Files.readAllBytes(romFile.toPath());

        int maxCycles = 5000000; // exact number of cycles before we get correct output
        SavingSerialHandler savingSerialHandler = new SavingSerialHandler();
        Gameboy gameboy = new Gameboy.BusBuilder().cartridge(romBytes).serialHandler(savingSerialHandler).buildBus().buildCpu().build();

        gameboy.runCpuFor(maxCycles);

        assertEquals("09-op r,r\n\n\nPassed\n", savingSerialHandler.getSavedData());
    }

    @Test
    public void test10() throws IOException {
        File romFile = new File("src/test/java/emulator/application/tests/individual/10-bit ops.gb");
        byte[] romBytes = Files.readAllBytes(romFile.toPath());

        int maxCycles = 7000000; // exact number of cycles before we get correct output
        SavingSerialHandler savingSerialHandler = new SavingSerialHandler();
        Gameboy gameboy = new Gameboy.BusBuilder().cartridge(romBytes).serialHandler(savingSerialHandler).buildBus().buildCpu().build();

        gameboy.runCpuFor(maxCycles);

        assertEquals("10-bit ops\n\n\nPassed\n", savingSerialHandler.getSavedData());
    }

    @Test
    public void test11() throws IOException {
        File romFile = new File("src/test/java/emulator/application/tests/individual/11-op a,(hl).gb");
        byte[] romBytes = Files.readAllBytes(romFile.toPath());

        int maxCycles = 8000000; // exact number of cycles before we get correct output
        SavingSerialHandler savingSerialHandler = new SavingSerialHandler();
        Gameboy gameboy = new Gameboy.BusBuilder().cartridge(romBytes).serialHandler(savingSerialHandler).buildBus().buildCpu().build();

        gameboy.runCpuFor(maxCycles);

        assertEquals("11-op a,(hl)\n\n\nPassed\n", savingSerialHandler.getSavedData());
    }

}
