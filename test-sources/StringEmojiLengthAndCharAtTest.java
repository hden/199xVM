public class StringEmojiLengthAndCharAtTest {
    public static String run() {
        String s = "\uD83D\uDE00";
        return s.length() + "|" + (int) s.charAt(0) + "|" + (int) s.charAt(1);
    }
}
