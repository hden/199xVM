class BenchStringUtf16Ascii {
    static int run() {
        String s = "alphabet-soup";
        int sum = 0;
        for (int i = 0; i < 10000; i++) {
            sum += s.length();
            sum += s.charAt(3);
            sum += s.substring(2, 5).length();
            sum += s.indexOf("bet");
            sum += s.lastIndexOf('o');
        }
        return sum;
    }
}
