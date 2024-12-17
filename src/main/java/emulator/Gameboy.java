package emulator;

import emulator.bus.GameboyBus;
import emulator.bus.device.cartridge.Cartridge;
import emulator.cpu.CPU;
import emulator.cpu.GameboyRegisters;
import emulator.cpu.InstructionDecoder;
import emulator.cpu.InstructionFetcher;
import emulator.interrupts.InterruptController;
import emulator.interrupts.Timer;
import emulator.bus.ISerialHandler;
import emulator.bus.PrinterSerialHandler;

public class Gameboy {
    
    private final Cartridge rom;
    private final InterruptController interruptController;
    private final Timer timer;
    private final ISerialHandler serialHandler;
    private final GameboyBus bus;
    private final GameboyRegisters registers;
    private final InstructionFetcher instructionFetcher;
    private final InstructionDecoder instructionDecoder;
    private final CPU cpu;

    public Gameboy(GameboyBuilder builder) {
        this.rom = builder.rom;
        this.interruptController = builder.interruptController;
        this.timer = builder.timer;
        this.serialHandler = builder.serialHandler;
        this.bus = builder.bus;
        this.registers = builder.registers;
        this.instructionFetcher = builder.instructionFetcher;
        this.instructionDecoder = builder.instructionDecoder;
        this.cpu = builder.cpu;
    }

    public Gameboy(byte[] romBytes) {
        this.rom = new Cartridge(romBytes);
        this.interruptController = new InterruptController();
        this.timer = new Timer(interruptController);
        this.serialHandler = new PrinterSerialHandler();
        this.bus = new GameboyBus(rom, interruptController, timer, serialHandler);
        this.registers = new GameboyRegisters(bus);
        this.instructionFetcher = new InstructionFetcher(registers, bus);
        this.instructionDecoder = new InstructionDecoder(instructionFetcher, registers, bus, interruptController);
        this.cpu = new CPU(bus, registers, interruptController, instructionDecoder, instructionFetcher);
    }

    public void runCpuFor(int maxCycles) {
        int currCycle = 0;
        while(currCycle++ < maxCycles) {
            int numCycles = cpu.executeNext();
            numCycles += cpu.checkInterrupts();
            timer.addCycles(numCycles*4);
        }
    }

    public static class BusBuilder {
      private Cartridge rom;
      private InterruptController interruptController;
      private Timer timer;
      private ISerialHandler serialHandler;
      private GameboyBus bus;

      public BusBuilder cartridge(Cartridge rom) {
          if (this.rom != null) {
              throw new IllegalStateException("Cartridge is already set");
          }
          this.rom = rom;
          return this;
      }

      public BusBuilder cartridge(byte[] romBytes) {
          if (this.rom != null) {
              throw new IllegalStateException("Cartridge is already set");
          }
          this.rom = new Cartridge(romBytes);
          return this;
      }

      public BusBuilder interruptController(InterruptController interruptController) {
          this.interruptController = interruptController;
          return this;
      }

      public BusBuilder timer(Timer timer) {
          this.timer = timer;
          return this;
      }

      public BusBuilder serialHandler(ISerialHandler serialHandler) {
          this.serialHandler = serialHandler;
          return this;
      }

      public CpuBuilder buildBus() {
        if (this.rom == null) {
            throw new IllegalStateException("Cartridge is required");
        }
        if (this.interruptController == null) {
          this.interruptController = new InterruptController();
        }
        if (this.timer == null) {
          this.timer = new Timer(this.interruptController);
        }
        if (this.serialHandler == null) {
            this.serialHandler = new PrinterSerialHandler();
        }
        this.bus = new GameboyBus(this.rom, this.interruptController, this.timer, this.serialHandler);
        return new CpuBuilder(this);
      }
    }

    public static class CpuBuilder {
      private Cartridge rom;
      private InterruptController interruptController;
      private Timer timer;
      private ISerialHandler serialHandler;
      private GameboyBus bus;
      private GameboyRegisters registers;
      private InstructionFetcher instructionFetcher;
      private InstructionDecoder instructionDecoder;
      private CPU cpu;

      public CpuBuilder(BusBuilder busBuilder) {
          this.rom = busBuilder.rom;
          this.interruptController = busBuilder.interruptController;
          this.timer = busBuilder.timer;
          this.serialHandler = busBuilder.serialHandler;
          this.bus = busBuilder.bus;
      }

      // short circuit, skipping the bus builder
      public CpuBuilder(Timer timer, GameboyBus bus) {
          this.timer = timer;
          this.bus = bus;
      }

      public CpuBuilder registers(GameboyRegisters registers) {
          this.registers = registers;
          return this;
      }

      public CpuBuilder instructionFetcher(InstructionFetcher instructionFetcher) {
          this.instructionFetcher = instructionFetcher;
          return this;
      }

      public CpuBuilder instructionDecoder(InstructionDecoder instructionDecoder) {
          this.instructionDecoder = instructionDecoder;
          return this;
      }

      public GameboyBuilder buildCpu() {
        if (this.registers == null) {
          this.registers = new GameboyRegisters(this.bus);
        }
        if (this.instructionFetcher == null) {
          this.instructionFetcher = new InstructionFetcher(this.registers, this.bus);
        }
        if (this.instructionDecoder == null) {
          this.instructionDecoder = new InstructionDecoder(this.instructionFetcher, this.registers, this.bus, this.interruptController);
        }
        
        this.cpu = new CPU(this.bus, this.registers, this.interruptController, this.instructionDecoder, this.instructionFetcher);
        return new GameboyBuilder(this);
      }
    }

    public static class GameboyBuilder {
        private Cartridge rom;
        private InterruptController interruptController;
        private Timer timer;
        private ISerialHandler serialHandler;
        private GameboyBus bus;
        private GameboyRegisters registers;
        private InstructionFetcher instructionFetcher;
        private InstructionDecoder instructionDecoder;
        private CPU cpu;

        public GameboyBuilder(CpuBuilder cpuBuilder) {
          this.rom = cpuBuilder.rom;
          this.interruptController = cpuBuilder.interruptController;
          this.timer = cpuBuilder.timer;
          this.serialHandler = cpuBuilder.serialHandler;
          this.bus = cpuBuilder.bus;
          this.registers = cpuBuilder.registers;
          this.instructionFetcher = cpuBuilder.instructionFetcher;
          this.instructionDecoder = cpuBuilder.instructionDecoder;
          this.cpu = cpuBuilder.cpu;
        }

        // short circuit, skipping steps 1 + 2
        public GameboyBuilder(Timer timer, CPU cpu) {
            this.timer = timer;
            this.cpu = cpu;
        }

        public Gameboy build() {
            return new Gameboy(this);
        }
    }
}
