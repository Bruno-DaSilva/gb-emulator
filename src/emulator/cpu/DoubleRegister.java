package emulator.cpu;

public class DoubleRegister {
    private Register higherRegister;
    private Register lowerRegister;

    public DoubleRegister(Register higherRegister, Register lowerRegister) {
        this.higherRegister = higherRegister;
        this.lowerRegister = lowerRegister;
    }

    public void setValue(int value) {
        lowerRegister.setValue((byte) (value & 0xFF));
        higherRegister.setValue((byte) ((value >> 8) & 0xFF));
    }
    public void setValue(byte higherBits, byte lowerBits) {
        higherRegister.setValue(higherBits);
        lowerRegister.setValue(lowerBits);
    }

    public int getValue() {
        return (higherRegister.getValue() & 0xFF) << 8 | (lowerRegister.getValue() & 0xFF);
    }

    public Register getLowerRegister() {
        return lowerRegister;
    }

    public Register getHigherRegister() {
        return higherRegister;
    }
}
