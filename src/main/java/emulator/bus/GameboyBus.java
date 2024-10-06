package emulator.bus;

import emulator.HRAM;
import emulator.WRAM;
import emulator.interrupts.InterruptController;
import emulator.interrupts.Timer;

public class GameboyBus implements IBus {
    // 0000	- 3FFF emulator.ROM bank 0
    // 4000 - 7FFF emulator.ROM bank 1 (switchable)
    // C000 - CFFF work ram
    // D000 - DFFF work ram
    private final IBusDevice cartridge;
    private final WRAM wram1;
    private final WRAM wram2;
    private final HRAM hram;
    private final InterruptController interruptController;
    private final Timer timer;
    private final ISerialHandler serialHandler;

    private byte serialData;

    public GameboyBus(IBusDevice cartridge, InterruptController interruptController, Timer timer, ISerialHandler serialHandler) {
        this.cartridge = cartridge;
        this.interruptController = interruptController;
        this.timer = timer;
        this.wram1 = new WRAM();
        this.wram2 = new WRAM();
        this.hram = new HRAM();
        this.serialHandler = serialHandler;
    }

    public byte readByteAt(int addr) {
        if (addr < 0x8000) {
            return cartridge.readByteAt(addr);
        }
        if (addr >= 0xC000 && addr < 0xD000) {
            return wram1.readByteAt(addr - 0xC000);
        }
        if (addr >= 0xD000 && addr < 0xE000) {
            return wram2.readByteAt(addr - 0xD000);
        }
        if (Integer.compareUnsigned(addr, 0x8000) >= 0 && Integer.compareUnsigned(addr, 0xA000) < 0) {
            // PPU
            return 0;
        }
        if (addr >= 0xFF40 && addr <= 0xFF45 || addr >= 0xFF47 && addr <= 0xFF4B) {
            // PPU
            if (addr == 0xFF44)
                return (byte) 0x90; // STUB
            return 0;
        }
        if (addr >= 0xFF80 && addr <= 0xFFEF) {
            return hram.readByteAt(addr - 0xFF80);
        }
        if (addr == 0xFF4D) {
            return (byte) 0xFF;
        }
        if (addr == 0xFF0F) {
            return interruptController.getInterruptFlag();
        }
        if (addr == 0xFF05) {
            // TIMA
            return timer.getTIMA();
        }
        if (addr == 0xFF06) {
            // TMA
            return timer.getTMA();
        }
        if (addr == 0xFF07) {
            // TAC
            return timer.getTAC();
        }
        throw new IndexOutOfBoundsException("address " + String.format("0x%02X%n", addr) + "cannot be read from");
    }
    public void writeByteAt(int addr, byte value) {
        if (addr < 0x8000 || (addr >= 0xA000 && addr < 0xC000)) {
//            throw new IllegalArgumentException("Cannot write to ROM at address " + String.format("0x%02X%n", addr) + " with value " + String.format("0x%02X%n", value));
            cartridge.writeByteAt(addr, value);
            return;
        }
        if (addr >= 0xC000 && addr < 0xD000) {
            wram1.writeByteAt(addr - 0xC000, value);
            return;
        }
        if (addr >= 0xD000 && addr < 0xE000) {
            wram2.writeByteAt(addr - 0xD000, value);
            return;
        }
        if (Integer.compareUnsigned(addr, 0x8000) >= 0 && Integer.compareUnsigned(addr, 0xA000) < 0) {
            // PPU
            return;
        }
        if (addr >= 0xFF40 && addr <= 0xFF45 || addr >= 0xFF47 && addr <= 0xFF4B) {
            // PPU
            return;
        }
        if (addr >= 0xFF80 && addr <= 0xFFEF) {
            hram.writeByteAt(addr - 0xFF80, value);
            return;
        }
        if (addr >= 0xFF00) {
            handleIOWrite(addr, value);
            return;
        }

        throw new IndexOutOfBoundsException("address " + String.format("0x%02X%n", addr) + "cannot be written to");
    }

    private void handleIOWrite(int addr, byte value) {
        if (addr == 0xFF01) {
            // serial data
            serialData = value;
//            System.out.println("Writing SERIAL data... " + String.format("0x%02X", addr) + " with value " + String.format("0x%02X", value));
        } else if (addr == 0xFF02) {
            // serial control
            if ((value & 0x80) == 0x80) {
                char dataToWrite = (char) (serialData & 0xFF);
                serialHandler.writeSerial(dataToWrite);
            }
//            System.out.println("Writing SERIAL control... " + String.format("0x%02X", addr) + " with value " + String.format("0x%02X", value));
        } else if (addr == 0xFF04) {
//            System.out.println("Writing to special DIV address " + String.format("0x%02X", addr) + " with value " + String.format("0x%02X", value));
        } else if (addr == 0xFF05) {
//            System.out.println("Writing to TIMER TIMA address " + String.format("0x%02X", addr) + " with value " + String.format("0x%02X", value));
            timer.setTIMA(value);
        } else if (addr == 0xFF06) {
//            System.out.println("Writing to TIMER TMA address " + String.format("0x%02X", addr) + " with value " + String.format("0x%02X", value));
            timer.setTMA(value);
        } else if (addr == 0xFF07) {
//            System.out.println("Writing to TIMER TAC " + String.format("0x%02X", addr) + " with value " + String.format("0x%02X", value));
            timer.setTAC(value);
        } else if (addr >= 0xFF10 && addr <= 0xFF14) {
//            System.out.println("Writing to special SOUND address " + String.format("0x%02X", addr) + " with value " + String.format("0x%02X", value));
        } else if (addr >= 0xFF16 && addr <= 0xFF1E) {
//            System.out.println("Writing to special SOUND address " + String.format("0x%02X", addr) + " with value " + String.format("0x%02X", value));
        } else if (addr >= 0xFF20 && addr <= 0xFF26) {
//            System.out.println("Writing to special SOUND address " + String.format("0x%02X", addr) + " with value " + String.format("0x%02X", value));
        } else if (addr >= 0xFF30 && addr <= 0xFF3F) {
//            System.out.println("Writing to special SOUND address " + String.format("0x%02X", addr) + " with value " + String.format("0x%02X", value));
        } else if (addr == 0xFF0F) {
//            System.out.println("Writing to special Interrupt Flag address " + String.format("0x%02X", addr) + " with value " + String.format("0x%02X", value));
            interruptController.setInterruptFlag(value);
        } else if (addr == 0xFFFF) {
//            System.out.println("Writing to special Interrupt Enable address " + String.format("0x%02X", addr) + " with value " + String.format("0x%02X", value));
            interruptController.setInterruptEnable(value);
        } else {
            throw new IndexOutOfBoundsException("address " + String.format("0x%02X%n", addr) + "cannot be written to");
        }
    }
}
