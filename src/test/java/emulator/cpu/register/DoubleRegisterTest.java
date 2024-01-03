package emulator.cpu.register;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class DoubleRegisterTest {
    @Test
    public void testGetValueBasic() {
        Register higherRegister = new Register((byte) 0xAB);
        Register lowerRegister = new Register((byte) 0xCD);
        DoubleRegister doubleRegister = new DoubleRegister(higherRegister, lowerRegister);

        int value = doubleRegister.getValue();

        assertEquals(0xABCD, value);
    }
    @Test
    public void testGetValueWithMaxValue() {
        Register higherRegister = new Register((byte) 0xFF);
        Register lowerRegister = new Register((byte) 0xFF);
        DoubleRegister doubleRegister = new DoubleRegister(higherRegister, lowerRegister);

        int value = doubleRegister.getValue();

        assertEquals(0xFFFF, value);
    }

    @Test
    public void testGetValueWithMinValue() {
        Register higherRegister = new Register((byte) 0x00);
        Register lowerRegister = new Register((byte) 0x00);
        DoubleRegister doubleRegister = new DoubleRegister(higherRegister, lowerRegister);

        int value = doubleRegister.getValue();

        assertEquals(0x0000, value);
    }


    @Test
    public void testSetValueBasic() {
        Register higherRegister = new Register((byte) 0x00);
        Register lowerRegister = new Register((byte) 0x00);
        DoubleRegister doubleRegister = new DoubleRegister(higherRegister, lowerRegister);

        doubleRegister.setValue(0xABCD);

        assertEquals((byte) 0xCD, lowerRegister.getValue());
        assertEquals((byte) 0xAB, higherRegister.getValue());
    }

    @Test
    public void testSetValueWithMaxValue() {
        Register higherRegister = new Register((byte) 0x00);
        Register lowerRegister = new Register((byte) 0x00);
        DoubleRegister doubleRegister = new DoubleRegister(higherRegister, lowerRegister);

        doubleRegister.setValue(0xFFFF);

        assertEquals((byte) 0xFF, higherRegister.getValue());
        assertEquals((byte) 0xFF, lowerRegister.getValue());
    }

    @Test
    public void testSetValueWithMinValue() {
        Register higherRegister = new Register((byte) 0x00);
        Register lowerRegister = new Register((byte) 0x00);
        DoubleRegister doubleRegister = new DoubleRegister(higherRegister, lowerRegister);

        doubleRegister.setValue(0x0000);

        assertEquals((byte) 0x00, higherRegister.getValue());
        assertEquals((byte) 0x00, lowerRegister.getValue());
    }

    @Test
    public void testSetValueWithNegativeValue() {
        Register higherRegister = new Register((byte) 0x00);
        Register lowerRegister = new Register((byte) 0x00);
        DoubleRegister doubleRegister = new DoubleRegister(higherRegister, lowerRegister);

        doubleRegister.setValue(-1);

        assertEquals((byte) 0xFF, higherRegister.getValue());
        assertEquals((byte) 0xFF, lowerRegister.getValue());
    }

    @Test
    public void testSetValueWithOverflow() {
        Register higherRegister = new Register((byte) 0x00);
        Register lowerRegister = new Register((byte) 0x00);
        DoubleRegister doubleRegister = new DoubleRegister(higherRegister, lowerRegister);

        doubleRegister.setValue(0x12345);

        assertEquals((byte) 0x45, lowerRegister.getValue());
        assertEquals((byte) 0x23, higherRegister.getValue());
    }

    @Test
    public void testSetValueTwoBytes() {
        Register higherRegister = new Register((byte) 0x00);
        Register lowerRegister = new Register((byte) 0x00);
        DoubleRegister doubleRegister = new DoubleRegister(higherRegister, lowerRegister);

        doubleRegister.setValue((byte) 0xAB, (byte) 0xCD);

        assertEquals((byte) 0xAB, higherRegister.getValue());
        assertEquals((byte) 0xCD, lowerRegister.getValue());
    }

}