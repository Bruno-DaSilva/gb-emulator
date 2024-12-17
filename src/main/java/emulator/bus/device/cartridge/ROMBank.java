package emulator.bus.device.cartridge;

public class ROMBank {
    private byte[] bankContent;

    public ROMBank(byte[] bankContent) {
        if (bankContent == null) {
            throw new IllegalArgumentException("ROM Bank must not be null");
        }
        if (bankContent.length != 0x4000) {
            throw new IllegalArgumentException("ROM Banks must be 16KB in length, not " + bankContent.length);
        }
        this.bankContent = bankContent;
    }

    public byte readByteAt(int addr) {

        return bankContent[addr];
    }
}
