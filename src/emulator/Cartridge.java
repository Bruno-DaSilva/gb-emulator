package emulator;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;

public class Cartridge {
    private final CartridgeType cartridgeType;
    private ROMBank bank0;
    private ROMBank bank1;
    private ROMBank[] indexableRomBanks;
    private int currentActiveRomBank;
    private int lowerBankBits;
    private int higherBankBits;

    public Cartridge(File romFile) throws IOException {
        byte[] romBytes = Files.readAllBytes(romFile.toPath());

        bank0 = new ROMBank(Arrays.copyOfRange(romBytes, 0, 0x4000));
        switch(romBytes[0x0147]) {
            case 0x00:
                // No MBC
                this.cartridgeType = CartridgeType.NONE;
                break;
            case 0x01:
            case 0x02:
            case 0x03:
                // MBC1
                this.cartridgeType = CartridgeType.MBC1;
                break;
            default:
                throw new IllegalArgumentException("Cartridge type " + romBytes[0x0147] + " not supported.");
        }

        if (cartridgeType.equals(CartridgeType.NONE)) {
            bank1 = new ROMBank(Arrays.copyOfRange(romBytes, 0x4000, 0x8000));
        } else if (cartridgeType.equals(CartridgeType.MBC1)) {
            byte romSizeCode = romBytes[0x0148];
            if ((romSizeCode & 0xFF) > 0x08) {
                throw new IllegalArgumentException("Invalid rom size code at 0x148: " + romSizeCode);
            }
            int numExtraRomBanks = (0x02 << romSizeCode) - 1;
            indexableRomBanks = new ROMBank[numExtraRomBanks];
            for (int i = 0; i < indexableRomBanks.length; ++i) {
                int from = 0x4000 * (i+1);
                int to = 0x4000 * (i+1) + 0x4000;
                indexableRomBanks[i] = new ROMBank(Arrays.copyOfRange(romBytes, from, to));
                currentActiveRomBank = 0;
            }
        }

    }

    public byte readByteAt(int addr) {
        if (cartridgeType.equals(CartridgeType.NONE)) {
            if (addr < 0x4000) {
                return bank0.readByteAt(addr);
            } else if (addr < 0x8000) {
                return bank1.readByteAt(addr - 0x4000);
            } else {
                throw new IndexOutOfBoundsException("Cartridge read address " + String.format("0x%02X", addr) + " exceeds memory");
            }
        } else if (cartridgeType.equals(CartridgeType.MBC1)) {
            if (addr < 0x4000) {
                return bank0.readByteAt(addr);
            } else if (addr < 0x8000) {
                return indexableRomBanks[currentActiveRomBank].readByteAt(addr - 0x4000);
            } else {
                throw new IndexOutOfBoundsException("Cartridge read address " + String.format("0x%02X", addr) + " exceeds memory");
            }
        } else {
            throw new UnsupportedOperationException("Cartridge does not support banking mode: " + cartridgeType);
        }
    }

    public void writeByteAt(int addr, byte value) {
        if (cartridgeType.equals(CartridgeType.NONE)) {
            throw new IllegalArgumentException("Cannot write to any values with no MBC.");
        } else if (cartridgeType.equals(CartridgeType.MBC1)) {
            if (addr < 0x2000) {
                // ram enable
                System.out.println("RAM enable");
            } else if (addr < 0x4000) {
                // Low order (5 bits) ROM Bank number
                if ((value & 0b11111) == 0) {
                    value = 0b00001;
                }
                lowerBankBits = value & 0b11111;
                currentActiveRomBank = (lowerBankBits | (higherBankBits << 5)) - 1;
//                System.err.print(" Switching to ROM bank " + currentActiveRomBank + " ");
            } else if (addr < 0x6000) {
                // ram bank number OR upper bits of rom bank number
                higherBankBits = value & 0b11;
                currentActiveRomBank = (lowerBankBits | (higherBankBits << 5)) - 1;
//                System.err.print(" Switching to ROM bank " + currentActiveRomBank + " ");
            } else if (addr < 0x8000) {
                // banking mode select
                if ((value & 0b1) == 0) {
                    // normal
                    System.out.println("Banking mode");
                } else {
                    // RAM Banking Mode / Advanced ROM Banking Mode
                    throw new UnsupportedOperationException("Cartridge does not support advanced banking mode.");
                }
            }
        }
    }
}
