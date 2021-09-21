package emulator.cpu;

public class FlagRegister extends Register {

    public FlagRegister(byte value) {
        super(value);
        setValue(value);
    }

    @Override
    public byte getValue() {
        return value;
    }
    @Override
    public void setValue(byte value) {
        this.value = (byte) (value & 0xF0);
    }

    public void setZ(int value) {
        setZ(value != 0);
    }
    public void setN(int value) {
        setN(value != 0);
    }
    public void setH(int value) {
        setH(value != 0);
    }
    public void setC(int value) {
        setC(value != 0);
    }

    public void setH(byte orig, byte target, Operator operator) {
        setH(orig, target, 0, operator);
    }

    public void setH(byte orig, byte target, int carry, Operator operator) {
        int value;
        if (operator.equals(Operator.SUB)) {
            value = (((orig & 0xF) - (target & 0xF) - carry) & 0x10);
        } else if (operator.equals(Operator.ADD)) {
            value = (((orig & 0xF) + (target & 0xF) + carry) & 0x10);
        } else {
            throw new UnsupportedOperationException("Cannot calculate FlagRegister setH() automatically with operator " + operator);
        }
        setH(value);
    }

    public void setC(byte orig, byte target, Operator operator) {
        setC(orig, target, 0, operator);
    }

    public void setC(byte orig, byte target, int carry, Operator operator) {
        boolean flag;
        if (operator.equals(Operator.SUB)) {
            flag = ((target & 0xFF) + carry) > (orig & 0xFF);
        } else if (operator.equals(Operator.ADD)) {
            flag = ((orig & 0xFF) + (target & 0xFF) + carry) > 0xFF;
        } else {
            throw new UnsupportedOperationException("Cannot calculate FlagRegister setH() automatically with operator " + operator);
        }
        setC(flag);
    }

    public void setZ(boolean flag) {
        if (flag) {
            value = (byte) (value | 0b10000000);
        } else {
            value = (byte) (value & 0b01111111);
        }
    }
    public void setN(boolean flag) {
        if (flag) {
            value = (byte) (value | 0b01000000);
        } else {
            value = (byte) (value & 0b10111111);
        }
    }
    public void setH(boolean flag) {
        if (flag) {
            value = (byte) (value | 0b00100000);
        } else {
            value = (byte) (value & 0b11011111);
        }
    }
    public void setC(boolean flag) {
        if (flag) {
            value = (byte) (value | 0b00010000);
        } else {
            value = (byte) (value & 0b11101111);
        }
    }

    public boolean getZ() {
        return (value & 0b10000000) != 0;
    }
    public boolean getN() {
        return (value & 0b01000000) != 0;
    }
    public boolean getH() {
        return (value & 0b00100000) != 0;
    }
    public boolean getC() {
        return (value & 0b00010000) != 0;
    }

    @Override
    public int getAccessCost() {
        return 0;
    }
}
