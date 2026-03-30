import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RegexFindBmpNonAsciiOffsetsTest {
    public static String run() {
        Matcher m = Pattern.compile("x").matcher("\u3042x");
        boolean found = m.find();
        return found + "|" + m.start() + "|" + m.end() + "|" + (found ? m.group() : "miss");
    }
}
