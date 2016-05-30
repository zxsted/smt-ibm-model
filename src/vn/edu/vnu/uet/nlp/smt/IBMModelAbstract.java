package vn.edu.vnu.uet.nlp.smt;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import gnu.trove.map.hash.TObjectDoubleHashMap;

public abstract class IBMModelAbstract {
	NumberFormat formatter = new DecimalFormat("#0.0000");

	List<SentencePair> sentPairs;

	Dictionary enDict;
	Dictionary foDict;

	TObjectDoubleHashMap<WordPair> t;
	TObjectDoubleHashMap<WordPair> count;
	Map<Integer, Double> total;

	static final int MAX_ITER_1 = 10;
	static final int MAX_ITER_2 = 10;
	protected boolean CONVERGE = false;

	public IBMModelAbstract(String enFile, String foFile) {
		System.out.println("Reading training data...");

		enDict = new Dictionary(enFile);
		foDict = new Dictionary(foFile);

		initSentPairs(enFile, foFile);

		initTransProbs();
	}

	public abstract void train();

	protected void initCount() {
		count = new TObjectDoubleHashMap<WordPair>();
	}

	protected void initTotal() {
		total = new HashMap<Integer, Double>();
	}

	private void initTransProbs() {
		t = new TObjectDoubleHashMap<WordPair>();

		// uniform distribution
		double value = 1 / (double) enDict.size();

		for (int e = 0; e < enDict.size(); e++) {
			for (int f = 0; f < foDict.size(); f++) {
				t.put(new WordPair(e, f), value);
			}
		}
	}

	private void initSentPairs(String enFile, String foFile) {
		sentPairs = new ArrayList<SentencePair>();

		try {
			BufferedReader enBr = Files.newBufferedReader(Paths.get(enFile), StandardCharsets.UTF_8);
			BufferedReader foBr = Files.newBufferedReader(Paths.get(foFile), StandardCharsets.UTF_8);

			String enLine, foLine;

			while ((enLine = enBr.readLine()) != null && (foLine = foBr.readLine()) != null) {
				String[] enLineWords = enLine.split("\\s+");
				String[] foLineWords = foLine.split("\\s+");

				int[] enArray = new int[enLineWords.length];
				int[] foArray = new int[foLineWords.length];

				for (int i = 0; i < enArray.length; i++) {
					enArray[i] = enDict.getIndex(enLineWords[i]);
				}

				for (int i = 0; i < foArray.length; i++) {
					foArray[i] = foDict.getIndex(foLineWords[i]);
				}

				sentPairs.add(new SentencePair(new Sentence(enArray), new Sentence(foArray)));
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	public double getProb(String enWord, String foWord) {
		return t.get(new WordPair(enDict.getIndex(enWord), foDict.getIndex(foWord)));
	}

	public double getProb(int e, int f) {
		return t.get(new WordPair(e, f));
	}

	public Dictionary getEngDict() {
		return enDict;
	}

	public Dictionary getForeignDict() {
		return foDict;
	}

	public void printTransProbs() {
		System.out.print("\t");
		for (int e = 0; e < enDict.size(); e++) {
			System.out.print(enDict.getWord(e) + "\t");
		}
		System.out.println();
		for (int f = 0; f < foDict.size(); f++) {
			System.out.print(foDict.getWord(f) + "\t");
			for (int e = 0; e < enDict.size(); e++) {
				System.out.print(formatter.format(getProb(e, f)) + "\t");
			}
			System.out.println();
		}
	}

	public void printDicts() {
		System.out.println("English dictionary:\n" + enDict.size());
		System.out.println("Foreign dictionary:\n" + foDict.size());
	}
}
