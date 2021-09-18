package emulator.cpu;

public class Register implements InstructionTarget8Bit {
    private byte value;

    public Register(byte value) {
        this.value = value;
    }

    public byte getValue() {
        return value;
    }

    public void setValue(byte value) {
        this.value = value;
    }
}
