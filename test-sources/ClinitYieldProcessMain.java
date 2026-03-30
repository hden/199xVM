class ClinitYieldTarget {
    static final String VALUE = initValue();

    private static String initValue() {
        for (int i = 0; i < 1024; i++) {
            Thread.yield();
        }
        return "I";
    }
}

public class ClinitYieldProcessMain {
    public static void main(String[] args) {
        String value = ClinitYieldTarget.VALUE;
        System.out.print(value);
        System.out.print("done");
    }
}
