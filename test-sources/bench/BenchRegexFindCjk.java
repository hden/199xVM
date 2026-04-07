import java.util.regex.Matcher;
import java.util.regex.Pattern;

class BenchRegexFindCjk {
    static int run() {
        Pattern p = Pattern.compile("\uD55C\u4E2D");
        int sum = 0;
        for (int i = 0; i < 10000; i++) {
            Matcher m = p.matcher("\u6F22\u3042\uD55C\u4E2D\u6587");
            if (m.find()) {
                sum += m.start();
                sum += m.end();
            }
        }
        return sum;
    }
}
