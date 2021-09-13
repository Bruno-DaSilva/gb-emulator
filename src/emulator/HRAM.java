package emulator;

public class HRAM {
    private byte[] ram;

    public HRAM() {
        ram = new byte[127];
    }

    public byte readByteAt(int addr) {
        return ram[addr];
    }
    public void writeByteAt(int addr, byte a) {
        ram[addr] = a;
    }
}
