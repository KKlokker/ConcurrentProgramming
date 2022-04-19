import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

//Modified version of example original code by Fabrizio Montesi <fmontesi@imada.sdu.dk>
public class WalkExecutorFuture
{
	public static void main()
	{
		// word -> number of times it appears over all files
		Map< Path, FileInfo > occurrences = new HashMap<>();
		ExecutorService executor = Executors.newWorkStealingPool();
        CompletionService<Map< Path, FileInfo >> service = new ExecutorCompletionService<>(executor);
		try {
			List< Future< Map< Path, FileInfo > > > futures =
				Files.walk( Paths.get( "textFiles" ) )
					.filter( Files::isRegularFile )
					.map( filepath ->
						service.submit( () -> getFileInfo(filepath))
					)
					.collect( Collectors.toList() );
			for( Future< Map< Path, FileInfo > > future : futures ) {
				Map< Path, FileInfo > fileOccurrences = service.take().get();
				fileOccurrences.forEach( (file, info) -> {occurrences.merge( file, info, (info1, info2) -> {return info1;}); if(info.lines >= 10) executor.shutdownNow();} );
				
			} 
		} catch( InterruptedException | ExecutionException | IOException e ) {
			e.printStackTrace();
		}
		
		try {
			executor.shutdown();
			executor.awaitTermination( 1, TimeUnit.DAYS );
		} catch( InterruptedException e ) {
			e.printStackTrace();
		}
		
		occurrences.forEach( (file, info) -> System.out.println( file + ": " + info ) );
	}

    private static Map<Path, FileInfo> getFileInfo(Path filepath) {
        Map<Path, FileInfo> map = new HashMap<>();
        try {
            FileInfo data = new FileInfo(
                Files.lines(filepath).map(String::length).reduce(0,Integer::sum), 
                Files.lines(filepath).count(), 
                Files.lines(filepath).filter(line -> {return (line.length() == 0) ? false : line.charAt(0) == 'L';}).count()
            );
            map.put(filepath, data);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return map;
    }

    private static class FileInfo {
        public Integer size;
        public long lines, linesAndL;

        public FileInfo(Integer size, long lines, long linesAndL) {
            this.lines = lines;
            this.linesAndL = linesAndL;
            this.size = size;
        }

        public String toString() {
            return "size: " + size + " lines: " + lines + " lines starting with L: " + linesAndL;
        }
    }
}