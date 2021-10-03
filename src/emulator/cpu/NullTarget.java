package emulator.cpu;

public class NullTarget implements InstructionTarget8Bit {
    @Override
    public byte getValue() {
        throw new UnsupportedOperationException("NullTarget methods cannot be called.");
    }

    @Override
    public void setValue(byte value) {
        throw new UnsupportedOperationException("NullTarget methods cannot be called.");
    }

    @Override
    public int getAccessCost() {
        throw new UnsupportedOperationException("NullTarget methods cannot be called.");
    }
}
