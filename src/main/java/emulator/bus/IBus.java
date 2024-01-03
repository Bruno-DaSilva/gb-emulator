package emulator.bus;

public interface IBus {
    byte readByteAt(int addr);
    void writeByteAt(int addr, byte value);
}
