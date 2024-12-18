package emulator.bus.device.cartridge;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class DefaultCartridgeTest {

    /**
     * Test that ensures a Cartridge with CartridgeType.NONE is correctly initialized.
     */
    @Test
    void test_NoneCartridge_ValidInitialization() {
        // Set up a valid ROM with CartridgeType.NONE
        byte[] romBytes = new byte[0x8000]; // 32KB ROM
        romBytes[0x0147] = 0x00; // CartridgeType.NONE
        romBytes[0x0148] = 0x00; // ROM size is standard 32KB

        // Create a Cartridge instance and ensure no exception is thrown
        assertDoesNotThrow(() -> new DefaultCartridge(romBytes));
    }

    /**
     * Test that ensures an exception is thrown for CartridgeType.NONE with invalid extra ROM banks.
     */
    @Test
    void test_NoneCartridge_InvalidExtraRomBanksThrowsException() {
        // Set up an invalid ROM with CartridgeType.NONE (rom size code does not match supported size)
        byte[] romBytes = new byte[0x10000]; // 64KB ROM
        romBytes[0x0147] = 0x00; // CartridgeType.NONE
        romBytes[0x0148] = 0x01; // Invalid ROM size (not supported for CartridgeType.NONE)

        // Create a Cartridge instance and expect an exception
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> new DefaultCartridge(romBytes));

        // Verify exception message
        assertEquals("Only one extra ROM bank supported for no MBC.", exception.getMessage());
    }

    /**
     * Test that ensures an exception is thrown when attempting to write to a cartridge
     * with CartridgeType.NONE (no memory bank controller).
     */
    @Test
    void test_NoneCartridge_WriteByteAtThrowsException() {
        // Set up a ROM with CartridgeType.NONE
        byte[] romBytes = new byte[0x8000]; // 32KB ROM size
        romBytes[0x0147] = 0x00; // CartridgeType.NONE
        romBytes[0x0148] = 0x00; // ROM size is standard 32KB

        // Create a Cartridge instance
        Cartridge cartridge = new DefaultCartridge(romBytes);

        // Attempt to write to the cartridge and expect an exception
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                cartridge.writeByteAt(0x2000, (byte) 0x01) // Writing to address 0x2000
        );

        // Verify the exception message
        assertEquals("Cannot write to any values with no MBC.", exception.getMessage());
    }

    /**
     * Test that ensures reading from a CartridgeType.NONE does not cause an exception
     * and returns the correct value from the ROM.
     */
    @Test
    void test_NoneCartridge_ReadByteAt() {
        // Set up a ROM with CartridgeType.NONE
        byte[] romBytes = new byte[0x8000]; // 32KB ROM size
        romBytes[0x0147] = 0x00; // CartridgeType.NONE
        romBytes[0x0148] = 0x00; // ROM size is standard 32KB
        romBytes[0x0000] = (byte) 0x42; // first rombank, lowest address
        romBytes[0x3999] = (byte) 0x43; // first rombank, highest address
        romBytes[0x4000] = (byte) 0x69; // second rombank, lowest address
        romBytes[0x7999] = (byte) 0x70; // second rombank, highest address

        // Create a Cartridge instance
        Cartridge cartridge = new DefaultCartridge(romBytes);

        // Read the value in first rombank
        byte value = cartridge.readByteAt(0x0000);
        assertEquals((byte) 0x42, value);
        value = cartridge.readByteAt(0x3999);
        assertEquals((byte) 0x43, value);
        // Read the value in second rombank
        value = cartridge.readByteAt(0x4000);
        assertEquals((byte) 0x69, value);
        value = cartridge.readByteAt(0x7999);
        assertEquals((byte) 0x70, value);

        assertThrows(IndexOutOfBoundsException.class, () -> cartridge.readByteAt(0x8000));
        assertThrows(IndexOutOfBoundsException.class, () -> cartridge.readByteAt(-1));
    }
}
