class ClinitChainYieldBase {
    static final String VALUE = initValue();

    private static String initValue() {
        for (int i = 0; i < 512; i++) {
            Thread.yield();
        }
        System.out.print("B");
        return "base";
    }
}

class ClinitChainYieldMid extends ClinitChainYieldBase {
    static final String VALUE = initValue();

    private static String initValue() {
        for (int i = 0; i < 512; i++) {
            Thread.yield();
        }
        System.out.print("M");
        return ClinitChainYieldBase.VALUE + ":mid";
    }
}

class ClinitChainYieldLeaf extends ClinitChainYieldMid {
    static final String VALUE = initValue();

    private static String initValue() {
        for (int i = 0; i < 512; i++) {
            Thread.yield();
        }
        System.out.print("L");
        return ClinitChainYieldMid.VALUE + ":leaf";
    }
}

public class ClinitChainYieldProcessMain {
    public static void main(String[] args) {
        String value = ClinitChainYieldLeaf.VALUE;
        System.out.print(value);
    }
}
