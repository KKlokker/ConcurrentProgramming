import java.util.HashSet;
import java.util.Set;
public class PersonSet {

    public static void main(String[] args) {
        
        PersonSet set = new PersonSet();
        Thread t1 = new Thread(() -> {for(int i = 0; i < 100; i++) {set.addPerson(new Person());}});
        Thread t2 = new Thread(() -> {Set<Person> unsafeSet = set.unsafeGet(); System.out.println(unsafeSet.size());});
        t1.start();
        t2.start();
        try {
            t1.join();
            t2.join();
        }
        catch (InterruptedException e) { e.printStackTrace();}
    }
    private final Set<Person> mySet = new HashSet<Person>();
    
    public PersonSet() {

    }
    public synchronized void addPerson(Person p) {
        mySet.add(p);
    }

    public synchronized boolean containsPerson(Person p) {
        return mySet.contains(p);
    }

    public Set<Person> unsafeGet() {
        return mySet;
    }
}