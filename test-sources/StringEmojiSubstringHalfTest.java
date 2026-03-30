public class StringEmojiSubstringHalfTest {
    public static String run() {
        String s = "\uD83D\uDE00";
        String high = s.substring(0, 1);
        String low = s.substring(1, 2);
        return high.length() + "|" + (int) high.charAt(0) + "|" + low.length() + "|" + (int) low.charAt(0);
    }
}
