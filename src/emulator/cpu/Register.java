package emulator.cpu;

public class Register implements InstructionTarget8Bit {
    protected byte value;

    public Register(byte value) {
        this.value = value;
    }

    public byte getValue() {
        return value;
    }

    public void setValue(byte value) {
        this.value = value;
    }

    @Override
    public int getAccessCost() {
        return 0;
    }
}
