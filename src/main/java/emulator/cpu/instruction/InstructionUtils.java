package emulator.cpu.instruction;

import emulator.cpu.Operator;
import emulator.bus.GameboyBus;
import emulator.cpu.GameboyRegisters;
import emulator.cpu.register.DoubleRegister;
import emulator.cpu.register.InstructionTarget8Bit;

public class InstructionUtils {

    public static InstructionTarget8Bit getRegisterFor(byte xxx, GameboyRegisters registers) {
        if (xxx == 0b000) {
            return registers.getB();
        } else if (xxx == 0b001) {
            return registers.getC();
        } else if (xxx == 0b010) {
            return registers.getD();
        } else if (xxx == 0b011) {
            return registers.getE();
        } else if (xxx == 0b100) {
            return registers.getH();
        } else if (xxx == 0b101) {
            return registers.getL();
        } else if (xxx == 0b110) {
            return registers.getHLMemoryPointer();
        } else if (xxx == 0b111) {
            return registers.getA();
        } else {
            throw new IllegalArgumentException("Invalid input for getRegisterFor(): " + xxx);
        }
    }

    public static DoubleRegister getDoubleRegisterFor(byte xx, GameboyRegisters registers) {
        if (xx == 0b00) {
            return registers.getBC();
        } else if (xx == 0b01) {
            return registers.getDE();
        } else if (xx == 0b10) {
            return registers.getHL();
        } else if (xx == 0b11) {
            return registers.getSP();
        } else {
            throw new IllegalArgumentException("Invalid input for getDoubleRegisterFor(): " + xx);
        }
    }

    public static void pushToStack(int value, GameboyRegisters registers, GameboyBus bus) {
        registers.getSP().setValue(registers.getSP().getValue() - 1);
        bus.writeByteAt(registers.getSP().getValue(), (byte) ((value >> 8) & 0xFF));
        registers.getSP().setValue(registers.getSP().getValue() - 1);
        bus.writeByteAt(registers.getSP().getValue(), (byte) (value & 0xFF));
    }

    public static int popFromStack(GameboyRegisters registers, GameboyBus bus) {
        byte lowerByte = bus.readByteAt(registers.getSP().getValue());
        registers.getSP().setValue(registers.getSP().getValue() + 1);
        byte higherByte = bus.readByteAt(registers.getSP().getValue());
        registers.getSP().setValue(registers.getSP().getValue() + 1);

        return (lowerByte & 0xFF) | ((higherByte & 0xFF) << 8);
    }

    public static Operator getAluOperatorFor(byte opxxx) {
        if (opxxx == 0b000) {
            return Operator.ADD;
        } else if (opxxx == 0b001) {
            return Operator.ADC;
        } else if (opxxx == 0b010) {
            return Operator.SUB;
        } else if (opxxx == 0b011) {
            return Operator.SBC;
        } else if (opxxx == 0b100) {
            return Operator.AND;
        } else if (opxxx == 0b101) {
            return Operator.XOR;
        } else if (opxxx == 0b110) {
            return Operator.OR;
        } else if (opxxx == 0b111) {
            return Operator.CP;
        } else {
            throw new IllegalArgumentException("Invalid input for getAluOperatorFor(): " + opxxx);
        }
    }

    public static boolean getFlagForCond(byte cc, GameboyRegisters registers) {
        if (cc == 0b00) {
            // NZ
            return !registers.getF().getZ();
        } else if (cc == 0b01) {
            // Z
            return registers.getF().getZ();
        } else if (cc == 0b10) {
            // NC
            return !registers.getF().getC();
        } else if (cc == 0b11) {
            // C
            return registers.getF().getC();
        } else {
            throw new IllegalArgumentException("Invalid input for getFlagForCond(): " + cc);
        }
    }

    public static DoubleRegister getDoublePushPopRegisterFor(byte xx, GameboyRegisters registers) {
        if (xx == 0b00) {
            return registers.getBC();
        } else if (xx == 0b01) {
            return registers.getDE();
        } else if (xx == 0b10) {
            return registers.getHL();
        } else if (xx == 0b11) {
            return registers.getAF();
        } else {
            throw new IllegalArgumentException("Invalid input for getDoublePushPopRegisterFor(): " + xx);
        }
    }
}
