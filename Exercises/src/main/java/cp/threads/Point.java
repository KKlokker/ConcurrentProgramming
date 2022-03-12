public class Point {
    public Counter count1, count2;

    public Point() {
        count1 = new Counter();
        count2 = new Counter();
    }

    //Could client side lockign be possible, if the counters were private?
    //No the locking would stil lonly effect the point and its counters, and not the count itself.
    public boolean areEqual() {
        synchronized(count1) {
            synchronized(count2) {
                return count1 == count2;
            }
        }
    }
}
