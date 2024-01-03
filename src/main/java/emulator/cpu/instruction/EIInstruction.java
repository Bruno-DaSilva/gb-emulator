package emulator.cpu.instruction;

import emulator.interrupts.InterruptController;

public class EIInstruction implements Instruction {
    private InterruptController interruptController;

    public EIInstruction(InterruptController interruptController) {
        this.interruptController = interruptController;
    }

    @Override
    public int execute(byte instruction) {
        interruptController.setInterruptMasterEnable(true);
        return 1;
    }
}
