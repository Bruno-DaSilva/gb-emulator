package emulator.bus.device.cartridge;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CartridgeTest {
    /**
     * Test that ensures parseRomSize throws an exception for out-of-bounds ROM size codes.
     */
    @Test
    void test_General_OutOfBoundsRomSizeCodeThrowsException() {
        // Set up a ROM with an invalid ROM size code
        byte[] romBytes = new byte[0x8000]; // 32KB ROM

        // Create a Cartridge instance and expect an exception
        for (int i = Byte.MIN_VALUE; i <= Byte.MAX_VALUE; i++) {
            romBytes[0x0147] = (byte) i;
            if (i >= 0x00 && i <= 0x03) {
                // these are valid values, skip them
                continue;
            }
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> Cartridge.createCartridge(romBytes));
            // Verify exception message
            assertEquals("Cartridge type " + i + " not supported.", exception.getMessage());
        }
    }




    //////////////////////////////////////////////////////////////////////////////////////////////////////

}