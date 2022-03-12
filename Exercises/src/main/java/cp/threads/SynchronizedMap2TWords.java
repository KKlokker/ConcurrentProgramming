import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

//Modified version of example original code by Fabrizio Montesi <fmontesi@imada.sdu.dk>
// This is the example developed in the video 15-SynchronizedMap2TWords
public class SynchronizedMap2TWords {
	public static void main(String[] args) {
		// word -> number of times that it appears over all files
		Map< String, Integer > occurrences = computeOccurrences(Stream.of("text1.txt", "text2.txt"));
		


		occurrences.forEach( (word, n) -> System.out.println( word + ": " + n ) );
	}

	private static Map<String, Integer> computeOccurrences(Stream<String> filenames) {
		Map<String, Integer> occurrences = new HashMap<>();
		filenames.parallel().forEach((filename) -> {
		try {
			Files.lines( Paths.get( filename ) ).flatMap( Words::extractWords ).map( String::toLowerCase ).forEach( s -> {
				synchronized( occurrences ) {
					occurrences.merge( s, 1, Integer::sum );
				}
			} );
		} catch( IOException e ) {
			e.printStackTrace();
		}
		});
		return occurrences;
	}
}