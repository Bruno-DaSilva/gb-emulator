package emulator.cpu;

import java.util.Optional;

public class FlagModification {
//    Flag flag;
    boolean value;

    public FlagModification(int value) {
//        this.flag = flag;
        this.value = value != 0;
    }
    public FlagModification(boolean value) {
//        this.flag = flag;
        this.value = value;
    }


}
