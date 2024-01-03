package emulator.cpu.instruction;

import static emulator.cpu.instruction.InstructionUtils.getDoubleRegisterFor;
import static emulator.cpu.instruction.InstructionUtils.getRegisterFor;

import emulator.cpu.Operator;
import emulator.cpu.GameboyRegisters;
import emulator.cpu.register.InstructionTarget8Bit;

public class DECInstruction {

    public static class SingleRegister implements Instruction {
        private GameboyRegisters registers;

        public SingleRegister(GameboyRegisters registers) {
            this.registers = registers;
        }

        @Override
        public int execute(byte instruction) {
            // DEC r1
            byte xxx = (byte) ((instruction & 0b00_111_000) >>> 3);
            InstructionTarget8Bit xxxRegister = getRegisterFor(xxx, registers);

            byte orig = xxxRegister.getValue();
            xxxRegister.setValue((byte) (xxxRegister.getValue() - 1));

            registers.getF().setZ(xxxRegister.getValue() == 0);
            registers.getF().setN(true);
            registers.getF().setH(orig, (byte) 0x1, Operator.SUB);

            return xxxRegister.getAccessCost() * 2 + 1;
        }
    }

    public static class DoubleRegister implements Instruction {
        private GameboyRegisters registers;

        public DoubleRegister(GameboyRegisters registers) {
            this.registers = registers;
        }

        @Override
        public int execute(byte instruction) {
            // DEC rr
            byte xx = (byte) ((instruction & 0b00_110_000) >>> 4);
            emulator.cpu.register.DoubleRegister xxDblRegisters = getDoubleRegisterFor(xx, registers);
            xxDblRegisters.setValue(xxDblRegisters.getValue() - 1);

            return 2;
        }
    }

}
