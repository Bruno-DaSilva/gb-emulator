package emulator.bus.device;

public interface IBusDevice {
    byte readByteAt(int addr);
    void writeByteAt(int addr, byte a);
}
