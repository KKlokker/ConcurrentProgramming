import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
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
public class ExamOld {
	// Do not change this method
	public static void main(String[] args) {
		//String[] test = new String[]{"allLines", "data"};
		String[] test = new String[]{"longestLine", "data"};
		//String[] test = new String[]{"vowels", "data", "7"};
		//String[] test = new String[]{"suffix", "data", "erd", "5"};
		args = test;
		checkArguments(args.length > 0,
				"You must choose a command: help, allLines, longestLine, vowels, or suffix.");
		switch (args[0]) {
			case "help":
				System.out.println(
						"Available commands: help, allLines, longestLine, vowels, or suffix.\nFor example, try:\n\tjava Exam allLines data");
				break;
			case "allLines":
				checkArguments(args.length == 2, "Usage: java Exam.java allLines <directory>");
				String tests = args[1];
				Utils.doAndMeasure(() -> {
				List<LocatedWord> uniqueWords = findWordsCommonToAllLines(Paths.get(tests));
				System.out.println("Found " + uniqueWords.size() + " words");
				uniqueWords.forEach( locatedWord ->
					System.out.println( locatedWord.word + ":" + locatedWord.filepath ) );
				});
				break;
			case "longestLine":
				checkArguments(args.length == 2, "Usage: java Exam.java longestLine <directory>");
				final String path = args[1];
				for(int i = 0; i < 10; i++) {
					Utils.doAndMeasure(() -> {
				Location location = longestLine(Paths.get(path));
				System.out.println("Line with highest number of letters found at " + location.filepath + ":" + location.line );
				});
			} 
				break;
			case "vowels":
				
				checkArguments(args.length == 3, "Usage: java Exam.java vowels <directory> <vowels>");
				int vowels = Integer.parseInt(args[2]);
				Path filePath = Paths.get(args[1]);
				Utils.doAndMeasure(() -> {
				Optional<LocatedWord> word = wordWithVowels(filePath, vowels);
				if(word.isPresent()) {
					LocatedWord locatedWord = word.get();
					System.out.println("Found " + locatedWord.word + " in " + locatedWord.filepath);
				}
				});
				
					break;
			case "suffix":
				checkArguments(args.length == 4, "Usage: java Exam.java suffix <directory> <suffix> <length>");
				String t1 = args[1]; String t2 = args[2];
				int length = Integer.parseInt(args[3]);
				Utils.doAndMeasure(() -> {
				List<LocatedWord> words = wordsEndingWith(Paths.get(t1),t2, length);
				if( words.size() > length ) {
					System.out.println( "WARNING: Implementation of wordsEndingWith computes more than " + length + " words!" );
				}
				});
				//words.forEach(loc -> System.out.println(loc.word + ":" + loc.filepath));
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
					AtomicBoolean isInit = new AtomicBoolean(false); //Used to make set equal to all words of first line
					Set<String> fileSet = ConcurrentHashMap.newKeySet();
					try {
						Files.lines(file)
						.parallel()
						.anyMatch(line -> {
							//Gets set of all words in lower case
							Set<String> wordSet = extractWords(line)
								.map( String::toLowerCase)
								.collect(Collectors.toSet());

							if(wordSet.size() != 0) { //Ignore if line is empty
								//Initialize set, by adding all words of first line
								if(!isInit.getAndSet(true)) fileSet.addAll(wordSet); 
								//If the set is 0 after init just stop reading lines from file
								else if(fileSet.size() == 0) return true; 
								//Make intersect between current words in common and words in line
								else fileSet.retainAll(wordSet); 
							}
							return false; //Check for another line
						});
					} catch (IOException e) {
						e.printStackTrace();
					}
					//create LocatedWord for every common word in file
					for(String word: fileSet) returnList.add(new LocatedWord(word, file)); 
				});
		} catch (IOException e) {
			e.printStackTrace();
		}
		return returnList;
	}

	/**
	 * Creates a stream of words from a string
	 * @param s the string of which the words are found
	 * @return A stream of words
	 */
	private static Stream< String > extractWords( String s ) {
		List< String > words = new ArrayList<>();
		
		int start = 0;
		int end = 0;
		while( end != s.length() ) {
			char ch = Character.toLowerCase(s.charAt(end));
			//If the character is a letter or number in ascii
			if(!(96 < ch && ch < 123 || 47 < ch && ch < 58)){
				if(end != start) words.add(s.substring(start, end));
				end++;
				start = end;
			}
			else end++;
		}
		if(end != start) words.add(s.substring(start, end));
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
		LocationLength fileLength = new LocationLength(-1, 0, Paths.get(""));
		try {
			Files
				.walk(dir)
				.parallel()
				.filter(file -> file.toString().endsWith(".txt"))
				.forEach(file -> {
					try {
						//Finding the longest line in each file
						LocationLength sentenceLength = new LocationLength(-1,0,Paths.get(""));
						Counter lineCounter = new Counter(); 
						//Executor used for parrellizing lines, but still making the task in order to get line numbering
						ExecutorService service = Executors.newWorkStealingPool();
						Files.lines(file)
							.forEach((line) -> {
								//The line number must be saved before submitted, otherwise wrong line numbers are used
								int lineNumber = lineCounter.incrementAndGet(); 
								service.submit(() -> lengthAdd(sentenceLength, line, lineNumber, file));
							});
						service.shutdown();
						//Here the executor has 1 day for shutdown, this is for very big use cases, but has to be high due to unknown use
						service.awaitTermination(1,TimeUnit.DAYS);
						//Test if the files longest line is bigger than the current biggest
						synchronized(fileLength) {
							trySwitch(fileLength,sentenceLength);
						}
					} catch (IOException | InterruptedException e) {
						e.printStackTrace();
					}
				});
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		return fileLength.loc;
	}

	/**
	 * Function for taking the current longest line, if the current given line is bigger then updating the longest line
	 * @param currentBest Current longest line found
	 * @param line The string of the line
	 * @param lineNumber The line number in the file
	 * @param file The path to the file of which the line exist
	 */
	private static void lengthAdd(LocationLength currentBest, String line, int lineNumber, Path file) {
		//Rough filtering of lines which are smaller before filtering
		if(line.length() < currentBest.length) return;
		
		int count = 0;
		//Count each character which is a letter using ascii
		for(int i = 0; i < line.length(); i++) {
			char ch = Character.toLowerCase(line.charAt(i));
			if(96 < ch && ch < 123) count++;
		}
		LocationLength lineLoc = new LocationLength(count,lineNumber,file);
		synchronized(currentBest) {
			trySwitch(currentBest,lineLoc);
		}
	}
		
	/**
	 * Method for exhanging two locationLength in case one is bigger 
	 * or equal and the new LocationLength file is lexicographically lower
	 * or if file name is also the same the lowest line number
	 * @param oldLocLength The old LocationLength
	 * @param newLocLength The new LocationLength
	 */
	private static void trySwitch(LocationLength oldLocLength, LocationLength newLocLength) {
		if(
			//The new is bigger
			oldLocLength.length < newLocLength.length || 
			
			//The new is equal but file name precedes
			oldLocLength.length == newLocLength.length && 
			newLocLength.loc.filepath.compareTo(oldLocLength.loc.filepath) < 0 ||

			//Length and file name equal but line number is lower
			oldLocLength.length == newLocLength.length &&
			oldLocLength.loc.filepath.equals(newLocLength.loc.filepath) &&
			oldLocLength.loc.line > newLocLength.loc.line
		) {
			oldLocLength.length = newLocLength.length;
			oldLocLength.loc = newLocLength.loc;
		}
	}

	/**
	 * Class used for storing a location of a line and its length
	 */
	private static class LocationLength {
		private int length;
		private Location loc;

		/**
		 * Constructor which creates Location for the LocationLength object
		 * @param length Length of the line
		 * @param lineNumber Line number of line 
		 * @param file File of which the line exist
		 */
		LocationLength(int length, int lineNumber, Path file) {
			this.length = length;
			this.loc = new Location(file, lineNumber);
		}
	}

	/**
	 * Counter class used for counting line numbers. 
	 * Is used to prevent problems with non final integers.
	 */
	private static class Counter {
		int count;

		private Counter() {
			count = 1;
		}

		private int incrementAndGet() {
			return count++;
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
		//A list was used as a wrapper class for parsing to the stream, due to LocatedWord's attributes being final
		List<LocatedWord> list = new ArrayList<>();
		try {
			//Find txt files
			boolean isFound = Files.walk(dir)
				.parallel()
				.filter(file -> file.toString().endsWith(".txt"))
				.anyMatch(file -> {
					try {
						//Find matching lines
						return Files.lines(file)
							.parallel()
							.flatMap(ExamOld::extractWords)
							.filter(word -> word.length() >= vowels)
							.anyMatch(word -> {
								return numberOfVowels(word,vowels,file,list);									
							});
					} catch (IOException e) {
						e.printStackTrace();
					}
					return false;
			});
			if(isFound) found = Optional.of(list.get(0));
		} catch (IOException e) {
			e.printStackTrace();
		}
		return found;
	}

	/**
	 * Method for checking if a word contains the correct number of vowels and adds it the LocatedWord list
	 * @param word The word of checking
	 * @param vowels The number of vowels which should be in the word
	 * @param file The file of which the word exist
	 * @param list The list of which holds found words
	 * @return true if the words meets the criteria of vowels
	 */
	private static boolean numberOfVowels(String word, int vowels,Path file, List<LocatedWord> list) {
		int count = 0;
		//For loops which count every occurence of a vowel, if it exceeds then it simply returns false
		for (int i=0 ; i<word.length(); i++){
			char ch = Character.toLowerCase(word.charAt(i));
			if(ch == 'a'|| ch == 'e'|| ch == 'i' ||ch == 'o' ||ch == 'u'){
			   count ++;
			}
			if(count > vowels) return false;
		 }
		 //If they match it is returned true and the LocatedWord is inserted into the optional
		 if(count == vowels) {
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
		List<LocatedWord> locations = new ArrayList<>();
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
							.flatMap(ExamOld::extractWords)
							//Filter words which are smaller than suffix
							.filter(word -> word.length() >= suffix.length())
							//Filter all words without suffix 
							.filter(word -> word.endsWith(suffix))
							.anyMatch(word -> {
								synchronized(locations) {
									//If the limit is not hit the word is added
									if(locations.size() < limit) {
										locations.add(new LocatedWord(word, file));
										//If limit is reached then return true and stop stream
										if(locations.size() == limit)
											return true;										
									}
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
		return locations;
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