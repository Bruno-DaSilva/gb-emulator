package emulator.interrupts;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class InterruptControllerTest {

    private InterruptController interruptController;

    @BeforeEach
    void setUp() {
        interruptController = new InterruptController();
    }

    ///////////////////////////////////////////////////////////////////////
    ///////////////////////// interrupt flag //////////////////////////////
    ///////////////////////////////////////////////////////////////////////
    @Test
    void testSetInterruptFlagTooLarge() {
        interruptController.setInterruptFlag((byte) 0b100000);
        assertEquals((byte) 0b00000, interruptController.getInterruptFlag());
    }
    @Test
    void testSetInterruptEnableTooLarge() {
        interruptController.setInterruptEnable((byte) 0b100000);
        assertEquals((byte) 0b00000, interruptController.getInterruptEnable());
    }

    ///////////////////////////////////////////////////////////////////////
    /////////////////////// getHighestPriorityInterrupt ///////////////////
    ///////////////////////////////////////////////////////////////////////
    @Test
    void testGetHighestPriorityInterruptAddress() {
        interruptController.setInterruptFlag(  (byte) 0b00001); // V-Blank is set
        interruptController.setInterruptEnable((byte) 0b00001); // V-Blank is enabled
        interruptController.setInterruptMasterEnable(true);
        assertTrue(interruptController.interruptReady());
        assertEquals(0x40, interruptController.getHighestPriorityInterruptAddress());
    }
    @Test
    void testGetHighestPriorityInterruptAddressWithMultipleFlagsAndEnablesSet() {
        interruptController.setInterruptFlag(  (byte) 0b11111);
        interruptController.setInterruptEnable((byte) 0b11111);
        interruptController.setInterruptMasterEnable(true);
        assertTrue(interruptController.interruptReady());
        assertEquals(0x40, interruptController.getHighestPriorityInterruptAddress());
    }
    @Test
    void testGetHighestPriorityInterruptAddressReverseOrder() {
        interruptController.setInterruptFlag(  (byte) 0b10000); // Only Joypad interrupt is set
        interruptController.setInterruptEnable((byte) 0b11111); // All interrupts are enabled
        interruptController.setInterruptMasterEnable(true);
        assertTrue(interruptController.interruptReady());
        assertEquals(0x60, interruptController.getHighestPriorityInterruptAddress());
    }
    @Test
    void testGetHighestPriorityInterruptAddressWithMixedFlags() {
        interruptController.setInterruptFlag(  (byte) 0b11010); // Joypad, Serial, and LCD STAT interrupts are set
        interruptController.setInterruptEnable((byte) 0b10001); // V-Blank and Joypad interrupts are enabled
        interruptController.setInterruptMasterEnable(true);
        assertTrue(interruptController.interruptReady());
        assertEquals(0x60, interruptController.getHighestPriorityInterruptAddress()); // Joypad interrupt is highest priority
    }
    @Test
    void testGetHighestPriorityInterruptAddressWithFlagsSetButNoMatch() {
        interruptController.setInterruptFlag(  (byte) 0b10100); // Joypad and Timer interrupts are set
        interruptController.setInterruptEnable((byte) 0b01010); // Serial and LCD STAT interrupts are enabled
        interruptController.setInterruptMasterEnable(true);
        assertFalse(interruptController.interruptReady());
        assertThrows(UnsupportedOperationException.class, () -> {
            interruptController.getHighestPriorityInterruptAddress();
        });
    }
    @Test
    void testGetHighestPriorityInterruptAddressWithNoFlagsSet() {
        interruptController.setInterruptFlag(  (byte) 0b00000); // No interrupts are set
        interruptController.setInterruptEnable((byte) 0b10101); // Joypad, Timer, and V-Blank interrupts are enabled
        interruptController.setInterruptMasterEnable(true);
        assertFalse(interruptController.interruptReady());
        assertThrows(UnsupportedOperationException.class, () -> {
            interruptController.getHighestPriorityInterruptAddress();
        });
    }
    @Test
    void testGetHighestPriorityInterruptAddressWithIMEDisabled() {
        interruptController.setInterruptFlag(  (byte) 0b10101);
        interruptController.setInterruptEnable((byte) 0b10101);
        interruptController.setInterruptMasterEnable(false);
        assertFalse(interruptController.interruptReady());
        assertThrows(UnsupportedOperationException.class, () -> {
            interruptController.getHighestPriorityInterruptAddress();
        });
    }
    ////////////////////////////////////////////////////////////////////////
    @Test
    void testGetHighestPriorityInterruptAddressWithEveryCombination() {
        for (int flag = 0; flag <= 0b11111; flag++) {
            for (int enable = 0; enable <= 0b11111; enable++) {
                interruptController.setInterruptFlag((byte) flag);
                interruptController.setInterruptEnable((byte) enable);
                interruptController.setInterruptMasterEnable(true);
                if ((flag & enable) > 0) {
                    assertTrue(interruptController.interruptReady());

                    int matchingFlags = (flag & enable); 
                    int highestEnabledNumBitsFromRight = 0;
                    while ((matchingFlags & 0b1) == 0) {
                        highestEnabledNumBitsFromRight += 1;
                        matchingFlags = matchingFlags >> 1;
                    }
                    int expectedAddress = 0x40 + (highestEnabledNumBitsFromRight << 3);
                    assertEquals(expectedAddress, interruptController.getHighestPriorityInterruptAddress());
                } else {
                    assertFalse(interruptController.interruptReady());
                    assertThrows(UnsupportedOperationException.class, () -> {
                        interruptController.getHighestPriorityInterruptAddress();
                    });
                }
            }
        }
    }

    ////////////////////////////////////////////////////////////////////////
    @Test
    void testGetHighestPriorityInterruptAddressUnsetsFlagAfterReading() {
        interruptController.setInterruptFlag(  (byte) 0b10001); // V-Blank is set
        interruptController.setInterruptEnable((byte) 0b00001); // V-Blank is enabled
        interruptController.setInterruptMasterEnable(true);
        assertTrue(interruptController.interruptReady());
        assertEquals(0x40, interruptController.getHighestPriorityInterruptAddress());
        assertEquals((byte) 0b10000, interruptController.getInterruptFlag());
    }

    @Test
    void testGetHighestPriorityInterruptAddressWithEveryCombinationUnsetsFlag() {
        for (int flag = 0; flag <= 0b11111; flag++) {
            for (int enable = 0; enable <= 0b11111; enable++) {
                interruptController.setInterruptFlag((byte) flag);
                interruptController.setInterruptEnable((byte) enable);
                interruptController.setInterruptMasterEnable(true);
                if ((flag & enable) > 0) {
                    assertTrue(interruptController.interruptReady());

                    interruptController.getHighestPriorityInterruptAddress();
                    int matchingFlags = (flag & enable);
                    int mostRightBit = ( matchingFlags & ~(matchingFlags-1) ); // this is a fancy bitwise identity
                    int expectedFlagAfterReading = flag & ~mostRightBit;
                    assertEquals(expectedFlagAfterReading, interruptController.getInterruptFlag());
                } else {
                    assertFalse(interruptController.interruptReady());
                    assertThrows(UnsupportedOperationException.class, () -> {
                        interruptController.getHighestPriorityInterruptAddress();
                    });
                }
            }
        }
    }

    ////////////////////////////////////////////////////////////////////
    ////////////////////// Testing interruptReady //////////////////////
    ////////////////////////////////////////////////////////////////////
    @Test
    void testInterruptReadyWhenMasterEnableTrueAndInterruptsMatch() {
        interruptController.setInterruptMasterEnable(true);
        interruptController.setInterruptEnable((byte) 0b10101);
        interruptController.setInterruptFlag((byte) 0b00101); // Overlap in interruptEnable and interruptFlag

        assertTrue(interruptController.interruptReady());
    }

    @Test
    void testInterruptReadyWhenMasterEnableFalse() {
        interruptController.setInterruptMasterEnable(false);
        interruptController.setInterruptEnable((byte) 0b10101);
        interruptController.setInterruptFlag((byte) 0b00101); // Overlap in interruptEnable and interruptFlag

        assertFalse(interruptController.interruptReady());
    }

    @Test
    void testInterruptReadyWhenNoMatchingFlagsAndEnables() {
        interruptController.setInterruptMasterEnable(true);
        interruptController.setInterruptEnable((byte) 0b10100); // No overlap
        interruptController.setInterruptFlag((byte) 0b01010);

        assertFalse(interruptController.interruptReady());
    }

    @Test
    void testInterruptReadyWhenBothFlagsAndEnablesAreZero() {
        interruptController.setInterruptMasterEnable(true);
        interruptController.setInterruptEnable((byte) 0b00000); // No enabled interrupts
        interruptController.setInterruptFlag((byte) 0b00000);

        assertFalse(interruptController.interruptReady());
    }

    ////////////////////////////////////////////////////////////////////
    ///////////////////////// master enable ////////////////////////////
    ////////////////////////////////////////////////////////////////////

    @Test
    void testSetAndGetInterruptMasterEnable() {
        interruptController.setInterruptMasterEnable(true);
        assertTrue(interruptController.getInterruptMasterEnable());

        interruptController.setInterruptMasterEnable(false);
        assertFalse(interruptController.getInterruptMasterEnable());
    }

    ////////////////////////////////////////////////////////////////////
    //////////////////////// setTimerInterruptFlag /////////////////////
    ////////////////////////////////////////////////////////////////////

    @Test
    void testSetTimerInterruptFlag() {
        // Ensure the Timer interrupt flag (`0b00100`) is initially unset
        interruptController.setInterruptFlag((byte) 0b00000);

        interruptController.setTimerInterruptFlag();

        assertEquals((byte) 0b00100, interruptController.getInterruptFlag());
    }

    ////////////////////////////////////////////////////////////////////
    ////////////////////////////// halted //////////////////////////////
    ////////////////////////////////////////////////////////////////////
    @Test
    void testSetAndIsHalted() {
        interruptController.setHalted(true);
        assertTrue(interruptController.isHalted());

        interruptController.setHalted(false);
        assertFalse(interruptController.isHalted());
    }


}