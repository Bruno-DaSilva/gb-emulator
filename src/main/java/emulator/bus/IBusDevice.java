package emulator.bus;

public interface IBusDevice {
    byte readByteAt(int addr);
    void writeByteAt(int addr, byte a);
}
