package emulator.cpu;

public interface InstructionTarget8Bit {
    byte getValue();
    void setValue(byte value);
    int getAccessCost();
}
