package emulator.cpu.instruction;

public class EmptyInstruction implements Instruction {

    private int numCycles;

    public EmptyInstruction(int numCycles) {
        this.numCycles = numCycles;
    }

    @Override
    public int execute(byte instruction) {
        return numCycles;
    }
}
