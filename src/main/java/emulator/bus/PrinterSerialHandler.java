package emulator.bus;

public class PrinterSerialHandler implements ISerialHandler {
    @Override
    public void writeSerial(char data) {
        System.err.print(data);
    }
}
