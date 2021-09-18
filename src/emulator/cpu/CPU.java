package emulator.cpu;

import emulator.Bus;

public class CPU {
    // Order of registers; B,C,D,E,H,L,(HL),A
    private Bus bus;
    private int pc;

    private Register A;
    private Register H;
    private Register L;
    private Register D;
    private Register E;
    private Register B;
    private Register C;
    private DoubleRegister HL;
    private DoubleRegister DE;
    private DoubleRegister BC;
    private DoubleRegister SP;

    private InstructionTarget8Bit HLMemoryPointer;

    private boolean z;
    private boolean n;
    private boolean h;
    private boolean c;


    public CPU(Bus bus) {
        this.bus = bus;

        // initial values after boot
        this.pc = 0x0100;
        this.A = new Register((byte) 0x01);
        this.writeRegisterF((byte) 0xb0);
        this.B = new Register((byte) 0x00);
        this.C = new Register((byte) 0x13);
        this.D = new Register((byte) 0x00);
        this.E = new Register((byte) 0xD8);
        this.H = new Register((byte) 0x01);
        this.L = new Register((byte) 0x4D);
        this.HL = new DoubleRegister(H, L);
        this.DE = new DoubleRegister(D, E);
        this.BC = new DoubleRegister(B, C);
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

    /**
     * Instructions required for 04-op r,imm.s:
     * ld r, n
     * call nn
     * ret
     * ld r, (nn)
     * ld r, (HL)
     * push r
     * ld (nn), a
     * ld bc, nn
     * ld de, nn
     * pop r
     * jp nn
     * inc r
     * ld r, r
     * jr cc, e
     *
     */

    public void executeNext() {
//        InstructionLogger.logInstruction(A, readRegisterF(), B, C, D, E, H, L, SP, pc, bus);
//        System.out.println("Executing at   " + String.format("0x%02X", pc));
        // fetch
        byte nextInstruction = readNextByte();
//        System.out.println("Instruction is " + String.format("0x%02X", nextInstruction));


        // decode
        if (nextInstruction == (byte) 0x00) {
            // NOP
        } else if (nextInstruction == (byte) 0x10) {
            // STOP (disable interrupts)
            readNextByte();
            // TODO
        } else if ((nextInstruction & 0b11_000_111) == 0b00_000_110) {
            // 0b00xxx110
            // LD r1, n --- LD (HL), n
            byte xxx = (byte) ((nextInstruction & 0b00_111_000) >>> 3);
            byte value = readNextByte();
            InstructionTarget8Bit xxxRegister = getRegisterFor(xxx);
            xxxRegister.setValue(value);

        } else if ((nextInstruction & 0b11_000_000) == 0b01_000_000) {
            // LD r1, r2
            byte xxx = (byte) ((nextInstruction & 0b00_111_000) >> 3);
            byte yyy = (byte) (nextInstruction & 0b00_000_111);

            InstructionTarget8Bit xxxRegister = getRegisterFor(xxx);
            InstructionTarget8Bit yyyRegister = getRegisterFor(yyy);

            if (xxxRegister == HLMemoryPointer && yyyRegister == HLMemoryPointer) {
                // HALT
                // TODO
            } else {
                // LD r1, r2
                xxxRegister.setValue(yyyRegister.getValue());
            }

        } else if ((nextInstruction & 0b11_000_111) == 0b00_000_100) {
            // INC r1
            byte xxx = (byte) ((nextInstruction & 0b00_111_000) >>> 3);
            InstructionTarget8Bit xxxRegister = getRegisterFor(xxx);

            byte orig = xxxRegister.getValue();
            xxxRegister.setValue((byte) (xxxRegister.getValue() + 1));

            z = xxxRegister.getValue() == 0;
            n = false;
            h = (((orig & 0xf) + 0x1) & 0x10) == 0x10;
        } else if ((nextInstruction & 0b11_000_111) == 0b00_000_101) {
            // DEC r1
            byte xxx = (byte) ((nextInstruction & 0b00_111_000) >>> 3);
            InstructionTarget8Bit xxxRegister = getRegisterFor(xxx);

            byte orig = xxxRegister.getValue();
            xxxRegister.setValue((byte) (xxxRegister.getValue() - 1));

            z = xxxRegister.getValue() == 0;
            n = true;
            h = (((orig & 0xf) - 0x1) & 0x10) == 0x10;
        } else if ((nextInstruction & 0b11_00_1111) == 0b00_00_0001) {
            // LD rr, nn
            byte xx = (byte) ((nextInstruction & 0b00_110_000) >>> 4);
            DoubleRegister xxDblRegisters = getDoubleRegisterFor(xx);
            byte lowerBits = readNextByte();
            byte higherBits = readNextByte();

            xxDblRegisters.setValue(higherBits, lowerBits);

        } else if ((nextInstruction & 0b11_00_1111) == 0b00_00_0011) {
            // INC rr
            byte xx = (byte) ((nextInstruction & 0b00_110_000) >>> 4);
            DoubleRegister xxDblRegisters = getDoubleRegisterFor(xx);
            xxDblRegisters.setValue(xxDblRegisters.getValue() + 1);

        } else if ((nextInstruction & 0b11_00_1111) == 0b00_00_1011) {
            // DEC rr
            byte xx = (byte) ((nextInstruction & 0b00_110_000) >>> 4);
            DoubleRegister xxDblRegisters = getDoubleRegisterFor(xx);
            xxDblRegisters.setValue(xxDblRegisters.getValue() - 1);

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
        } else if ((nextInstruction & 0b11_00_1111) == 0b11_00_0001) {
            // POP rr
            byte xx = (byte) ((nextInstruction & 0b00_11_0000) >>> 4);
            if (xx == 0b11) {
                // POP AF
                InstructionTarget8Bit higherRegister = A;

                writeRegisterF(bus.readByteAt(SP.getValue()));
                SP.setValue(SP.getValue() + 1);
                higherRegister.setValue(bus.readByteAt(SP.getValue()));
                SP.setValue(SP.getValue() + 1);
            } else {
                // POP BC|DE|HL
                DoubleRegister xxDoubleRegister = getDoubleRegisterFor(xx);
                Register lowerRegister = xxDoubleRegister.getLowerRegister();
                Register higherRegister = xxDoubleRegister.getHigherRegister();

                lowerRegister.setValue(bus.readByteAt(SP.getValue()));
                SP.setValue(SP.getValue() + 1);
                higherRegister.setValue(bus.readByteAt(SP.getValue()));
                SP.setValue(SP.getValue() + 1);
            }
        } else if ((nextInstruction & 0b11_00_1111) == 0b11_00_0101) {
            // PUSH rr
            byte xx = (byte) ((nextInstruction & 0b00_11_0000) >>> 4);
            if (xx == 0b11) {
                // PUSH AF
                InstructionTarget8Bit higherRegister = A;

                SP.setValue(SP.getValue() - 1);
                bus.writeByteAt(SP.getValue(), higherRegister.getValue());
                SP.setValue(SP.getValue() - 1);
                bus.writeByteAt(SP.getValue(), readRegisterF());
            } else {
                // PUSH BC|DE|HL
                DoubleRegister xxDoubleRegister = getDoubleRegisterFor(xx);
                Register lowerRegister = xxDoubleRegister.getLowerRegister();
                Register higherRegister = xxDoubleRegister.getHigherRegister();

                SP.setValue(SP.getValue() - 1);
                bus.writeByteAt(SP.getValue(), higherRegister.getValue());
                SP.setValue(SP.getValue() - 1);
                bus.writeByteAt(SP.getValue(), lowerRegister.getValue());
            }
        } else if ((nextInstruction & 0b11111_000) == 0b10000_000) {
            // ADD A, r
            byte xxx = (byte) (nextInstruction & 0b00000_111);
            InstructionTarget8Bit xxxRegister = getRegisterFor(xxx);

            byte orig = A.getValue();
            byte toAdd = xxxRegister.getValue();

            A.setValue((byte) ((orig & 0xFF) + (toAdd & 0xFF)));

            z = A.getValue() == 0;
            n = false;
            h = (((orig & 0xF) + (toAdd & 0xF)) & 0x10) == 0x10;
            c = (((orig & 0xFF) + (toAdd & 0xFF)) & 0x100) == 0x100;
        } else if ((nextInstruction & 0b11111_000) == 0b10010_000) {
            // SUB A, r
            byte xxx = (byte) (nextInstruction & 0b00000_111);
            InstructionTarget8Bit xxxRegister = getRegisterFor(xxx);

            byte orig = A.getValue();
            byte toSub = xxxRegister.getValue();

            A.setValue((byte) ((orig & 0xFF) - (toSub & 0xFF)));

            z = A.getValue() == 0;
            n = true;
            h = (((orig & 0xF) - (toSub & 0xF)) & 0x10) == 0x10;
            c = (toSub & 0xFF) > (orig & 0xFF);
        } else if ((nextInstruction & 0b11111_000) == 0b10100_000) {
            // AND A, r
            byte xxx = (byte) (nextInstruction & 0b00000_111);
            InstructionTarget8Bit xxxRegister = getRegisterFor(xxx);

            byte orig = A.getValue();
            byte toAnd = xxxRegister.getValue();

            A.setValue((byte) (orig & toAnd));

            z = A.getValue() == 0;
            n = false;
            h = true;
            c = false;
        } else if ((nextInstruction & 0b11111_000) == 0b10110_000) {
            // OR A, r
            byte xxx = (byte) (nextInstruction & 0b00000_111);
            InstructionTarget8Bit xxxRegister = getRegisterFor(xxx);

            byte orig = A.getValue();
            byte toOr = xxxRegister.getValue();

            A.setValue((byte) (orig | toOr));

            z = A.getValue() == 0;
            n = false;
            h = false;
            c = false;
        } else if ((nextInstruction & 0b11111_000) == 0b10001_000) {
            // ADC A, r
            byte xxx = (byte) (nextInstruction & 0b00000_111);
            InstructionTarget8Bit xxxRegister = getRegisterFor(xxx);

            byte orig = A.getValue();
            byte toAdd = xxxRegister.getValue();
            int carry = c ? 1 : 0;

            A.setValue((byte) ((orig & 0xFF) + (toAdd & 0xFF) + carry));

            z = A.getValue() == 0;
            n = false;
            h = (((orig & 0xF) + (toAdd & 0xF) + carry) & 0x10) == 0x10;
            c = ((orig & 0xFF) + (toAdd & 0xFF) + carry) > 0xFF;
        } else if ((nextInstruction & 0b11111_000) == 0b10011_000) {
            // SBC A, r
            byte xxx = (byte) (nextInstruction & 0b00000_111);
            InstructionTarget8Bit xxxRegister = getRegisterFor(xxx);

            byte orig = A.getValue();
            byte toSub = xxxRegister.getValue();
            int carry = c ? 1 : 0;

            A.setValue((byte) ((orig & 0xFF) - (toSub & 0xFF) - carry));

            z = A.getValue() == 0;
            n = true;
            h = (((orig & 0xF) - (toSub & 0xF) - carry) & 0x10) == 0x10;
            c = ((toSub & 0xFF) + carry) > (orig & 0xFF);
        } else if ((nextInstruction & 0b11111_000) == 0b10101_000) {
            // XOR A, r
            byte xxx = (byte) (nextInstruction & 0b00000_111);
            InstructionTarget8Bit xxxRegister = getRegisterFor(xxx);

            byte orig = A.getValue();
            byte toXOR = xxxRegister.getValue();

            A.setValue((byte) (orig ^ toXOR));

            z = A.getValue() == 0;
            n = false;
            h = false;
            c = false;
        } else if ((nextInstruction & 0b11111_000) == 0b10111_000) {
            // CP A, r
            byte xxx = (byte) (nextInstruction & 0b00000_111);
            InstructionTarget8Bit xxxRegister = getRegisterFor(xxx);

            byte orig = A.getValue();
            byte toSub = xxxRegister.getValue();

            byte value = (byte) ((orig & 0xFF) - (toSub & 0xFF));

            z = value == 0;
            n = true;
            h = (((orig & 0xF) - (toSub & 0xF)) & 0x10) == 0x10;
            c = (toSub & 0xFF) > (orig & 0xFF);
        } else if ((nextInstruction & 0b11_00_1111) == 0b00_00_1001) {
            // ADD HL, rr
            byte xx = (byte) ((nextInstruction & 0b00_11_0000) >>> 4);
            DoubleRegister xxDblRegisters = getDoubleRegisterFor(xx);
            int orig = HL.getValue();
            int toAdd = xxDblRegisters.getValue();
            HL.setValue(orig + toAdd);

            n = false;
            h = (((orig & 0x0FFF) + (toAdd & 0x0FFF)) & 0b0001_0000_0000_0000) == 0b0001_0000_0000_0000;
            c = ((orig & 0xFFFF) + (toAdd & 0xFFFF)) > 0xFFFF;
        } else if ((nextInstruction & 0b111_00_111) == 0b110_00_000) {
            // RET cond
            byte cc = (byte) ((nextInstruction & 0b000_11_000) >>> 3);
            boolean flag = getFlagForCond(cc);
            if (flag) {
                byte lower = bus.readByteAt(SP.getValue());
                SP.setValue(SP.getValue() + 1);
                byte higher = bus.readByteAt(SP.getValue());
                SP.setValue(SP.getValue() + 1);
                pc = (higher & 0xFF) << 8 | (lower & 0xFF);
            }
        } else if ((nextInstruction & 0b111_00_111) == 0b110_00_010) {
            // JP cond, nn
            byte cc = (byte) ((nextInstruction & 0b000_11_000) >>> 3);
            boolean flag = getFlagForCond(cc);

            byte lowerBits = readNextByte();
            byte higherBits = readNextByte();
            int targetPc = (higherBits & 0xFF) << 8 | (lowerBits & 0xFF);

            if (flag) {
                pc = targetPc;
            }
        } else if ((nextInstruction & 0b111_00_111) == 0b110_00_100) {
            // CALL cond, nn
            byte cc = (byte) ((nextInstruction & 0b000_11_000) >>> 3);
            boolean flag = getFlagForCond(cc);

            byte lower = readNextByte();
            byte higher = readNextByte();
            int targetAddr = (higher & 0xFF) << 8 | (lower & 0xFF);
            if (flag) {
                int currentPC = pc;
                byte lowerPC = (byte) (currentPC & 0xFF);
                byte higherPC = (byte) ((currentPC >> 8) & 0xFF);
                SP.setValue(SP.getValue() - 1);
                bus.writeByteAt(SP.getValue(), higherPC);
                SP.setValue(SP.getValue() - 1);
                bus.writeByteAt(SP.getValue(), lowerPC);
                pc = targetAddr;
            }
        } else if ((nextInstruction & 0b11_000_111) == 0b11_000_111) {
            // RST i
            byte iii = (byte) (nextInstruction & 0b00_111_000);
            // push current pc onto the stack
            int currentPC = pc;
            byte lowerPC = (byte) (currentPC & 0xFF);
            byte higherPC = (byte) ((currentPC >> 8) & 0xFF);
            SP.setValue(SP.getValue() - 1);
            bus.writeByteAt(SP.getValue(), higherPC);
            SP.setValue(SP.getValue() - 1);
            bus.writeByteAt(SP.getValue(), lowerPC);

            // set pc to page i of memory
            pc = iii;


        } else if (nextInstruction == (byte) 0xCB) {
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
                z = xxxRegister.getValue() == 0;
                n = false;
                h = false;
                c = msb == 0x80;
            } else if ((nextInstruction & 0b11111_000) == 0b00001_000) {
                // RRC r
                byte xxx = (byte) (nextInstruction & 0b00000_111);
                InstructionTarget8Bit xxxRegister = getRegisterFor(xxx);
                byte orig = xxxRegister.getValue();
                int lsb = orig & 0x01;
                xxxRegister.setValue((byte) ((orig & 0xFF) >>> 1));
                xxxRegister.setValue((byte) (xxxRegister.getValue() | (lsb << 7)));
                z = xxxRegister.getValue() == 0;
                n = false;
                h = false;
                c = lsb == 0x01;
            } else if ((nextInstruction & 0b11111_000) == 0b00010_000) {
                // RL r
                byte xxx = (byte) (nextInstruction & 0b00000_111);
                InstructionTarget8Bit xxxRegister = getRegisterFor(xxx);
                byte orig = xxxRegister.getValue();
                int carry = c ? 1 : 0;
                xxxRegister.setValue((byte) ((orig & 0xFF) << 1));
                xxxRegister.setValue((byte) (xxxRegister.getValue() | carry));
                z = xxxRegister.getValue() == 0;
                n = false;
                h = false;
                c = (orig & 0x80) == 0x80;
            } else if ((nextInstruction & 0b11111_000) == 0b00011_000) {
                // RR r
                byte xxx = (byte) (nextInstruction & 0b00000_111);
                InstructionTarget8Bit xxxRegister = getRegisterFor(xxx);
                byte msb = (byte) ((c ? 1 : 0) << 7);
                byte orig = xxxRegister.getValue();
                xxxRegister.setValue((byte) ((xxxRegister.getValue() & 0xFF) >>> 1));
                xxxRegister.setValue((byte) (xxxRegister.getValue() | msb));
                z = xxxRegister.getValue() == 0;
                n = false;
                h = false;
                c = (orig & 0x01) == 0x01;
            } else if ((nextInstruction & 0b11111_000) == 0b00100_000) {
                // SLA r
                byte xxx = (byte) (nextInstruction & 0b00000_111);
                InstructionTarget8Bit xxxRegister = getRegisterFor(xxx);
                byte orig = xxxRegister.getValue();
                xxxRegister.setValue((byte) ((orig & 0xFF) << 1));
                z = xxxRegister.getValue() == 0;
                n = false;
                h = false;
                c = (orig & 0x80) == 0x80;
            } else if ((nextInstruction & 0b11111_000) == 0b00101_000) {
                // SRA r
                byte xxx = (byte) (nextInstruction & 0b00000_111);
                InstructionTarget8Bit xxxRegister = getRegisterFor(xxx);
                byte orig = xxxRegister.getValue();
                int msb = orig & 0x80;
                xxxRegister.setValue((byte) ((orig & 0xFF) >>> 1));
                xxxRegister.setValue((byte) (xxxRegister.getValue() | msb));
                z = xxxRegister.getValue() == 0;
                n = false;
                h = false;
                c = (orig & 0x01) == 0x01;
            } else if ((nextInstruction & 0b11111_000) == 0b00110_000) {
                // SWAP r
                byte xxx = (byte) (nextInstruction & 0b00000_111);
                InstructionTarget8Bit xxxRegister = getRegisterFor(xxx);
                byte lower4  = (byte) ((xxxRegister.getValue() <<  4) & 0xF0);
                byte higher4 = (byte) ((xxxRegister.getValue() >>> 4) & 0x0F);
                xxxRegister.setValue((byte) (lower4 | higher4));

                z = xxxRegister.getValue() == 0;
                n = false;
                h = false;
                c = false;
            } else if ((nextInstruction & 0b11111_000) == 0b00111_000) {
                // SRL r
                byte xxx = (byte) (nextInstruction & 0b00000_111);
                InstructionTarget8Bit xxxRegister = getRegisterFor(xxx);
                byte orig = xxxRegister.getValue();
                xxxRegister.setValue((byte) ((orig & 0xFF) >>> 1));
                z = xxxRegister.getValue() == 0;
                n = false;
                h = false;
                c = (orig & 0x01) == 0x01;
            } else if ((nextInstruction & 0b11_000_000) == 0b01_000_000) {
                // BIT i, r
                byte iii = (byte) ((nextInstruction & 0b00_111_000) >>> 3);
                byte xxx = (byte) (nextInstruction & 0b00000_111);
                InstructionTarget8Bit xxxRegister = getRegisterFor(xxx);

                z = (xxxRegister.getValue() & (0x01 << iii)) >>> iii == 0x00;
                n = false;
                h = true;
            } else if ((nextInstruction & 0b11_000_000) == 0b10_000_000) {
                // RES i, r
                byte iii = (byte) ((nextInstruction & 0b00_111_000) >>> 3);
                byte xxx = (byte) (nextInstruction & 0b00000_111);
                InstructionTarget8Bit xxxRegister = getRegisterFor(xxx);

                int targetBit = (0x01 << iii);
                int orig = xxxRegister.getValue() & 0xFF;

                xxxRegister.setValue((byte) (orig & ~targetBit));
            } else if ((nextInstruction & 0b11_000_000) == 0b11_000_000) {
                // SET i, r
                byte iii = (byte) ((nextInstruction & 0b00_111_000) >>> 3);
                byte xxx = (byte) (nextInstruction & 0b00000_111);
                InstructionTarget8Bit xxxRegister = getRegisterFor(xxx);

                int targetBit = (0x01 << iii);
                int orig = xxxRegister.getValue() & 0xFF;

                xxxRegister.setValue((byte) (orig | targetBit));

            } else {
                System.out.println("Received 16 bit instruction 0xCB" + String.format("%02X", nextInstruction));
                throw new IndexOutOfBoundsException("Received invalid instruction for 16bit: " + String.format("0x%02X", nextInstruction));
            }

        } else if (nextInstruction == (byte) 0x20) {
            // JR nz, n
            byte addr = readNextByte();
            if (!z) {
                pc += addr;
            }
        } else if (nextInstruction == (byte) 0x30) {
            // JR NC, n
            byte addr = readNextByte();
            if (!c) {
                pc += addr;
            }
        } else if (nextInstruction == (byte) 0xC3) {
            // execute
            // JP nn
            byte lowerBits = readNextByte();
            byte higherBits = readNextByte();
            int targetPc = (higherBits & 0xFF) << 8 | (lowerBits & 0xFF);

            pc = targetPc;
        } else if (nextInstruction == (byte) 0xF3) {
            // DI (disable interrupts)
            // TODO
        } else if (nextInstruction == (byte) 0xEA) {
            // LD (nn), A
            byte lowerBits = readNextByte();
            byte higherBits = readNextByte();
            int targetAddr = (higherBits & 0xFF) << 8 | (lowerBits & 0xFF);
            bus.writeByteAt(targetAddr, A.getValue());
        } else if (nextInstruction == (byte) 0xE0) {
            // LD (n), A
            byte higher = (byte) 0xFF;
            byte lower = readNextByte();
            int targetAddr = (higher & 0xFF) << 8 | (lower & 0xFF);

            bus.writeByteAt(targetAddr, A.getValue());
        } else if (nextInstruction == (byte) 0xCD) {
            // CALL nn
            byte lower = readNextByte();
            byte higher = readNextByte();
            int targetAddr = (higher & 0xFF) << 8 | (lower & 0xFF);

            int currentPC = pc;
            byte lowerPC = (byte) (currentPC & 0xFF);
            byte higherPC = (byte) ((currentPC >> 8) & 0xFF);
            SP.setValue(SP.getValue() - 1);
            bus.writeByteAt(SP.getValue(), higherPC);
            SP.setValue(SP.getValue() - 1);
            bus.writeByteAt(SP.getValue(), lowerPC);
            pc = targetAddr;
        } else if (nextInstruction == (byte) 0x18) {
            // JR n
            byte addrOffset = readNextByte();
            pc += addrOffset;
        } else if (nextInstruction == (byte) 0xC9) {
            // RET
            byte lower = bus.readByteAt(SP.getValue());
            SP.setValue(SP.getValue() + 1);
            byte higher = bus.readByteAt(SP.getValue());
            SP.setValue(SP.getValue() + 1);
            pc = (higher & 0xFF) << 8 | (lower & 0xFF);
        } else if (nextInstruction == (byte) 0x28) {
            // JR Z, n
            byte addr = readNextByte();
            if (z) {
                pc += addr;
            }
        } else if (nextInstruction == (byte) 0xF0) {
            // LD A, (n)
            byte higher = (byte) 0xFF;
            byte lower = readNextByte();
            int targetAddr = (higher & 0xFF) << 8 | (lower & 0xFF);

            A.setValue(bus.readByteAt(targetAddr));
        } else if (nextInstruction == (byte) 0xFE) {
            // CP n
            byte value = readNextByte();
            byte orig = A.getValue();
            byte result = (byte) ((orig & 0xFF) - (value & 0xFF));

            z = result == 0;
            n = true;
            h = (((orig & 0xF) - (value & 0xF)) & 0x10) == 0x10;
            c = (value & 0xFF) > (orig & 0xFF);
        } else if (nextInstruction == (byte) 0xFA) {
            // LD A, (nn)
            byte lowerBits = readNextByte();
            byte higherBits = readNextByte();
            int targetAddr = (higherBits & 0xFF) << 8 | (lowerBits & 0xFF);
            A.setValue(bus.readByteAt(targetAddr));
        } else if (nextInstruction == (byte) 0xE6) {
            // AND n
            byte value = readNextByte();
            A.setValue((byte) (A.getValue() & value));
            z = A.getValue()==0;
            n = false;
            h = true;
            c = false;
        } else if (nextInstruction == (byte) 0xC6) {
            // ADD A, n
            byte value = readNextByte();
            byte orig = A.getValue();
            A.setValue((byte) ((orig & 0xFF) + (value & 0xFF)));

            z = A.getValue() == 0;
            n = false;
            h = (((orig & 0xF) + (value & 0xF)) & 0x10) == 0x10;
            c = (((orig & 0xFF) + (value & 0xFF)) & 0x100) == 0x100;
        } else if (nextInstruction == (byte) 0xD6) {
            // SUB A, n
            byte value = readNextByte();
            byte orig = A.getValue();
            A.setValue((byte) ((orig & 0xFF) - (value & 0xFF)));

            z = A.getValue() == 0;
            n = true;
            h = (((orig & 0xF) - (value & 0xF)) & 0x10) == 0x10;
            c = (value & 0xFF) > (orig & 0xFF);

        } else if (nextInstruction == (byte) 0x1F) {
            // RRA
            byte msb = (byte) ((c ? 1 : 0) << 7);
            byte orig = A.getValue();
            A.setValue((byte) ((A.getValue() & 0xFF) >>> 1));
            A.setValue((byte) (A.getValue() | msb));
            z = false;
            n = false;
            h = false;
            c = (orig & 0x01) == 0x01;
        } else if (nextInstruction == (byte) 0xEE) {
            // XOR n
            byte value = readNextByte();
            A.setValue((byte) (A.getValue() ^ value));
            z = A.getValue() == 0;
            n = false;
            h = false;
            c = false;
        } else if (nextInstruction == (byte) 0xCE) {
            // ADC A, n
            byte value = readNextByte();
            int orig = A.getValue();
            int carry = c ? 1:0;
            int sum = (A.getValue() & 0xFF) + (value & 0xFF) + carry;
            A.setValue((byte) sum);
            z = A.getValue() == 0;
            n = false;
            h = (((orig & 0xF) + (value & 0xF) + carry) & 0x10) == 0x10;
            c = ((orig & 0xFF) + (value & 0xFF) + carry) > 0xFF;
        } else if (nextInstruction == (byte) 0xE9) {
            // JP HL
            pc = HL.getValue();
        } else if (nextInstruction == (byte) 0x38) {
            // JR C, n
            byte addr = readNextByte();
            if (c) {
                pc += addr;
            }
        } else if (nextInstruction == (byte) 0xF6) {
            // OR n
            byte value = readNextByte();
            A.setValue((byte) (A.getValue() | value));
            z = A.getValue()==0;
            n = false;
            h = false;
            c = false;
        } else if (nextInstruction == (byte) 0xDE) {
            // SBC A, n
            byte value = readNextByte();
            int orig = A.getValue();
            int carry = c ? 1 : 0;

            A.setValue((byte) ((orig & 0xFF) - (value & 0xFF) - carry));
            z = A.getValue() == 0;
            n = true;
            h = (((orig & 0xF) - (value & 0xF) - carry) & 0x10) == 0x10;
            c = ((value & 0xFF) + carry) > (orig & 0xFF);
        } else if (nextInstruction == (byte) 0x2F) {
            // CPL
            A.setValue((byte) (~A.getValue()));

            n = true;
            h = true;
        } else if (nextInstruction == (byte) 0x37) {
            // SCF
            n = false;
            h = false;
            c = true;
        } else if (nextInstruction == (byte) 0x3F) {
            // CCF
            n = false;
            h = false;
            c = !c;
        } else if (nextInstruction == (byte) 0x07) {
            // RLCA
            byte orig = A.getValue();
            byte msb = (byte) ((orig & 0x80) >>> 7);
            A.setValue((byte) ((A.getValue() & 0xFF) << 1));
            A.setValue((byte) (A.getValue() | msb));
            z = false;
            n = false;
            h = false;
            c = msb == 0x01;
        } else if (nextInstruction == (byte) 0x17) {
            // RLA
            byte orig = A.getValue();
            byte msb = (byte) ((orig & 0x80) >>> 7);
            byte carry = (byte) (c ? 1:0);
            A.setValue((byte) ((A.getValue() & 0xFF) << 1));
            A.setValue((byte) (A.getValue() | carry));
            z = false;
            n = false;
            h = false;
            c = msb == 0x01;
        } else if (nextInstruction == (byte) 0x0F) {
            // RRCA
            byte orig = A.getValue();
            byte lsb = (byte) (orig & 0x01);
            A.setValue((byte) ((A.getValue() & 0xFF) >>> 1));
            A.setValue((byte) (A.getValue() | lsb << 7));
            z = false;
            n = false;
            h = false;
            c = lsb == 0x01;
        } else if (nextInstruction == (byte) 0x08) {
            // LD (nn), SP
            Register lowerRegister = SP.getLowerRegister();
            Register higherRegister = SP.getHigherRegister();

            byte lowerBits = readNextByte();
            byte higherBits = readNextByte();
            int targetAddr = (higherBits & 0xFF) << 8 | (lowerBits & 0xFF);

            bus.writeByteAt(targetAddr, lowerRegister.getValue());
            bus.writeByteAt(targetAddr+1, higherRegister.getValue());
        } else if (nextInstruction == (byte) 0xF9) {
            // LD SP, HL
            SP.setValue(HL.getValue());
        } else if (nextInstruction == (byte) 0xE8) {
            // ADD SP, s8
            byte value = readNextByte();
            int orig = SP.getValue();
            SP.setValue(value + orig);

            z = false;
            n = false;
            h = (((orig & 0xF) + (value & 0xF)) & 0x10) == 0x10;
            c = (((orig & 0xFF) + (value & 0xFF)) & 0x100) == 0x100;
        } else if (nextInstruction == (byte) 0xF8) {
            // LD HL, SP+s8
            byte value = readNextByte();
            int orig = SP.getValue();
            HL.setValue(value + orig);

            z = false;
            n = false;
            h = (((orig & 0xF) + (value & 0xF)) & 0x10) == 0x10;
            c = (((orig & 0xFF) + (value & 0xFF)) & 0x100) == 0x100;
        } else if (nextInstruction == (byte) 0xF2) {
            // LD A, (C)
            byte higher = (byte) 0xFF;
            byte lower = C.getValue();
            int targetAddr = (higher & 0xFF) << 8 | (lower & 0xFF);

            A.setValue(bus.readByteAt(targetAddr));
        } else if (nextInstruction == (byte) 0xE2) {
            // LD (C), A
            byte higher = (byte) 0xFF;
            byte lower = C.getValue();
            int targetAddr = (higher & 0xFF) << 8 | (lower & 0xFF);

            bus.writeByteAt(targetAddr, A.getValue());
        } else if (nextInstruction == (byte) 0x27) {
            // DAA
            byte value = A.getValue();
            byte correction = 0x00;
            if (h || (!n && (value & 0x0F) > 9)) {
                correction = 0x06;
            }

            if (c || (!n && (value & 0xFF) > 0x99)) {
                correction |= 0x60;
                c = true;
            }

            value = (byte) ((value & 0xFF) + (n ? -correction : correction));
            A.setValue(value);
            z = value == 0;
            h = false;
        } else if (nextInstruction == (byte) 0xD9) {
            // RETI TODO interrupt return
            byte lower = bus.readByteAt(SP.getValue());
            SP.setValue(SP.getValue() + 1);
            byte higher = bus.readByteAt(SP.getValue());
            SP.setValue(SP.getValue() + 1);
            pc = (higher & 0xFF) << 8 | (lower & 0xFF);
        } else if (nextInstruction == (byte) 0xFB) {
            // EI
            // TODO
        } else {
            System.out.println("Received invalid instruction: " + String.format("0x%02X", nextInstruction));
            throw new IndexOutOfBoundsException("Received invalid instruction: " + String.format("0x%02X", nextInstruction));
        }
    }

    private boolean getFlagForCond(byte cc) {
        if (cc == 0b00) {
            // NZ
            return !z;
        } else if (cc == 0b01) {
            // Z
            return z;
        } else if (cc == 0b10) {
            // NC
            return !c;
        } else if (cc == 0b11) {
            // C
            return c;
        } else {
            throw new IllegalArgumentException("Invalid input for getFlagForCond(): " + cc);
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

    private byte readRegisterF() {
        int zFlag = z ? 1:0;
        int nFlag = n ? 1:0;
        int hFlag = h ? 1:0;
        int cFlag = c ? 1:0;
        return (byte) (zFlag << 7 | nFlag << 6 | hFlag << 5 | cFlag << 4);
    }
    private void writeRegisterF(byte value) {
        z = ((value >> 7) & 0x01) == 0x01;
        n = ((value >> 6) & 0x01) == 0x01;
        h = ((value >> 5) & 0x01) == 0x01;
        c = ((value >> 4) & 0x01) == 0x01;
    }

    private byte readNextByte() {
        byte nextByte = bus.readByteAt(pc);
        pc += 1;
        return nextByte;
    }
}
