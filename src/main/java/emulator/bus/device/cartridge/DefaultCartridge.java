package emulator.bus.device.cartridge;

import java.util.Arrays;

public class DefaultCartridge extends Cartridge {
    public DefaultCartridge(byte[] romBytes) {
        bank0 = new ROMBank(Arrays.copyOfRange(romBytes, 0, 0x4000));
        this.cartridgeType = parseCartridgeType(romBytes);
        if (cartridgeType != CartridgeType.NONE) {
            throw new IllegalArgumentException("Mismatch between DefaultCartridge constructor and underlying cartridge type in ROM");
        }

        int numExtraRomBanks = parseRomSize(romBytes);
        if (numExtraRomBanks != 1) {
            throw new IllegalArgumentException("Only one extra ROM bank supported for no MBC.");
        }
        indexableRomBanks = new ROMBank[1];
        indexableRomBanks[0] = new ROMBank(Arrays.copyOfRange(romBytes, 0x4000, 0x8000));
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
        throw new IllegalArgumentException("Cannot write to any values with no MBC.");
    }
}
