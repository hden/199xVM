import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RegexFindEmojiOffsetsTest {
    public static String run() {
        Matcher m = Pattern.compile("\uD83D\uDE00").matcher("a\uD83D\uDE00b\uD83D\uDE00c");
        boolean found = m.find();
        return found + "|" + m.start() + "|" + m.end() + "|" + (found ? m.group() : "miss");
    }
}
