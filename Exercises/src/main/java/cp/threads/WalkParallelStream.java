import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

//Modified version of example original code by Fabrizio Montesi <fmontesi@imada.sdu.dk>
public class WalkParallelStream
{
	public static void main()
	{
		try {
			Map<Path, FileInfo> occurrences =
				Files
					.walk( Paths.get( "textFiles" ) )
					.filter( Files::isRegularFile )
					.collect( Collectors.toList() )
					.parallelStream()
					.filter(filepath -> { 
						Long length = (long) 0;
						try {
							length = Files.lines(filepath).count();
						} catch (IOException e) {
							e.printStackTrace();
						}
						return length > 10;
				})
					.collect( Collectors.toMap(
						path -> path,
						path -> getFileInfo(path),
						(path1,path2) -> {return path1;}
					) );
			occurrences.forEach( (word, n) -> System.out.println( word + ": " + n ) );
		} catch( IOException e ) {
			e.printStackTrace();
		}
	}
	private static FileInfo getFileInfo(Path filepath) {
        try {
            return new FileInfo(
                Files.lines(filepath).map(String::length).reduce(0,Integer::sum), 
                Files.lines(filepath).count(), 
                Files.lines(filepath).filter(line -> {return (line.length() == 0) ? false : line.charAt(0) == 'L';}).count()
            );
			
        } catch (IOException e) {
            e.printStackTrace();
        }
		return null;
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