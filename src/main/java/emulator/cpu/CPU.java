package emulator.cpu;

import emulator.Bus;
import emulator.interrupts.InterruptController;
import emulator.cpu.register.*;

public class CPU {
    // Order of registers; B,C,D,E,H,L,(HL),A
    private Bus bus;
    private InterruptController interruptController;
    private Registers registers;

    private boolean isHalted;


    public CPU(Bus bus, InterruptController interruptController) {
        this.bus = bus;
        this.interruptController = interruptController;

        registers = new Registers(bus);
        
        // initial values after boot
        

        initMemory();
    }

    private void initMemory() {
        bus.writeByteAt(0xFF05, (byte) 0x00); // ; TIMA
        bus.writeByteAt(0xFF06, (byte) 0x00); // ; TMA
        bus.writeByteAt(0xFF07, (byte) 0x00); // ; TAC
        bus.writeByteAt(0xFF10, (byte) 0x80); // ; NR10
        bus.writeByteAt(0xFF11, (byte) 0xBF); // ; NR11
        bus.writeByteAt(0xFF12, (byte) 0xF3); // ; NR12
        bus.writeByteAt(0xFF14, (byte) 0xBF); // ; NR14
        bus.writeByteAt(0xFF16, (byte) 0x3F); // ; NR21
        bus.writeByteAt(0xFF17, (byte) 0x00); // ; NR22
        bus.writeByteAt(0xFF19, (byte) 0xBF); // ; NR24
        bus.writeByteAt(0xFF1A, (byte) 0x7F); // ; NR30
        bus.writeByteAt(0xFF1B, (byte) 0xFF); // ; NR31
        bus.writeByteAt(0xFF1C, (byte) 0x9F); // ; NR32
        bus.writeByteAt(0xFF1E, (byte) 0xBF); // ; NR33
        bus.writeByteAt(0xFF20, (byte) 0xFF); // ; NR41
        bus.writeByteAt(0xFF21, (byte) 0x00); // ; NR42
        bus.writeByteAt(0xFF22, (byte) 0x00); // ; NR43
        bus.writeByteAt(0xFF23, (byte) 0xBF); // ; NR30
        bus.writeByteAt(0xFF24, (byte) 0x77); // ; NR50
        bus.writeByteAt(0xFF25, (byte) 0xF3); // ; NR51
        bus.writeByteAt(0xFF26, (byte) 0xF1); // ; NR52
        bus.writeByteAt(0xFF40, (byte) 0x91); // ; LCDC
        bus.writeByteAt(0xFF42, (byte) 0x00); // ; SCY
        bus.writeByteAt(0xFF43, (byte) 0x00); // ; SCX
        bus.writeByteAt(0xFF45, (byte) 0x00); // ; LYC
        bus.writeByteAt(0xFF47, (byte) 0xFC); // ; BGP
        bus.writeByteAt(0xFF48, (byte) 0xFF); // ; OBP0
        bus.writeByteAt(0xFF49, (byte) 0xFF); // ; OBP1
        bus.writeByteAt(0xFF4A, (byte) 0x00); // ; WY
        bus.writeByteAt(0xFF4B, (byte) 0x00); // ; WX
        bus.writeByteAt(0xFFFF, (byte) 0x00); // ; IE
        System.out.println("Setup memory!\n");
    }

    public int executeNext() {
        if (isHalted) {
            if (!interruptController.getInterruptMasterEnable() && (interruptController.getInterruptFlag() & interruptController.getInterruptEnable()) != 0) {
                isHalted = false;
            } else {
                return 1;
            }
        }

//        InstructionLogger.logInstruction(A, F, B, C, D, E, H, L, SP, PC, bus);
        // fetch
        byte nextInstruction = readNextByte();


        // decode
        if (nextInstruction == (byte) 0x00) {
            // NOP
            return 1;
        } else if (nextInstruction == (byte) 0x10) {
            // STOP (disable interrupts)
            readNextByte();
            return 1;
            // TODO
        } else if (nextInstruction == (byte) 0x76) {
            this.isHalted = true;
            return 1;
        } else if (nextInstruction == (byte) 0xFB) {
            // EI
            interruptController.setInterruptMasterEnable(true);
            return 1;
        } else if (nextInstruction == (byte) 0xF3) {
            // DI (disable interrupts)
            interruptController.setInterruptMasterEnable(false);
            return 1;
        } else if ((nextInstruction & 0b11_000_111) == 0b00_000_110) {
            // LD r1, n --- LD (HL), n
            byte xxx = (byte) ((nextInstruction & 0b00_111_000) >>> 3);
            byte value = readNextByte();
            InstructionTarget8Bit xxxRegister = getRegisterFor(xxx);
            xxxRegister.setValue(value);

            return xxxRegister.getAccessCost() + 2;
        } else if ((nextInstruction & 0b11_000_000) == 0b01_000_000) {
            // LD r1, r2
            byte xxx = (byte) ((nextInstruction & 0b00_111_000) >> 3);
            byte yyy = (byte) (nextInstruction & 0b00_000_111);

            InstructionTarget8Bit xxxRegister = getRegisterFor(xxx);
            InstructionTarget8Bit yyyRegister = getRegisterFor(yyy);

            xxxRegister.setValue(yyyRegister.getValue());

            return 1 + xxxRegister.getAccessCost() + yyyRegister.getAccessCost();
        } else if ((nextInstruction & 0b11_000_111) == 0b00_000_100) {
            // INC r1
            byte xxx = (byte) ((nextInstruction & 0b00_111_000) >>> 3);
            InstructionTarget8Bit xxxRegister = getRegisterFor(xxx);

            byte orig = xxxRegister.getValue();
            xxxRegister.setValue((byte) (xxxRegister.getValue() + 1));

            registers.getF().setZ(xxxRegister.getValue() == 0);
            registers.getF().setN(false);
            registers.getF().setH(orig, (byte) 0x1, Operator.ADD);

            return xxxRegister.getAccessCost()*2 + 1;
        } else if ((nextInstruction & 0b11_000_111) == 0b00_000_101) {
            // DEC r1
            byte xxx = (byte) ((nextInstruction & 0b00_111_000) >>> 3);
            InstructionTarget8Bit xxxRegister = getRegisterFor(xxx);

            byte orig = xxxRegister.getValue();
            xxxRegister.setValue((byte) (xxxRegister.getValue() - 1));

            registers.getF().setZ(xxxRegister.getValue() == 0);
            registers.getF().setN(true);
            registers.getF().setH(orig, (byte) 0x1, Operator.SUB);

            return xxxRegister.getAccessCost()*2 + 1;
        } else if ((nextInstruction & 0b11_00_1111) == 0b00_00_0001) {
            // LD rr, nn
            byte xx = (byte) ((nextInstruction & 0b00_110_000) >>> 4);
            DoubleRegister xxDblRegisters = getDoubleRegisterFor(xx);
            byte lowerBits = readNextByte();
            byte higherBits = readNextByte();

            xxDblRegisters.setValue(higherBits, lowerBits);

            return 3;
        } else if ((nextInstruction & 0b11_00_1111) == 0b00_00_0011) {
            // INC rr
            byte xx = (byte) ((nextInstruction & 0b00_110_000) >>> 4);
            DoubleRegister xxDblRegisters = getDoubleRegisterFor(xx);
            xxDblRegisters.setValue(xxDblRegisters.getValue() + 1);

            return 2;

        } else if ((nextInstruction & 0b11_00_1111) == 0b00_00_1011) {
            // DEC rr
            byte xx = (byte) ((nextInstruction & 0b00_110_000) >>> 4);
            DoubleRegister xxDblRegisters = getDoubleRegisterFor(xx);
            xxDblRegisters.setValue(xxDblRegisters.getValue() - 1);

            return 2;
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
            return 2;
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
            return 2;
        } else if ((nextInstruction & 0b11_00_1111) == 0b11_00_0001) {
            // POP rr
            byte xx = (byte) ((nextInstruction & 0b00_11_0000) >>> 4);

            // POP AF|BC|DE|HL
            DoubleRegister xxDoubleRegister = getDoublePushPopRegisterFor(xx);

            xxDoubleRegister.setValue(popFromStack());

            return 3;
        } else if ((nextInstruction & 0b11_00_1111) == 0b11_00_0101) {
            // PUSH rr
            byte xx = (byte) ((nextInstruction & 0b00_11_0000) >>> 4);

            // PUSH AF|BC|DE|HL
            DoubleRegister xxDoubleRegister = getDoublePushPopRegisterFor(xx);

            pushToStack(xxDoubleRegister.getValue());

            return 4;
        } else if ((nextInstruction & 0b11_000_000) == 0b10_000_000) {
            // ADD/SUB/etc A, r
            byte opxxx = (byte) ((nextInstruction & 0b00_111_000) >> 3);
            Operator operator = getAluOperatorFor(opxxx);

            byte xxx = (byte) (nextInstruction & 0b00_000_111);
            InstructionTarget8Bit xxxRegister = getRegisterFor(xxx);

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

            return 1 + xxxRegister.getAccessCost();
        } else if ((nextInstruction & 0b11_00_1111) == 0b00_00_1001) {
            // ADD HL, rr
            byte xx = (byte) ((nextInstruction & 0b00_11_0000) >>> 4);
            DoubleRegister xxDblRegisters = getDoubleRegisterFor(xx);
            int orig = registers.getHL().getValue();
            int toAdd = xxDblRegisters.getValue();
            registers.getHL().setValue(orig + toAdd);

            registers.getF().setN(false);
            registers.getF().setH(((orig & 0x0FFF) + (toAdd & 0x0FFF)) & 0b0001_0000_0000_0000);
            registers.getF().setC(((orig & 0xFFFF) + (toAdd & 0xFFFF)) > 0xFFFF);

            return 2;
        } else if ((nextInstruction & 0b111_00_111) == 0b110_00_000) {
            // RET cond
            byte cc = (byte) ((nextInstruction & 0b000_11_000) >>> 3);
            boolean flag = getFlagForCond(cc);
            if (flag) {
                registers.getPC().setValue(popFromStack());
                return 5;
            }

            return 2;
        } else if ((nextInstruction & 0b111_00_111) == 0b110_00_010) {
            // JP cond, nn
            byte cc = (byte) ((nextInstruction & 0b000_11_000) >>> 3);
            boolean flag = getFlagForCond(cc);

            byte lowerAddressBits = readNextByte();
            byte higherAddressBits = readNextByte();

            if (flag) {
                registers.getPC().setValue(higherAddressBits, lowerAddressBits);
                return 4;
            }
            return 3;
        } else if ((nextInstruction & 0b111_00_111) == 0b110_00_100) {
            // CALL cond, nn
            byte cc = (byte) ((nextInstruction & 0b000_11_000) >>> 3);
            boolean flag = getFlagForCond(cc);

            byte lower = readNextByte();
            byte higher = readNextByte();
            int targetAddr = (higher & 0xFF) << 8 | (lower & 0xFF);
            if (flag) {
                pushToStack(registers.getPC().getValue());
                registers.getPC().setValue(targetAddr);
                return 6;
            }
            return 3;
        } else if ((nextInstruction & 0b11_000_111) == 0b11_000_111) {
            // RST i
            byte iii = (byte) (nextInstruction & 0b00_111_000);
            // push current pc onto the stack
            pushToStack(registers.getPC().getValue());

            // set pc to page i of memory
            registers.getPC().setValue(iii);

            return 4;
        }
        else if (nextInstruction == (byte) 0xCB) {
            // 16 bit opcode....
            nextInstruction = readNextByte();
            if ((nextInstruction & 0b11111_000) == 0b00000_000) {
                // RLC r
                byte xxx = (byte) (nextInstruction & 0b00000_111);
                InstructionTarget8Bit xxxRegister = getRegisterFor(xxx);
                byte orig = xxxRegister.getValue();
                int msb = orig & 0x80;
                xxxRegister.setValue((byte) ((orig & 0xFF) << 1));
                xxxRegister.setValue((byte) (xxxRegister.getValue() | (msb >>> 7)));
                registers.getF().setZ(xxxRegister.getValue() == 0);
                registers.getF().setN(false);
                registers.getF().setH(false);
                registers.getF().setC(msb == 0x80);

                return 2 + xxxRegister.getAccessCost();
            } else if ((nextInstruction & 0b11111_000) == 0b00001_000) {
                // RRC r
                byte xxx = (byte) (nextInstruction & 0b00000_111);
                InstructionTarget8Bit xxxRegister = getRegisterFor(xxx);
                byte orig = xxxRegister.getValue();
                int lsb = orig & 0x01;
                xxxRegister.setValue((byte) ((orig & 0xFF) >>> 1));
                xxxRegister.setValue((byte) (xxxRegister.getValue() | (lsb << 7)));
                registers.getF().setZ(xxxRegister.getValue() == 0);
                registers.getF().setN(false);
                registers.getF().setH(false);
                registers.getF().setC(lsb == 0x01);

                return 2 + xxxRegister.getAccessCost();
            } else if ((nextInstruction & 0b11111_000) == 0b00010_000) {
                // RL r
                byte xxx = (byte) (nextInstruction & 0b00000_111);
                InstructionTarget8Bit xxxRegister = getRegisterFor(xxx);
                byte orig = xxxRegister.getValue();
                int carry = registers.getF().getC() ? 1 : 0;
                xxxRegister.setValue((byte) ((orig & 0xFF) << 1));
                xxxRegister.setValue((byte) (xxxRegister.getValue() | carry));
                registers.getF().setZ(xxxRegister.getValue() == 0);
                registers.getF().setN(false);
                registers.getF().setH(false);
                registers.getF().setC(orig & 0x80);

                return 2 + xxxRegister.getAccessCost();
            } else if ((nextInstruction & 0b11111_000) == 0b00011_000) {
                // RR r
                byte xxx = (byte) (nextInstruction & 0b00000_111);
                InstructionTarget8Bit xxxRegister = getRegisterFor(xxx);
                byte msb = (byte) ((registers.getF().getC() ? 1 : 0) << 7);
                byte orig = xxxRegister.getValue();
                xxxRegister.setValue((byte) ((xxxRegister.getValue() & 0xFF) >>> 1));
                xxxRegister.setValue((byte) (xxxRegister.getValue() | msb));
                registers.getF().setZ(xxxRegister.getValue() == 0);
                registers.getF().setN(false);
                registers.getF().setH(false);
                registers.getF().setC(orig & 0x01);

                return 2 + xxxRegister.getAccessCost();
            } else if ((nextInstruction & 0b11111_000) == 0b00100_000) {
                // SLA r
                byte xxx = (byte) (nextInstruction & 0b00000_111);
                InstructionTarget8Bit xxxRegister = getRegisterFor(xxx);
                byte orig = xxxRegister.getValue();
                xxxRegister.setValue((byte) ((orig & 0xFF) << 1));
                registers.getF().setZ(xxxRegister.getValue() == 0);
                registers.getF().setN(false);
                registers.getF().setH(false);
                registers.getF().setC(orig & 0x80);

                return 2 + xxxRegister.getAccessCost();
            } else if ((nextInstruction & 0b11111_000) == 0b00101_000) {
                // SRA r
                byte xxx = (byte) (nextInstruction & 0b00000_111);
                InstructionTarget8Bit xxxRegister = getRegisterFor(xxx);
                byte orig = xxxRegister.getValue();
                int msb = orig & 0x80;
                xxxRegister.setValue((byte) ((orig & 0xFF) >>> 1));
                xxxRegister.setValue((byte) (xxxRegister.getValue() | msb));
                registers.getF().setZ(xxxRegister.getValue() == 0);
                registers.getF().setN(false);
                registers.getF().setH(false);
                registers.getF().setC(orig & 0x01);

                return 2 + xxxRegister.getAccessCost();
            } else if ((nextInstruction & 0b11111_000) == 0b00110_000) {
                // SWAP r
                byte xxx = (byte) (nextInstruction & 0b00000_111);
                InstructionTarget8Bit xxxRegister = getRegisterFor(xxx);
                byte lower4  = (byte) ((xxxRegister.getValue() <<  4) & 0xF0);
                byte higher4 = (byte) ((xxxRegister.getValue() >>> 4) & 0x0F);
                xxxRegister.setValue((byte) (lower4 | higher4));

                registers.getF().setZ(xxxRegister.getValue() == 0);
                registers.getF().setN(false);
                registers.getF().setH(false);
                registers.getF().setC(false);

                return 2 + xxxRegister.getAccessCost();
            } else if ((nextInstruction & 0b11111_000) == 0b00111_000) {
                // SRL r
                byte xxx = (byte) (nextInstruction & 0b00000_111);
                InstructionTarget8Bit xxxRegister = getRegisterFor(xxx);
                byte orig = xxxRegister.getValue();
                xxxRegister.setValue((byte) ((orig & 0xFF) >>> 1));
                registers.getF().setZ(xxxRegister.getValue() == 0);
                registers.getF().setN(false);
                registers.getF().setH(false);
                registers.getF().setC(orig & 0x01);

                return 2 + xxxRegister.getAccessCost();
            } else if ((nextInstruction & 0b11_000_000) == 0b01_000_000) {
                // BIT i, r
                byte iii = (byte) ((nextInstruction & 0b00_111_000) >>> 3);
                byte xxx = (byte) (nextInstruction & 0b00000_111);
                InstructionTarget8Bit xxxRegister = getRegisterFor(xxx);

                int compliment = ~xxxRegister.getValue();
                int masked = (compliment & (0x01 << iii));
                registers.getF().setZ(masked);
                registers.getF().setN(false);
                registers.getF().setH(true);

                return 2 + xxxRegister.getAccessCost();
            } else if ((nextInstruction & 0b11_000_000) == 0b10_000_000) {
                // RES i, r
                byte iii = (byte) ((nextInstruction & 0b00_111_000) >>> 3);
                byte xxx = (byte) (nextInstruction & 0b00000_111);
                InstructionTarget8Bit xxxRegister = getRegisterFor(xxx);

                int targetBit = (0x01 << iii);
                int orig = xxxRegister.getValue() & 0xFF;

                xxxRegister.setValue((byte) (orig & ~targetBit));

                return 2 + xxxRegister.getAccessCost();
            } else if ((nextInstruction & 0b11_000_000) == 0b11_000_000) {
                // SET i, r
                byte iii = (byte) ((nextInstruction & 0b00_111_000) >>> 3);
                byte xxx = (byte) (nextInstruction & 0b00000_111);
                InstructionTarget8Bit xxxRegister = getRegisterFor(xxx);

                int targetBit = (0x01 << iii);
                int orig = xxxRegister.getValue() & 0xFF;

                xxxRegister.setValue((byte) (orig | targetBit));

                return 2 + xxxRegister.getAccessCost();
            } else {
                System.out.println("Received 16 bit instruction 0xCB" + String.format("%02X", nextInstruction));
                throw new IndexOutOfBoundsException("Received invalid instruction for 16bit: " + String.format("0x%02X", nextInstruction));
            }

        }
        else if (nextInstruction == (byte) 0x20) {
            // JR nz, n
            byte addr = readNextByte();
            if (!registers.getF().getZ()) {
                registers.getPC().setValue(registers.getPC().getValue() + addr);
                return 3;
            }
            return 2;
        } else if (nextInstruction == (byte) 0x28) {
            // JR Z, n
            byte addr = readNextByte();
            if (registers.getF().getZ()) {
                registers.getPC().setValue(registers.getPC().getValue() + addr);
                return 3;
            }
            return 2;
        } else if (nextInstruction == (byte) 0x30) {
            // JR NC, n
            byte addr = readNextByte();
            if (!registers.getF().getC()) {
                registers.getPC().setValue(registers.getPC().getValue() + addr);
                return 3;
            }
            return 2;
        } else if (nextInstruction == (byte) 0x38) {
            // JR C, n
            byte addr = readNextByte();
            if (registers.getF().getC()) {
                registers.getPC().setValue(registers.getPC().getValue() + addr);
                return 3;
            }
            return 2;
        } else if (nextInstruction == (byte) 0x18) {
            // JR n
            byte addrOffset = readNextByte();
            registers.getPC().setValue(registers.getPC().getValue() + addrOffset);

            return 3;
        } else if (nextInstruction == (byte) 0xC3) {
            // JP nn
            byte lowerBits = readNextByte();
            byte higherBits = readNextByte();
            int targetPc = (higherBits & 0xFF) << 8 | (lowerBits & 0xFF);

            registers.getPC().setValue(targetPc);

            return 4;
        } else if (nextInstruction == (byte) 0xE9) {
            // JP HL
            registers.getPC().setValue(registers.getHL().getValue());

            return 1;
        } else if (nextInstruction == (byte) 0xE0) {
            // LD (n), A
            byte higher = (byte) 0xFF;
            byte lower = readNextByte();
            int targetAddr = (higher & 0xFF) << 8 | (lower & 0xFF);

            bus.writeByteAt(targetAddr, registers.getA().getValue());

            return 3;
        } else if (nextInstruction == (byte) 0xF0) {
            // LD A, (n)
            byte higher = (byte) 0xFF;
            byte lower = readNextByte();
            int targetAddr = (higher & 0xFF) << 8 | (lower & 0xFF);

            registers.getA().setValue(bus.readByteAt(targetAddr));

            return 3;
        } else if (nextInstruction == (byte) 0xFA) {
            // LD A, (nn)
            byte lowerBits = readNextByte();
            byte higherBits = readNextByte();
            int targetAddr = (higherBits & 0xFF) << 8 | (lowerBits & 0xFF);
            registers.getA().setValue(bus.readByteAt(targetAddr));

            return 4;
        } else if (nextInstruction == (byte) 0xEA) {
            // LD (nn), A
            byte lowerBits = readNextByte();
            byte higherBits = readNextByte();
            int targetAddr = (higherBits & 0xFF) << 8 | (lowerBits & 0xFF);
            bus.writeByteAt(targetAddr, registers.getA().getValue());

            return 4;
        } else if (nextInstruction == (byte) 0xF2) {
            // LD A, (C)
            byte higher = (byte) 0xFF;
            byte lower = registers.getC().getValue();
            int targetAddr = (higher & 0xFF) << 8 | (lower & 0xFF);

            registers.getA().setValue(bus.readByteAt(targetAddr));

            return 2;
        } else if (nextInstruction == (byte) 0xE2) {
            // LD (C), A
            byte higher = (byte) 0xFF;
            byte lower = registers.getC().getValue();
            int targetAddr = (higher & 0xFF) << 8 | (lower & 0xFF);

            bus.writeByteAt(targetAddr, registers.getA().getValue());

            return 2;
        } else if (nextInstruction == (byte) 0x08) {
            // LD (nn), SP
            Register lowerRegister = registers.getSP().getLowerRegister();
            Register higherRegister = registers.getSP().getHigherRegister();

            byte lowerBits = readNextByte();
            byte higherBits = readNextByte();
            int targetAddr = (higherBits & 0xFF) << 8 | (lowerBits & 0xFF);

            bus.writeByteAt(targetAddr, lowerRegister.getValue());
            bus.writeByteAt(targetAddr+1, higherRegister.getValue());

            return 5;
        } else if (nextInstruction == (byte) 0xF9) {
            // LD SP, HL
            registers.getSP().setValue(registers.getHL().getValue());

            return 2;
        } else if (nextInstruction == (byte) 0xE8) {
            // ADD SP, s8
            byte value = readNextByte();
            int orig = registers.getSP().getValue();
            registers.getSP().setValue(value + orig);

            registers.getF().setZ(false);
            registers.getF().setN(false);

            // https://stackoverflow.com/questions/57958631/game-boy-half-carry-flag-and-16-bit-instructions-especially-opcode-0xe8
            registers.getF().setH((byte) orig, value, Operator.ADD); // ((orig & 0xF) + (value & 0xF)) & 0x10);
            registers.getF().setC((byte) orig, value, Operator.ADD); // c = (((orig & 0xFF) + (value & 0xFF)) & 0x100) == 0x100;

            return 4;
        } else if (nextInstruction == (byte) 0xF8) {
            // LD HL, SP+s8
            byte value = readNextByte();
            int orig = registers.getSP().getValue();
            registers.getHL().setValue(value + orig);

            registers.getF().setZ(false);
            registers.getF().setN(false);
            registers.getF().setH(((orig & 0xF) + (value & 0xF)) & 0x10);
            registers.getF().setC(((orig & 0xFF) + (value & 0xFF)) & 0x100);

            return 3;
        } else if (nextInstruction == (byte) 0xCD) {
            // CALL nn
            byte lower = readNextByte();
            byte higher = readNextByte();
            int targetAddr = (higher & 0xFF) << 8 | (lower & 0xFF);

            pushToStack(registers.getPC().getValue());
            registers.getPC().setValue(targetAddr);

            return 6;
        } else if (nextInstruction == (byte) 0xC9) {
            // RET
            registers.getPC().setValue(popFromStack());

            return 4;
        } else if (nextInstruction == (byte) 0xD9) {
            // RETI
            registers.getPC().setValue(popFromStack());

            interruptController.setInterruptMasterEnable(true);

            return 4;
        } else if ((nextInstruction & 0b11_000_111) == 0b11_000_110) {
            byte opxxx = (byte) ((nextInstruction & 0b00_111_000) >> 3);
            Operator operator = getAluOperatorFor(opxxx);

            byte orig = registers.getA().getValue();
            byte target = readNextByte();
            int carry = registers.getF().getC() ? 1 : 0;

            byte value = operator.apply(orig, target, carry);
            if (!operator.equals(Operator.CP))
                registers.getA().setValue(value);

            registers.getF().setZ(value == 0);
            registers.getF().setN(operator);
            registers.getF().setH(orig, target, carry, operator);
            registers.getF().setC(orig, target, carry, operator);

            return 2;
        } else if (nextInstruction == (byte) 0x2F) {
            // CPL
            registers.getA().setValue((byte) (~registers.getA().getValue()));

            registers.getF().setN(true);
            registers.getF().setH(true);

            return 1;

        } else if (nextInstruction == (byte) 0x3F) {
            // CCF
            registers.getF().setN(false);
            registers.getF().setH(false);
            registers.getF().setC(!registers.getF().getC());

            return 1;
        } else if (nextInstruction == (byte) 0x37) {
            // SCF
            registers.getF().setN(false);
            registers.getF().setH(false);
            registers.getF().setC(true);

            return 1;
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

            return 1;
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

            return 1;
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

            return 1;
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

            return 1;
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

            return 1;
        } else {
            System.out.println("Received invalid instruction: " + String.format("0x%02X", nextInstruction));
            throw new IndexOutOfBoundsException("Received invalid instruction: " + String.format("0x%02X", nextInstruction));
        }
    }

    private void pushToStack(int value) {
        registers.getSP().setValue(registers.getSP().getValue() - 1);
        bus.writeByteAt(registers.getSP().getValue(), (byte) ((value >> 8) & 0xFF));
        registers.getSP().setValue(registers.getSP().getValue() - 1);
        bus.writeByteAt(registers.getSP().getValue(), (byte) (value & 0xFF));
    }

    private int popFromStack() {
        byte lowerByte = bus.readByteAt(registers.getSP().getValue());
        registers.getSP().setValue(registers.getSP().getValue() + 1);
        byte higherByte = bus.readByteAt(registers.getSP().getValue());
        registers.getSP().setValue(registers.getSP().getValue() + 1);

        return (lowerByte & 0xFF) | ((higherByte & 0xFF) << 8);
    }

    private Operator getAluOperatorFor(byte opxxx) {
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

    private boolean getFlagForCond(byte cc) {
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

    private DoubleRegister getDoublePushPopRegisterFor(byte xx) {
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

    private DoubleRegister getDoubleRegisterFor(byte xx) {
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

    private InstructionTarget8Bit getRegisterFor(byte xxx) {
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

    private byte readNextByte() {
        byte nextByte = bus.readByteAt(registers.getPC().getValue());
        registers.getPC().setValue(registers.getPC().getValue() + 1);
        return nextByte;
    }

    public int checkInterrupts() {
        if (interruptController.interruptReady()) {
            int jumpAddress = interruptController.getHighestPriorityInterruptAddress();
//            System.out.printf("Servicing interrupt: 0x%02X\n", jumpAddress);

            //  Reset the IME flag and prevent all interrupts.
            interruptController.setInterruptMasterEnable(false);

            //  The PC (program counter) is pushed onto the stack.
            byte lowerPC = registers.getPC().getLowerRegister().getValue();
            byte higherPC = registers.getPC().getHigherRegister().getValue();
            registers.getSP().setValue(registers.getSP().getValue() - 1);
            bus.writeByteAt(registers.getSP().getValue(), higherPC);
            registers.getSP().setValue(registers.getSP().getValue() - 1);
            bus.writeByteAt(registers.getSP().getValue(), lowerPC);

            //  Jump to the starting address of the interrupt.
            registers.getPC().setValue(jumpAddress);

            isHalted = false;

            return 5;
        }
        return 0;
    }
}
