package emulator.bus.device.cartridge;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ROMBankTest {

    @Test
    void testReadByteAt_FirstAddress() {
        // Arrange: Create a ROM bank with known data
        byte[] bankContent = new byte[0x4000];
        bankContent[0] = 0x12;  // Set the first byte
        ROMBank romBank = new ROMBank(bankContent);

        // Act: Read the first byte
        byte result = romBank.readByteAt(0);

        // Assert: The byte matches the expected value
        assertEquals((byte) 0x12, result);
    }

    @Test
    void testReadByteAt_MiddleAddress() {
        // Arrange: Create a ROM bank with known data
        byte[] bankContent = new byte[0x4000];
        bankContent[0x2000] = 0x34;  // Set a byte in the middle of the bank
        ROMBank romBank = new ROMBank(bankContent);

        // Act: Read the byte from the middle address
        byte result = romBank.readByteAt(0x2000);

        // Assert: The byte matches the expected value
        assertEquals((byte) 0x34, result);
    }

    @Test
    void testReadByteAt_LastAddress() {
        // Arrange: Create a ROM bank with known data
        byte[] bankContent = new byte[0x4000];
        bankContent[0x3FFF] = 0x56;  // Set the last byte
        ROMBank romBank = new ROMBank(bankContent);

        // Act: Read the last byte
        byte result = romBank.readByteAt(0x3FFF);

        // Assert: The byte matches the expected value
        assertEquals((byte) 0x56, result);
    }

    @Test
    void testReadByteAt_OutOfBoundsThrowsException() {
        byte[] bankContent = new byte[0x4000];
        ROMBank romBank = new ROMBank(bankContent);

        // Act & Assert
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> romBank.readByteAt(-1));
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> romBank.readByteAt(0x4000));
    }

    @Test
    void testReadByteAt_CheckData() {
        // Arrange
        byte[] bankContent = new byte[0x4000];
        for (int i = 0; i < bankContent.length; i++) {
            bankContent[i] = (byte) (i % 256); // Fill with some pattern
        }
        ROMBank romBank = new ROMBank(bankContent);

        // Act & Assert
        for (int i = 0; i < bankContent.length; i++) {
            assertEquals(bankContent[i], romBank.readByteAt(i));
        }
    }

    @Test
    void testConstructor_NullBankContent() {
        assertThrows(IllegalArgumentException.class, () -> new ROMBank(null));
    }

    @Test
    void testConstructor_BankTooSmall() {
        // Arrange: Create an invalid bank content smaller than 16KB
        byte[] invalidBankContent = new byte[0x2000];  // 8KB

        // Act & Assert: Expect IllegalArgumentException to be thrown
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new ROMBank(invalidBankContent)
        );
        assertEquals("ROM Banks must be 16KB in length, not 8192", exception.getMessage());
    }

    @Test
    void testConstructor_BankTooLarge() {
        // Arrange: Create an invalid bank content larger than 16KB
        byte[] invalidBankContent = new byte[0x8000];  // 32KB

        // Act & Assert: Expect IllegalArgumentException to be thrown
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new ROMBank(invalidBankContent)
        );
        assertEquals("ROM Banks must be 16KB in length, not 32768", exception.getMessage());
    }
}