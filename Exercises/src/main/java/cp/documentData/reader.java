import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

public class reader {

    public static void main(String[] args){
        characterMaps();
    }

    public static void loremIpsum() {
        try ( Stream < String > lines = Files.lines(Paths.get("loremipsim.txt"))) {
            ArrayList<String> test = lines.flatMap(words -> Stream.of(words.split(" "))).filter(words -> words.startsWith("c")).collect(ArrayList<String>::new, (x, y) -> x.add(y), (x, y) -> x.addAll(y));
            test.stream().forEach(System.out::println);
        }
        catch ( IOException e ) {
                e.printStackTrace();
        }
    }

    public static void countLWords() {
        try ( Stream < String > lines = Files.lines(Paths.get("loremipsim.txt"))) {
            long lWords = lines.flatMap(words -> Stream.of(words.split(" "))).filter(words -> words.contains("l")).count();
            System.out.println("Words with L is " + lWords);
        }
        catch ( IOException e ) {
                e.printStackTrace();
        }
    }

    public static void countCs() {
        try ( Stream < String > lines = Files.lines(Paths.get("loremipsim.txt"))) {
            long Cs = lines.flatMap(words -> Stream.of(words.split(""))).mapToInt(character -> character.charAt(0) == 'c' ? 1 : 0).sum();
            System.out.println("C appears " + Cs);
        }
        catch ( IOException e ) {
                e.printStackTrace();
        }
    }

    public static void characterMaps() {
        try ( Stream < String > lines = Files.lines(Paths.get("loremipsim.txt"))) {
            lines.flatMap(words -> Stream.of(words.split("")))
                .map(word -> {Map<String, Integer> m = new HashMap<>(); m.put(word,1); return m;})
                .reduce( new HashMap< String, Integer >(), ( m1, m2 ) -> { Map< String, Integer > result = new HashMap<>(m1); m2.forEach( ( key, value ) -> result.merge( key, value, Integer::sum ) ); return result;} )
                .forEach((word,value) -> System.out.println(word + " -> " + value));
        }
        catch ( IOException e ) {
                e.printStackTrace();
        }
    }
}