import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.ArrayList;

public class ConcurrentMap
{
	public static void main() throws IOException
	{
        //Modified version of example original code by Fabrizio Montesi <fmontesi@imada.sdu.dk>
		// word -> number of times that it appears over all files
		Map< String, Integer > occurrences = new ConcurrentHashMap<>();
		
		List< String > filenames = new ArrayList<>();
		filenames.add("threads/text1.txt");
		filenames.add("threads/text2.txt");
		
		
		CountDownLatch latch = new CountDownLatch( filenames.size() );
		
		

		try {
			Files.walk( Paths.get( "textFiles" ) )
			.filter( Files::isRegularFile )
			.map( filename -> new Thread( () -> {
				computeOccurrences( filename, occurrences );
				latch.countDown();
			} ) )
			.forEach( Thread::start );
			latch.await();
			occurrences.forEach( (word, n) -> System.out.println( word + ": " + n ) );
		} catch( InterruptedException e ) {
			e.printStackTrace();
		}
		
//		occurrences.forEach( (word, n) -> System.out.println( word + ": " + n ) );
	}
	
	private static void computeOccurrences( Path filename, Map< String, Integer > occurrences )
	{
		try {
			Files.lines( filename )
				.flatMap( Words::extractWords )
				.map( String::toLowerCase )
                .filter(s -> {return s.charAt(0) == 'n';})
				.forEach( s -> {
					occurrences.merge( s, 1, Integer::sum );
				} );
		} catch( IOException e ) {
			e.printStackTrace();
		}
	}
}