import java.util.regex.Matcher;
import java.util.regex.Pattern;

class BenchRegexFindEmoji {
    static int run() {
        Pattern p = Pattern.compile("\uD83D\uDE00");
        int sum = 0;
        for (int i = 0; i < 10000; i++) {
            Matcher m = p.matcher("a\uD83D\uDE00b\uD83D\uDE00c");
            if (m.find()) {
                sum += m.start();
                sum += m.end();
            }
        }
        return sum;
    }
}
