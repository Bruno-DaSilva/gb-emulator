package emulator.bus.device.cartridge;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * TODO: MBC1 implementation currently does not handle cartridge sizes above 512KB, which requires extra logic.
 *   Also does not support RAM yet.
 */
public class MBC1CartridgeTest {
    /**
     * Test that ensures readByteAt returns correct values from the first ROM bank
     */
    @Test
    void test_ReadByteAt_FirstROMBank() {
        byte[] romBankSizes = {
                0x00, // 2 ROM banks (32KB)
                0x01, // 4 ROM banks (64KB)
                0x02, // 8 ROM banks (128KB)
                0x03, // 16 ROM banks (256KB)
                0x04, // 32 ROM banks (512KB)
                // 0x05, TODO: more rom bank modes, special cases because of how internal wiring works
                // 0x06,
                // none exist above 2MB
        };
        for (byte romBankSize : romBankSizes) {
            byte[] romBytes = new byte[32768 * (int) Math.pow(2, romBankSize)];
            Arrays.fill(romBytes, 0, romBytes.length, (byte) 0x11);
            romBytes[0x0147] = 0x01; // CartridgeType.MBC1
            romBytes[0x0148] = romBankSize;

            // Create a Cartridge instance
            Cartridge cartridge = new MBC1Cartridge(romBytes);

            // Bank 0 reads
            for (int i = 0x0000; i < 0x3FFF; i++) {
                if (i == 0x0147 || i == 0x0148) {
                    continue; // skip any header values we set
                }
                assertEquals((byte) 0x11, cartridge.readByteAt(i)); // Lowest address in bank 0
            }
        }
    }

    /**
     * Test that ensures readByteAt returns correct values from the indexable rom banks,
     *   specifically using the lower bank index bits.
     */
    @Test
    void test_ReadByteAt_LowerIndexableROMBanks() {
        byte[] romBankSizes = {
                0x00, // 2 ROM banks (32KB)
                0x01, // 4 ROM banks (64KB)
                0x02, // 8 ROM banks (128KB)
                0x03, // 16 ROM banks (256KB)
                0x04, // 32 ROM banks (512KB)
                // 0x05, TODO: more rom bank modes, special cases because of how internal wiring works
                // 0x06,
                // none exist above 2MB
        };
        for (byte romBankSize : romBankSizes) {
            int numRomBanks = (int) Math.pow(2, romBankSize);
            byte[] romBytes = new byte[32768*numRomBanks];

            // fill each 16KB with a different set of values
            for (int i = 0; i < romBytes.length; i++) {
                romBytes[i] = (byte) (i / 0x4000);
            }
            
            romBytes[0x0147] = 0x01; // CartridgeType.MBC1
            romBytes[0x0148] = romBankSize;

            // Create a Cartridge instance
            MBC1Cartridge cartridge = new MBC1Cartridge(romBytes);

            // Index into every ROM Bank and try to read a value from bank 0 and bank i
            for (int bankIdx = 1; bankIdx < numRomBanks; bankIdx++) {
                cartridge.writeByteAt(0x2000, (byte) bankIdx);

                // check every byte
                for (int i = 0x0000; i < 0x3FFF; i++) {
                    // read from bank i
                    assertEquals((byte) bankIdx, cartridge.readByteAt(0x4000 + i));

                    // read from bank 0
                    if (i == 0x0147 || i == 0x0148) {
                        continue; // skip any header values we set
                    }
                    assertEquals((byte) 0x00, cartridge.readByteAt(i));
                }
            }

            // Special case: write a 0 to bank index defaults instead to 1
            cartridge.writeByteAt(0x2000, (byte) 0b00000);
            for (int i = 0x0000; i < 0x3FFF; i++) {
                // should read from bank 1
                assertEquals((byte) 0x01, cartridge.readByteAt(0x4000 + i));

                // read from bank 0
                if (i == 0x0147 || i == 0x0148) {
                    continue; // skip any header values we set
                }
                assertEquals((byte) 0x00, cartridge.readByteAt(i));
            }

        }
    }

    /**
     * Test that ensures parseRomSize throws an exception for out-of-bounds ROM size codes.
     */
    @Test
    void test_OutOfBoundsRomSizeCodeThrowsException() {
        // Set up a ROM with an invalid ROM size code
        byte[] romBytes = new byte[0x8000]; // 32KB ROM
        romBytes[0x0147] = 0x01; // CartridgeType.MBC1

        // Create a Cartridge instance and expect an exception
        for (int i = Byte.MIN_VALUE; i <= Byte.MAX_VALUE; i++) {
            romBytes[0x0148] = (byte) i;
            if (i >= 0x00 && i <= 0x08) {
                // these are valid values, skip them
                continue;
            }
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> new MBC1Cartridge(romBytes));
            // Verify exception message
            assertEquals("Invalid rom size code at 0x148: " + i, exception.getMessage());
        }
    }

    /**
     * Test that ensures readByteAt on MBC1 throws exceptions when accessing invalid addresses.
     */
    @Test
    void test_ReadByteAt_InvalidAddressThrowsException() {
        // Set up a Cartridge with CartridgeType.MBC1
        byte[] romBytes = new byte[0x10000]; // 64KB ROM
        romBytes[0x0147] = 0x01; // CartridgeType.MBC1
        romBytes[0x0148] = 0x01; // 4 ROM banks (64KB)

        // Create Cartridge instance
        Cartridge cartridge = new MBC1Cartridge(romBytes);

        // Invalid address reads
        assertThrows(IndexOutOfBoundsException.class, () -> cartridge.readByteAt(0x8000));
        assertThrows(IndexOutOfBoundsException.class, () -> cartridge.readByteAt(-1));
    }

    /**
     * Test that ensures advanced banking mode in MBC1 throw an exception.
     */
    @Test
    void test_UnsupportedBankingMode() {
        // Set up a ROM with CartridgeType.MBC1
        byte[] romBytes = new byte[0x8000]; // 32KB ROM size
        romBytes[0x0147] = 0x01; // CartridgeType.MBC1
        romBytes[0x0148] = 0x00; // Two ROM banks (32KB total)

        // Create a Cartridge instance
        Cartridge cartridge = new MBC1Cartridge(romBytes);

        // Attempt to enable unsupported banking mode
        UnsupportedOperationException exception = assertThrows(UnsupportedOperationException.class, () ->
                cartridge.writeByteAt(0x6000, (byte) 0x01) // Advanced banking mode unsupported
        );

        // Verify the exception message content
        assertEquals("Cartridge does not yet support advanced banking mode.", exception.getMessage());
    }

    /**
     * Test that ensures that mismatching cartridge tyoe in ROM throws an exception
     */
    @Test
    void test_WrongCartridgeType() {
        // Set up a ROM with CartridgeType.MBC1
        byte[] romBytes = new byte[0x8000]; // 32KB ROM size
        romBytes[0x0147] = 0x00; // CartridgeType.NONE
        romBytes[0x0148] = 0x00; // Two ROM banks (32KB total)

        // Attempt to enable unsupported banking mode
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                new MBC1Cartridge(romBytes)
        );

        // Verify the exception message content
        assertEquals("Mismatch between MBC1Cartridge constructor and underlying cartridge type in ROM", exception.getMessage());
    }
}
