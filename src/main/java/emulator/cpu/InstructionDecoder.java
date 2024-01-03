package emulator.cpu;

import static emulator.cpu.instruction.InstructionUtils.*;

import emulator.bus.GameboyBus;
import emulator.cpu.instruction.*;
import emulator.cpu.register.DoubleRegister;
import emulator.cpu.register.InstructionTarget8Bit;
import emulator.cpu.register.Register;
import emulator.interrupts.InterruptController;

public class InstructionDecoder {

    private GameboyRegisters registers;
    private GameboyBus bus;
    private InterruptController interruptController;
    private InstructionFetcher instructionFetcher;

    public InstructionDecoder(InstructionFetcher instructionFetcher, GameboyRegisters registers, GameboyBus bus, InterruptController interruptController) {

        this.instructionFetcher = instructionFetcher;
        this.registers = registers;
        this.bus = bus;
        this.interruptController = interruptController;
    }
    
    public Instruction decode(byte nextInstruction) {
        if (nextInstruction == (byte) 0x00) {
            // NOP
            return new NOPInstruction();
        } else if (nextInstruction == (byte) 0x10) {
            // STOP (disable interrupts)
            return new STOPInstruction(instructionFetcher);
        } else if (nextInstruction == (byte) 0x76) {
            // HALT
            return new HALTInstruction(interruptController);
        } else if (nextInstruction == (byte) 0xFB) {
            // EI
            return new EIInstruction(interruptController);
        } else if (nextInstruction == (byte) 0xF3) {
            // DI (disable interrupts)
            return new DIInstruction(interruptController);
        } else if ((nextInstruction & 0b11_000_111) == 0b00_000_110) {
            // LD r1, n --- LD (HL), n
            byte xxx = (byte) ((nextInstruction & 0b00_111_000) >>> 3);
            byte value = instructionFetcher.fetchNextByte();
            InstructionTarget8Bit xxxRegister = getRegisterFor(xxx, registers);
            xxxRegister.setValue(value);

            return new EmptyInstruction(xxxRegister.getAccessCost() + 2);
        } else if ((nextInstruction & 0b11_000_000) == 0b01_000_000) {
            // LD r1, r2
            byte xxx = (byte) ((nextInstruction & 0b00_111_000) >> 3);
            byte yyy = (byte) (nextInstruction & 0b00_000_111);

            InstructionTarget8Bit xxxRegister = getRegisterFor(xxx, registers);
            InstructionTarget8Bit yyyRegister = getRegisterFor(yyy, registers);

            xxxRegister.setValue(yyyRegister.getValue());

            return new EmptyInstruction(1 + xxxRegister.getAccessCost() + yyyRegister.getAccessCost());
        } else if ((nextInstruction & 0b11_000_111) == 0b00_000_100) {
            // INC r1
            return new INCInstruction.SingleRegister(registers);
        } else if ((nextInstruction & 0b11_000_111) == 0b00_000_101) {
            // DEC r1
            return new DECInstruction.SingleRegister(registers);
        } else if ((nextInstruction & 0b11_00_1111) == 0b00_00_0001) {
            // LD rr, nn
            byte xx = (byte) ((nextInstruction & 0b00_110_000) >>> 4);
            DoubleRegister xxDblRegisters = getDoubleRegisterFor(xx, registers);
            byte lowerBits = instructionFetcher.fetchNextByte();
            byte higherBits = instructionFetcher.fetchNextByte();

            xxDblRegisters.setValue(higherBits, lowerBits);

            return new EmptyInstruction(3);
        } else if ((nextInstruction & 0b11_00_1111) == 0b00_00_0011) {
            // INC rr
            return new INCInstruction.DoubleRegister(registers);
        } else if ((nextInstruction & 0b11_00_1111) == 0b00_00_1011) {
            // DEC rr
            return new DECInstruction.DoubleRegister(registers);
        } else if ((nextInstruction & 0b11_00_1111) == 0b00_00_0010) {
            // LD (rr), A
            byte xx = (byte) ((nextInstruction & 0b00_110_000) >>> 4);
            if (xx == 0b00) {
                // LD (BC), A
                int memoryLocation = registers.getBC().getValue();
                bus.writeByteAt(memoryLocation, registers.getA().getValue());
            } else if (xx == 0b01) {
                // LD (DE), A
                int memoryLocation = registers.getDE().getValue();
                bus.writeByteAt(memoryLocation, registers.getA().getValue());
            } else if (xx == 0b10) {
                // LD (HL++), A
                int memoryLocation = registers.getHL().getValue();
                bus.writeByteAt(memoryLocation, registers.getA().getValue());
                registers.getHL().setValue(registers.getHL().getValue() + 1);
            } else if (xx == 0b11) {
                // LD (HL--), A
                int memoryLocation = registers.getHL().getValue();
                bus.writeByteAt(memoryLocation, registers.getA().getValue());
                registers.getHL().setValue(registers.getHL().getValue() - 1);
            }
            return new EmptyInstruction(2);
        } else if ((nextInstruction & 0b11_00_1111) == 0b00_00_1010) {
            // LD A, (rr)
            byte xx = (byte) ((nextInstruction & 0b00_110_000) >>> 4);
            if (xx == 0b00) {
                // LD A, (BC)
                int memoryLocation = registers.getBC().getValue();
                registers.getA().setValue(bus.readByteAt(memoryLocation));
            } else if (xx == 0b01) {
                // LD A, (DE)
                int memoryLocation = registers.getDE().getValue();
                registers.getA().setValue(bus.readByteAt(memoryLocation));
            } else if (xx == 0b10) {
                // LD A, (HL++)
                int memoryLocation = registers.getHL().getValue();
                registers.getA().setValue(bus.readByteAt(memoryLocation));
                registers.getHL().setValue(registers.getHL().getValue() + 1);
            } else if (xx == 0b11) {
                // LD A, (HL--)
                int memoryLocation = registers.getHL().getValue();
                registers.getA().setValue(bus.readByteAt(memoryLocation));
                registers.getHL().setValue(registers.getHL().getValue() - 1);
            }
            return new EmptyInstruction(2);
        } else if ((nextInstruction & 0b11_00_1111) == 0b11_00_0001) {
            // 1 800 361 5373
            // 0142163126549
            // POP rr
            byte xx = (byte) ((nextInstruction & 0b00_11_0000) >>> 4);

            // POP AF|BC|DE|HL
            DoubleRegister xxDoubleRegister = getDoublePushPopRegisterFor(xx, registers);

            xxDoubleRegister.setValue(popFromStack(registers, bus));

            return new EmptyInstruction(3);
        } else if ((nextInstruction & 0b11_00_1111) == 0b11_00_0101) {
            // PUSH rr
            byte xx = (byte) ((nextInstruction & 0b00_11_0000) >>> 4);

            // PUSH AF|BC|DE|HL
            DoubleRegister xxDoubleRegister = getDoublePushPopRegisterFor(xx, registers);

            pushToStack(xxDoubleRegister.getValue(), registers, bus);

            return new EmptyInstruction(4);
        } else if ((nextInstruction & 0b11_000_000) == 0b10_000_000) {
            // ADD/SUB/etc A, r
            byte opxxx = (byte) ((nextInstruction & 0b00_111_000) >> 3);
            Operator operator = getAluOperatorFor(opxxx);

            byte xxx = (byte) (nextInstruction & 0b00_000_111);
            InstructionTarget8Bit xxxRegister = getRegisterFor(xxx, registers);

            byte orig = registers.getA().getValue();
            byte target = xxxRegister.getValue();
            int carry = registers.getF().getC() ? 1 : 0;

            byte value = operator.apply(orig, target, carry);
            if (!operator.equals(Operator.CP))
                registers.getA().setValue(value);

            registers.getF().setZ(value == 0);
            registers.getF().setN(operator);
            registers.getF().setH(orig, target, carry, operator);
            registers.getF().setC(orig, target, carry, operator);

            return new EmptyInstruction(1 + xxxRegister.getAccessCost());
        } else if ((nextInstruction & 0b11_00_1111) == 0b00_00_1001) {
            // ADD HL, rr
            byte xx = (byte) ((nextInstruction & 0b00_11_0000) >>> 4);
            DoubleRegister xxDblRegisters = getDoubleRegisterFor(xx, registers);
            int orig = registers.getHL().getValue();
            int toAdd = xxDblRegisters.getValue();
            registers.getHL().setValue(orig + toAdd);

            registers.getF().setN(false);
            registers.getF().setH(((orig & 0x0FFF) + (toAdd & 0x0FFF)) & 0b0001_0000_0000_0000);
            registers.getF().setC(((orig & 0xFFFF) + (toAdd & 0xFFFF)) > 0xFFFF);

            return new EmptyInstruction(2);
        } else if ((nextInstruction & 0b111_00_111) == 0b110_00_000) {
            // RET cond
            byte cc = (byte) ((nextInstruction & 0b000_11_000) >>> 3);
            boolean flag = getFlagForCond(cc, registers);
            if (flag) {
                registers.getPC().setValue(popFromStack(registers, bus));
                return new EmptyInstruction(5);
            }

            return new EmptyInstruction(2);
        } else if ((nextInstruction & 0b111_00_111) == 0b110_00_010) {
            // JP cond, nn
            byte cc = (byte) ((nextInstruction & 0b000_11_000) >>> 3);
            boolean flag = getFlagForCond(cc, registers);

            byte lowerAddressBits = instructionFetcher.fetchNextByte();
            byte higherAddressBits = instructionFetcher.fetchNextByte();

            if (flag) {
                registers.getPC().setValue(higherAddressBits, lowerAddressBits);
                return new EmptyInstruction(4);
            }
            return new EmptyInstruction(3);
        } else if ((nextInstruction & 0b111_00_111) == 0b110_00_100) {
            // CALL cond, nn
            byte cc = (byte) ((nextInstruction & 0b000_11_000) >>> 3);
            boolean flag = getFlagForCond(cc, registers);

            byte lower = instructionFetcher.fetchNextByte();
            byte higher = instructionFetcher.fetchNextByte();
            int targetAddr = (higher & 0xFF) << 8 | (lower & 0xFF);
            if (flag) {
                pushToStack(registers.getPC().getValue(), registers, bus);
                registers.getPC().setValue(targetAddr);
                return new EmptyInstruction(6);
            }
            return new EmptyInstruction(3);
        } else if ((nextInstruction & 0b11_000_111) == 0b11_000_111) {
            // RST i
            byte iii = (byte) (nextInstruction & 0b00_111_000);
            // push current pc onto the stack
            pushToStack(registers.getPC().getValue(), registers, bus);

            // set pc to page i of memory
            registers.getPC().setValue(iii);

            return new EmptyInstruction(4);
        }
        else if (nextInstruction == (byte) 0xCB) {
            // 16 bit opcode....
            nextInstruction = instructionFetcher.fetchNextByte();
            if ((nextInstruction & 0b11111_000) == 0b00000_000) {
                // RLC r
                byte xxx = (byte) (nextInstruction & 0b00000_111);
                InstructionTarget8Bit xxxRegister = getRegisterFor(xxx, registers);
                byte orig = xxxRegister.getValue();
                int msb = orig & 0x80;
                xxxRegister.setValue((byte) ((orig & 0xFF) << 1));
                xxxRegister.setValue((byte) (xxxRegister.getValue() | (msb >>> 7)));
                registers.getF().setZ(xxxRegister.getValue() == 0);
                registers.getF().setN(false);
                registers.getF().setH(false);
                registers.getF().setC(msb == 0x80);

                return new EmptyInstruction(2 + xxxRegister.getAccessCost());
            } else if ((nextInstruction & 0b11111_000) == 0b00001_000) {
                // RRC r
                byte xxx = (byte) (nextInstruction & 0b00000_111);
                InstructionTarget8Bit xxxRegister = getRegisterFor(xxx, registers);
                byte orig = xxxRegister.getValue();
                int lsb = orig & 0x01;
                xxxRegister.setValue((byte) ((orig & 0xFF) >>> 1));
                xxxRegister.setValue((byte) (xxxRegister.getValue() | (lsb << 7)));
                registers.getF().setZ(xxxRegister.getValue() == 0);
                registers.getF().setN(false);
                registers.getF().setH(false);
                registers.getF().setC(lsb == 0x01);

                return new EmptyInstruction(2 + xxxRegister.getAccessCost());
            } else if ((nextInstruction & 0b11111_000) == 0b00010_000) {
                // RL r
                byte xxx = (byte) (nextInstruction & 0b00000_111);
                InstructionTarget8Bit xxxRegister = getRegisterFor(xxx, registers);
                byte orig = xxxRegister.getValue();
                int carry = registers.getF().getC() ? 1 : 0;
                xxxRegister.setValue((byte) ((orig & 0xFF) << 1));
                xxxRegister.setValue((byte) (xxxRegister.getValue() | carry));
                registers.getF().setZ(xxxRegister.getValue() == 0);
                registers.getF().setN(false);
                registers.getF().setH(false);
                registers.getF().setC(orig & 0x80);

                return new EmptyInstruction(2 + xxxRegister.getAccessCost());
            } else if ((nextInstruction & 0b11111_000) == 0b00011_000) {
                // RR r
                byte xxx = (byte) (nextInstruction & 0b00000_111);
                InstructionTarget8Bit xxxRegister = getRegisterFor(xxx, registers);
                byte msb = (byte) ((registers.getF().getC() ? 1 : 0) << 7);
                byte orig = xxxRegister.getValue();
                xxxRegister.setValue((byte) ((xxxRegister.getValue() & 0xFF) >>> 1));
                xxxRegister.setValue((byte) (xxxRegister.getValue() | msb));
                registers.getF().setZ(xxxRegister.getValue() == 0);
                registers.getF().setN(false);
                registers.getF().setH(false);
                registers.getF().setC(orig & 0x01);

                return new EmptyInstruction(2 + xxxRegister.getAccessCost());
            } else if ((nextInstruction & 0b11111_000) == 0b00100_000) {
                // SLA r
                byte xxx = (byte) (nextInstruction & 0b00000_111);
                InstructionTarget8Bit xxxRegister = getRegisterFor(xxx, registers);
                byte orig = xxxRegister.getValue();
                xxxRegister.setValue((byte) ((orig & 0xFF) << 1));
                registers.getF().setZ(xxxRegister.getValue() == 0);
                registers.getF().setN(false);
                registers.getF().setH(false);
                registers.getF().setC(orig & 0x80);

                return new EmptyInstruction(2 + xxxRegister.getAccessCost());
            } else if ((nextInstruction & 0b11111_000) == 0b00101_000) {
                // SRA r
                byte xxx = (byte) (nextInstruction & 0b00000_111);
                InstructionTarget8Bit xxxRegister = getRegisterFor(xxx, registers);
                byte orig = xxxRegister.getValue();
                int msb = orig & 0x80;
                xxxRegister.setValue((byte) ((orig & 0xFF) >>> 1));
                xxxRegister.setValue((byte) (xxxRegister.getValue() | msb));
                registers.getF().setZ(xxxRegister.getValue() == 0);
                registers.getF().setN(false);
                registers.getF().setH(false);
                registers.getF().setC(orig & 0x01);

                return new EmptyInstruction(2 + xxxRegister.getAccessCost());
            } else if ((nextInstruction & 0b11111_000) == 0b00110_000) {
                // SWAP r
                byte xxx = (byte) (nextInstruction & 0b00000_111);
                InstructionTarget8Bit xxxRegister = getRegisterFor(xxx, registers);
                byte lower4  = (byte) ((xxxRegister.getValue() <<  4) & 0xF0);
                byte higher4 = (byte) ((xxxRegister.getValue() >>> 4) & 0x0F);
                xxxRegister.setValue((byte) (lower4 | higher4));

                registers.getF().setZ(xxxRegister.getValue() == 0);
                registers.getF().setN(false);
                registers.getF().setH(false);
                registers.getF().setC(false);

                return new EmptyInstruction(2 + xxxRegister.getAccessCost());
            } else if ((nextInstruction & 0b11111_000) == 0b00111_000) {
                // SRL r
                byte xxx = (byte) (nextInstruction & 0b00000_111);
                InstructionTarget8Bit xxxRegister = getRegisterFor(xxx, registers);
                byte orig = xxxRegister.getValue();
                xxxRegister.setValue((byte) ((orig & 0xFF) >>> 1));
                registers.getF().setZ(xxxRegister.getValue() == 0);
                registers.getF().setN(false);
                registers.getF().setH(false);
                registers.getF().setC(orig & 0x01);

                return new EmptyInstruction(2 + xxxRegister.getAccessCost());
            } else if ((nextInstruction & 0b11_000_000) == 0b01_000_000) {
                // BIT i, r
                byte iii = (byte) ((nextInstruction & 0b00_111_000) >>> 3);
                byte xxx = (byte) (nextInstruction & 0b00000_111);
                InstructionTarget8Bit xxxRegister = getRegisterFor(xxx, registers);

                int compliment = ~xxxRegister.getValue();
                int masked = (compliment & (0x01 << iii));
                registers.getF().setZ(masked);
                registers.getF().setN(false);
                registers.getF().setH(true);

                return new EmptyInstruction(2 + xxxRegister.getAccessCost());
            } else if ((nextInstruction & 0b11_000_000) == 0b10_000_000) {
                // RES i, r
                byte iii = (byte) ((nextInstruction & 0b00_111_000) >>> 3);
                byte xxx = (byte) (nextInstruction & 0b00000_111);
                InstructionTarget8Bit xxxRegister = getRegisterFor(xxx, registers);

                int targetBit = (0x01 << iii);
                int orig = xxxRegister.getValue() & 0xFF;

                xxxRegister.setValue((byte) (orig & ~targetBit));

                return new EmptyInstruction(2 + xxxRegister.getAccessCost());
            } else if ((nextInstruction & 0b11_000_000) == 0b11_000_000) {
                // SET i, r
                byte iii = (byte) ((nextInstruction & 0b00_111_000) >>> 3);
                byte xxx = (byte) (nextInstruction & 0b00000_111);
                InstructionTarget8Bit xxxRegister = getRegisterFor(xxx, registers);

                int targetBit = (0x01 << iii);
                int orig = xxxRegister.getValue() & 0xFF;

                xxxRegister.setValue((byte) (orig | targetBit));

                return new EmptyInstruction(2 + xxxRegister.getAccessCost());
            } else {
                System.out.println("Received 16 bit instruction 0xCB" + String.format("%02X", nextInstruction));
                throw new IndexOutOfBoundsException("Received invalid instruction for 16bit: " + String.format("0x%02X", nextInstruction));
            }

        }
        else if (nextInstruction == (byte) 0x20) {
            // JR nz, n
            byte addr = instructionFetcher.fetchNextByte();
            if (!registers.getF().getZ()) {
                registers.getPC().setValue(registers.getPC().getValue() + addr);
                return new EmptyInstruction(3);
            }
            return new EmptyInstruction(2);
        } else if (nextInstruction == (byte) 0x28) {
            // JR Z, n
            byte addr = instructionFetcher.fetchNextByte();
            if (registers.getF().getZ()) {
                registers.getPC().setValue(registers.getPC().getValue() + addr);
                return new EmptyInstruction(3);
            }
            return new EmptyInstruction(2);
        } else if (nextInstruction == (byte) 0x30) {
            // JR NC, n
            byte addr = instructionFetcher.fetchNextByte();
            if (!registers.getF().getC()) {
                registers.getPC().setValue(registers.getPC().getValue() + addr);
                return new EmptyInstruction(3);
            }
            return new EmptyInstruction(2);
        } else if (nextInstruction == (byte) 0x38) {
            // JR C, n
            byte addr = instructionFetcher.fetchNextByte();
            if (registers.getF().getC()) {
                registers.getPC().setValue(registers.getPC().getValue() + addr);
                return new EmptyInstruction(3);
            }
            return new EmptyInstruction(2);
        } else if (nextInstruction == (byte) 0x18) {
            // JR n
            byte addrOffset = instructionFetcher.fetchNextByte();
            registers.getPC().setValue(registers.getPC().getValue() + addrOffset);

            return new EmptyInstruction(3);
        } else if (nextInstruction == (byte) 0xC3) {
            // JP nn
            byte lowerBits = instructionFetcher.fetchNextByte();
            byte higherBits = instructionFetcher.fetchNextByte();
            int targetPc = (higherBits & 0xFF) << 8 | (lowerBits & 0xFF);

            registers.getPC().setValue(targetPc);

            return new EmptyInstruction(4);
        } else if (nextInstruction == (byte) 0xE9) {
            // JP HL
            registers.getPC().setValue(registers.getHL().getValue());

            return new EmptyInstruction(1);
        } else if (nextInstruction == (byte) 0xE0) {
            // LD (n), A
            byte higher = (byte) 0xFF;
            byte lower = instructionFetcher.fetchNextByte();
            int targetAddr = (higher & 0xFF) << 8 | (lower & 0xFF);

            bus.writeByteAt(targetAddr, registers.getA().getValue());

            return new EmptyInstruction(3);
        } else if (nextInstruction == (byte) 0xF0) {
            // LD A, (n)
            byte higher = (byte) 0xFF;
            byte lower = instructionFetcher.fetchNextByte();
            int targetAddr = (higher & 0xFF) << 8 | (lower & 0xFF);

            registers.getA().setValue(bus.readByteAt(targetAddr));

            return new EmptyInstruction(3);
        } else if (nextInstruction == (byte) 0xFA) {
            // LD A, (nn)
            byte lowerBits = instructionFetcher.fetchNextByte();
            byte higherBits = instructionFetcher.fetchNextByte();
            int targetAddr = (higherBits & 0xFF) << 8 | (lowerBits & 0xFF);
            registers.getA().setValue(bus.readByteAt(targetAddr));

            return new EmptyInstruction(4);
        } else if (nextInstruction == (byte) 0xEA) {
            // LD (nn), A
            byte lowerBits = instructionFetcher.fetchNextByte();
            byte higherBits = instructionFetcher.fetchNextByte();
            int targetAddr = (higherBits & 0xFF) << 8 | (lowerBits & 0xFF);
            bus.writeByteAt(targetAddr, registers.getA().getValue());

            return new EmptyInstruction(4);
        } else if (nextInstruction == (byte) 0xF2) {
            // LD A, (C)
            byte higher = (byte) 0xFF;
            byte lower = registers.getC().getValue();
            int targetAddr = (higher & 0xFF) << 8 | (lower & 0xFF);

            registers.getA().setValue(bus.readByteAt(targetAddr));

            return new EmptyInstruction(2);
        } else if (nextInstruction == (byte) 0xE2) {
            // LD (C), A
            byte higher = (byte) 0xFF;
            byte lower = registers.getC().getValue();
            int targetAddr = (higher & 0xFF) << 8 | (lower & 0xFF);

            bus.writeByteAt(targetAddr, registers.getA().getValue());

            return new EmptyInstruction(2);
        } else if (nextInstruction == (byte) 0x08) {
            // LD (nn), SP
            Register lowerRegister = registers.getSP().getLowerRegister();
            Register higherRegister = registers.getSP().getHigherRegister();

            byte lowerBits = instructionFetcher.fetchNextByte();
            byte higherBits = instructionFetcher.fetchNextByte();
            int targetAddr = (higherBits & 0xFF) << 8 | (lowerBits & 0xFF);

            bus.writeByteAt(targetAddr, lowerRegister.getValue());
            bus.writeByteAt(targetAddr+1, higherRegister.getValue());

            return new EmptyInstruction(5);
        } else if (nextInstruction == (byte) 0xF9) {
            // LD SP, HL
            registers.getSP().setValue(registers.getHL().getValue());

            return new EmptyInstruction(2);
        } else if (nextInstruction == (byte) 0xE8) {
            // ADD SP, s8
            byte value = instructionFetcher.fetchNextByte();
            int orig = registers.getSP().getValue();
            registers.getSP().setValue(value + orig);

            registers.getF().setZ(false);
            registers.getF().setN(false);

            // https://stackoverflow.com/questions/57958631/game-boy-half-carry-flag-and-16-bit-instructions-especially-opcode-0xe8
            registers.getF().setH((byte) orig, value, Operator.ADD); // ((orig & 0xF) + (value & 0xF)) & 0x10);
            registers.getF().setC((byte) orig, value, Operator.ADD); // c = (((orig & 0xFF) + (value & 0xFF)) & 0x100) == 0x100;

            return new EmptyInstruction(4);
        } else if (nextInstruction == (byte) 0xF8) {
            // LD HL, SP+s8
            byte value = instructionFetcher.fetchNextByte();
            int orig = registers.getSP().getValue();
            registers.getHL().setValue(value + orig);

            registers.getF().setZ(false);
            registers.getF().setN(false);
            registers.getF().setH(((orig & 0xF) + (value & 0xF)) & 0x10);
            registers.getF().setC(((orig & 0xFF) + (value & 0xFF)) & 0x100);

            return new EmptyInstruction(3);
        } else if (nextInstruction == (byte) 0xCD) {
            // CALL nn
            byte lower = instructionFetcher.fetchNextByte();
            byte higher = instructionFetcher.fetchNextByte();
            int targetAddr = (higher & 0xFF) << 8 | (lower & 0xFF);

            pushToStack(registers.getPC().getValue(), registers, bus);
            registers.getPC().setValue(targetAddr);

            return new EmptyInstruction(6);
        } else if (nextInstruction == (byte) 0xC9) {
            // RET
            registers.getPC().setValue(popFromStack(registers, bus));

            return new EmptyInstruction(4);
        } else if (nextInstruction == (byte) 0xD9) {
            // RETI
            registers.getPC().setValue(popFromStack(registers, bus));

            interruptController.setInterruptMasterEnable(true);

            return new EmptyInstruction(4);
        } else if ((nextInstruction & 0b11_000_111) == 0b11_000_110) {
            // ADD/SUB/etc. A, d8
            // (operate with A and the immediate value in the next byte)
            byte opxxx = (byte) ((nextInstruction & 0b00_111_000) >> 3);
            Operator operator = getAluOperatorFor(opxxx);

            byte orig = registers.getA().getValue();
            byte immediateValue = instructionFetcher.fetchNextByte();
            int carry = registers.getF().getC() ? 1 : 0;

            byte value = operator.apply(orig, immediateValue, carry);
            if (!operator.equals(Operator.CP))
                registers.getA().setValue(value);

            registers.getF().setZ(value == 0);
            registers.getF().setN(operator);
            registers.getF().setH(orig, immediateValue, carry, operator);
            registers.getF().setC(orig, immediateValue, carry, operator);

            return new EmptyInstruction(2);
        } else if (nextInstruction == (byte) 0x2F) {
            // CPL
            registers.getA().setValue((byte) (~registers.getA().getValue()));

            registers.getF().setN(true);
            registers.getF().setH(true);

            return new EmptyInstruction(1);

        } else if (nextInstruction == (byte) 0x3F) {
            // CCF
            registers.getF().setN(false);
            registers.getF().setH(false);
            registers.getF().setC(!registers.getF().getC());

            return new EmptyInstruction(1);
        } else if (nextInstruction == (byte) 0x37) {
            // SCF
            registers.getF().setN(false);
            registers.getF().setH(false);
            registers.getF().setC(true);

            return new EmptyInstruction(1);
        } else if (nextInstruction == (byte) 0x1F) {
            // RRA
            byte msb = (byte) ((registers.getF().getC() ? 1 : 0) << 7);
            byte orig = registers.getA().getValue();
            registers.getA().setValue((byte) ((registers.getA().getValue() & 0xFF) >>> 1));
            registers.getA().setValue((byte) (registers.getA().getValue() | msb));
            registers.getF().setZ(false);
            registers.getF().setN(false);
            registers.getF().setH(false);
            registers.getF().setC(orig & 0x01);

            return new EmptyInstruction(1);
        } else if (nextInstruction == (byte) 0x17) {
            // RLA
            byte orig = registers.getA().getValue();
            byte msb = (byte) ((orig & 0x80) >>> 7);
            byte carry = (byte) (registers.getF().getC() ? 1:0);
            registers.getA().setValue((byte) ((registers.getA().getValue() & 0xFF) << 1));
            registers.getA().setValue((byte) (registers.getA().getValue() | carry));
            registers.getF().setZ(false);
            registers.getF().setN(false);
            registers.getF().setH(false);
            registers.getF().setC(msb == 0x01);

            return new EmptyInstruction(1);
        } else if (nextInstruction == (byte) 0x0F) {
            // RRCA
            byte orig = registers.getA().getValue();
            byte lsb = (byte) (orig & 0x01);
            registers.getA().setValue((byte) ((registers.getA().getValue() & 0xFF) >>> 1));
            registers.getA().setValue((byte) (registers.getA().getValue() | lsb << 7));
            registers.getF().setZ(false);
            registers.getF().setN(false);
            registers.getF().setH(false);
            registers.getF().setC(lsb == 0x01);

            return new EmptyInstruction(1);
        } else if (nextInstruction == (byte) 0x07) {
            // RLCA
            byte orig = registers.getA().getValue();
            byte msb = (byte) ((orig & 0x80) >>> 7);
            registers.getA().setValue((byte) ((registers.getA().getValue() & 0xFF) << 1));
            registers.getA().setValue((byte) (registers.getA().getValue() | msb));
            registers.getF().setZ(false);
            registers.getF().setN(false);
            registers.getF().setH(false);
            registers.getF().setC(msb == 0x01);

            return new EmptyInstruction(1);
        } else if (nextInstruction == (byte) 0x27) {
            // DAA
            byte value = registers.getA().getValue();
            byte correction = 0x00;
            if (registers.getF().getH() || (!registers.getF().getN() && (value & 0x0F) > 9)) {
                correction = 0x06;
            }

            if (registers.getF().getC() || (!registers.getF().getN() && (value & 0xFF) > 0x99)) {
                correction |= 0x60;
                registers.getF().setC(true);
            }

            value = (byte) ((value & 0xFF) + (registers.getF().getN() ? -correction : correction));
            registers.getA().setValue(value);
            registers.getF().setZ(value == 0);
            registers.getF().setH(false);

            return new EmptyInstruction(1);
        } else {
            System.out.println("Received invalid instruction: " + String.format("0x%02X", nextInstruction));
            throw new IndexOutOfBoundsException("Received invalid instruction: " + String.format("0x%02X", nextInstruction));
        }
    }
}
