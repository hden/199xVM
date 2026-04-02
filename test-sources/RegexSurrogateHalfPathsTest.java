import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RegexSurrogateHalfPathsTest {
    public static String run() {
        String emoji = "\uD83D\uDE00";
        String high = emoji.substring(0, 1);
        String low = emoji.substring(1, 2);

        boolean staticFull = Pattern.matches(high, high);
        Matcher exact = Pattern.compile(high).matcher(high);
        boolean instanceFull = exact.matches();

        Matcher find = Pattern.compile(high).matcher(high);
        boolean found = find.find();
        String group = found ? find.group() : "";
        boolean mismatch = Pattern.matches(high, low);

        return staticFull
            + "|" + instanceFull
            + "|" + found
            + "|" + (found ? find.start() : -1)
            + "|" + (found ? find.end() : -1)
            + "|" + group.length()
            + "|" + (group.length() > 0 ? (int) group.charAt(0) : -1)
            + "|" + mismatch;
    }
}
