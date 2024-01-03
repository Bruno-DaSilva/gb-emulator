package emulator.interrupts;

public class Timer {
    // TIMA Timer counter
    // TMA  Timer modulo register
    // TAC  Timer control register
    private byte TIMA;
    private byte TMA;
    private byte TAC;
    private int CPU_clocks_per_tick;
    private int currentCycles;


    private InterruptController interruptController;

    public Timer(InterruptController interruptController) {
        this.interruptController = interruptController;
    }

    public void addCycles(int cycles) {
        if (timerEnabled()) {
            currentCycles += cycles;
            if (currentCycles > CPU_clocks_per_tick) {
                currentCycles -= CPU_clocks_per_tick;
                incrementTimer();
            }
        }
    }

    private void incrementTimer() {
        int newTimerValue = (TIMA & 0xFF) + 1;
        TIMA = (byte) newTimerValue;
        if (newTimerValue > 0xFF) {
            // Overflow!
            TIMA = TMA;
            interruptController.setTimerInterruptFlag();
        }
    }

    private boolean timerEnabled() {
        return (TAC & 0b100) == 0b100;
    }

    public void setTMA(byte value) {
        this.TMA = value;
    }

    public void setTAC(byte value) {
        this.TAC = value;
        if ((TAC & 0b11) == 0) {
            CPU_clocks_per_tick = 1024;
        } else if ((TAC & 0b11) == 0b01) {
            CPU_clocks_per_tick = 16;
        } else if ((TAC & 0b11) == 0b10) {
            CPU_clocks_per_tick = 64;
        } else {
            CPU_clocks_per_tick = 256;
        }
    }

    public void setTIMA(byte value) {
        this.TIMA = value;
    }

    public byte getTIMA() {
        return TIMA;
    }

    public byte getTMA() {
        return TMA;
    }

    public byte getTAC() {
        return TAC;
    }
}
