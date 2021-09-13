package emulator;

public class CPU {
    // Order of registers; B,C,D,E,H,L,(HL),A
    private Bus bus;
    private int pc;
    private int sp;

    private byte A;
    private byte H;
    private byte L;
    private byte D;
    private byte E;
    private byte B;
    private byte C;

    private boolean z;
    private boolean n;
    private boolean h;
    private boolean c;


    public CPU(Bus bus) {
        this.bus = bus;
        this.pc = 0x0100;

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
//        System.out.println("Executing at   " + String.format("0x%02X", pc));
        // fetch
        byte nextInstruction = readNextByte();
//        System.out.println("Instruction is " + String.format("0x%02X", nextInstruction));


        // decode
        if (nextInstruction == (byte) 0x00) {
            // NOP
        } else if (nextInstruction == (byte) 0x10) {
            // STOP (disable interrupts)
            // TODO
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
        } else if (nextInstruction == (byte) 0x01) {
            // LD BC, d16
            byte lowerBits = readNextByte();
            byte higherBits = readNextByte();

            B = higherBits;
            C = lowerBits;
        } else if (nextInstruction == (byte) 0x11) {
            // LD DE, nn
            byte lowerBits = readNextByte();
            byte higherBits = readNextByte();

            D = higherBits;
            E = lowerBits;
        } else if (nextInstruction == (byte) 0x21) {
            // LD HL, nn
            byte lowerBits = readNextByte();
            byte higherBits = readNextByte();

            H = higherBits;
            L = lowerBits;
        } else if (nextInstruction == (byte) 0x31) {
            // LD SP, d16
            byte lowerBits = readNextByte();
            byte higherBits = readNextByte();

            sp = (higherBits & 0xFF) << 8 | (lowerBits & 0xFF);
        } else if (nextInstruction == (byte) 0x02) {
            // LD (BC), A
            int memoryLocation = (B & 0xFF) << 8 | (C & 0xFF);
            bus.writeByteAt(memoryLocation, A);
//            System.out.printf("Writing A (0x%02X) to 0x%02X%n", A, memoryLocation);
        } else if (nextInstruction == (byte) 0x12) {
            // LD (DE), A
            int memoryLocation = (D & 0xFF) << 8 | (E & 0xFF);
            bus.writeByteAt(memoryLocation, A);
//            System.out.printf("Writing A (0x%02X) to 0x%02X%n", A, memoryLocation);
        } else if (nextInstruction == (byte) 0x22) {
            // LD (HL++), A
            int memoryLocation = (H & 0xFF) << 8 | (L & 0xFF);
            bus.writeByteAt(memoryLocation, A);

            memoryLocation++;
            L = (byte) (memoryLocation & 0xFF);
            H = (byte) ((memoryLocation >> 8) & 0xFF);
//            System.out.printf("Writing A (0x%02X) to 0x%02X%n", A, memoryLocation);
        } else if (nextInstruction == (byte) 0x32) {
            // LD (HL--), A
            int memoryLocation = (H & 0xFF) << 8 | (L & 0xFF);
            bus.writeByteAt(memoryLocation, A);

            memoryLocation--;
            L = (byte) (memoryLocation & 0xFF);
            H = (byte) ((memoryLocation >> 8) & 0xFF);
//            System.out.printf("Writing A (0x%02X) to 0x%02X%n", A, memoryLocation);
        } else if (nextInstruction == (byte) 0x03) {
            // INC BC
            int currentValue = (B & 0xFF) << 8 | (C & 0xFF);
            currentValue++;

            C = (byte) (currentValue & 0xFF);
            B = (byte) ((currentValue >> 8) & 0xFF);
        } else if (nextInstruction == (byte) 0x13) {
            // INC DE
            int currentValue = (D & 0xFF) << 8 | (E & 0xFF);
            currentValue++;

            E = (byte) (currentValue & 0xFF);
            D = (byte) ((currentValue >> 8) & 0xFF);
        } else if (nextInstruction == (byte) 0x23) {
            // INC HL
            int currentValue = (H & 0xFF) << 8 | (L & 0xFF);
            currentValue++;

            L = (byte) (currentValue & 0xFF);
            H = (byte) ((currentValue >> 8) & 0xFF);
        } else if (nextInstruction == (byte) 0x33) {
            // INC SP
            sp++;
        } else if (nextInstruction == (byte) 0x04) {
            // INC B
            byte orig = B;
            B += 1;

            z = B == 0;
            n = false;
            h = (((orig & 0xf) + 0x1) & 0x10) == 0x10;
        } else if (nextInstruction == (byte) 0x14) {
            // INC D
            byte orig = D;
            D += 1;

            z = D == 0;
            n = false;
            h = (((orig & 0xf) + 0x1) & 0x10) == 0x10;
        } else if (nextInstruction == (byte) 0x24) {
            // INC H
            byte orig = H;
            H += 1;

            z = H == 0;
            n = false;
            h = (((orig & 0xf) + 0x1) & 0x10) == 0x10;
        } else if (nextInstruction == (byte) 0x34) {
            // INC (HL)
            int memoryLocation = (H & 0xFF) << 8 | (L & 0xFF);
            byte byteRead = bus.readByteAt(memoryLocation);

            byte orig = byteRead;
            byteRead += 1;

            bus.writeByteAt(memoryLocation, byteRead);

            z = byteRead == 0;
            n = false;
            h = (((orig & 0xf) + 0x1) & 0x10) == 0x10;
        } else if (nextInstruction == (byte) 0x05) {
            // DEC B
            byte orig = B;
            B -= 1;

            z = B == 0;
            n = true;
            h = (((orig & 0xf) - 0x1) & 0x10) == 0x10;
        } else if (nextInstruction == (byte) 0x15) {
            // DEC D
            byte orig = D;
            D -= 1;

            z = D == 0;
            n = true;
            h = (((orig & 0xf) - 0x1) & 0x10) == 0x10;
        } else if (nextInstruction == (byte) 0x25) {
            // DEC H
            byte orig = H;
            H -= 1;

            z = H == 0;
            n = true;
            h = (((orig & 0xf) - 0x1) & 0x10) == 0x10;
        } else if (nextInstruction == (byte) 0x35) {
            // DEC (HL)
            int memoryLocation = (H & 0xFF) << 8 | (L & 0xFF);
            byte byteRead = bus.readByteAt(memoryLocation);

            byte orig = byteRead;
            byteRead -= 1;

            bus.writeByteAt(memoryLocation, byteRead);

            z = byteRead == 0;
            n = true;
            h = (((orig & 0xf) - 0x1) & 0x10) == 0x10;
        } else if (nextInstruction == (byte) 0x06) {
            // LD B, n
            byte value = readNextByte();
            B = value;
        } else if (nextInstruction == (byte) 0x16) {
            // LD D, n
            byte value = readNextByte();
            D = value;
        } else if (nextInstruction == (byte) 0x26) {
            // LD H, n
            byte value = readNextByte();
            H = value;
        } else if (nextInstruction == (byte) 0x36) {
            // LD (HL), n
            int memoryLocation = (H & 0xFF) << 8 | (L & 0xFF);
            byte value = readNextByte();
            bus.writeByteAt(memoryLocation, value);

        } else if (nextInstruction == (byte) 0xC3) {
            // execute
            // JP nn
            byte lowerBits = readNextByte();
            byte higherBits = readNextByte();
            int targetPc = (higherBits & 0xFF) << 8 | (lowerBits & 0xFF);

            pc = targetPc;
        }  else if (nextInstruction == (byte) 0x47) {
            // LD B, A
            B = A;
        }  else if (nextInstruction == (byte) 0x0E) {
            // LD C, n
            byte bits = readNextByte();
            C = bits;
        } else if (nextInstruction == (byte) 0x2A) {
            // LD A, (HL++)
            int memoryLocation = (H & 0xFF) << 8 | (L & 0xFF);
            A = bus.readByteAt(memoryLocation);
            memoryLocation++;
            L = (byte) (memoryLocation & 0xFF);
            H = (byte) ((memoryLocation >> 8) & 0xFF);
        } else if (nextInstruction == (byte) 0x1C) {
            // INC E

            byte E_orig = E;
            E += 1;

            z = E == 0;
            n = false;
            h = (((E_orig & 0xf) + 0x1) & 0x10) == 0x10;
        } else if (nextInstruction == (byte) 0x14){
            // INC D
            byte D_orig = D;
            D += 1;

            z = D == 0;
            n = false;
            h = (((D_orig & 0xf) + 0x1) & 0x10) == 0x10;
        } else if (nextInstruction == (byte) 0x0D){
            // DEC C
            byte C_orig = C;
            C -= 1;

            z = C == 0;
            n = true;
            h = (((C_orig & 0xf) - 0x1) & 0x10) == 0x10;
        } else if (nextInstruction == (byte) 0x78) {
            // LD A, B
            A = B;
        } else if (nextInstruction == (byte) 0xF3) {
            // DI (disable interrupts)
            // TODO
        } else if (nextInstruction == (byte) 0xEA) {
            // LD (nn), A
            byte lowerBits = readNextByte();
            byte higherBits = readNextByte();
            int targetAddr = (higherBits & 0xFF) << 8 | (lowerBits & 0xFF);
            bus.writeByteAt(targetAddr, A);
        } else if (nextInstruction == (byte) 0x3E) {
            // LD A, n
            byte value = readNextByte();
            A = value;
        } else if (nextInstruction == (byte) 0xE0) {
            // LD (n), A
            byte higher = (byte) 0xFF;
            byte lower = readNextByte();
            int targetAddr = (higher & 0xFF) << 8 | (lower & 0xFF);

            bus.writeByteAt(targetAddr, A);
        } else if (nextInstruction == (byte) 0xCD) {
            // CALL nn
            byte lower = readNextByte();
            byte higher = readNextByte();
            int targetAddr = (higher & 0xFF) << 8 | (lower & 0xFF);

            int currentPC = pc;
            byte lowerPC = (byte) (currentPC & 0xFF);
            byte higherPC = (byte) ((currentPC >> 8) & 0xFF);
            sp--;
            bus.writeByteAt(sp, higherPC);
            sp--;
            bus.writeByteAt(sp, lowerPC);
            pc = targetAddr;
        } else if (nextInstruction == (byte) 0x7D) {
            // LD A, L
            A = L;
        } else if (nextInstruction == (byte) 0x7C) {
            // LD A, H
            A = H;
        } else if (nextInstruction == (byte) 0x18) {
            // JR n
            byte addrOffset = readNextByte();
            pc += addrOffset;
        } else if (nextInstruction == (byte) 0xC9) {
            // RET
            byte lower = bus.readByteAt(sp);
            sp++;
            byte higher = bus.readByteAt(sp);
            sp++;
            pc = (higher & 0xFF) << 8 | (lower & 0xFF);
        } else if (nextInstruction == (byte) 0xE5) {
            // PUSH HL
            sp--;
            bus.writeByteAt(sp, H);
            sp--;
            bus.writeByteAt(sp, L);
        } else if (nextInstruction == (byte) 0xE1) {
            // POP HL
            L = bus.readByteAt(sp);
            sp++;
            H = bus.readByteAt(sp);
            sp++;
        } else if (nextInstruction == (byte) 0xF5) {
            // PUSH AF
            sp--;
            bus.writeByteAt(sp, A);
            sp--;
            bus.writeByteAt(sp, readRegisterF());
        } else if (nextInstruction == (byte) 0xF1) {
            // POP AF
            A = bus.readByteAt(sp);
            sp++;
            writeRegisterF(bus.readByteAt(sp));
            sp++;
        } else if (nextInstruction == (byte) 0xC5) {
            // PUSH BC
            sp--;
            bus.writeByteAt(sp, B);
            sp--;
            bus.writeByteAt(sp, C);
        } else if (nextInstruction == (byte) 0xC1) {
            // POP BC
            C = bus.readByteAt(sp);
            sp++;
            B = bus.readByteAt(sp);
            sp++;
        } else if (nextInstruction == (byte) 0xB1) {
            // OR C
            A = (byte) (A | C);
            z = A == 0;
            n = false;
            h = false;
            c = false;
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

            A = bus.readByteAt(targetAddr);
        } else if (nextInstruction == (byte) 0xFE) {
            // CP n
            byte value = readNextByte();
            byte compare = (byte) (A - value);

            z = compare == 0;
            n = true;
            h = (((A & 0xF) - (value & 0xF)) & 0x10) == 0x10;
            c = value > A;
        } else if (nextInstruction == (byte) 0xFA) {
            // LD A, (nn)
            byte lowerBits = readNextByte();
            byte higherBits = readNextByte();
            int targetAddr = (higherBits & 0xFF) << 8 | (lowerBits & 0xFF);
            A = bus.readByteAt(targetAddr);
        } else if (nextInstruction == (byte) 0xE6) {
            // AND n
            byte value = readNextByte();
            A = (byte) (A & value);
            z = A==0;
            n = false;
            h = true;
            c = false;
        } else if (nextInstruction == (byte) 0xC4) {
            // CALL NZ, nn
            byte lowerBits = readNextByte();
            byte higherBits = readNextByte();
            int targetAddr = (higherBits & 0xFF) << 8 | (lowerBits & 0xFF);
            if (!z) {
                int currentPC = pc;
                byte lowerPC = (byte) (currentPC & 0xFF);
                byte higherPC = (byte) ((currentPC >> 8) & 0xFF);
                sp--;
                bus.writeByteAt(sp, higherPC);
                sp--;
                bus.writeByteAt(sp, lowerPC);
                pc = targetAddr;
            }
        } else if (nextInstruction == (byte) 0x77) {
            // LD (HL), A
            int memoryLocation = (H & 0xFF) << 8 | (L & 0xFF);
            bus.writeByteAt(memoryLocation, A);
        } else if (nextInstruction == (byte) 0x2C) {
            // INC L
            byte orig = L;
            L += 1;

            z = L == 0;
            n = false;
            h = (((orig & 0xf) + 0x1) & 0x10) == 0x10;
        } else if (nextInstruction == (byte) 0x1A) {
            // LD A, (DE)
            int memoryLocation = (D & 0xFF) << 8 | (E & 0xFF);
            A = bus.readByteAt(memoryLocation);
        } else if (nextInstruction == (byte) 0xA9) {
            // XOR C
            A = (byte) (A ^ C);
            z = A == 0;
            n = false;
            h = false;
            c = false;
        } else if (nextInstruction == (byte) 0xC6) {
            // ADD A, n
            byte value = readNextByte();
            byte orig = A;
            A = (byte) (A + value);

            z = A == 0;
            n = false;
            h = (((orig & 0xF) + (value & 0xF)) & 0x10) == 0x10;
            c = (A + value & 0x100) == 0x100;
        } else if (nextInstruction == (byte) 0xD6) {
            // SUB n
            byte value = readNextByte();
            byte orig = A;
            A = (byte) (A - value);

            z = A == 0;
            n = true;
            h = (((orig & 0xF) - (value & 0xF)) & 0x10) == 0x10;
            c = value > orig;
        } else if (nextInstruction == (byte) 0xB7) {
            // OR A
            A = (byte) (A | A);
            z = A == 0;
            n = false;
            h = false;
            c = false;
        } else if (nextInstruction == (byte) 0xD5) {
            // PUSH DE
            sp--;
            bus.writeByteAt(sp, D);
            sp--;
            bus.writeByteAt(sp, E);
        } else if (nextInstruction == (byte) 0xD1) {
            // POP DE
            E = bus.readByteAt(sp);
            sp++;
            D = bus.readByteAt(sp);
            sp++;
        } else if (nextInstruction == (byte) 0x46) {
            // LD B, (HL)
            int memoryLocation = (H & 0xFF) << 8 | (L & 0xFF);
            B = bus.readByteAt(memoryLocation);
        } else if (nextInstruction == (byte) 0x2D) {
            // DEC L
            byte orig = L;
            L -= 1;

            z = L == 0;
            n = true;
            h = (((orig & 0xf) - 0x1) & 0x10) == 0x10;
        } else if (nextInstruction == (byte) 0x4E) {
            // LD C, (HL)
            int memoryLocation = (H & 0xFF) << 8 | (L & 0xFF);
            C = bus.readByteAt(memoryLocation);
        } else if (nextInstruction == (byte) 0x56) {
            // LD D, (HL)
            int memoryLocation = (H & 0xFF) << 8 | (L & 0xFF);
            D = bus.readByteAt(memoryLocation);
        } else if (nextInstruction == (byte) 0xAE) {
            // XOR (HL)
            int memoryLocation = (H & 0xFF) << 8 | (L & 0xFF);
            byte value = bus.readByteAt(memoryLocation);
            A = (byte) (A ^ value);
            z = A == 0;
            n = false;
            h = false;
            c = false;
        } else if (nextInstruction == (byte) 0xCB) {
            // 16 bit opcode....
            nextInstruction = readNextByte();
            if (nextInstruction == (byte) 0x38) {
                // SRL B
                byte orig = B;
                B = (byte) ((B & 0xFF) >>> 1);
                z = B == 0;
                n = false;
                h = false;
                c = (orig & 0x80) == 0x80;
            } else if (nextInstruction == (byte) 0x19) {
                // RR C
                byte msb = (byte) ((c ? 1 : 0) << 7);
                byte orig = C;
                C = (byte) ((C & 0xFF) >>> 1);
                C = (byte) (C | msb);
                z = C == 0;
                n = false;
                h = false;
                c = ((orig & 0xFF) >>> 7) == 0x01;
            } else if (nextInstruction == (byte) 0x1A) {
                // RR D
                byte msb = (byte) ((c ? 1 : 0) << 7);
                byte orig = D;
                D = (byte) ((D & 0xFF) >>> 1);
                D = (byte) (D | msb);
                z = D == 0;
                n = false;
                h = false;
                c = ((orig & 0xFF) >>> 7) == 0x01;

            } else {
                System.out.println("Received 16 bit instruction 0xCB" + String.format("%02X", nextInstruction));
                throw new IndexOutOfBoundsException("Received invalid instruction: " + String.format("0x%02X", nextInstruction));
            }
        } else if (nextInstruction == (byte) 0x1F) {
            // RRA
            byte msb = (byte) ((c ? 1 : 0) << 7);
            byte orig = A;
            A = (byte) ((A & 0xFF) >>> 1);
            A = (byte) (A | msb);
            z = A == 0;
            n = false;
            h = false;
            c = ((orig & 0xFF) >>> 7) == 0x01;
        } else if (nextInstruction == (byte) 0x5F) {
            // LD E, A
            E = A;
        } else if (nextInstruction == (byte) 0xEE) {
            // XOR n
            byte value = readNextByte();
            A = (byte) (A ^ value);
            z = A == 0;
            n = false;
            h = false;
            c = false;
        } else if (nextInstruction == (byte) 0x79) {
            // LD A, C
            A = C;
        } else if (nextInstruction == (byte) 0x4F) {
            // LD C, A
            C = A;
        } else if (nextInstruction == (byte) 0x7A) {
            // LD A, D
            A = D;
        } else if (nextInstruction == (byte) 0x57) {
            // LD D, A
            D = A;
        } else if (nextInstruction == (byte) 0x7B) {
            // LD A, E
            A = E;
        } else if (nextInstruction == (byte) 0x72) {
            // LD (HL), D
            int memoryLocation = (H & 0xFF) << 8 | (L & 0xFF);
            bus.writeByteAt(memoryLocation, D);
        } else if (nextInstruction == (byte) 0x71) {
            // LD (HL), C
            int memoryLocation = (H & 0xFF) << 8 | (L & 0xFF);
            bus.writeByteAt(memoryLocation, C);
        } else if (nextInstruction == (byte) 0x70) {
            // LD (HL), C
            int memoryLocation = (H & 0xFF) << 8 | (L & 0xFF);
            bus.writeByteAt(memoryLocation, B);
        } else if (nextInstruction == (byte) 0xCE) {
            // ADC A, n
            byte value = readNextByte();
            int orig = A;
            int sum = (A & 0xFF) + (value & 0xFF) + (c ? 1:0);
            A = (byte) sum;
            z = A == 0;
            n = false;
            h = (((orig & 0xf) + 0x1 + (value & 0xFF)) & 0x10) == 0x10;
            c = ((sum >>> 8) & 0x01) == 0x01;
        } else if (nextInstruction == (byte) 0xD0) {
            // RET NC
            if (!c) {
                byte lower = bus.readByteAt(sp);
                sp++;
                byte higher = bus.readByteAt(sp);
                sp++;
                pc = (higher & 0xFF) << 8 | (lower & 0xFF);
            }
        } else if (nextInstruction == (byte) 0x3D) {
            // DEC A
            byte orig = A;
            A -= 1;

            z = A == 0;
            n = true;
            h = (((orig & 0xf) - 0x1) & 0x10) == 0x10;
        } else {
            System.out.println("Received invalid instruction: " + String.format("0x%02X", nextInstruction));
            throw new IndexOutOfBoundsException("Received invalid instruction: " + String.format("0x%02X", nextInstruction));
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
