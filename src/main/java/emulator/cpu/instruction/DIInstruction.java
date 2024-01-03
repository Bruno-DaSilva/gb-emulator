package emulator.cpu.instruction;

import emulator.interrupts.InterruptController;

public class DIInstruction implements Instruction {
    private InterruptController interruptController;

    public DIInstruction(InterruptController interruptController) {
        this.interruptController = interruptController;
    }

    @Override
    public int execute(byte instruction) {
        interruptController.setInterruptMasterEnable(false);
        return 1;
    }
}
