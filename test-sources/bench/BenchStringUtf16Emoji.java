class BenchStringUtf16Emoji {
    static int run() {
        String s = "a\uD83D\uDE00b\uD83D\uDE00c";
        int sum = 0;
        for (int i = 0; i < 10000; i++) {
            sum += s.length();
            sum += s.charAt(1);
            sum += s.charAt(2);
            sum += s.substring(1, 3).length();
            sum += s.indexOf("\uD83D\uDE00");
            sum += s.lastIndexOf('c');
        }
        return sum;
    }
}
