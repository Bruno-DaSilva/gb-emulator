package emulator.cpu.instruction;

import emulator.cpu.InstructionFetcher;

public class STOPInstruction implements Instruction {
    private InstructionFetcher instructionFetcher;

    public STOPInstruction(InstructionFetcher instructionFetcher) {
        this.instructionFetcher = instructionFetcher;
    }

    @Override
    public int execute(byte instruction) {
        byte nextByte = instructionFetcher.fetchNextByte();
        assert nextByte == 0x00;
        // TODO
        return 1;
    }
}
