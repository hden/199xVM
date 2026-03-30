import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RegexFindCjkOffsetsTest {
    public static String run() {
        Matcher m = Pattern.compile("\uD55C\u4E2D").matcher("\u6F22\u3042\uD55C\u4E2D\u6587");
        boolean found = m.find();
        return found + "|" + m.start() + "|" + m.end() + "|" + (found ? m.group() : "miss");
    }
}
