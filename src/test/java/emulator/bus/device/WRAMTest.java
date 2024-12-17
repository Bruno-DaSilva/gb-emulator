package emulator.bus.device;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class WRAMTest {

    @Test
    void writeByteAt_validAddressAndValue() {
        // Arrange
        WRAM wram = new WRAM();
        int address = 1000;
        byte value = 42;

        // Act
        wram.writeByteAt(address, value);
        byte result = wram.readByteAt(address);

        // Assert
        assertEquals(value, result, "The value written at the given address should be correctly stored.");
    }

    @Test
    void writeByteAt_minAddress() {
        // Arrange
        WRAM wram = new WRAM();
        int address = 0;
        byte value = 123;

        // Act
        wram.writeByteAt(address, value);
        byte result = wram.readByteAt(address);

        // Assert
        assertEquals(value, result, "The value written at the minimum address should be correctly stored.");
    }

    @Test
    void writeByteAt_maxAddress() {
        // Arrange
        WRAM wram = new WRAM();
        int address = 4095;
        byte value = -127;

        // Act
        wram.writeByteAt(address, value);
        byte result = wram.readByteAt(address);

        // Assert
        assertEquals(value, result, "The value written at the maximum address should be correctly stored.");
    }

    @Test
    void writeByteAt_overwriteExistingValue() {
        // Arrange
        WRAM wram = new WRAM();
        int address = 2500;
        byte initialValue = 34;
        byte newValue = 99;

        // Act
        wram.writeByteAt(address, initialValue);
        wram.writeByteAt(address, newValue);
        byte result = wram.readByteAt(address);

        // Assert
        assertEquals(newValue, result, "The value at the given address should be updated to the latest written value.");
    }

    @Test
    void writeByteAt_outOfBoundsAddress() {
        // Arrange
        WRAM wram = new WRAM();

        // Act & Assert
        assertThrows(IndexOutOfBoundsException.class, () -> wram.writeByteAt(-1, (byte) 23),
                "Accessing a negative address should throw an exception");
        assertThrows(IndexOutOfBoundsException.class, () -> wram.writeByteAt(4096, (byte) 23),
                "Accessing an address beyond the valid limit should throw an exception");
    }

    @Test
    void readByteAt_defaultValues() {
        // Arrange
        WRAM wram = new WRAM();

        // Act & Assert
        for (int i = 0; i < 4096; i++) {
            assertEquals(0, wram.readByteAt(i), "Unwritten memory should be initialized to 0");
        }
    }
}
