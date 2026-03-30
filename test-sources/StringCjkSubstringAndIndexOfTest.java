public class StringCjkSubstringAndIndexOfTest {
    public static String run() {
        String s = "\u6F22\u3042\uD55C\u4E2D\u6587";
        return s.substring(1, 4) + "|" + s.indexOf("\uD55C") + "|" + s.lastIndexOf('\u6587');
    }
}
