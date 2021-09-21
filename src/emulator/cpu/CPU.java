package emulator.cpu;

import emulator.Bus;
import emulator.InterruptController;

public class CPU {
    // Order of registers; B,C,D,E,H,L,(HL),A
    private Bus bus;
    private InterruptController interruptController;
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

    private InstructionTarget8Bit HLMemoryPointer;

    private boolean isHalted;


    public CPU(Bus bus, InterruptController interruptController) {
        this.bus = bus;
        this.interruptController = interruptController;

        // initial values after boot
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

            F.setZ(xxxRegister.getValue() == 0);
            F.setN(false);
            F.setH(orig, (byte) 0x1, Operator.ADD);

            return xxxRegister.getAccessCost()*2 + 1;
        } else if ((nextInstruction & 0b11_000_111) == 0b00_000_101) {
            // DEC r1
            byte xxx = (byte) ((nextInstruction & 0b00_111_000) >>> 3);
            InstructionTarget8Bit xxxRegister = getRegisterFor(xxx);

            byte orig = xxxRegister.getValue();
            xxxRegister.setValue((byte) (xxxRegister.getValue() - 1));

            F.setZ(xxxRegister.getValue() == 0);
            F.setN(true);
            F.setH(orig, (byte) 0x1, Operator.SUB);

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
                int memoryLocation = BC.getValue();
                bus.writeByteAt(memoryLocation, A.getValue());
            } else if (xx == 0b01) {
                // LD (DE), A
                int memoryLocation = DE.getValue();
                bus.writeByteAt(memoryLocation, A.getValue());
            } else if (xx == 0b10) {
                // LD (HL++), A
                int memoryLocation = HL.getValue();
                bus.writeByteAt(memoryLocation, A.getValue());
                HL.setValue(HL.getValue() + 1);
            } else if (xx == 0b11) {
                // LD (HL--), A
                int memoryLocation = HL.getValue();
                bus.writeByteAt(memoryLocation, A.getValue());
                HL.setValue(HL.getValue() - 1);
            }
            return 2;
        } else if ((nextInstruction & 0b11_00_1111) == 0b00_00_1010) {
            // LD A, (rr)
            byte xx = (byte) ((nextInstruction & 0b00_110_000) >>> 4);
            if (xx == 0b00) {
                // LD A, (BC)
                int memoryLocation = BC.getValue();
                A.setValue(bus.readByteAt(memoryLocation));
            } else if (xx == 0b01) {
                // LD A, (DE)
                int memoryLocation = DE.getValue();
                A.setValue(bus.readByteAt(memoryLocation));
            } else if (xx == 0b10) {
                // LD A, (HL++)
                int memoryLocation = HL.getValue();
                A.setValue(bus.readByteAt(memoryLocation));
                HL.setValue(HL.getValue() + 1);
            } else if (xx == 0b11) {
                // LD A, (HL--)
                int memoryLocation = HL.getValue();
                A.setValue(bus.readByteAt(memoryLocation));
                HL.setValue(HL.getValue() - 1);
            }
            return 2;
        } else if ((nextInstruction & 0b11_00_1111) == 0b11_00_0001) {
            // POP rr
            byte xx = (byte) ((nextInstruction & 0b00_11_0000) >>> 4);

            // POP AF|BC|DE|HL
            DoubleRegister xxDoubleRegister = getDoublePushPopRegisterFor(xx);
            Register lowerRegister = xxDoubleRegister.getLowerRegister();
            Register higherRegister = xxDoubleRegister.getHigherRegister();

            lowerRegister.setValue(bus.readByteAt(SP.getValue()));
            SP.setValue(SP.getValue() + 1);
            higherRegister.setValue(bus.readByteAt(SP.getValue()));
            SP.setValue(SP.getValue() + 1);

            return 3;
        } else if ((nextInstruction & 0b11_00_1111) == 0b11_00_0101) {
            // PUSH rr
            byte xx = (byte) ((nextInstruction & 0b00_11_0000) >>> 4);

            // PUSH AF|BC|DE|HL
            DoubleRegister xxDoubleRegister = getDoublePushPopRegisterFor(xx);
            Register lowerRegister = xxDoubleRegister.getLowerRegister();
            Register higherRegister = xxDoubleRegister.getHigherRegister();

            SP.setValue(SP.getValue() - 1);
            bus.writeByteAt(SP.getValue(), higherRegister.getValue());
            SP.setValue(SP.getValue() - 1);
            bus.writeByteAt(SP.getValue(), lowerRegister.getValue());

            return 4;
        } else if ((nextInstruction & 0b11111_000) == 0b10000_000) {
            // ADD A, r
            byte xxx = (byte) (nextInstruction & 0b00000_111);
            InstructionTarget8Bit xxxRegister = getRegisterFor(xxx);

            byte orig = A.getValue();
            byte toAdd = xxxRegister.getValue();

            A.setValue((byte) ((orig & 0xFF) + (toAdd & 0xFF)));

            F.setZ(A.getValue() == 0);
            F.setN(false);
            F.setH(orig, toAdd, Operator.ADD);
            F.setC(orig, toAdd, Operator.ADD);

            return 1 + xxxRegister.getAccessCost();
        } else if ((nextInstruction & 0b11111_000) == 0b10010_000) {
            // SUB A, r
            byte xxx = (byte) (nextInstruction & 0b00000_111);
            InstructionTarget8Bit xxxRegister = getRegisterFor(xxx);

            byte orig = A.getValue();
            byte toSub = xxxRegister.getValue();

            A.setValue((byte) ((orig & 0xFF) - (toSub & 0xFF)));

            F.setZ(A.getValue() == 0);
            F.setN(true);
            F.setH(orig, toSub, Operator.SUB);
            F.setC(orig, toSub, Operator.SUB);

            return 1 + xxxRegister.getAccessCost();
        } else if ((nextInstruction & 0b11111_000) == 0b10100_000) {
            // AND A, r
            byte xxx = (byte) (nextInstruction & 0b00000_111);
            InstructionTarget8Bit xxxRegister = getRegisterFor(xxx);

            byte orig = A.getValue();
            byte toAnd = xxxRegister.getValue();

            A.setValue((byte) (orig & toAnd));

            F.setZ(A.getValue() == 0);
            F.setN(false);
            F.setH(true);
            F.setC(false);

            return 1 + xxxRegister.getAccessCost();
        } else if ((nextInstruction & 0b11111_000) == 0b10110_000) {
            // OR A, r
            byte xxx = (byte) (nextInstruction & 0b00000_111);
            InstructionTarget8Bit xxxRegister = getRegisterFor(xxx);

            byte orig = A.getValue();
            byte toOr = xxxRegister.getValue();

            A.setValue((byte) (orig | toOr));

            F.setZ(A.getValue() == 0);
            F.setN(false);
            F.setH(false);
            F.setC(false);

            return 1 + xxxRegister.getAccessCost();
        } else if ((nextInstruction & 0b11111_000) == 0b10001_000) {
            // ADC A, r
            byte xxx = (byte) (nextInstruction & 0b00000_111);
            InstructionTarget8Bit xxxRegister = getRegisterFor(xxx);

            byte orig = A.getValue();
            byte toAdd = xxxRegister.getValue();
            int carry = F.getC() ? 1 : 0;

            A.setValue((byte) ((orig & 0xFF) + (toAdd & 0xFF) + carry));

            F.setZ(A.getValue() == 0);
            F.setN(false);
            F.setH(orig, toAdd, carry, Operator.ADD);
            F.setC(orig, toAdd, carry, Operator.ADD);

            return 1 + xxxRegister.getAccessCost();
        } else if ((nextInstruction & 0b11111_000) == 0b10011_000) {
            // SBC A, r
            byte xxx = (byte) (nextInstruction & 0b00000_111);
            InstructionTarget8Bit xxxRegister = getRegisterFor(xxx);

            byte orig = A.getValue();
            byte toSub = xxxRegister.getValue();
            int carry = F.getC() ? 1 : 0;

            A.setValue((byte) ((orig & 0xFF) - (toSub & 0xFF) - carry));

            F.setZ(A.getValue() == 0);
            F.setN(true);
            F.setH(orig, toSub, carry, Operator.SUB);
            F.setC(orig, toSub, carry, Operator.SUB);

            return 1 + xxxRegister.getAccessCost();
        } else if ((nextInstruction & 0b11111_000) == 0b10101_000) {
            // XOR A, r
            byte xxx = (byte) (nextInstruction & 0b00000_111);
            InstructionTarget8Bit xxxRegister = getRegisterFor(xxx);

            byte orig = A.getValue();
            byte toXOR = xxxRegister.getValue();

            A.setValue((byte) (orig ^ toXOR));

            F.setZ(A.getValue() == 0);
            F.setN(false);
            F.setH(false);
            F.setC(false);

            return 1 + xxxRegister.getAccessCost();
        } else if ((nextInstruction & 0b11111_000) == 0b10111_000) {
            // CP A, r
            byte xxx = (byte) (nextInstruction & 0b00000_111);
            InstructionTarget8Bit xxxRegister = getRegisterFor(xxx);

            byte orig = A.getValue();
            byte toSub = xxxRegister.getValue();

            byte value = (byte) ((orig & 0xFF) - (toSub & 0xFF));

            F.setZ(value == 0);
            F.setN(true);
            F.setH(orig, toSub,Operator.SUB);
            F.setC(orig, toSub,Operator.SUB);

            return 1 + xxxRegister.getAccessCost();
        } else if ((nextInstruction & 0b11_00_1111) == 0b00_00_1001) {
            // ADD HL, rr
            byte xx = (byte) ((nextInstruction & 0b00_11_0000) >>> 4);
            DoubleRegister xxDblRegisters = getDoubleRegisterFor(xx);
            int orig = HL.getValue();
            int toAdd = xxDblRegisters.getValue();
            HL.setValue(orig + toAdd);

            F.setN(false);
            F.setH(((orig & 0x0FFF) + (toAdd & 0x0FFF)) & 0b0001_0000_0000_0000);
            F.setC(((orig & 0xFFFF) + (toAdd & 0xFFFF)) > 0xFFFF);

            return 2;
        } else if ((nextInstruction & 0b111_00_111) == 0b110_00_000) {
            // RET cond
            byte cc = (byte) ((nextInstruction & 0b000_11_000) >>> 3);
            boolean flag = getFlagForCond(cc);
            if (flag) {
                byte lowerAddressBits = bus.readByteAt(SP.getValue());
                SP.setValue(SP.getValue() + 1);
                byte higherAddressBits = bus.readByteAt(SP.getValue());
                SP.setValue(SP.getValue() + 1);
                PC.setValue(higherAddressBits, lowerAddressBits);
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
                PC.setValue(higherAddressBits, lowerAddressBits);
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
                byte lowerPC = PC.getLowerRegister().getValue();
                byte higherPC = PC.getHigherRegister().getValue();
                SP.setValue(SP.getValue() - 1);
                bus.writeByteAt(SP.getValue(), higherPC);
                SP.setValue(SP.getValue() - 1);
                bus.writeByteAt(SP.getValue(), lowerPC);
                PC.setValue(targetAddr);
                return 6;
            }
            return 3;
        } else if ((nextInstruction & 0b11_000_111) == 0b11_000_111) {
            // RST i
            byte iii = (byte) (nextInstruction & 0b00_111_000);
            // push current pc onto the stack
            int currentPC = PC.getValue();
            byte lowerPC = (byte) (currentPC & 0xFF);
            byte higherPC = (byte) ((currentPC >> 8) & 0xFF);
            SP.setValue(SP.getValue() - 1);
            bus.writeByteAt(SP.getValue(), higherPC);
            SP.setValue(SP.getValue() - 1);
            bus.writeByteAt(SP.getValue(), lowerPC);

            // set pc to page i of memory
            PC.setValue(iii);

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
                F.setZ(xxxRegister.getValue() == 0);
                F.setN(false);
                F.setH(false);
                F.setC(msb == 0x80);

                return 2 + xxxRegister.getAccessCost();
            } else if ((nextInstruction & 0b11111_000) == 0b00001_000) {
                // RRC r
                byte xxx = (byte) (nextInstruction & 0b00000_111);
                InstructionTarget8Bit xxxRegister = getRegisterFor(xxx);
                byte orig = xxxRegister.getValue();
                int lsb = orig & 0x01;
                xxxRegister.setValue((byte) ((orig & 0xFF) >>> 1));
                xxxRegister.setValue((byte) (xxxRegister.getValue() | (lsb << 7)));
                F.setZ(xxxRegister.getValue() == 0);
                F.setN(false);
                F.setH(false);
                F.setC(lsb == 0x01);

                return 2 + xxxRegister.getAccessCost();
            } else if ((nextInstruction & 0b11111_000) == 0b00010_000) {
                // RL r
                byte xxx = (byte) (nextInstruction & 0b00000_111);
                InstructionTarget8Bit xxxRegister = getRegisterFor(xxx);
                byte orig = xxxRegister.getValue();
                int carry = F.getC() ? 1 : 0;
                xxxRegister.setValue((byte) ((orig & 0xFF) << 1));
                xxxRegister.setValue((byte) (xxxRegister.getValue() | carry));
                F.setZ(xxxRegister.getValue() == 0);
                F.setN(false);
                F.setH(false);
                F.setC(orig & 0x80);

                return 2 + xxxRegister.getAccessCost();
            } else if ((nextInstruction & 0b11111_000) == 0b00011_000) {
                // RR r
                byte xxx = (byte) (nextInstruction & 0b00000_111);
                InstructionTarget8Bit xxxRegister = getRegisterFor(xxx);
                byte msb = (byte) ((F.getC() ? 1 : 0) << 7);
                byte orig = xxxRegister.getValue();
                xxxRegister.setValue((byte) ((xxxRegister.getValue() & 0xFF) >>> 1));
                xxxRegister.setValue((byte) (xxxRegister.getValue() | msb));
                F.setZ(xxxRegister.getValue() == 0);
                F.setN(false);
                F.setH(false);
                F.setC(orig & 0x01);

                return 2 + xxxRegister.getAccessCost();
            } else if ((nextInstruction & 0b11111_000) == 0b00100_000) {
                // SLA r
                byte xxx = (byte) (nextInstruction & 0b00000_111);
                InstructionTarget8Bit xxxRegister = getRegisterFor(xxx);
                byte orig = xxxRegister.getValue();
                xxxRegister.setValue((byte) ((orig & 0xFF) << 1));
                F.setZ(xxxRegister.getValue() == 0);
                F.setN(false);
                F.setH(false);
                F.setC(orig & 0x80);

                return 2 + xxxRegister.getAccessCost();
            } else if ((nextInstruction & 0b11111_000) == 0b00101_000) {
                // SRA r
                byte xxx = (byte) (nextInstruction & 0b00000_111);
                InstructionTarget8Bit xxxRegister = getRegisterFor(xxx);
                byte orig = xxxRegister.getValue();
                int msb = orig & 0x80;
                xxxRegister.setValue((byte) ((orig & 0xFF) >>> 1));
                xxxRegister.setValue((byte) (xxxRegister.getValue() | msb));
                F.setZ(xxxRegister.getValue() == 0);
                F.setN(false);
                F.setH(false);
                F.setC(orig & 0x01);

                return 2 + xxxRegister.getAccessCost();
            } else if ((nextInstruction & 0b11111_000) == 0b00110_000) {
                // SWAP r
                byte xxx = (byte) (nextInstruction & 0b00000_111);
                InstructionTarget8Bit xxxRegister = getRegisterFor(xxx);
                byte lower4  = (byte) ((xxxRegister.getValue() <<  4) & 0xF0);
                byte higher4 = (byte) ((xxxRegister.getValue() >>> 4) & 0x0F);
                xxxRegister.setValue((byte) (lower4 | higher4));

                F.setZ(xxxRegister.getValue() == 0);
                F.setN(false);
                F.setH(false);
                F.setC(false);

                return 2 + xxxRegister.getAccessCost();
            } else if ((nextInstruction & 0b11111_000) == 0b00111_000) {
                // SRL r
                byte xxx = (byte) (nextInstruction & 0b00000_111);
                InstructionTarget8Bit xxxRegister = getRegisterFor(xxx);
                byte orig = xxxRegister.getValue();
                xxxRegister.setValue((byte) ((orig & 0xFF) >>> 1));
                F.setZ(xxxRegister.getValue() == 0);
                F.setN(false);
                F.setH(false);
                F.setC(orig & 0x01);

                return 2 + xxxRegister.getAccessCost();
            } else if ((nextInstruction & 0b11_000_000) == 0b01_000_000) {
                // BIT i, r
                byte iii = (byte) ((nextInstruction & 0b00_111_000) >>> 3);
                byte xxx = (byte) (nextInstruction & 0b00000_111);
                InstructionTarget8Bit xxxRegister = getRegisterFor(xxx);

                int compliment = ~xxxRegister.getValue();
                int masked = (compliment & (0x01 << iii));
                F.setZ(masked);
                F.setN(false);
                F.setH(true);

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
            if (!F.getZ()) {
                PC.setValue(PC.getValue() + addr);
                return 3;
            }
            return 2;
        } else if (nextInstruction == (byte) 0x28) {
            // JR Z, n
            byte addr = readNextByte();
            if (F.getZ()) {
                PC.setValue(PC.getValue() + addr);
                return 3;
            }
            return 2;
        } else if (nextInstruction == (byte) 0x30) {
            // JR NC, n
            byte addr = readNextByte();
            if (!F.getC()) {
                PC.setValue(PC.getValue() + addr);
                return 3;
            }
            return 2;
        } else if (nextInstruction == (byte) 0x38) {
            // JR C, n
            byte addr = readNextByte();
            if (F.getC()) {
                PC.setValue(PC.getValue() + addr);
                return 3;
            }
            return 2;
        } else if (nextInstruction == (byte) 0x18) {
            // JR n
            byte addrOffset = readNextByte();
            PC.setValue(PC.getValue() + addrOffset);

            return 3;
        } else if (nextInstruction == (byte) 0xC3) {
            // JP nn
            byte lowerBits = readNextByte();
            byte higherBits = readNextByte();
            int targetPc = (higherBits & 0xFF) << 8 | (lowerBits & 0xFF);

            PC.setValue(targetPc);

            return 4;
        } else if (nextInstruction == (byte) 0xE9) {
            // JP HL
            PC.setValue(HL.getValue());

            return 1;
        } else if (nextInstruction == (byte) 0xE0) {
            // LD (n), A
            byte higher = (byte) 0xFF;
            byte lower = readNextByte();
            int targetAddr = (higher & 0xFF) << 8 | (lower & 0xFF);

            bus.writeByteAt(targetAddr, A.getValue());

            return 3;
        } else if (nextInstruction == (byte) 0xF0) {
            // LD A, (n)
            byte higher = (byte) 0xFF;
            byte lower = readNextByte();
            int targetAddr = (higher & 0xFF) << 8 | (lower & 0xFF);

            A.setValue(bus.readByteAt(targetAddr));

            return 3;
        } else if (nextInstruction == (byte) 0xFA) {
            // LD A, (nn)
            byte lowerBits = readNextByte();
            byte higherBits = readNextByte();
            int targetAddr = (higherBits & 0xFF) << 8 | (lowerBits & 0xFF);
            A.setValue(bus.readByteAt(targetAddr));

            return 4;
        } else if (nextInstruction == (byte) 0xEA) {
            // LD (nn), A
            byte lowerBits = readNextByte();
            byte higherBits = readNextByte();
            int targetAddr = (higherBits & 0xFF) << 8 | (lowerBits & 0xFF);
            bus.writeByteAt(targetAddr, A.getValue());

            return 4;
        } else if (nextInstruction == (byte) 0xF2) {
            // LD A, (C)
            byte higher = (byte) 0xFF;
            byte lower = C.getValue();
            int targetAddr = (higher & 0xFF) << 8 | (lower & 0xFF);

            A.setValue(bus.readByteAt(targetAddr));

            return 2;
        } else if (nextInstruction == (byte) 0xE2) {
            // LD (C), A
            byte higher = (byte) 0xFF;
            byte lower = C.getValue();
            int targetAddr = (higher & 0xFF) << 8 | (lower & 0xFF);

            bus.writeByteAt(targetAddr, A.getValue());

            return 2;
        } else if (nextInstruction == (byte) 0x08) {
            // LD (nn), SP
            Register lowerRegister = SP.getLowerRegister();
            Register higherRegister = SP.getHigherRegister();

            byte lowerBits = readNextByte();
            byte higherBits = readNextByte();
            int targetAddr = (higherBits & 0xFF) << 8 | (lowerBits & 0xFF);

            bus.writeByteAt(targetAddr, lowerRegister.getValue());
            bus.writeByteAt(targetAddr+1, higherRegister.getValue());

            return 5;
        } else if (nextInstruction == (byte) 0xF9) {
            // LD SP, HL
            SP.setValue(HL.getValue());

            return 2;
        } else if (nextInstruction == (byte) 0xE8) {
            // ADD SP, s8
            byte value = readNextByte();
            int orig = SP.getValue();
            SP.setValue(value + orig);

            F.setZ(false);
            F.setN(false);

            // https://stackoverflow.com/questions/57958631/game-boy-half-carry-flag-and-16-bit-instructions-especially-opcode-0xe8
            F.setH((byte) orig, value, Operator.ADD); // ((orig & 0xF) + (value & 0xF)) & 0x10);
            F.setC((byte) orig, value, Operator.ADD); // c = (((orig & 0xFF) + (value & 0xFF)) & 0x100) == 0x100;

            return 4;
        } else if (nextInstruction == (byte) 0xF8) {
            // LD HL, SP+s8
            byte value = readNextByte();
            int orig = SP.getValue();
            HL.setValue(value + orig);

            F.setZ(false);
            F.setN(false);
            F.setH(((orig & 0xF) + (value & 0xF)) & 0x10);
            F.setC(((orig & 0xFF) + (value & 0xFF)) & 0x100);

            return 3;
        } else if (nextInstruction == (byte) 0xCD) {
            // CALL nn
            byte lower = readNextByte();
            byte higher = readNextByte();
            int targetAddr = (higher & 0xFF) << 8 | (lower & 0xFF);

            byte lowerPC = PC.getLowerRegister().getValue();
            byte higherPC = PC.getHigherRegister().getValue();
            SP.setValue(SP.getValue() - 1);
            bus.writeByteAt(SP.getValue(), higherPC);
            SP.setValue(SP.getValue() - 1);
            bus.writeByteAt(SP.getValue(), lowerPC);
            PC.setValue(targetAddr);

            return 6;
        } else if (nextInstruction == (byte) 0xC9) {
            // RET
            byte lowerAddressBits = bus.readByteAt(SP.getValue());
            SP.setValue(SP.getValue() + 1);
            byte higherAddressBits = bus.readByteAt(SP.getValue());
            SP.setValue(SP.getValue() + 1);
            PC.setValue(higherAddressBits, lowerAddressBits);

            return 4;
        } else if (nextInstruction == (byte) 0xD9) {
            // RETI
            byte lowerAddressBits = bus.readByteAt(SP.getValue());
            SP.setValue(SP.getValue() + 1);
            byte higherAddressBits = bus.readByteAt(SP.getValue());
            SP.setValue(SP.getValue() + 1);
            PC.setValue(higherAddressBits, lowerAddressBits);

            interruptController.setInterruptMasterEnable(true);

            return 4;
        } else if (nextInstruction == (byte) 0xC6) {
            // ADD A, n
            byte value = readNextByte();
            byte orig = A.getValue();
            A.setValue((byte) ((orig & 0xFF) + (value & 0xFF)));

            F.setZ(A.getValue() == 0);
            F.setN(false);
            F.setH(orig, value, Operator.ADD);
            F.setC(orig, value, Operator.ADD);

            return 2;
        } else if (nextInstruction == (byte) 0xD6) {
            // SUB A, n
            byte value = readNextByte();
            byte orig = A.getValue();
            A.setValue((byte) ((orig & 0xFF) - (value & 0xFF)));

            F.setZ(A.getValue() == 0);
            F.setN(true);
            F.setH(orig, value, Operator.SUB);
            F.setC(orig, value, Operator.SUB);

            return 2;
        } else if (nextInstruction == (byte) 0xE6) {
            // AND n
            byte value = readNextByte();
            A.setValue((byte) (A.getValue() & value));
            F.setZ(A.getValue() == 0);
            F.setN(false);
            F.setH(true);
            F.setC(false);

            return 2;
        } else if (nextInstruction == (byte) 0xF6) {
            // OR n
            byte value = readNextByte();
            A.setValue((byte) (A.getValue() | value));
            F.setZ(A.getValue() == 0);
            F.setN(false);
            F.setH(false);
            F.setC(false);

            return 2;
        } else if (nextInstruction == (byte) 0xCE) {
            // ADC A, n
            byte value = readNextByte();
            byte orig = A.getValue();
            int carry = F.getC() ? 1:0;
            int sum = (orig & 0xFF) + (value & 0xFF) + carry;
            A.setValue((byte) sum);
            F.setZ(A.getValue() == 0);
            F.setN(false);
            F.setH(orig, value, carry, Operator.ADD);
            F.setC(orig, value, carry, Operator.ADD);

            return 2;
        } else if (nextInstruction == (byte) 0xDE) {
            // SBC A, n
            byte value = readNextByte();
            byte orig = A.getValue();
            int carry = F.getC() ? 1 : 0;

            A.setValue((byte) ((orig & 0xFF) - (value & 0xFF) - carry));
            F.setZ(A.getValue() == 0);
            F.setN(true);
            F.setH(orig, value, carry, Operator.SUB);
            F.setC(orig, value, carry, Operator.SUB);

            return 2;
        } else if (nextInstruction == (byte) 0xEE) {
            // XOR n
            byte value = readNextByte();
            A.setValue((byte) (A.getValue() ^ value));
            F.setZ(A.getValue() == 0);
            F.setN(false);
            F.setH(false);
            F.setC(false);

            return 2;
        } else if (nextInstruction == (byte) 0xFE) {
            // CP n
            byte value = readNextByte();
            byte orig = A.getValue();
            byte result = (byte) ((orig & 0xFF) - (value & 0xFF));

            F.setZ(result == 0);
            F.setN(true);
            F.setH(orig, value, Operator.SUB);
            F.setC(orig, value, Operator.SUB);

            return 2;
        } else if (nextInstruction == (byte) 0x2F) {
            // CPL
            A.setValue((byte) (~A.getValue()));

            F.setN(true);
            F.setH(true);

            return 1;

        } else if (nextInstruction == (byte) 0x3F) {
            // CCF
            F.setN(false);
            F.setH(false);
            F.setC(!F.getC());
            
            return 1;
        } else if (nextInstruction == (byte) 0x37) {
            // SCF
            F.setN(false);
            F.setH(false);
            F.setC(true);

            return 1;
        } else if (nextInstruction == (byte) 0x1F) {
            // RRA
            byte msb = (byte) ((F.getC() ? 1 : 0) << 7);
            byte orig = A.getValue();
            A.setValue((byte) ((A.getValue() & 0xFF) >>> 1));
            A.setValue((byte) (A.getValue() | msb));
            F.setZ(false);
            F.setN(false);
            F.setH(false);
            F.setC(orig & 0x01);

            return 1;
        } else if (nextInstruction == (byte) 0x17) {
            // RLA
            byte orig = A.getValue();
            byte msb = (byte) ((orig & 0x80) >>> 7);
            byte carry = (byte) (F.getC() ? 1:0);
            A.setValue((byte) ((A.getValue() & 0xFF) << 1));
            A.setValue((byte) (A.getValue() | carry));
            F.setZ(false);
            F.setN(false);
            F.setH(false);
            F.setC(msb == 0x01);

            return 1;
        } else if (nextInstruction == (byte) 0x0F) {
            // RRCA
            byte orig = A.getValue();
            byte lsb = (byte) (orig & 0x01);
            A.setValue((byte) ((A.getValue() & 0xFF) >>> 1));
            A.setValue((byte) (A.getValue() | lsb << 7));
            F.setZ(false);
            F.setN(false);
            F.setH(false);
            F.setC(lsb == 0x01);

            return 1;
        } else if (nextInstruction == (byte) 0x07) {
            // RLCA
            byte orig = A.getValue();
            byte msb = (byte) ((orig & 0x80) >>> 7);
            A.setValue((byte) ((A.getValue() & 0xFF) << 1));
            A.setValue((byte) (A.getValue() | msb));
            F.setZ(false);
            F.setN(false);
            F.setH(false);
            F.setC(msb == 0x01);

            return 1;
        } else if (nextInstruction == (byte) 0x27) {
            // DAA
            byte value = A.getValue();
            byte correction = 0x00;
            if (F.getH() || (!F.getN() && (value & 0x0F) > 9)) {
                correction = 0x06;
            }

            if (F.getC() || (!F.getN() && (value & 0xFF) > 0x99)) {
                correction |= 0x60;
                F.setC(true);
            }

            value = (byte) ((value & 0xFF) + (F.getN() ? -correction : correction));
            A.setValue(value);
            F.setZ(value == 0);
            F.setH(false);

            return 1;
        } else {
            System.out.println("Received invalid instruction: " + String.format("0x%02X", nextInstruction));
            throw new IndexOutOfBoundsException("Received invalid instruction: " + String.format("0x%02X", nextInstruction));
        }
    }

    private boolean getFlagForCond(byte cc) {
        if (cc == 0b00) {
            // NZ
            return !F.getZ();
        } else if (cc == 0b01) {
            // Z
            return F.getZ();
        } else if (cc == 0b10) {
            // NC
            return !F.getC();
        } else if (cc == 0b11) {
            // C
            return F.getC();
        } else {
            throw new IllegalArgumentException("Invalid input for getFlagForCond(): " + cc);
        }
    }

    private DoubleRegister getDoublePushPopRegisterFor(byte xx) {
        if (xx == 0b00) {
            return BC;
        } else if (xx == 0b01) {
            return DE;
        } else if (xx == 0b10) {
            return HL;
        } else if (xx == 0b11) {
            return AF;
        } else {
            throw new IllegalArgumentException("Invalid input for getDoublePushPopRegisterFor(): " + xx);
        }
    }

    private DoubleRegister getDoubleRegisterFor(byte xx) {
        if (xx == 0b00) {
            return BC;
        } else if (xx == 0b01) {
            return DE;
        } else if (xx == 0b10) {
            return HL;
        } else if (xx == 0b11) {
            return SP;
        } else {
            throw new IllegalArgumentException("Invalid input for getDoubleRegisterFor(): " + xx);
        }
    }

    private InstructionTarget8Bit getRegisterFor(byte xxx) {
        if (xxx == 0b000) {
            return B;
        } else if (xxx == 0b001) {
            return C;
        } else if (xxx == 0b010) {
            return D;
        } else if (xxx == 0b011) {
            return E;
        } else if (xxx == 0b100) {
            return H;
        } else if (xxx == 0b101) {
            return L;
        } else if (xxx == 0b110) {
            return HLMemoryPointer;
        } else if (xxx == 0b111) {
            return A;
        } else {
            throw new IllegalArgumentException("Invalid input for getRegisterFor(): " + xxx);
        }
    }

    private byte readNextByte() {
        byte nextByte = bus.readByteAt(PC.getValue());
        PC.setValue(PC.getValue() + 1);
        return nextByte;
    }

    public int checkInterrupts() {
        if (interruptController.interruptReady()) {
            int jumpAddress = interruptController.getHighestPriorityInterruptAddress();
//            System.out.printf("Servicing interrupt: 0x%02X\n", jumpAddress);

            //  Reset the IME flag and prevent all interrupts.
            interruptController.setInterruptMasterEnable(false);

            //  The PC (program counter) is pushed onto the stack.
            byte lowerPC = PC.getLowerRegister().getValue();
            byte higherPC = PC.getHigherRegister().getValue();
            SP.setValue(SP.getValue() - 1);
            bus.writeByteAt(SP.getValue(), higherPC);
            SP.setValue(SP.getValue() - 1);
            bus.writeByteAt(SP.getValue(), lowerPC);

            //  Jump to the starting address of the interrupt.
            PC.setValue(jumpAddress);

            isHalted = false;

            return 5;
        }
        return 0;
    }
}
