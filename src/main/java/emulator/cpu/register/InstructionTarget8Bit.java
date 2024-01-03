package emulator.cpu.register;

public interface InstructionTarget8Bit {
    byte getValue();
    void setValue(byte value);
    int getAccessCost();
}
