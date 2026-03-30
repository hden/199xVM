public class StringSurrogateHashAndInternTest {
    public static String run() {
        String emoji = "\uD83D\uDE00";
        String high = emoji.substring(0, 1);
        String same = new String(new char[] { high.charAt(0) });
        return emoji.hashCode() + "|" + high.hashCode() + "|" + (high.intern() == same.intern());
    }
}
