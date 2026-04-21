public class LoaderDistinctClassMirrorTest {
    public static String run() {
        byte[] bytes = LoaderPhase0Fixtures.bytes(LoaderPhase0Fixtures.SAME_NAME_B64);
        Class<?> first = new LoaderPhase0Fixtures.ExposedLoader().define("phase0.SameName", bytes);
        Class<?> second = new LoaderPhase0Fixtures.ExposedLoader().define("phase0.SameName", bytes);
        return first != second ? "distinct" : "same";
    }
}
