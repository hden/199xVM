import java.util.regex.Matcher;
import java.util.regex.Pattern;

class BenchRegexFindEmptyEmoji {
    static int run() {
        Pattern p = Pattern.compile("");
        int sum = 0;
        for (int i = 0; i < 10000; i++) {
            Matcher m = p.matcher("\uD83D\uDE00");
            while (m.find()) {
                sum += m.start();
                sum += m.end();
            }
        }
        return sum;
    }
}
