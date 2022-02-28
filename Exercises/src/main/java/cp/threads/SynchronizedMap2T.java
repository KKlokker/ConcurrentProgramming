import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

//Modified version of example original code by Fabrizio Montesi <fmontesi@imada.sdu.dk>
// This is the example developed in the video 13-SynchronizedMap2T
public class SynchronizedMap2T {
	public static void main(String[] args) {
		Map< Character, Long > occurrences = initThread(Paths.get("text1.txt"));
		Map< Character, Long > occurrences2 = initThread(Paths.get("text2.txt"));
		System.out.println( "e -> " + occurrences2.get( 'e' ) );
		occurrences.forEach((c,v) -> {occurrences2.merge( c, v, Long::sum );});
		System.out.println( "e -> " + occurrences2.get( 'e' ) );
	}

	private static Map< Character, Long > initThread(Path textPath) {
		Map< Character, Long > occurrences = new HashMap<>();
		Thread t1 = new Thread( () ->
			countLetters( textPath, occurrences ) );
		t1.start();
		try {
			t1.join();
		} catch( InterruptedException e ) {
			e.printStackTrace();
		}
		return occurrences;
	}

	private static void countLetters( Path textPath, Map< Character, Long > occurrences) {
		try( Stream< String > lines = Files.lines( textPath ) ) {
			lines.forEach( line -> {
				for( int i = 0; i < line.length(); i++ ) {
					final char c = line.charAt( i );
						occurrences.merge( c, 1L, Long::sum );
				}
			} );
		} catch( IOException e ) {
			e.printStackTrace();
		}
	}
}