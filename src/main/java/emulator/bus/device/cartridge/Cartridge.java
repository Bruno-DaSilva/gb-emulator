package emulator.bus.device.cartridge;

import emulator.bus.device.IBusDevice;

public abstract class Cartridge implements IBusDevice {
    protected CartridgeType cartridgeType;
    protected ROMBank bank0;
    protected ROMBank[] indexableRomBanks;
    protected int currentActiveRomBank;
    protected int lowerBankBits;
    protected int higherBankBits;

    public static Cartridge createCartridge(byte[] romBytes) {
        CartridgeType cartridgeType = parseCartridgeType(romBytes);
        switch (cartridgeType) {
            case NONE -> {
                return new DefaultCartridge(romBytes);
            }
            case MBC1 -> {
                return new MBC1Cartridge(romBytes);
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
    protected int parseRomSize(byte[] romBytes) {
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
    protected static CartridgeType parseCartridgeType(byte[] romBytes) {
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

    public abstract byte readByteAt(int addr);

    public abstract void writeByteAt(int addr, byte value);
}
