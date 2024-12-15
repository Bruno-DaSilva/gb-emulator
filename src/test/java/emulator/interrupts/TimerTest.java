package emulator.interrupts;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TimerTest {

    @Test
    void testAddCyclesWhenTimerDisabled() {
        InterruptController interruptController = new InterruptController();
        Timer timer = new Timer(interruptController);

        timer.setTAC((byte) 0b000); // Disabling the timer
        timer.addCycles(10); // Adding cycles

        // Since the timer is disabled, TIMA should not increment
        assertEquals(0, timer.getTIMA());
    }

    @Test
    void testAddCyclesWhenTimerEnabledAndNoOverflow() {
        InterruptController interruptController = new InterruptController();
        Timer timer = new Timer(interruptController);

        timer.setTAC((byte) 0b100); // Enabling the timer with default clock (1024 cycles per tick)
        timer.setTIMA((byte) 10);
        timer.addCycles(500); // Adding cycles less than CPU clocks per tick

        // TIMA should remain the same
        assertEquals(10, timer.getTIMA());
    }

    @Test
    void testAddCyclesWhenTimerEnabledAndCausesIncrement() {
        InterruptController interruptController = new InterruptController();
        Timer timer = new Timer(interruptController);

        timer.setTAC((byte) 0b100); // Enabling the timer with default clock (1024 cycles per tick)
        timer.setTIMA((byte) 10);
        timer.addCycles(1500); // Adding cycles, causing an overflow

        // TIMA should increment by 1 and currentCycles should reset
        assertEquals(11, timer.getTIMA());
    }

    @Test
    void testAddCyclesWhenTimerEnabledAndCausesOverflowToTMA() {
        InterruptController interruptController = new InterruptController();
        Timer timer = new Timer(interruptController);

        timer.setTAC((byte) 0b100); // Enabling the timer with default clock (1024 cycles per tick)
        timer.setTIMA((byte) 255); // Setting TIMA to max value
        timer.setTMA((byte) 42); // Setting TMA to a custom value
        timer.addCycles(1500); // Adding cycles, causing overflow

        // TIMA should reset to TMA, and interrupt flag should be set
        assertEquals(42, timer.getTIMA());
        assertTrue(interruptController.getTimerInterruptFlag());
    }

    @Test
    void testAddCyclesForDifferentClockSpeeds() {
        InterruptController interruptController = new InterruptController();
        Timer timer = new Timer(interruptController);

        // 16 cycles per tick
        timer.setTAC((byte) 0b101); // Enable timer with 16 clock cycles per tick
        timer.setTIMA((byte) 5);
        timer.addCycles(17); // Should increment TIMA by 1
        assertEquals(6, timer.getTIMA());

        timer.addCycles(300); // Should increment TIMA by 18
        assertEquals(24, timer.getTIMA());

        // Set to 256 cycles per tick
        timer.setTAC((byte) 0b111); // Enable timer with 256 clock cycles per tick
        timer.setTIMA((byte) 8);
        timer.addCycles(300); // Should increment TIMA by 1
        assertEquals(9, timer.getTIMA());
    }
}