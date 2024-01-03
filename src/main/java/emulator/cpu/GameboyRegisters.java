package emulator.cpu;

import emulator.bus.IBus;
import emulator.cpu.register.DoubleRegister;
import emulator.cpu.register.FlagRegister;
import emulator.cpu.register.Register;
import emulator.cpu.register.RegisterMemoryAddress;

public class GameboyRegisters {
    private DoubleRegister PC;
    private Register A;
    private Register H;
    private Register L;
    private Register D;
    private Register E;
    private Register B;
    private Register C;
    private FlagRegister F;
    private DoubleRegister HL;
    private DoubleRegister DE;
    private DoubleRegister BC;
    private DoubleRegister SP;
    private DoubleRegister AF;

    private RegisterMemoryAddress HLMemoryPointer;

    public GameboyRegisters(IBus bus) {
        this.PC = new DoubleRegister(new Register((byte) 0x00), new Register((byte) 0x00));
        this.PC.setValue(0x0100);
        this.A = new Register((byte) 0x01);
        this.F = new FlagRegister((byte) 0xb0);
        this.B = new Register((byte) 0x00);
        this.C = new Register((byte) 0x13);
        this.D = new Register((byte) 0x00);
        this.E = new Register((byte) 0xD8);
        this.H = new Register((byte) 0x01);
        this.L = new Register((byte) 0x4D);
        this.HL = new DoubleRegister(H, L);
        this.DE = new DoubleRegister(D, E);
        this.BC = new DoubleRegister(B, C);
        this.AF = new DoubleRegister(A, F);
        this.SP = new DoubleRegister(new Register((byte) 0x00), new Register((byte) 0x00));
        this.SP.setValue(0xFFFE);

        this.HLMemoryPointer = new RegisterMemoryAddress(HL, bus);
    }

    public DoubleRegister getPC() {
        return PC;
    }

    public Register getA() {
        return A;
    }

    public Register getH() {
        return H;
    }

    public Register getL() {
        return L;
    }

    public Register getD() {
        return D;
    }

    public Register getE() {
        return E;
    }

    public Register getB() {
        return B;
    }

    public Register getC() {
        return C;
    }

    public FlagRegister getF() {
        return F;
    }

    public DoubleRegister getHL() {
        return HL;
    }

    public DoubleRegister getDE() {
        return DE;
    }

    public DoubleRegister getBC() {
        return BC;
    }

    public DoubleRegister getSP() {
        return SP;
    }

    public DoubleRegister getAF() {
        return AF;
    }

    public RegisterMemoryAddress getHLMemoryPointer() {
        return HLMemoryPointer;
    }
}
