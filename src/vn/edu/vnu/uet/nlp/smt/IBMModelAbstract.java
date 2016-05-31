package vn.edu.vnu.uet.nlp.smt;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import gnu.trove.map.hash.TObjectDoubleHashMap;

public abstract class IBMModelAbstract {
	NumberFormat formatter = new DecimalFormat("#0.0000");

	List<SentencePair> sentPairs;

	Dictionary enDict;
	Dictionary foDict;

	TObjectDoubleHashMap<WordPair> t;

	TObjectDoubleHashMap<WordPair> count;
	double[] total;

	static final int MAX_ITER_1 = 3;
	static final int MAX_ITER_2 = 3;
	protected boolean CONVERGE = false;

	public IBMModelAbstract(String enFile, String foFile) {
		System.out.println("Reading training data...");

		enDict = new Dictionary(enFile);
		foDict = new Dictionary(foFile);

		initSentPairs(enFile, foFile);

		initTransProbs();

		initCount();
		initTotal();
	}

	public IBMModelAbstract(String model) {
		File fol = new File(model);

		if (!fol.isDirectory()) {
			System.err.println(model + " is not a folder! Cannot load model!");
			return;
		}

		if (!model.endsWith("/")) {
			model = model + File.pathSeparator;
		}

		// Load translation probabilities
		String tFileName = model + IConstants.transProbsModelName;
		String enFileName = model + IConstants.enDictName;
		String foFileName = model + IConstants.foDictName;

		try {
			t = Utils.loadObject(tFileName);
			enDict = Utils.loadObject(enFileName);
			foDict = Utils.loadObject(foFileName);
		} catch (IOException | ClassNotFoundException e) {
			e.printStackTrace();
		}

	}

	public abstract void train();

	protected void initCount() {
		if (count == null) {
			count = new TObjectDoubleHashMap<WordPair>();

			for (SentencePair p : sentPairs) {
				for (int j = 1; j <= p.getE().length(); j++) {
					for (int i = 1; i <= p.getF().length(); i++) {
						WordPair ef = p.getWordPair(j, i);
						count.put(ef, 0.0);
					}
				}
			}

		} else {
			Set<WordPair> keySet = count.keySet();

			for (WordPair ef : keySet) {
				count.put(ef, 0.0);
			}
		}
	}

	protected void initTotal() {
		total = new double[foDict.size()];
	}

	private void initTransProbs() {
		t = new TObjectDoubleHashMap<WordPair>();

		// uniform distribution
		double value = 1 / (double) enDict.size();

		for (SentencePair p : sentPairs) {
			for (int j = 1; j <= p.getE().length(); j++) {
				for (int i = 1; i <= p.getF().length(); i++) {
					WordPair ef = p.getWordPair(j, i);
					t.put(ef, value);
				}
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

				if (enArray.length > 100 || foArray.length > 100) {
					continue;
				}

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
		return getProb(enDict.getIndex(enWord), foDict.getIndex(foWord));
	}

	public double getProb(int e, int f) {
		WordPair ef = new WordPair(e, f);
		if (t.contains(ef)) {
			return t.get(ef);
		}
		return 0;
	}

	public Dictionary getEngDict() {
		return enDict;
	}

	public Dictionary getForeignDict() {
		return foDict;
	}

	public void printTransProbs() {
		if (enDict.size() > 10 || foDict.size() > 10) {
			return;
		}

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

	public void printDictsInfo() {
		System.out.println("English dictionary size: " + enDict.size());
		System.out.println("Foreign dictionary size: " + foDict.size());
	}

	public void save(String folder) throws IOException {
		File fol = new File(folder);
		if (!fol.exists()) {
			fol.mkdir();
		}

		if (!fol.isDirectory()) {
			System.err.println(folder + " is not a folder! Cannot save model!");
			return;
		}

		if (!folder.endsWith("/")) {
			folder = folder + File.pathSeparator;
		}

		// Save translation probabilities
		String tFileName = folder + IConstants.transProbsModelName;
		Utils.saveObject(t, tFileName);

		// Save english dictionary
		String enFileName = folder + IConstants.enDictName;
		Utils.saveObject(enDict, enFileName);

		// Save foreign dictionary
		String foFileName = folder + IConstants.foDictName;
		Utils.saveObject(foDict, foFileName);
	}
}
