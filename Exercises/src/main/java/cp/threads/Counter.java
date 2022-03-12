public class Counter {
    private int i;

    public Counter() {
        i = 0;
    }

    public synchronized void syncronizedincrement() {
        i++;
    }

    public synchronized void decement() {
        i--;
    }

    public int value() {
        return i;
    }
}
