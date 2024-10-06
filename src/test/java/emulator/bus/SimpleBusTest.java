package emulator.bus;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class SimpleBusTest {
    @Test
    public void testReadByteAt() {
        SimpleBus bus = new SimpleBus(256);
        bus.writeByteAt(0x10, (byte) 0xAB);
        bus.writeByteAt(0x20, (byte) 0xCE);

        assertEquals((byte) 0xAB, bus.readByteAt(0x10));
        assertEquals((byte) 0xCD, bus.readByteAt(0x20));
    }

    @Test
    public void testWriteByteAt() {
        SimpleBus bus = new SimpleBus(256);
        bus.writeByteAt(0x10, (byte) 0xAB);
        bus.writeByteAt(0x20, (byte) 0xCD);

        assertEquals((byte) 0xAB, bus.readByteAt(0x10));
        assertEquals((byte) 0xCD, bus.readByteAt(0x20));
    }
}