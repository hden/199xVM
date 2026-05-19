public class DefineClassExplicitNameMismatchTest {
    public static String run() {
        LoaderPhase0Fixtures.ExposedLoader loader = new LoaderPhase0Fixtures.ExposedLoader();
        byte[] bytes = LoaderPhase0Fixtures.bytes(LoaderPhase0Fixtures.SAME_NAME_B64);
        try {
            loader.define("phase0.WrongName", bytes);
            return "no-error";
        } catch (NoClassDefFoundError e) {
            return "NoClassDefFoundError";
        } catch (Throwable t) {
            return LoaderPhase0Fixtures.simpleName(t);
        }
    }
}
