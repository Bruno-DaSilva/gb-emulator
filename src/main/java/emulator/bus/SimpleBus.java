package emulator.bus;

/**
 * SimpleBus will hold a list of bytes, and will be able to read and write bytes at a given address.
 * This is a very simple implementation of a bus, and will primarily be used for testing purposes.
 */
public class SimpleBus implements IBus {
    private byte[] memory;
    
    /**
     * Create a new SimpleBus with a given size.
     * @param size The size of the bus.
     */
    public SimpleBus(int size) {
        memory = new byte[size];
    }

    /**
     * Read a byte at a given address.
     * @param address The address to read from.
     * @return The byte at the given address.
     */
    @Override
    public byte readByteAt(int address) {
        return memory[address];
    }

    /**
     * Write a byte at a given address.
     * @param address The address to write to.
     * @param value The byte to write.
     */
    @Override
    public void writeByteAt(int address, byte value) {
        memory[address] = value;
    }
}
