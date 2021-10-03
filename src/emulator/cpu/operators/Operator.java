package emulator.cpu.operators;

import emulator.cpu.Cost;
import emulator.cpu.Flag;
import emulator.cpu.FlagModification;
import emulator.cpu.InstructionTarget8Bit;

import java.util.Map;
import java.util.Set;

public abstract class Operator {

    public Cost apply(byte dest, byte source, int carry) {
        throw new UnsupportedOperationException();
    };
    public Cost apply(byte dest, int carry) {
        throw new UnsupportedOperationException();
    };

    abstract Map<Flag, FlagModification> getFlagModifications(InstructionTarget8Bit destination, InstructionTarget8Bit source);
}
