package emulator.cartridge;

public class ROMBank {
    private byte[] bankContent;

    public ROMBank(byte[] bankContent) {
        if (bankContent.length != 0x4000) {
            throw new IllegalArgumentException("ROM Banks must be 16KB in length, not " + bankContent.length);
        }
        this.bankContent = bankContent;
    }

    public byte readByteAt(int addr) {

        return bankContent[addr];
    }
}
