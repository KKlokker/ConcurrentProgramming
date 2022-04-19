import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.stream.Collectors;
import java.util.stream.Stream;

//Modified version of example original code by Fabrizio Montesi <fmontesi@imada.sdu.dk>
public class WalkParallelStreamFindAny
{
	public static void main()
	{
		try {
			boolean found =
				Files
					.walk( Paths.get( "textFiles" ) )
					.filter( Files::isRegularFile )
					.collect( Collectors.toList() )
					.parallelStream()
					.anyMatch( file -> {
                        
                        try {
                            return Files.lines(file).count() > 10;
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        return false;
                    } );
			System.out.println( found );
		} catch( IOException e ) {
			e.printStackTrace();
		}
	}
}