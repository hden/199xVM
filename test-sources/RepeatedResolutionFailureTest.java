public class RepeatedResolutionFailureTest {
    public static String run() throws Exception {
        String missingClass = LoaderPhase0Fixtures.repeatedFailure(
                "phase0.MissingClassCaller",
                new String[] { "phase0.MissingClassCaller" },
                new byte[][] {
                    LoaderPhase0Fixtures.bytes(LoaderPhase0Fixtures.MISSING_CLASS_CALLER_B64)
                });
        return missingClass;
    }
}
