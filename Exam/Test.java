public class Test {
    private static final int ROUNDS = 5;

    public static void main(String[] args) {
        String dataDir = "data";

        String[] allLinesArgs = {
                "allLines",
                dataDir,
        };

        String[] longestLineArgs = {
                "longestLine",
                dataDir,
        };

        String[] vowelsArgs = {
                "vowels",
                dataDir,
                "3",
        };

        String[] suffixArgs = {
                "suffix",
                dataDir,
                "um",
                "10",
        };

        System.out.println(" --- allLines --- ");

        System.out.println();
        test(() -> Exam.main(allLinesArgs));
        System.out.println();

        System.out.println(" --- longestLine --- ");

        System.out.println();
        test(() -> Exam.main(longestLineArgs));
        System.out.println();

        System.out.println(" --- vowels --- ");

        System.out.println();
        test(() -> Exam.main(vowelsArgs));
        System.out.println();

        System.out.println(" --- suffix --- ");

        System.out.println();
        test(() -> Exam.main(suffixArgs));
        System.out.println();
    }

    public static void test(Runnable runnable) {
        long startTime, elapsedMillis, totalMillis = 0;

        for (int i = 0; i < ROUNDS; i++) {
            System.out.println(" - RUN " + (i + 1) + " - ");
            System.out.println();

            startTime = System.currentTimeMillis();
            runnable.run();
            elapsedMillis = System.currentTimeMillis() - startTime;
            totalMillis += elapsedMillis;

            System.out.println();
            System.out.println(" - END RUN " + (i + 1) + " - ");
            System.out.println("Elapsed time (in milliseconds): " + elapsedMillis);
            System.out.println(" - END RUN " + (i + 1) + " - ");
            System.out.println();
        }

        System.out.println();
        System.out.println("TOTAL TIME (in milliseconds):\t" + totalMillis);
        System.out.println("AVERAGE TIME (in milliseconds):\t" + totalMillis / ROUNDS);
    }
}