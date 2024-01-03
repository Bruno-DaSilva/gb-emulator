package emulator.cpu.register;

import emulator.bus.IBus;

public class RegisterMemoryAddress implements InstructionTarget8Bit {
    private IBus bus;
    private DoubleRegister addressRegister;

    public RegisterMemoryAddress(DoubleRegister addressRegister, IBus bus) {
        this.addressRegister = addressRegister;
        this.bus = bus;
    }

    @Override
    public byte getValue() {
        return bus.readByteAt(addressRegister.getValue());
    }

    @Override
    public void setValue(byte value) {
        bus.writeByteAt(addressRegister.getValue(), value);
    }

    @Override
    public int getAccessCost() {
        return 1;
    }
}
