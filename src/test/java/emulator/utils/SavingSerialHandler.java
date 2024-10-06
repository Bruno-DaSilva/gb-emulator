package emulator.utils;

import emulator.bus.ISerialHandler;

public class SavingSerialHandler implements ISerialHandler {
    private final StringBuilder sb = new StringBuilder();

    @Override
    public void writeSerial(char data) {
        sb.append(data);
        System.err.print(data);
    }

    public String getSavedData() {
        return sb.toString();
    }
}
