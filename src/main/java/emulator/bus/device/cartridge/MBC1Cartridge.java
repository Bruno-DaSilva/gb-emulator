package emulator.bus.device.cartridge;

import java.util.Arrays;

public class MBC1Cartridge extends Cartridge {

    public MBC1Cartridge(byte[] romBytes) {
        bank0 = new ROMBank(Arrays.copyOfRange(romBytes, 0, 0x4000));

        this.cartridgeType = parseCartridgeType(romBytes);
        if (cartridgeType != CartridgeType.MBC1) {
            throw new IllegalArgumentException("Mismatch between MBC1Cartridge constructor and underlying cartridge type in ROM");
        }

        // (max 2MByte ROM and/or 32 KiB RAM)
        int numExtraRomBanks = parseRomSize(romBytes);
        indexableRomBanks = new ROMBank[numExtraRomBanks];
        for (int i = 0; i < indexableRomBanks.length; ++i) {
            int from = 0x4000 * (i + 1);
            int to = 0x4000 * (i + 1) + 0x4000;
            indexableRomBanks[i] = new ROMBank(Arrays.copyOfRange(romBytes, from, to));
        }
        currentActiveRomBank = 0;
    }

    public byte readByteAt(int addr) {
        if (addr < 0x4000) {
            return bank0.readByteAt(addr);
        } else if (addr < 0x8000) {
            return indexableRomBanks[currentActiveRomBank].readByteAt(addr - 0x4000);
        } else {
            throw new IndexOutOfBoundsException("Cartridge read address " + String.format("0x%02X", addr) + " exceeds memory");
        }
    }

    public void writeByteAt(int addr, byte value) {
        if (addr < 0x2000) {
            // ram enable
            System.out.println("RAM enable");
            throw new UnsupportedOperationException("Cartridge does not yet support RAM.");
        } else if (addr < 0x4000) {
            // Low order (5 bits) ROM Bank number
            if ((value & 0b11111) == 0) {
                value = 0b00001;
            }
            lowerBankBits = value & 0b11111;
            currentActiveRomBank = (lowerBankBits | (higherBankBits << 5)) - 1;
//                System.err.print(" Switching to ROM bank " + currentActiveRomBank + " ");
        } else if (addr < 0x6000) {
            // TODO: ram bank number OR upper bits of rom bank number, depending on cartridge size
            throw new UnsupportedOperationException("Cartridge does not yet support RAM bank switching.");
            // higherBankBits = value & 0b11;
            // currentActiveRomBank = (lowerBankBits | (higherBankBits << 5)) - 1;
//                System.err.print(" Switching to ROM bank " + currentActiveRomBank + " ");
        } else if (addr < 0x8000) {
            // banking mode select
            if ((value & 0b1) == 0) {
                // normal
                // 0000–3FFF is locked to ROM bank 0
                // A000–BFFF is locked to RAM bank 0
                System.out.println("Switched MBC1 banking mode to locked/default.");
            } else {
                // RAM Banking Mode / Advanced ROM Banking Mode
                throw new UnsupportedOperationException("Cartridge does not yet support advanced banking mode.");
            }
        }
    }
}
