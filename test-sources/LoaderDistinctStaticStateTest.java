public class LoaderDistinctStaticStateTest {
    public static String run() throws Exception {
        byte[] bytes = LoaderPhase0Fixtures.bytes(LoaderPhase0Fixtures.ISOLATED_B64);
        Class<?> first = new LoaderPhase0Fixtures.ExposedLoader().define("phase0.Isolated", bytes);
        Class<?> second = new LoaderPhase0Fixtures.ExposedLoader().define("phase0.Isolated", bytes);

        LoaderPhase0Fixtures.invokeSetInt(first, 7);
        String firstSnapshot = LoaderPhase0Fixtures.invokeString(first, "snapshot");
        String secondSnapshot = LoaderPhase0Fixtures.invokeString(second, "snapshot");
        if ("7:1".equals(firstSnapshot) && "1:1".equals(secondSnapshot)) {
            return "isolated";
        }
        return new StringBuilder().append(firstSnapshot).append('|').append(secondSnapshot).toString();
    }
}
