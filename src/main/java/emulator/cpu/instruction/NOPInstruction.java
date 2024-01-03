package emulator.cpu.instruction;

public class NOPInstruction implements Instruction {
    @Override
    public int execute(byte instruction) {
        return 1;
    }
}
