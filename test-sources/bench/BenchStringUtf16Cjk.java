class BenchStringUtf16Cjk {
    static int run() {
        String s = "\u6F22\u3042\uD55C\u4E2D\u6587";
        int sum = 0;
        for (int i = 0; i < 10000; i++) {
            sum += s.length();
            sum += s.charAt(2);
            sum += s.substring(1, 4).length();
            sum += s.indexOf("\uD55C");
            sum += s.lastIndexOf('\u6587');
        }
        return sum;
    }
}
