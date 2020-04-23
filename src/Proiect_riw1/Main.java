package Proiect_riw1;

import java.io.IOException;
import org.apache.commons.io.FileUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Scanner;
import java.util.Set;

import com.mongodb.client.MongoDatabase; 
import com.mongodb.client.MongoCollection;
import com.mongodb.MongoClient; 
import com.mongodb.MongoCredential;
import com.mongodb.client.FindIterable;

public class Main {

	public static void main(String[] args) throws IOException{
		Main MainObj = new Main();
		String cale = "D:\\Scoala\\workspace_riw\\RIW1_v2";
		String[] extension = new String[] { "html", "htm", "txt" };
		//String pathHtmlFiles = cale + "/getHtmlFiles";
		String pathTestFiles = cale + "/test-files";
		File director = new File(pathTestFiles);
		boolean Ok = true;
		List<File> filesFromDirectory = (List<File>) FileUtils.listFiles(director, extension, Ok);
		String stopWords = new Scanner(new File(cale + "/stopwords.txt"))
				.useDelimiter("\\Z").next();
		System.out.println("S-au citit stop wordurile de la calea : "+cale + "/stopwords.txt");
		 //array-urile sunt goale
		int indexFisier = 0;
		String finalOutput = cale + "/Rezultat/FinalOutput.txt";//rezultate finale (INDEX-UL INDIRECT)
		String finalIDF = cale + "/Rezultat/FinalIDF.txt";
		String finalRez = cale + "/Rezultat/FinalRez.txt";
		for (File file : filesFromDirectory) {
			String pathFiles = cale + "/Rezultat/";
			String name1 = "In" + indexFisier;//generare nume fisier de intrare
			String name2 = "Output" + indexFisier;//generare nume fisier iesire
			File intrare = new File(pathFiles + name1 + ".txt");//generare fisier input
			File iesire = new File(pathFiles + name2 + ".txt");//generare fisier output

			PrintWriter writer1 = new PrintWriter(pathFiles + name1 + ".txt");//obiect pentru scriere in fisier input
			PrintWriter writer2 = new PrintWriter(pathFiles + name2 + ".txt");//obiect pentru scriere in fisier output (INDEXUL DIRECT)
			
			MainObj.putDataInText(file, writer1);//citirea datelor din fisierele html si scrierea intr-un txt de intrare
			MainObj.readDataFromFile(intrare, writer2, file,cale, stopWords);//citire cuvinte din fisier + generare index <document, cuvant, aparitii>
			
			PrintWriter output = new PrintWriter(finalOutput);//generare fisier final de iesire
			MainObj.IndirectIndex(iesire, output);//Numarare aparitie cuvinte
			indexFisier++;
		}
		PrintWriter fidf = new PrintWriter(finalIDF);
		PrintWriter fr = new PrintWriter(finalRez);
		MainObj.afisareIndex(finalOutput);
		File fileIDE = new File(finalOutput);
		getIDF(fileIDE, 25);
		// afisez cuvintele cu idf calculat
		showIDF(fidf);
		DeterminareVector(fr, fileIDE);
		//scan.close();
	}
	//afisare index indirect <cuvant, fisier, numar_aparitii>
	public void afisareIndex(String file)
	{
		BufferedReader reader;
		try {
		reader = new BufferedReader(new FileReader(file));
		String line = reader.readLine();
		while(line != null)
		{
			System.out.println(line);
			line = reader.readLine();
		}
		reader.close();
		}catch (IOException e) {
			e.printStackTrace();
		}
	}
	//citirea datelor din fisierele text si eliminarea stopwordurilor
	public void readDataFromFile(File file, PrintWriter output, File files,String cale, String stopWords) throws FileNotFoundException {
		HashMap<String, Integer> tabel = new HashMap<String, Integer>();
		//@SuppressWarnings("resource")
		try {
			Scanner scan = new Scanner(file);
			String words;
			while (scan.hasNext()) {
				words = scan.next();
				words = words.toLowerCase();
				//System.out.println(words);
				if (words.contains("\"") || words.contains(",") || words.contains(".") || words.contains("!")
						|| words.contains("?") || words.contains("/")) {
					words = words.replace(",", "");
				}
				words.replaceAll("\\p{Punct}", "");
				//System.out.println(words);
				if (stopWords.contains(words)) {
					words = "";
				}

				if (words != "") {
					String canonicWords = getCanonicForm(words);

					if (tabel.containsKey(canonicWords)) {
						tabel.put(canonicWords, tabel.get(canonicWords) + 1);
					} else {
						tabel.put(canonicWords, 1);
					}
				}
			}
			scan.close();
			afisare(output, tabel, files);
			output.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}

	// afisare cuvinte din directoare
	public void afisare(PrintWriter writer, HashMap<String, Integer> tabel, File file) {
		for (Map.Entry<String, Integer> entry : tabel.entrySet()) {
			writer.write(file.toString() + " ");
			writer.write(entry.getKey() + " ");
			writer.write(entry.getValue().toString());
			writer.write('\n');
		}
	}
	// aflam forma canonica din alg porter
	public String getCanonicForm(String word) {
		PorterAlgorithm porterAlg = new PorterAlgorithm();
		String p1 = porterAlg.step1(word);
		String p2 = porterAlg.step2(p1);
		String p3 = porterAlg.step3(p2);
		String p4 = porterAlg.step4(p3);
		String p5 = porterAlg.step5(p4);
		return p5;
	}

	//citirea datelor din fisierele html si punere in fisiere txt
	public void putDataInText(File file, PrintWriter writer) {
		try {
			Document doc = Jsoup.parse(file, "UTF-8");
			String title = doc.title();
			String text = doc.body().text();
			// keywords
			String keywords = doc.head().select("meta[name=keywords]").attr("content");
			// desc
				String description = doc.head().select("meta[name=description]").attr("content");			
			// luam toate linkurile din pagina
			//org.jsoup.select.Elements links = doc.getElementsByTag("a");
			writer.write(title);
			writer.write('\n');
			writer.write(keywords);
			writer.write('\n');
			writer.write(description);
			writer.write('\n');

			writer.write(text);
			writer.write('\n');

			writer.close();

		} catch (IOException e) {
			System.err.println(e);
			e.printStackTrace();
		}
	}
	//extragere cuvinte din fisier text
	private static HashMap<String, String> finalTable = new HashMap<String, String>();

	// fol pt formarea indexului indirect
	public void IndirectIndex(File file, PrintWriter output) throws IOException {
		BufferedReader br = new BufferedReader(new FileReader(file));
		String line = null;

		while ((line = br.readLine()) != null) {
			String[] words = line.split(" ");
			String text = words[0] + " : " + words[2] + " ";
			if (finalTable.containsKey(words[1])) {
				finalTable.put(words[1], finalTable.get(words[1]) + text);
			} else {
				finalTable.put(words[1], text);
			}
		}

		showWords(output, finalTable);
		output.close();
		br.close();
	}

	// afisez pe consola
	private static void showWords(PrintWriter output, HashMap<String, String> tableFinal2) {
		// Creating a Mongo client 
				//MongoDB STUFF ----------------------------------------------------------------
			      //MongoClient mongo = new MongoClient( "localhost" , 27017 );  
			      //MongoCredential credential; 
			      //credential = MongoCredential.createCredential("elPatron", "riw_cautare", 
			      // "password".toCharArray()); 
			      //System.out.println("Connected to the database successfully");  
			      //MongoDatabase database = mongo.getDatabase("riw_cautare"); 
			      //System.out.println("Credentials ::"+ credential);
			      //database.createCollection("ElColectiune"); 
			      //System.out.println("Collection created successfully"); 
			      //MongoCollection<org.bson.Document> collection = database.getCollection("ElColectiune"); 
			      //System.out.println("Collection ElColectiune selected successfully"); 
			    			//System.out.println("Document inserted successfully");
			    			//FindIterable<org.bson.Document> iterDoc = collection.find();
			    			//int i = 1;
			    			// Getting the iterator
			    			//Iterator it = iterDoc.iterator();
			    			//while (it.hasNext()) {
			    			//	System.out.println(it.next());
			    			//	i++;
			    			//}
			    //MongoDB STUFF ------------------------------------------------------------------
		for (Entry<String, String> entry : tableFinal2.entrySet()) {
			output.write(entry.getKey());
			output.write(" ");
			output.write(entry.getValue().toString());
			output.write('\n');
			 //org.bson.Document document = new org.bson.Document("titlu", "<cuvant, document, tf*idf>")
		    	//		.append("cuvantDoc", entry.getKey())
		    	//	.append("valoare", entry.getValue().toString());
		    			
		    			//Inserting document into the collection
		    	//		collection.insertOne(document);
		}

	}

	//hash utilizat pentru salvarea cuvantului cu idf-ul propriu
	static HashMap<String, Double> tableIDF = new HashMap<String, Double>();
	//calculare idf
	//pentru cuvintele care apar in toate fisierele html idf = 0
	public static void getIDF(File file, int nr_fisiere) throws IOException {

		FileReader fileReader = new FileReader(file);

		BufferedReader br = new BufferedReader(fileReader);

		String line = null;
		// daca nu mai sunt linii va returna null
		while ((line = br.readLine()) != null) {
			// citeste pana la EOF
			System.out.println(line);
			String[] words = line.split(" ");
			// cuvantul e primul
			String cuvant = words[0];
			int x = (words.length / 2);
			double rez = 0.0;
			if (nr_fisiere % x == 0) {
				if (nr_fisiere / x != 0)
					rez = Math.log(nr_fisiere / x);
				else
					rez = 0.0;
			} else {
				if (nr_fisiere / x != 0)
					rez = Math.log(nr_fisiere / (x - 1));
				else
					rez = 0.0;
			}

			if (!tableIDF.containsKey(cuvant)) {
				tableIDF.put(cuvant, rez);
			}
		}
		br.close();
	}
	//afisarea idf pe consola si inserarea intr-un fisier
	private static void showIDF(PrintWriter idf) {
		for (Entry<String, Double> entry : tableIDF.entrySet()) {
			System.out.println(entry.getKey() + " : " + entry.getValue().toString());
			idf.write(entry.getKey() + " : " + entry.getValue().toString() + "\n");			
		}
	}
	//not used yet
	//distanta cosinus intre un document si query
	//Vector-ul va fi <cuvant, document, valoare(tf*idf)>
	
	private static HashMap<String, String> finalVec = new HashMap<String, String>();
	private static void DeterminareVector(PrintWriter outvec, File inputDoc) throws IOException {
		BufferedReader br = new BufferedReader(new FileReader(inputDoc));
		String line = null;
		double pondere;
		Iterator iterator = tableIDF.entrySet().iterator();
		while ((line = br.readLine()) != null ) {//&& iterator.hasNext()
			Map.Entry valIDF = (Map.Entry) iterator.next();
			String[] words = line.split(" ");
			//String cuvant = words[3];
			//System.out.println(words.length + "\n");
			//System.out.println(cuvant + "\n");
			//String text = words[0] + " : " + words[2] + " ";
			//pondere = Integer.parseInt(words[3]) * Integer.parseInt((String)valIDF.getValue());
			int i=0;
			while(i < words.length)
			{
				if (i< 4) {
					System.out.println("Primul index: "+words[i] + " " + words[i+1] + " " + words[i+3]);
					System.out.println(" --> " + valIDF.getValue());
					pondere = Integer.parseInt(words[i+3]) * (double) valIDF.getValue();
					finalVec.put(words[i]+ " " + words[i+1], Double.toString(pondere));
					i += 4;	
				}
				else {
					System.out.println("Restul de indecsi: " + words[i] + " " + words[i+2]);
					pondere = Integer.parseInt(words[i+2]) * (double) valIDF.getValue();
					finalVec.put(words[0] + " " + words[1], Double.toString(pondere));
					i += 3;
				}
							
			}	
		}
		showWords(outvec, finalVec);
		outvec.close();
		br.close();
	}
	
	@SuppressWarnings("unused")
	private double calculateScore(List<Double> queryWeights, List<Double> documentWeights) {
        double upper = 0;
        double lower = 0;
        double queryWeightsLength = 0;
        double documentWeightsLength = 0;

        for (int i = 0; i < queryWeights.size(); ++i) {
            upper += queryWeights.get(i) * documentWeights.get(i);
            queryWeightsLength += queryWeights.get(i) * queryWeights.get(i);
            documentWeightsLength += documentWeights.get(i) * documentWeights.get(i);
        }

        queryWeightsLength = Math.sqrt(queryWeightsLength);
        documentWeightsLength = Math.sqrt(documentWeightsLength);
        lower = queryWeightsLength * documentWeightsLength;

        return upper / lower;
    }

}
