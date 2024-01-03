package emulator.interrupts;

public class InterruptController {

    byte interruptFlag;
    byte interruptEnable;
    private boolean masterEnable;
    private boolean isHalted;

    public void setInterruptFlag(byte value) {
        interruptFlag = (byte) (value & 0b11111);
    }
    public byte getInterruptFlag() {
        return interruptFlag;
    }

    public byte getInterruptEnable() {
        return interruptEnable;
    }

    public void setInterruptEnable(byte value) {
        interruptEnable = (byte) (value & 0b11111);
    }

    public boolean interruptReady() {
        if (masterEnable) {
            if ((interruptFlag & interruptEnable) > 0) {
                return true;
            }
        }
        return false;
    }

    public int getHighestPriorityInterruptAddress() {
        if (!masterEnable) {
            throw new UnsupportedOperationException("Cannot send interrupt when IME is disabled");
        }
        int enabledFlags = (interruptFlag & interruptEnable);
        if ((enabledFlags & 0b1) == 0b1) {
            interruptFlag = (byte) (interruptFlag & ~(0b1));
            return 0x40;
        } else if ((enabledFlags & 0b10) == 0b10) {
            interruptFlag = (byte) (interruptFlag & ~(0b10));
            return 0x48;
        } else if ((enabledFlags & 0b100) == 0b100) {
            interruptFlag = (byte) (interruptFlag & ~(0b100));
            return 0x50;
        } else if ((enabledFlags & 0b1000) == 0b1000) {
            interruptFlag = (byte) (interruptFlag & ~(0b1000));
            return 0x58;
        } else if ((enabledFlags & 0b10000) == 0b10000) {
            interruptFlag = (byte) (interruptFlag & ~(0b10000));
            return 0x60;
        }

        throw new UnsupportedOperationException("Cannot send interrupt when no flags are set.");
    }

    public void setInterruptMasterEnable(boolean masterEnable) {
        this.masterEnable = masterEnable;
    }


    public void setTimerInterruptFlag() {
        interruptFlag |= 0b00100;
    }

    public boolean getInterruptMasterEnable() {
        return masterEnable;
    }

    public void setHalted(boolean isHalted) {
        this.isHalted = isHalted;
    }

    public boolean isHalted() {
        return isHalted;
    }
}
