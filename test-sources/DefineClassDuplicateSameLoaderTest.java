public class DefineClassDuplicateSameLoaderTest {
    public static String run() {
        LoaderPhase0Fixtures.ExposedLoader loader = new LoaderPhase0Fixtures.ExposedLoader();
        byte[] bytes = LoaderPhase0Fixtures.bytes(LoaderPhase0Fixtures.SAME_NAME_B64);
        loader.define("phase0.SameName", bytes);
        try {
            loader.define("phase0.SameName", bytes);
            return "no-error";
        } catch (LinkageError e) {
            return "LinkageError";
        } catch (Throwable t) {
            return LoaderPhase0Fixtures.simpleName(t);
        }
    }
}
