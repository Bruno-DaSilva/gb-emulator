package emulator.cpu.operators;

import emulator.cpu.*;

import java.util.HashMap;
import java.util.Map;

public class RLC extends Operator {

    @Override
    public Cost getOutput(byte dest, int carry) {

        byte output;
        int msb = dest & 0x80;
        output = ((byte) ((dest & 0xFF) << 1));
        output = (byte) (output | (msb >>> 7));

        return output;
    }

    @Override
    public Map<Flag, FlagModification> getFlagModifications(byte leftInput, byte rightInput, byte finalValue, int carry) {
        Map<Flag, FlagModification> flagMap = new HashMap<>();

        int msb = leftInput & 0x80;
        flagMap.put(Flag.Z, new FlagModification(finalValue == 0));
        flagMap.put(Flag.N, new FlagModification(false));
        flagMap.put(Flag.H, new FlagModification(false));
        flagMap.put(Flag.C, new FlagModification(msb == 0x80));


        return flagMap;
    }

    public Cost getCost(InstructionTarget8Bit destination) {
        return new Cost(2 + destination.getAccessCost());
    }
}
