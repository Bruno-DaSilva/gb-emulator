package emulator.bus.device;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class HRAMTest {

    @Test
    void readByteAt_validAddress() {
        // Arrange
        HRAM hram = new HRAM();
        int address = 10;
        byte value = 42;

        // Act
        hram.writeByteAt(address, value);
        byte result = hram.readByteAt(address);

        // Assert
        assertEquals(value, result, "The value written at the given address should be correctly stored.");
    }

    @Test
    void readByteAt_defaultValue() {
        // Arrange
        HRAM hram = new HRAM();
        int address = 25;

        // Act
        byte result = hram.readByteAt(address);

        // Assert
        assertEquals(0, result, "Uninitialized memory should return the default value of 0.");
    }

    @Test
    void readByteAt_minAddress() {
        // Arrange
        HRAM hram = new HRAM();
        int address = 0;
        byte value = 7;

        // Act
        hram.writeByteAt(address, value);
        byte result = hram.readByteAt(address);

        // Assert
        assertEquals(value, result, "The value written at the minimum address should be correctly stored.");
    }

    @Test
    void readByteAt_maxAddress() {
        // Arrange
        HRAM hram = new HRAM();
        int address = 126;
        byte value = -45;

        // Act
        hram.writeByteAt(address, value);
        byte result = hram.readByteAt(address);

        // Assert
        assertEquals(value, result, "The value written at the maximum address should be correctly stored.");
    }

    @Test
    void readByteAt_outOfBoundsThrowsException() {
        // Arrange
        HRAM hram = new HRAM();

        // Act & Assert
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> hram.readByteAt(-1),
                "Accessing a negative address should throw an exception.");
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> hram.readByteAt(127),
                "Accessing an address outside valid range should throw an exception.");
    }

    @Test
    void writeByteAt_overwriteValue() {
        // Arrange
        HRAM hram = new HRAM();
        int address = 50;
        byte initialValue = 32;
        byte newValue = 64;

        // Act
        hram.writeByteAt(address, initialValue);
        hram.writeByteAt(address, newValue);
        byte result = hram.readByteAt(address);

        // Assert
        assertEquals(newValue, result, "The value at the address should be overwritten with the new value.");
    }
}