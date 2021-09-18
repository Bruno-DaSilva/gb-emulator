package emulator.cpu;

import emulator.Bus;

public class RegisterMemoryAddress implements InstructionTarget8Bit {
    private Bus bus;
    private DoubleRegister addressRegister;

    public RegisterMemoryAddress(DoubleRegister addressRegister, Bus bus) {
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
}
