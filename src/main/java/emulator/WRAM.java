package emulator;

public class WRAM {
    private byte[] ram;

    public WRAM() {
        ram = new byte[4096];
    }

    public byte readByteAt(int addr) {
        return ram[addr];
    }
    public void writeByteAt(int addr, byte a) {
        ram[addr] = a;
    }
}
