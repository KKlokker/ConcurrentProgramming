import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

//Modified version of example original code by Fabrizio Montesi <fmontesi@imada.sdu.dk>
public class SynchronizedMap {
	private static AtomicInteger globalTotalN = new AtomicInteger(0);
	//Using a global variable makes the code easier. The downside to it, is there is more wait time among the threads on incrementing the value, due to locking,
	//where as each thread having a variable means less locking among the threads, until the final add up.
	public static void main(String[] args) {
		// word -> number of times that it appears over all files
		Map< String, Integer > occurrences = new HashMap<>();
		
		List< String > filenames = List.of(
					 			"text1.txt",
					  			"text2.txt"
							);
		
		CountDownLatch latch = new CountDownLatch( filenames.size() );
		AtomicInteger totalN = new AtomicInteger(0);
		filenames.stream()
		  	.map( filename -> new Thread( () -> {
				totalN.addAndGet(computeOccurrences( filename, occurrences ));
				latch.countDown();
			} ) )
			.forEach( Thread::start );

			try {
			  	latch.await();
				System.out.println("Total number of words starting with n:" + totalN);
				System.out.println("Total number of words starting with n global:" + globalTotalN);
			 	} catch( InterruptedException e ) {
					e.printStackTrace();
				}
			}
	
	private static int computeOccurrences( String filename, Map< String, Integer > occurrences )
		{
			AtomicInteger n = new AtomicInteger(0);
			try {
				Files.lines( Paths.get( filename ) )
					.flatMap( Words::extractWords )
					.map( String::toLowerCase )
					.forEach( s -> {
						if(Character.toUpperCase(s.charAt(0)) == 'N') {n.incrementAndGet(); globalTotalN.incrementAndGet();}
						synchronized( occurrences ) {
							occurrences.merge( s, 1, Integer::sum );
						}
					} );
				} catch( IOException e ) {
					e.printStackTrace();
				}
			return n.get();
		}
}	
