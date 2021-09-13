package emulator;

public class ROM {
    private byte[] romContent;

    public ROM(byte[] romContent) {
        this.romContent = romContent;
    }

    public byte readByteAt(int addr) {
        return romContent[addr];
    }
}
