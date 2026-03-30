public class StringConcatSurrogateHalfTest {
    public static String run() {
        String emoji = "\uD83D\uDE00";
        String high = emoji.substring(0, 1);
        String low = emoji.substring(1, 2);
        String rebuilt = "" + high + low;
        return rebuilt.length()
            + "|" + (int) rebuilt.charAt(0)
            + "|" + (int) rebuilt.charAt(1)
            + "|" + rebuilt.equals(emoji);
    }
}
