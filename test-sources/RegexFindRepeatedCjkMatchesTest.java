import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RegexFindRepeatedCjkMatchesTest {
    public static String run() {
        Matcher m = Pattern.compile("\uD55C").matcher("\u6F22\uD55C\u6F22\uD55C");
        StringBuilder sb = new StringBuilder();
        while (m.find()) {
            if (sb.length() > 0) {
                sb.append(",");
            }
            sb.append(m.start()).append(":").append(m.end());
        }
        return sb.toString();
    }
}
