package dev.swell.chip8;

public class Opcode {

    private static final boolean PRINT_DEBUG = false;

    private final int opcode;
    private final int nnn;
    private final int n;
    private final int x;
    private final int y;
    private final int kk;
    private final int instruction;

    public Opcode(int instruction) {
        this.opcode = instruction;
        this.instruction = instruction & 0xf000;
        this.nnn = instruction & 0x0fff;
        this.n = instruction & 0x000f;
        this.x = (instruction & 0x0f00) >> 8;
        this.y = (instruction & 0x00f0) >> 4;
        this.kk = (instruction & 0x00ff);
    }


    public int getOpcode() {
        return opcode;
    }

    public int getNnn() {
        return nnn;
    }

    public int getN() {
        return n;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getKk() {
        return kk;
    }

    public int getInstruction() {
        return instruction;
    }

    public void debug() {
        if (!PRINT_DEBUG) return;
        switch (instruction) {
            case 0:
                switch (opcode) {
                    case 0x0e0: IO.println(String.format("[%x] CLS", opcode)); break;
                    case 0x0ee: IO.println(String.format("[%x] RET", opcode)); break;
                }
                break;
            case 0x1000: IO.println(String.format("[%x] JP %x", opcode, nnn)); break;
            case 0x2000: IO.println(String.format("[%x] CALL %x", opcode, nnn)); break;
            case 0x3000: IO.println(String.format("[%x] IF V[%x], %x", opcode, x,  kk)); break;
            case 0x4000: IO.println(String.format("[%x] SNE V[%x], %x", opcode, x,  kk)); break;
            case 0x5000: IO.println(String.format("[%x] IF V[%x], V[%x]", opcode, x,  y)); break;
            case 0x6000: IO.println(String.format("[%x] LD V[%x], %x", opcode, x,  kk)); break;
            case 0x7000: IO.println(String.format("[%x] ADD V[%x], %x", opcode, x,  kk)); break;
            case 0x8000: {
                switch (n) {
                    case 0: IO.println(String.format("[%x] LD V[%x], V[%x]", opcode, x,  y)); break;
                    case 1: IO.println(String.format("[%x] OR V[%x], V[%x]", opcode, x,  y)); break;
                    case 2: IO.println(String.format("[%x] AND V[%x], V[%x]", opcode, x,  y)); break;
                    case 3: IO.println(String.format("[%x] XOR V[%x], V[%x]", opcode, x,  y)); break;
                    case 4: IO.println(String.format("[%x] AND V[%x], V[%x], VF=carry", opcode, x,  y)); break;
                    case 5: IO.println(String.format("[%x] SUB V[%x], V[%x], VF = NOT 0", opcode, x,  y)); break;
                    case 6: IO.println(String.format("[%x] SHR V[%x], V[%x]", opcode, x,  y)); break;
                    case 7: IO.println(String.format("[%x] SUBN V[%x], V[%x]", opcode, x,  y)); break;
                    case 0xE: IO.println(String.format("[%x] SHL V[%x], V[%x]", opcode, x,  y)); break;
                    default: IO.println(String.format("[%x] V[%x], V[%x] NOT IMPLEMENTED", opcode, x,  y)); break;
                }
                break;
            }
            default:
                IO.println(String.format("[ %x ] NOT IMPLEMENTED.",  opcode));
        }

    }
}
