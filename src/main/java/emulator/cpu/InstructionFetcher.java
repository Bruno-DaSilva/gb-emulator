package emulator.cpu;

import emulator.bus.IBus;

public class InstructionFetcher {

    private GameboyRegisters registers;
    private IBus bus;

    public InstructionFetcher(GameboyRegisters registers, IBus bus) {
        this.registers = registers;
        this.bus = bus;
    }

    public byte fetchNextByte() {
        byte nextByte = bus.readByteAt(registers.getPC().getValue());
        registers.getPC().setValue(registers.getPC().getValue() + 1);
        return nextByte;
    }
}
