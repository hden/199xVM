import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RegexFindEmojiWildcardTest {
    public static String run() {
        Matcher matcher = Pattern.compile(".").matcher("\uD83D\uDE00");
        boolean found = matcher.find();
        String group = found ? matcher.group() : "";
        return found
            + "|" + (found ? matcher.start() : -1)
            + "|" + (found ? matcher.end() : -1)
            + "|" + group.length()
            + "|" + (group.length() > 0 ? (int) group.charAt(0) : -1)
            + "|" + (group.length() > 1 ? (int) group.charAt(1) : -1);
    }
}
