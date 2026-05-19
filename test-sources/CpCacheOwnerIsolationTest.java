public class CpCacheOwnerIsolationTest {
    public static String run() throws Exception {
        LoaderPhase0Fixtures.BytesLoader left = new LoaderPhase0Fixtures.BytesLoader(
                new String[] { "phase0.MemberCaller", "phase0.Owner" },
                new byte[][] {
                    LoaderPhase0Fixtures.bytes(LoaderPhase0Fixtures.MEMBER_CALLER_B64),
                    LoaderPhase0Fixtures.bytes(LoaderPhase0Fixtures.OWNER_LEFT_B64)
                });
        LoaderPhase0Fixtures.BytesLoader right = new LoaderPhase0Fixtures.BytesLoader(
                new String[] { "phase0.MemberCaller", "phase0.Owner" },
                new byte[][] {
                    LoaderPhase0Fixtures.bytes(LoaderPhase0Fixtures.MEMBER_CALLER_B64),
                    LoaderPhase0Fixtures.bytes(LoaderPhase0Fixtures.OWNER_RIGHT_B64)
                });

        String leftValue = LoaderPhase0Fixtures.invokeString(left.loadClass("phase0.MemberCaller"), "read");
        String rightValue = LoaderPhase0Fixtures.invokeString(right.loadClass("phase0.MemberCaller"), "read");
        return new StringBuilder().append(leftValue).append('|').append(rightValue).toString();
    }
}
