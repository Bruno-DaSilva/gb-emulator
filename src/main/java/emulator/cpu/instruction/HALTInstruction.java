package emulator.cpu.instruction;

import emulator.interrupts.InterruptController;

public class HALTInstruction implements Instruction {
    private InterruptController interruptController;

    public HALTInstruction(InterruptController interruptController) {

        this.interruptController = interruptController;
    }

    @Override
    public int execute(byte instruction) {
        interruptController.setHalted(true);
        return 1;
    }
}
