public class CallerLoaderClassResolutionTest {
    public static String run() throws Exception {
        LoaderPhase0Fixtures.BytesLoader left = new LoaderPhase0Fixtures.BytesLoader(
                new String[] { "phase0.Caller", "phase0.Resolved" },
                new byte[][] {
                    LoaderPhase0Fixtures.bytes(LoaderPhase0Fixtures.CALLER_B64),
                    LoaderPhase0Fixtures.bytes(LoaderPhase0Fixtures.RESOLVED_LEFT_B64)
                });
        LoaderPhase0Fixtures.BytesLoader right = new LoaderPhase0Fixtures.BytesLoader(
                new String[] { "phase0.Caller", "phase0.Resolved" },
                new byte[][] {
                    LoaderPhase0Fixtures.bytes(LoaderPhase0Fixtures.CALLER_B64),
                    LoaderPhase0Fixtures.bytes(LoaderPhase0Fixtures.RESOLVED_RIGHT_B64)
                });

        String leftValue = LoaderPhase0Fixtures.invokeString(left.loadClass("phase0.Caller"), "call");
        String rightValue = LoaderPhase0Fixtures.invokeString(right.loadClass("phase0.Caller"), "call");
        return new StringBuilder().append(leftValue).append('|').append(rightValue).toString();
    }
}
