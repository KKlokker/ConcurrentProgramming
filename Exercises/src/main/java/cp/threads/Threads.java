import java.util.ArrayList;

public class Threads {
    
    private static class Counter {
        static int i = 0;

        private synchronized static void increment() {
            i++;
        }

        private synchronized static void decrement() {
            i--;
        }
    }

    private final class UnmutableCounter {
        final private int i;

        private UnmutableCounter(int i) {
            this.i = i;
        }

        final private int value() {
            return i;
        }
        
    }
    public static void main(String[] args) {
        Counter.increment();
        Counter.decrement();
    }
}
