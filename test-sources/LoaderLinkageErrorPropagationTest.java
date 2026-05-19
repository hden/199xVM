import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class LoaderLinkageErrorPropagationTest {
    public static String run() throws Exception {
        LoaderPhase0Fixtures.LinkageThrowingLoader loader = new LoaderPhase0Fixtures.LinkageThrowingLoader(
                "phase0.MissingDependency",
                new String[] { "phase0.MissingClassCaller" },
                new byte[][] {
                    LoaderPhase0Fixtures.bytes(LoaderPhase0Fixtures.MISSING_CLASS_CALLER_B64)
                });
        Class<?> caller = loader.loadClass("phase0.MissingClassCaller");
        Method m = caller.getDeclaredMethod("run", new Class<?>[0]);
        try {
            m.invoke(null, new Object[0]);
            return "none";
        } catch (InvocationTargetException e) {
            return LoaderPhase0Fixtures.simpleName(e.getCause());
        } catch (Throwable t) {
            return LoaderPhase0Fixtures.simpleName(t);
        }
    }
}
