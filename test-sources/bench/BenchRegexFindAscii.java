import java.util.regex.Matcher;
import java.util.regex.Pattern;

class BenchRegexFindAscii {
    static int run() {
        Pattern p = Pattern.compile("needle");
        int sum = 0;
        for (int i = 0; i < 10000; i++) {
            Matcher m = p.matcher("prefix-needle-suffix");
            if (m.find()) {
                sum += m.start();
                sum += m.end();
            }
        }
        return sum;
    }
}
