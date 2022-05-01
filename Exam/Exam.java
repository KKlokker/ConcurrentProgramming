import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.BreakIterator;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/*
This is the exam for Concurrent Programming, Spring 2022.

Your task is to implement the following methods of class Exam:
- findWordsCommonToAllLines;
- longestLine;
- wordWithVowels;
- wordsEndingWith.

These methods search text files for particular words.
You must use a BreakIterator to identify words in a text file,
which you can obtain by calling BreakIterator.getWordInstance().
For more details on the usage of BreakIterator, please see the corresponding video lecture in the
course.

The implementations of these methods must exploit concurrency to achieve improved performance.

The only code that you can change is the implementation of these methods.
In particular, you cannot change the signatures (return type, name, parameters) of any method, and
you cannot edit method main.
The current code of these methods throws an UnsupportedOperationException: remove that line before
proceeding on to the implementation.

Original code by Fabrizio Montesi <fmontesi@imada.sdu.dk>
	
*/
public class Exam {
	// Do not change this method
	public static void main(String[] args) {
		checkArguments(args.length > 0,
				"You must choose a command: help, allLines, longestLine, vowels, or suffix.");
		switch (args[0]) {
			case "help":
				System.out.println(
						"Available commands: help, allLines, longestLine, vowels, or suffix.\nFor example, try:\n\tjava Exam allLines data");
				break;
			case "allLines":
				Utils.doAndMeasure(() -> {
				checkArguments(args.length == 2, "Usage: java Exam.java allLines <directory>");
				List<LocatedWord> uniqueWords = findWordsCommonToAllLines(Paths.get(args[1]));
				System.out.println("Found " + uniqueWords.size() + " words");
				uniqueWords.forEach( locatedWord ->
					System.out.println( locatedWord.word + ":" + locatedWord.filepath ) );
				});
				break;
			case "longestLine":
			Utils.doAndMeasure(() -> {
				checkArguments(args.length == 2, "Usage: java Exam.java longestLine <directory>");
				Location location = longestLine(Paths.get(args[1]));
				System.out.println("Line with highest number of letters found at " + location.filepath + ":" + location.line );
			});
				break;
			case "vowels":
			Utils.doAndMeasure(() -> {
				checkArguments(args.length == 3, "Usage: java Exam.java vowels <directory> <vowels>");
				int vowels = Integer.parseInt(args[2]);
				Optional<LocatedWord> word = wordWithVowels(Paths.get(args[1]), vowels);
				word.ifPresentOrElse(
						locatedWord -> System.out.println("Found " + locatedWord.word + " in " + locatedWord.filepath),
						() -> System.out.println("No word found with " + args[2] + " vowels." ) );
					});break;
			case "suffix":
				checkArguments(args.length == 4, "Usage: java Exam.java suffix <directory> <suffix> <length>");
				int length = Integer.parseInt(args[3]);
				List<LocatedWord> words = wordsEndingWith(Paths.get(args[1]), args[2], length);
				if( words.size() > length ) {
					System.out.println( "WARNING: Implementation of wordsEndingWith computes more than " + args[3] + " words!" );
				}
				words.forEach(loc -> System.out.println(loc.word + ":" + loc.filepath));
				break;
			default:
				System.out.println("Unrecognised command: " + args[0] + ". Try java Exam.java help.");
				break;
		}
	}

	// Do not change this method
	private static void checkArguments(Boolean check, String message) {
		if (!check) {
			throw new IllegalArgumentException(message);
		}
	}

	/**
	 * Returns the words that appear on every line of a text file contained in the given directory.
	 *
	 * This method recursively visits a directory to find text files contained in it
	 * and its subdirectories (and the subdirectories of these subdirectories,
	 * etc.).
	 *
	 * You must consider only files ending with a ".txt" suffix. You are guaranteed
	 * that they will be text files.
	 *
	 * The method should return a list of LocatedWord objects (defined by the class
	 * at the end of this file), where each LocatedWord object should consist of:
	 * - a word appearing in every line of a file
	 * - the path to the file containing such word.
	 *
	 * All words appearing on every line of some file must appear in the list: words
	 * that can be in the list must be in the list.
	 *
	 * Words must be considered equal without considering differences between
	 * uppercase and lowercase letters. For example, the words "Hello", "hEllo" and
	 * "HELLo" must be considered equal to the word "hello".
	 *
	 * @param dir the directory to search
	 * @return a list of words that, within a file inside dir, appear on every line
	 */
	private static List<LocatedWord> findWordsCommonToAllLines(Path dir) {
		List<LocatedWord> returnList = new ArrayList<>();
		try {
			Files
				.walk(dir)
				.parallel()
				.filter(file -> file.toString().endsWith(".txt"))
				.forEach(file -> {
					AtomicBoolean isInit = new AtomicBoolean(false);
					new ConcurrentHashMap<>();
					Set<String> fileSet = ConcurrentHashMap.newKeySet();
					try {
						Files.lines(file)
						.parallel()
						.forEach(line -> {
							Set<String> wordSet = extractWords(line)
								.map( String::toLowerCase)
								.collect(Collectors.toSet());
							if(wordSet.size() != 0) {
								if(!isInit.getAndSet(true)) fileSet.addAll(wordSet);
								else fileSet.retainAll(wordSet);
							}
						});
					} catch (IOException e) {
						e.printStackTrace();
					}
					for(String word: fileSet) returnList.add(new LocatedWord(word, file));
				});
		} catch (IOException e) {
			e.printStackTrace();
		}
		return returnList;
	}

	private static Stream< String > extractWords( String s ) {
		List< String > words = new ArrayList<>();
		
		BreakIterator it = BreakIterator.getWordInstance();
		it.setText( s );
		
		int start = it.first();
		int end = it.next();
		while( end != BreakIterator.DONE ) {
			String word = s.substring( start, end );
			if ( Character.isLetterOrDigit( word.charAt( 0 ) ) ) 
				words.add( word );
			start = end;
			end = it.next();
		}
		
		return words.stream();
	}
	/** Returns the line with the highest number of letters among all the lines
	 * present in the text files contained in a directory.
	 *
	 * This method recursively visits a directory to find all the text files
	 * contained in it and its subdirectories (and the subdirectories of these
	 * subdirectories, etc.).
	 *
	 * You must consider only files ending with a ".txt" suffix. You are
	 * guaranteed that they will be text files.
	 *
	 * The method should return the longest line (counting only letters) found among all text files.
	 * If multiple lines are identified as longest, the method should return
	 * the one that belongs to the file whose name precedes the filename of the other longest line
	 * lexicographically, or if the filename is the same, the line which comes first in the file.
	 * To compare strings lexicographically, you can use String::compareTo.
	 * See also https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/String.html#compareTo(java.lang.String)
	 *
	 * @param dir the directory to search
	 * @return the line with the highest number of letters found among all text files inside of dir
	 */
	private static Location longestLine(Path dir) {
		Location lineLocation = new Location(null, 0);
		int length = 0;
		List<LocationLength> fileLengths = new ArrayList<>();
		try {
			Files
				.walk(dir)
				.parallel()
				.filter(file -> file.toString().endsWith(".txt"))
				.forEach(file -> {
					try {
						List<LocationLength> fileSentences = new ArrayList<>();
						AtomicInteger lineNumber = new AtomicInteger(0); //TODO another wrapper object for integer may be more efficient
						ExecutorService service = Executors.newWorkStealingPool();
						
						Files.lines(file)
							.forEach((line) -> {
								service.submit(() -> lengthAdd(fileSentences, line, lineNumber.incrementAndGet(), file));
							});
						service.shutdown();
						service.awaitTermination(15, TimeUnit.SECONDS);

						fileLengths.add(fileSentences.get(0));
					} catch (IOException e) {
						e.printStackTrace();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				});

			for(int i = 0; i < fileLengths.size(); i++)
				if(
					fileLengths.get(i).length > length || 
					fileLengths.get(i).length == length && 
					fileLengths.get(i).loc.filepath.compareTo(lineLocation.filepath) < 0
				) {length = fileLengths.get(i).length; lineLocation = fileLengths.get(i).loc;}
			
		} catch (IOException e) {
			e.printStackTrace();
		}

		return lineLocation;
	}

	private static void lengthAdd(List<LocationLength> list, String line, int lineNumber, Path file) {
		line = line.replaceAll("[^a-zA-Z]", "");
		
		synchronized(list) {
			if(list.size() == 0) list.add(new LocationLength(line.length(), lineNumber, file));
			else if(
				line.length() > list.get(0).length || 
				line.length() == list.get(0).length && 
				list.get(0).loc.filepath.compareTo(file) < 0
			) {list.remove(0); list.add(new LocationLength(line.length(), lineNumber, file));}
		}
	
	}
		
	private static class LocationLength {
		private int length;
		private Location loc;

		LocationLength(int length, int lineNumber, Path file) {
			this.length = length;
			this.loc = new Location(file, lineNumber);
		}
	}

	/**
	 * Returns an Optional<LocatedWord> (see below) about a word found in the files
	 * of the given directory containing the given number of vowels.
	 *
	 * This method recursively visits a directory to find text files contained in it
	 * and its subdirectories (and the subdirectories of these subdirectories,
	 * etc.).
	 *
	 * You must consider only files ending with a ".txt" suffix. You are guaranteed
	 * that they will be text files.
	 *
	 * The method should return an (optional) LocatedWord object (defined by the
	 * class at the end of this file), consisting of:
	 * - the word found that contains as many vowels as specified by the parameter n (and no more);
	 * - the path to the file containing the word.
	 *
	 * You can consider a letter to be a vowel according to either English or Danish.
	 *
	 * If a word satisfying the description above can be found, then the method
	 * should return an Optional containing the desired LocatedWord. Otherwise, if
	 * such a word cannot be found, the method should return Optional.empty().
	 *
	 * This method should return *as soon as possible*: as soon as a satisfactory
	 * word is found, the method should return a result without waiting for the
	 * processing of remaining files and/or other data.
	 *
	 * @param dir the directory to search
	 * @param vowels the number of vowels the word must contain
	 * @return an optional LocatedWord about a word containing exactly n vowels
	 */
	private static Optional<LocatedWord> wordWithVowels(Path dir, int vowels) {
		Optional<LocatedWord> found = Optional.empty();
		List<LocatedWord> location = new ArrayList<>();
		ExecutorService service = Executors.newWorkStealingPool();
		new Thread(() -> {
			try {
				//Find txt files
				Files.walk(dir)
					.parallel()
					.filter(file -> file.toString().endsWith(".txt"))
					.anyMatch(file -> {
						try {
							//Find matching lines
							return Files.lines(file)
								.parallel()
								.flatMap(Exam::extractWords)
								.anyMatch(word -> {
									try {
										return service.submit(() -> numberOfVowels(word,vowels,file,location)).get();
									} catch (InterruptedException | ExecutionException e) {
										e.printStackTrace();
									}
									return false;								
								});
						} catch (IOException e) {
							e.printStackTrace();
						}
						return false;
				});
			} catch (IOException e) {
				e.printStackTrace();
			}
			service.shutdown();
			}).start();
		while(location.size() == 0 && !service.isTerminated()) {} //Spin lock
		if(location.size() != 0) found = Optional.of(location.get(0));
		return found;
	}

	private static boolean numberOfVowels(String word, int vowels,Path file, List<LocatedWord> list) {
		if(vowels == (word.length() - word.replaceAll("[AEIOUaeiou]", "").length())) {
			list.add(new LocatedWord(word, file));
			return true;
		 }
		 return false;
	}

	/** Returns a list of words found in the given directory ending with the given suffix.
	 *
	 * This method recursively visits a directory to find text files
	 * contained in it and its subdirectories (and the subdirectories of these
	 * subdirectories, etc.).
	 *
	 * You must consider only files ending with a ".txt" suffix. You are
	 * guaranteed that they will be text files.
	 *
	 * The method should return a list of LocatedWord objects (defined by the
	 * class at the end of this file), consisting of:
	 * - the word found that ends with the given suffix;
	 * - the path to the file containing the word.
	 *
	 * The size of the returned list must not exceed the given limit.
	 * Therefore, this method should return *as soon as possible*: if the list
	 * reaches the given limit at any point during the computation, no more
	 * elements should be added to the list and remaining files and/or other lines
	 * should not be analysed.
	 *
	 * @param dir the directory to search
	 * @param suffix the suffix to be searched for
	 * @param limit the size limit for the returned list
	 * @return a list of locations where the given suffix has been found
	 */
	private static List<LocatedWord> wordsEndingWith(Path dir, String suffix, int limit) {
		List<LocatedWord> location = new ArrayList<>();
		ExecutorService service = Executors.newWorkStealingPool();
		new Thread(() -> {
			try {
				//Find txt files
				Files.walk(dir)
					.parallel()
					.filter(file -> file.toString().endsWith(".txt"))
					.anyMatch(file -> {
						try {
							//Find matching lines
							return Files.lines(file)
								.parallel()
								.flatMap(Exam::extractWords)
								.anyMatch(word -> {
									try {
										return service.submit(() -> wordSuffix(word,suffix,file,location, limit)).get();
									} catch (InterruptedException | ExecutionException e) {
										e.printStackTrace();
									}
									return false;								
								});
						} catch (IOException e) {
							e.printStackTrace();
						}
						return false;
				});
			} catch (IOException e) {
				e.printStackTrace();
			}
			service.shutdown();
			}).start();
		while(location.size() < limit && !service.isTerminated()) {} //Spin lock
		System.out.println(location.size());
		return location;
	}

	private static boolean wordSuffix(String word, String suffix, Path file, List<LocatedWord> list, int limit) {
		if(word.length() >= suffix.length() && word.substring(word.length()-suffix.length()).equals(suffix)) {
			synchronized(list) {
				if(list.size() < limit) {
				list.add(new LocatedWord(word, file));
				return false;
				}
				else return true;
			}
		 }
		 return false;
	}


	// Do not change this class
	private static class LocatedWord {
		private final String word; // the word
		private final Path filepath; // the file where the word has been found

		private LocatedWord(String word, Path filepath) {
			this.word = word;
			this.filepath = filepath;
		}
	}

	// Do not change this class
	private static class Location {
		private final Path filepath; // the file where the word has been found
		private final int line; // the line number at which the word has been found

		private Location(Path filepath, int line) {
			this.filepath = filepath;
			this.line = line;
		}
	}

	// Do not change this class
	private static class InternalException extends RuntimeException {
		private InternalException(String message) {
			super(message);
		}
	}
}