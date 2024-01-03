package emulator.cpu;

public enum Operator {
    ADD,
    SUB,
    AND,
    OR,
    ADC,
    SBC,
    XOR,
    CP;

    byte apply(byte left, byte right) {
        return apply(left, right, 0);
    }
    byte apply(byte left, byte right, int carry) {
        if (this.equals(Operator.ADD)) {
            return (byte) ((left & 0xFF) + (right & 0xFF));
        } else if (this.equals(Operator.ADC)) {
            return (byte) ((left & 0xFF) + (right & 0xFF) + carry);
        } else if (this.equals(Operator.SUB) || this.equals(Operator.CP)) {
            return (byte) ((left & 0xFF) - (right & 0xFF));
        } else if (this.equals(Operator.SBC)) {
            return (byte) ((left & 0xFF) - (right & 0xFF) - carry);
        } else if (this.equals(Operator.AND)) {
            return (byte) (left & right);
        } else if (this.equals(Operator.OR)) {
            return (byte) (left | right);
        } else if (this.equals(Operator.XOR)) {
            return (byte) (left ^ right);
        } else {
            throw new UnsupportedOperationException("Cannot calculate Operator apply() automatically with operator " + this);
        }
    }
}