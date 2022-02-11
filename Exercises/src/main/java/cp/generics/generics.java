import java.util.ArrayList;
import java.util.Collections;

public class generics {

    public static void main(String[] args){
        ArrayList<String> aList = new ArrayList<String>();
        aList.add("Yay");
        aList.add("Wow");
        aList.add("No");
        aList.add("Try Again!");
        try {
            Box<ArrayList<String>> boxArray = new Box<ArrayList<String>>(aList);            
            ArrayList<String> sorted = boxArray.apply(new BoxFunction<ArrayList<String>,ArrayList<String>>() {
                public ArrayList<String> apply(ArrayList<String> unsortedList) {
                    Collections.sort(unsortedList);
                    return unsortedList;
                }
            });            
            for(String s: sorted)
                System.out.println(s);
        }
        catch(IllegalArgumentException e) {
            System.out.println("Whoopsie doopsie");
        }
        try {
            Box<ArrayList<String>> boxArray = new Box<ArrayList<String>>(aList);            
            int sumOfLengths = boxArray.apply(new BoxFunction<ArrayList<String>,Integer>() {
                public Integer apply(ArrayList<String> unsortedList) {
                    int sum = 0;
                    for(String s: unsortedList)
                        sum = sum + s.length();
                    return sum;
                }
            });            
            System.out.println(sumOfLengths);
        }
        catch(IllegalArgumentException e) {
            System.out.println("Whoopsie doopsie");
        }
    }

    
}

