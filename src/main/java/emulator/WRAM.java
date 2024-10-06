package emulator;

import emulator.bus.IBusDevice;

public class WRAM implements IBusDevice {
    private final byte[] ram;

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
