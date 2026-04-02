import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RegexFindEmptyEmojiBoundariesTest {
    public static String run() {
        Matcher m = Pattern.compile("").matcher("\uD83D\uDE00");
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
