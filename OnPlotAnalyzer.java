import com.opencsv.*;						//библиотека для чтения csv файла
import edu.stanford.nlp.process.*;			//библиотека для приведения слов в начальную форму Stemmer
import picocli.CommandLine;					//библиотека picocli v3.9.5 для обработки параметров командной строки
import picocli.CommandLine.*;

import java.io.FileReader;
import java.io.IOException;

import java.util.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.concurrent.TimeUnit;

@Command(name = "OnPlotAnalyzer", header = "%n@|green Utility help|@")
class OnPlotAnalyzer implements Runnable{
	private static String director = "";	//имя режиссера
	private static String title = "";		//название фильма
	private static String filename = "";	//имя файла источника
	private static String genre = "";		//название жанра
	private static String country = "";		//название страны
	private static int mode = 0;			//режим работы программы:
						//режим 1:	--film_uniq_words		
						//			--director_uniq_words
						//			--country_uniq_words
						//			--genre_uniq_words
						//			сочетание режимов (director, country, genre)
						//режим 2:	--director_raiting
	private static boolean dirRating = false;
	private static boolean genRating = false;
	private static boolean conRating = false;
						//режим 3:

	//Чтение параметров командной строки
	@Option(names = {"-about"}, usageHelp = true, description = "Help menu")
    private boolean help;
    
    	
	@Option(names={"-f", "--filename"}, description="Path and name of file", required=true)
	void setFilename(String filename) { this.filename = filename;}
    
    //mode 1
	@Option(names = {"--director_uniq_words"}, arity = "1..*", description = "Director name")
    void setDirector(String dir[]) {
    	this.mode = 1;
    	for (int i = 0; i< dir.length; i++){
	    	if (i != 0){this.director += " ";}
    		this.director += dir[i];
    	}
    }
	
	@Option(names = {"--film_uniq_words"}, arity = "1..*", description = "Movie title")
    void setTitle(String mov[]) {
    	this.mode = 1;
    	for (int i = 0; i< mov.length; i++){
    		if (i != 0){this.title += " ";}
    		this.title += mov[i];
    	}
    }
    
    @Option(names={"--country_uniq_words"}, description="Сountry where the film was made")
	void setCountry(String country) {
		this.mode = 1; 
		this.country = country;
	}
    	
    @Option(names = {"--genre_uniq_words"}, arity = "1..*", description = "Movie genre")
    void setGenre(String gen[]) {
		this.mode = 1;
    	for (int i = 0; i< gen.length; i++){
    		if (i != 0){this.genre += " ";}
    		this.genre += gen[i];}
	}
	
	//mode 2
	@Option(names = {"--director_raiting"}, description = "Directors rating by the number of unique words")
    void setDirRating(boolean f){
    	this.mode = 2;
    	this.dirRating = true;
    }
    
    @Option(names = {"--genre_raiting"}, description = "Genres rating by the number of unique words")
    void setGenRating(boolean f){
    	this.mode = 2;
    	this.genRating = true;
    }
    
    @Option(names = {"--country_raiting"}, description = "Country rating by the number of unique words")
    void setConRating(boolean f){
    	this.mode = 2;
    	this.conRating = true;
    }
	
	
	public static void main(String[] args){
		CommandLine.run(new OnPlotAnalyzer(), args);
	}
		
	//mode 3
	@Option(names = {"--similar_directors"}, arity = "1..*", description = "List of directors in descending order of the degree of intersection of their vocabulary")
    void setDirec(String dir[]) {
    	this.mode = 3;
    	for (int i = 0; i< dir.length; i++){
	    	if (i != 0){this.director += " ";}
    		this.director += dir[i];
    	}
    }	
		
	/**
	 *	Запускает обработку параметров командной строки и саму утилиту.
	 */
	
	public void run(){
		if ((mode >= 1) && (mode <=3)){
			csvFileReader();
		}else{
			System.out.println("Program mode not specified!");
		}
	}
	
		
	/**
	 *	Организация чтения входного файла.
	 */
	
	public static void csvFileReader (){
		try (CSVReader csvReader = new CSVReader(new FileReader(filename));) {
			if ((csvReader.readNext()) != null){		//Чтение первой строки - заголовки столбцов обрабатываться не будут
				int numOfFileLines = 12023;
				int lineCounter = 0;	//количество обработанных строк файла
				int filmCounter = 0; 	//количество подошедших по запросу фильмов
				Map<String, Integer> uniqWords = new HashMap<String, Integer>();
				Map<String, Set<String>> records= new HashMap<String, Set<String>>();
	
				String[] values = null;
				Stemmer stemm = new Stemmer();
				/**
		 		 * Пока обрабатываются только первые 12023 строки файла (одна треть)
		 		 * т к стандартные библиотеки (в частности opencsv) не читаю далее этот файл корректно
		 		 */
		 		 
		 		System.out.print("Progress: 00%");
		 		System.out.write('\b');
		 		//System.out.write('\b');
		 		int percent = 0;
		 		boolean firstTen = true; 
		 		 
				while (((values = csvReader.readNext()) != null) && (lineCounter < numOfFileLines)) {
	  				if (values.length != 8){throw new IOException("Wrong CSV format, number of columns must be 8!");}
	  				
	  				switch (mode){
						case 1:	
							boolean flag = includePlotOrNot(values);
							if (flag){
								filmCounter++;
								uniqWords = includeString(values[7], uniqWords, stemm);
							}
							break;
						case 2:
						case 3: 
							records = filmsRating(values, records, stemm);
							break;
						default: System.out.println("Wrong mode! Please check your input!");
							break;
					}
					lineCounter++;
					
					//Отображение прогресса вычислений в процентах
					
					System.out.write('\b');
					if (percent >= 10){
						System.out.write('\b');
					}
					percent = (lineCounter * 100) / numOfFileLines;
					if ((percent == 10) && (firstTen)){
						System.out.write('\b');
						firstTen = false;
					}
					System.out.print(percent);	
					
			    }					
				System.out.println("%");
				
				switch (mode){
					case 1:	
						wordsRatingOut(uniqWords, filmCounter);
						break;
					case 2: 
						filmsRatingOut(records);
						break;
					case 3: 
						uniqWords = similarDirectorsRating(uniqWords, records);
						if (uniqWords.size() > 0){
							wordsRatingOut(uniqWords, filmCounter);
						}
						break;
					default: 
						break;
				}
    	
				
    		}		
		}catch(IOException e){
			System.out.println("Error! Can't parse file " + filename + "!");
			e.printStackTrace();
		}	
	}
		
	
	/** 
	 * Работы утилиты в режиме 1. Осуществляется при указании режимов:	
	 *		--film_uniq_words		
	 *		--director_uniq_words
	 *		--country_uniq_words
	 *		--genre_uniq_words
	 *		сочетание режимов (director, country, genre)
	 *
	 *	Определяет нужно ли включать текущую строку в рейтинг
	 */
	
	public static boolean includePlotOrNot(String[] values) throws IOException{
		
		int counter = 0;  		//Если counter = 1 выбран один режим из mode = 1, если counter > 1, то совокупность
		boolean lineControl = true;			//Этот флаг определяет нужно ли обрабатывать для рейтинга текущую строку
		if (title != ""){
			lineControl = false;				
			if (title.equals(values[1])){lineControl = true;}
		}else{
			if (director != ""){
				lineControl = false;				//Если имя режиссера задано, но в строке его нет, то строка не будет обработана для рейтинга
				String[] allDirectors = directorFieldParser(values[3]);	//Получение списка режиссеров в данной строке
										
				//for (int i = 0; i < allDirectors.length; i++){
				if (Arrays.stream(allDirectors).anyMatch(director::equals)){lineControl = true;}
				//}
				counter++;
			}
			if (lineControl){
				if (country != ""){
					lineControl = false;
					if (country.equals(values[2])){lineControl = true;}
				}
				counter++;
			}
			if (lineControl){
				if (genre != ""){
					lineControl = false;				
					String[] allGenres = genreFieldParser(values[5]);	//Получение списка стран в данной строке
										
					//for (int i = 0; i < allGenres.length; i++){
					if (Arrays.stream(allGenres).anyMatch(genre::equals)){lineControl = true;}
					//}
				}
				counter++;
			}
		}
		if (lineControl){		//Если все заданные параметры содержатся в строке, то она включается в рейтинг
			if (counter > 1){
				System.out.println("");
				System.out.print("Year:  " + values[0]);
				System.out.print("     Movie name:  " + values[1]);
				for (int q = values[1].length(); q < 40; q++){System.out.print(" ");}
				System.out.println("Director:  " + values[3]);
			}
		}
		return lineControl;
	}
	
	
	/**
	 *	Разбиваем поле plot на слова и включаем их в рейтинг.
	 *	Режим выводит рейтинг частоты употребления слов,
	 *	в соответствии с заданными критериями.
	 */
	 
	public static Map<String, Integer> includeString(String value, Map<String, Integer> uniqWords, Stemmer stemm){
		String[] plotField = prepForStemmer(value);	//Подготовка списка слов для обработки библиотекой Stemmer
		for (int j = 0; j<plotField.length; j++){		//Поочередная обработка слов
			String mstr = stemm.stem(plotField[j]);		
			if (uniqWords.keySet().contains(mstr)){
				int t = uniqWords.get(mstr);
				uniqWords.put(mstr, t + 1);
			}else{
				uniqWords.put(mstr, 1);
			}
		}
		return uniqWords;
	}
	
	
	/**
	 *	Выводит на экран результат работы в режимах 1 и 3 (mode==1 или mode==3).
	 *	Режим выводит рейтинг частоты употребления слов,
	 *	в соответствии с заданными критериями при mode == 1.
	 *  И 
	 */
	
	public static void wordsRatingOut(Map<String, Integer> uniqWords, int filmCounter){
        			
        //Сортировка по убыванию популярности
        LinkedList<Map.Entry<String, Integer>> list = new LinkedList<>(uniqWords.entrySet());
		Comparator<Map.Entry<String, Integer>> comparator = Comparator.comparing(Map.Entry::getValue);
		Collections.sort(list, comparator.reversed());
		
		String reqName = requestHeadline(); 	//Название режима, которое будет выведено в консоль
		
		System.out.println("\n" + reqName);
		if (mode == 1){System.out.println("Number of processed movies: " + filmCounter + "\n");}
		if (mode == 3){System.out.println("Input director: " + director + "\n");}
		for (int i = 0; i<list.size(); i++){
    		System.out.println(list.get(i));
        }
        if (mode == 1){System.out.println("\n" + "Words number: " + uniqWords.size());}
		//System.out.println("\n" + "Number of lines: " + counter);		//Количество обработанных строк файла записей
	}
	
	
	/** 
	 * Работы утилиты в режиме 2. Осуществляется при указании режимов:			
	 *		--director_raiting
	 *		--country_raiting
	 *		--genre_raiting
	 *		сочетание указанных выше режимов режимов
	 */
						
	public static Map<String, Set<String>> filmsRating(String[] values, Map<String, Set<String>> records, Stemmer stemm){
		String key = "";
		String[] allDirectors = directorFieldParser(values[3]);
		String[] allGenres = genreFieldParser(values[5]);
		int i = 0;
		int j = 0;
		do{							
			do{
				if ((dirRating) || (mode == 3)) {key = allDirectors[i];}
				if ((dirRating) && (genRating)){key += "#";}
				if (genRating) {key += allGenres[j];}
				if (conRating){
					if (key != ""){key += "#";}
					key += values[2];
				}
				Set<String> words = records.get(key);
				if (words == null){
					words = new HashSet<String>();
					records.put(key, words);
				}
		
				String[] plotField = prepForStemmer(values[7]);
				for (int w = 0; w<plotField.length; w++){		
					String mstr = stemm.stem(plotField[w]);	
					words.add(mstr);	
				}
				j++;
				key = "";
			}while(j < allGenres.length);
			j = 0;
			i++;
		}while(i < allDirectors.length);
		return records;
	}
	
	/**
	 *	Выводит на экран результат работы в режиме 2 (mode=2).
	 *	Режим выводит рейтинг по количеству уникальных слов
	 *	по режиссерам, странам, жанрам.
	 */
	
	public static void filmsRatingOut (Map<String, Set<String>> records){
		LinkedList<Map.Entry<String, Set<String>>> setList = new LinkedList<>(records.entrySet());
		LinkedList<Map.Entry<String, Integer>> list = new LinkedList<>();
		for (int i = 0; i<setList.size(); i++){
			String key = setList.get(i).getKey();
			Set<String> words = new HashSet<String>(setList.get(i).getValue());
			Integer size = words.size();
			Map<String, Integer> temp = new HashMap<String, Integer>();
			temp.put(key,size);
			for (Map.Entry <String, Integer> entry : temp.entrySet()){list.add(entry);}
        }
		
		Comparator<Map.Entry<String, Integer>> comparator = Comparator.comparing(Map.Entry::getValue);
		Collections.sort(list, comparator.reversed());
		
		String reqName = requestHeadline(); 	//Название режима, которое будет выведено в консоль
		System.out.println("\n" + reqName);
		if ((dirRating) && (!genRating) && (!conRating)){System.out.println("Total number of directors: " + setList.size() + "\n");}
		if ((!dirRating) && (genRating) && (!conRating)){System.out.println("Total number of genres: " + setList.size() + "\n");}
		if ((!dirRating) && (!genRating) && (conRating)){System.out.println("Total number of countries: " + setList.size() + "\n");}
		if (((dirRating) && (genRating)) || ((dirRating) && (conRating)) || ((genRating) && (conRating))){
			System.out.print("Total number of combinations ");
			String head = "";
			if (dirRating){
				System.out.print("director");
				head += "Director                                ";
			}
			if (genRating){
				if (dirRating){System.out.print("&");}
				System.out.print("genre");
				head += "Genre                                   ";
			}
			if (conRating){
				if ((dirRating) || (genRating)){System.out.print("&");}
				System.out.print("country");
				head += "Country                                 ";
			}
			System.out.println(": " + setList.size() + "\n");
			System.out.println(head + "Number");
		}
		
		for (int i = 0; i<list.size(); i++){
			Map.Entry<String, Integer> temp = list.get(i);
			String text = (String) temp.getKey();
			String[] textArr = text.split("#");
			for (int j = 0; j < textArr.length; j++){
				System.out.print(textArr[j]);
				for(int q = 0; q < (40-textArr[j].length()); q++){
					System.out.print(" ");
				}
			}
    		System.out.println(temp.getValue());
        }
	}
	
	
	/** 
	 * Работы утилиты в режиме 3. Осуществляется при указании режима:	
	 *		--similar_directors
	 * Возвращает карту с данными о пересечении словарных 
	 * запасов указанного режиссера со всеми остальными.
	 */
	
	public static Map<String,Integer> similarDirectorsRating(Map<String, Integer> uniqWords, Map<String, Set<String>> records){
		Set<String> words = new HashSet<String>();
		words = records.get(director);
		records.remove(director);
		
		if (words != null){
			Iterator recIt = records.entrySet().iterator();
    		while (recIt.hasNext()) {
    			int commWords = 0;
        		Map.Entry entry = (Map.Entry)recIt.next();
        		String curDirector = (String) entry.getKey();
        		Set<String> curWords = new HashSet<String>();
				curWords = records.get(curDirector);
        		curWords.retainAll(words);
        		commWords = curWords.size();
        		uniqWords.put((String) entry.getKey(), commWords);
       			recIt.remove();
    		}
    	}else{
    		System.out.println("\n" + "Error! This file does not contain the specified director!");
    	}
		return uniqWords;	
	}
	
	
	/**
	 * Разделяет входную строку с режиссерами на список режиссеров в конкретном фильме.
	 */

	public static String[] directorFieldParser(String source){			
		String str = source.replaceAll(" and ", ","); 		//Замена разделителей and и & на запятую для использования line.split(",");
		str = str.replaceAll(" & ", ","); 
		
		String[] dirExcepWords = {"II", ", Sr.", "Jr"};			//Список исключений, для корректного использования line.split(",");	
		for (int i = 0; i < dirExcepWords.length; i++){
			if (str.contains(", " + dirExcepWords[i])){
				str = str.replaceAll(", " + dirExcepWords[i], "# " + dirExcepWords[i]);
			}
		}
		String[] ans = str.split(",");
		
		//Удаление пробелов в начале и конце слова
		for (int i = 0; i<ans.length; i++){
			Character ch = ' ';
			ans[i] = extraCharRemove(ans[i], ch);
		}
		//После выполнения line.split(","); возвращаем на место запятые
		//не являющиеся разделителями.
		for (int i = 0; i < ans.length; i++){
			ans[i] = ans[i].replaceAll("#", ",");		
		}	
		return ans;
	}
	
	
	/**
	 * Разделяет входную строку с жанрами на список жанров в конкретном фильме.
	 */
	
	public static String[] genreFieldParser(String source){
		String str = source;
		str = extraCharRemove(str, '.');
		str = extraCharRemove(str, ',');
		str = str.replaceAll("(film genre)", "");
		str = str.replaceAll("/", ",");
		str = str.replaceAll("&", ",");
		
		String[] ans = str.split(",");
		
		//Удаление пробелов в начале и конце слова
		for (int i = 0; i<ans.length; i++){
			Character ch = ' ';
			ans[i] = extraCharRemove(ans[i], ch);
		}
		return ans;
	}

	/** 
	 * Преобразует поле plot для корректной обработки библиотекой Stemmer.
	 */
	public static String[] prepForStemmer(String value){
		Map<String, String> excepList = new HashMap<String, String>();
		excepList = plotExcepListGenerator();			/*Получение актуальных данных для обработки исключений
														при разделении строки поля plot на слова*/
		String str = value;
		for (Map.Entry<String, String> entry : excepList.entrySet()) {
			str = str.replaceAll(entry.getKey(), entry.getValue());         
    	}
		
		//StringBuilder использован для увеличения скорости работы метода toLowerCase
		StringBuilder plotField = new StringBuilder(str);
		for (int i = 0; i < plotField.length(); i++) {
   			char c = plotField.charAt(i);
   			plotField.setCharAt(i, Character.toLowerCase(c));
		}
		str = plotField.toString();
		String[] answer = str.split(" ");
		
		//Удаление кавычек в начале и конце слова
		for (int i = 0; i<answer.length; i++){
			Character ch = '\'';
			answer[i] = extraCharRemove(answer[i], ch);
		}
		return answer;
	}


	/**
	 *	Заполнение мапы с исключениями. Исключения помогают 
	 *  корректнее разбивать текст на отдельные слова.
	 */
	 
	 public static Map<String, String> plotExcepListGenerator(){
	 	Map<String, String> excepList = new HashMap<String, String>();

	 	excepList.put("n't", " not");
		excepList.put("'ll", " will");
		excepList.put("'d", " would");
		excepList.put("'ve", " have");
		excepList.put("'m", " am");
		excepList.put("'re", " are");
		
		excepList.put("' ", " ");
		excepList.put(" '", " ");
		excepList.put(" - ", " ");
		excepList.put("'s", "");
		excepList.put("’s", "");
		
		excepList.put("[^\\p{L}\\s\\-\\']", " ");		//Оставляет в строке только буквы различных алфавитов, тире и кавычки
		excepList.put("\\s+", " ");
		
		return excepList;
	 }
	

	/**
	 * Удаляет из начала и конца слова лишний символ,
	 * который мог остаться в процессе разделения строки на слова.
	 */

	public static String extraCharRemove(String source, Character ch){
		if (source.length() > 1){
			if ((source.charAt((source.length())-1)) == ch){
				source = source.substring(0, source.length()-2);
			}
			if (source.length() > 1){
				if (source.charAt(0) == ch){
					source = source.substring(1, source.length()-1);
				}
			}else{
				if (source.equals(ch.toString())){
					source = "";
				}
			}
		}else{
			if (source.equals(ch.toString())){
				source = "";
			}
		}
		return source;
	}

	/**
	 *Задает заголовок режима для вывода информации на экран
	 */
	 
	public static String requestHeadline (){
		String reqName = "";
		if (mode == 1){
			reqName = "Rating of unique words in movies by:" + "\n";
			if (title != ""){ reqName += "Movie title: " + title + "\n";}
			if (director != ""){ reqName += "Director: " + director + "\n";}
			if (country != ""){ reqName += "Origin: " + country + "\n";}
			if (genre != ""){ reqName += "Genre: " + genre + "\n";}
		}
		if (mode == 2){
			reqName = "Ranking by the number of unique words in plots." + "\n" + "Formed by:  ";
			if (dirRating){
				reqName += "directors";
			}
			if (genRating){
				if (dirRating){reqName +=", ";}
				reqName += "genres";
			}
			if (conRating){
				if ((dirRating) || (genRating)){reqName +=", ";}
				reqName += "countries";
			}
			reqName += "\n";
		}
		if (mode == 3){
			reqName = "List of directors in descending order of the degree of intersection of their vocabulary";
		}
		return reqName;
	}
}
