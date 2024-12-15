package emulator.cpu.register;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import emulator.bus.IBus;
import emulator.utils.SimpleBus;

public class RegisterMemoryAddressTest {
    @Test
    public void testGetValue() {
        IBus bus = new SimpleBus(0x10000);
        DoubleRegister addressRegister = new DoubleRegister(new Register((byte) 0x00), new Register((byte) 0x00));
        RegisterMemoryAddress registerMemoryAddress = new RegisterMemoryAddress(addressRegister, bus);

        addressRegister.setValue(0x1234);
        bus.writeByteAt(0x1234, (byte) 0xAB);
        byte value = registerMemoryAddress.getValue();

        assertEquals((byte) 0xAB, value);
    }

    @Test
    public void testSetValue() {
        IBus bus = new SimpleBus(0x10000);
        DoubleRegister addressRegister = new DoubleRegister(new Register((byte) 0x00), new Register((byte) 0x00));
        RegisterMemoryAddress registerMemoryAddress = new RegisterMemoryAddress(addressRegister, bus);

        addressRegister.setValue(0x1234);
        registerMemoryAddress.setValue((byte) 0xCD);
        byte value = bus.readByteAt(0x1234);

        assertEquals((byte) 0xCD, value);
    }

    @Test
    public void testGetAccessCost() {
        IBus bus = new SimpleBus(0x10000);
        DoubleRegister addressRegister = new DoubleRegister(new Register((byte) 0x00), new Register((byte) 0x00));
        RegisterMemoryAddress registerMemoryAddress = new RegisterMemoryAddress(addressRegister, bus);

        assertEquals(1, registerMemoryAddress.getAccessCost());
    }
}