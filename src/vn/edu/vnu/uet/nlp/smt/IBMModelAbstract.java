package vn.edu.vnu.uet.nlp.smt;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;

public abstract class IBMModelAbstract {
	NumberFormat formatter = new DecimalFormat("#0.0000");

	List<SentencePair> sentPairs;

	Dictionary enDict;
	Dictionary foDict;

	double[][] t;
	double[][] count;
	double[] total;

	static final int MAX_ITER_1 = 10;
	static final int MAX_ITER_2 = 100;
	protected boolean CONVERGE = false;

	public IBMModelAbstract(String enFile, String foFile) {
		initSentPairs(enFile, foFile);

		enDict = new Dictionary(enFile);
		foDict = new Dictionary(foFile);

		initTransProbs();
	}

	public abstract void train();

	protected void initCount() {
		count = new double[enDict.size()][foDict.size()];
	}

	protected void initTotal() {
		total = new double[foDict.size()];
	}

	private void initTransProbs() {
		t = new double[enDict.size()][foDict.size()];

		// uniform distribution
		double value = 1 / (double) enDict.size();

		for (int i = 0; i < enDict.size(); i++) {
			for (int j = 0; j < foDict.size(); j++) {
				t[i][j] = value;
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
				sentPairs.add(new SentencePair(new Sentence(enLine), new Sentence(foLine)));
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	public double getProb(String enWord, String foWord) {
		return t[enDict.getIndex(enWord)][foDict.getIndex(foWord)];
	}

	public Dictionary getEngDict() {
		return enDict;
	}

	public Dictionary getForeignDict() {
		return foDict;
	}

	public void printTransProbs() {
		System.out.print("\t");
		for (int i = 0; i < enDict.size(); i++) {
			System.out.print(enDict.getWord(i) + "\t");
		}
		System.out.println();
		for (int j = 0; j < foDict.size(); j++) {
			System.out.print(foDict.getWord(j) + "\t");
			for (int i = 0; i < enDict.size(); i++) {
				System.out.print(formatter.format(t[i][j]) + "\t");
			}
			System.out.println();
		}
	}

	public void printDicts() {
		System.out.println("English dictionary:\n" + enDict);
		System.out.println("Foreign dictionary:\n" + foDict);
	}
}
