package emulator;

public class Bus {
    // 0000	- 3FFF emulator.ROM bank 0
    // 4000 - 7FFF emulator.ROM bank 1 (switchable)

    // C000 - CFFF work ram
    // D000 - DFFF work ram

    private ROM rom;
    private WRAM wram1;
    private WRAM wram2;
    private HRAM hram;
    private byte serialData;

    public Bus(ROM rom) {
        this.wram1 = new WRAM();
        this.wram2 = new WRAM();
        this.hram = new HRAM();
        this.rom = rom;
    }

    public byte readByteAt(int addr) {
        if (addr < 0x8000) {
            return rom.readByteAt(addr);
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
        throw new IndexOutOfBoundsException("address " + String.format("0x%02X%n", addr) + "cannot be read from");
    }
    public void writeByteAt(int addr, byte value) {
        if (addr < 0x8000) {
            throw new IllegalArgumentException("Cannot write to ROM at address " + String.format("0x%02X%n", addr) + " with value " + String.format("0x%02X%n", value));
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
                System.err.print((char) (serialData & 0xFF));
            }
//            System.out.println("Writing SERIAL control... " + String.format("0x%02X", addr) + " with value " + String.format("0x%02X", value));
        } else if (addr >= 0xFF04 && addr <= 0xFF07) {
            System.out.println("Writing to special TIMER address " + String.format("0x%02X", addr) + " with value " + String.format("0x%02X", value));
        } else if (addr >= 0xFF10 && addr <= 0xFF14) {
            System.out.println("Writing to special SOUND address " + String.format("0x%02X", addr) + " with value " + String.format("0x%02X", value));
        } else if (addr >= 0xFF16 && addr <= 0xFF1E) {
            System.out.println("Writing to special SOUND address " + String.format("0x%02X", addr) + " with value " + String.format("0x%02X", value));
        } else if (addr >= 0xFF20 && addr <= 0xFF26) {
            System.out.println("Writing to special SOUND address " + String.format("0x%02X", addr) + " with value " + String.format("0x%02X", value));
        } else if (addr >= 0xFF30 && addr <= 0xFF3F) {
            System.out.println("Writing to special SOUND address " + String.format("0x%02X", addr) + " with value " + String.format("0x%02X", value));
        } else if (addr == 0xFF0F) {
            System.out.println("Writing to special Interrupt Flag address " + String.format("0x%02X", addr) + " with value " + String.format("0x%02X", value));
        } else if (addr == 0xFFFF) {
            System.out.println("Writing to special Interrupt Enable address " + String.format("0x%02X", addr) + " with value " + String.format("0x%02X", value));
        } else {
            throw new IndexOutOfBoundsException("address " + String.format("0x%02X%n", addr) + "cannot be written to");
        }
    }
}
