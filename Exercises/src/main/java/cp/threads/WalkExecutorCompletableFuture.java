import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

//Modified version of example original code by Fabrizio Montesi <fmontesi@imada.sdu.dk>
public class WalkExecutorCompletableFuture
{
	public static CompletableFuture<Void>[] futures;
	public static void main()
	{
		Map< Path, FileInfo > occurrences = new ConcurrentHashMap<>();
		try {
			futures = 
				Files.walk( Paths.get( "textFiles" ) )
					.filter( Files::isRegularFile )
					.map(file -> CompletableFuture
						.supplyAsync(() -> getFileInfo(file))
						.thenAccept(map -> 
							map.forEach((path, info) -> 
								occurrences.merge(path,info,(info1,info2)->
									{return info1;})
							)
						)
					)
					.collect( Collectors.toList()).toArray(new CompletableFuture[0] );
			
			CompletableFuture.allOf(futures).join();
		} catch(IOException e ) {
			e.printStackTrace();
		}
		
		
		occurrences.forEach( (file, info) -> System.out.println( file + ": " + info ) );
	}

    private static Map<Path, FileInfo> getFileInfo(Path filepath) {
		Map<Path, FileInfo> map = new HashMap<>();
        try {
			Long length = Files.lines(filepath).count();
            FileInfo data = new FileInfo(
                Files.lines(filepath).map(String::length).reduce(0,Integer::sum), 
                length, 
                Files.lines(filepath).filter(line -> {return (line.length() == 0) ? false : line.charAt(0) == 'L';}).count()
            );
			if(length >= 10) {
				CompletableFuture.allOf(futures).completeExceptionally(new InterruptedException());
			}
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