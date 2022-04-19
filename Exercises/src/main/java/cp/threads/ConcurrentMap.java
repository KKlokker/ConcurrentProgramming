import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ConcurrentMap
{
	public static void main() 
	{
        //Modified version of example original code by Fabrizio Montesi <fmontesi@imada.sdu.dk>
		// word -> number of times that it appears over all files
		Map< String, Integer > occurrences = new ConcurrentHashMap<>();
		//In this case the fixedPool woul probably be more optimal with only 3 files to work with
		ExecutorService executor = Executors.newWorkStealingPool();
		
		//CountDownLatch latch = new CountDownLatch( filenames.size() );
		try {
			Files.walk( Paths.get( "textFiles" ) )
			.filter( Files::isRegularFile )
			/*.map( filename -> new Thread( () -> {
				computeOccurrences( filename, occurrences );
				latch.countDown();
			} ) )*/
			.forEach(filename -> {executor.submit(() -> {computeOccurrences( filename, occurrences );});});
		}
		catch(IOException e) { e.printStackTrace();}
		//latch.await();
		occurrences.forEach( (word, n) -> System.out.println( word + ": " + n ) );
		
		
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