public class AnewArrayInitiatingLoaderTest {
    public static String run() throws Exception {
        LoaderPhase0Fixtures.BytesLoader loader = new LoaderPhase0Fixtures.BytesLoader(
                new String[] { "phase0.StringArrayCaller" },
                new byte[][] {
                    LoaderPhase0Fixtures.bytes(LoaderPhase0Fixtures.STRING_ARRAY_CALLER_B64)
                });
        String arrayName = LoaderPhase0Fixtures.invokeString(
                loader.loadClass("phase0.StringArrayCaller"),
                "run");
        Class<?> loaded = loader.findLoaded("[Ljava.lang.String;");
        return arrayName + "|" + (loaded == null ? "missing" : "found");
    }
}
