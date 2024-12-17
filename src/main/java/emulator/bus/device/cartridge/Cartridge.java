package emulator.bus.device.cartridge;

import emulator.bus.device.IBusDevice;

import java.util.Arrays;

public class Cartridge implements IBusDevice {
    private final CartridgeType cartridgeType;
    private ROMBank bank0;
    private ROMBank[] indexableRomBanks;
    private int currentActiveRomBank;
    private int lowerBankBits;
    private int higherBankBits;

    public Cartridge(byte[] romBytes) {
        bank0 = new ROMBank(Arrays.copyOfRange(romBytes, 0, 0x4000));
        this.cartridgeType = parseCartridgeType(romBytes);

        switch (cartridgeType) {
            case NONE -> {
                int numExtraRomBanks = parseRomSize(romBytes);
                if (numExtraRomBanks != 1) {
                    throw new IllegalArgumentException("Only one extra ROM bank supported for no MBC.");
                }
                indexableRomBanks = new ROMBank[1];
                indexableRomBanks[0] = new ROMBank(Arrays.copyOfRange(romBytes, 0x4000, 0x8000));
                currentActiveRomBank = 0;
            }
            case MBC1 -> {
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
            default ->
                    throw new IllegalArgumentException("Cartridge does not yet support cartridge type: " + cartridgeType);
        }
    }

    /**
     * 00h -  32KByte (no ROM bank switching, 2 total banks)
     * 01h -  64KByte (4 banks)
     * 02h - 128KByte (8 banks)
     * 03h - 256KByte (16 banks)
     * 04h - 512KByte (32 banks)
     * 05h -   1MByte (64 banks)
     * 06h -   2MByte (128 banks)
     * 07h -   4MByte (256 banks)
     * 08h -   8MByte (512 banks)
     *
     * @return number of extra rom banks in this cartridge
     */
    private int parseRomSize(byte[] romBytes) {
        byte romSizeCode = romBytes[0x0148];
        if (romSizeCode >= 0x00 && romSizeCode <= 0x08) {
            int romSizeInBytes = 32768 * (1 << romSizeCode);
            final int singleBankSize = 16384; // Each ROM bank is 16KB in size
            return (romSizeInBytes / singleBankSize) - 1; // Calculate based on banks
        }
        throw new IllegalArgumentException("Invalid rom size code at 0x148: " + romSizeCode);
    }

    /**
     *  00h  ROM ONLY
     *  01h  MBC1
     *  02h  MBC1+RAM
     *  03h  MBC1+RAM+BATTERY
     *  05h  MBC2
     *  06h  MBC2+BATTERY
     *  08h  ROM+RAM
     *  09h  ROM+RAM+BATTERY
     *  0Bh  MMM01
     *  0Ch  MMM01+RAM
     *  0Dh  MMM01+RAM+BATTERY
     *  0Fh  MBC3+TIMER+BATTERY
     *  10h  MBC3+TIMER+RAM+BATTERY
     *  11h  MBC3
     *  12h  MBC3+RAM
     *  13h  MBC3+RAM+BATTERY
     *  19h  MBC5
     *  1Ah  MBC5+RAM
     *  1Bh  MBC5+RAM+BATTERY
     *  1Ch  MBC5+RUMBLE
     *  1Dh  MBC5+RUMBLE+RAM
     *  1Eh  MBC5+RUMBLE+RAM+BATTERY
     *  20h  MBC6
     *  22h  MBC7+SENSOR+RUMBLE+RAM+BATTERY
     *  FCh  POCKET CAMERA
     *  FDh  BANDAI TAMA5
     *  FEh  HuC3
     *  FFh  HuC1+RAM+BATTERY
     */
    private CartridgeType parseCartridgeType(byte[] romBytes) {
        return switch (romBytes[0x0147]) {
            case 0x00 ->
                // No MBC
                CartridgeType.NONE;
            case 0x01, 0x02, 0x03 ->
                // MBC1
                CartridgeType.MBC1;
            // TODO: not yet implemented
//            case 0x05, 0x06 ->
//                // MBC2
//                CartridgeType.MBC2;
//            case 0x08, 0x09 ->
//                // ROM + RAM
//                CartridgeType.ROM_RAM;
//            case 0x0B, 0x0C, 0x0D ->
//                // MMM01
//                CartridgeType.MMM01;
//            case 0x0F, 0x10, 0x11, 0x12, 0x13 ->
//                // MBC3
//                CartridgeType.MBC3;
//            case 0x19, 0x1A, 0x1B ->
//                // MBC5
//                CartridgeType.MBC5;
//            case 0x1C, 0x1D, 0x1E ->
//                // MBC5 + RUMBLE
//                CartridgeType.MBC5_RUMBLE;
//            case 0x20 ->
//                // MBC6
//                CartridgeType.MBC6;
//            case 0x22 ->
//                // MBC7
//                CartridgeType.MBC7;
//            case (byte) 0xFC ->
//                // POCKET CAMERA
//                CartridgeType.POCKET_CAMERA;
//            case (byte) 0xFD ->
//                // BANDAI TAMA5
//                CartridgeType.BANDAI_TAMA5;
//            case (byte) 0xFE ->
//                // HuC3
//                CartridgeType.HuC3;
//            case (byte) 0xFF ->
//                // HuC1 + RAM + BATTERY
//                CartridgeType.HuC1;
            default -> throw new IllegalArgumentException("Cartridge type " + romBytes[0x0147] + " not supported.");
        };
    }

    public byte readByteAt(int addr) {
        switch (cartridgeType) {
            case NONE -> {
                if (addr < 0x4000) {
                    return bank0.readByteAt(addr);
                } else if (addr < 0x8000) {
                    return indexableRomBanks[currentActiveRomBank].readByteAt(addr - 0x4000);
                } else {
                    throw new IndexOutOfBoundsException("Cartridge read address " + String.format("0x%02X", addr) + " exceeds memory");
                }
            }
            case MBC1 -> {
                if (addr < 0x4000) {
                    return bank0.readByteAt(addr);
                } else if (addr < 0x8000) {
                    return indexableRomBanks[currentActiveRomBank].readByteAt(addr - 0x4000);
                } else {
                    throw new IndexOutOfBoundsException("Cartridge read address " + String.format("0x%02X", addr) + " exceeds memory");
                }
            }
            default ->
                    throw new UnsupportedOperationException("Cartridge does not support banking mode: " + cartridgeType);
        }
    }

    public void writeByteAt(int addr, byte value) {
        switch (cartridgeType) {
            case NONE -> throw new IllegalArgumentException("Cannot write to any values with no MBC.");
            case MBC1 -> {
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
            default ->
                    throw new UnsupportedOperationException("Cartridge does not support banking mode: " + cartridgeType);
        }
    }
}
