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

public class IBMModel1 {
	NumberFormat formatter = new DecimalFormat("#0.0000");

	List<SentencePair> sentPairs;

	Dictionary enDict;
	Dictionary foDict;

	double[][] t;
	double[][] count;
	double[] total;

	static final int MAX_ITER = 100;
	private boolean CONVERGE = false;

	public IBMModel1(String enFile, String foFile) {
		initSentPairs(enFile, foFile);

		enDict = new Dictionary(enFile);
		foDict = new Dictionary(foFile);

		initTransProbs();
	}

	public void execute() {
		System.out.println("Init:");
		printTransProbs();

		int iter = 1;
		while (!CONVERGE) {
			System.out.println("Iteration " + iter);

			initCount();
			initTotal();

			for (SentencePair p : sentPairs) {
				// compute normalization
				double[] subTotal = new double[p.getE().dictSize()];

				for (String e : p.getE().getWords()) {
					int indexE = enDict.getIndex(e);
					int subIndexE = p.getE().getIndexInDict(e);
					subTotal[subIndexE] = 0;

					for (String f : p.getF().getWords()) {
						int indexF = foDict.getIndex(f);
						subTotal[subIndexE] += t[indexE][indexF];
					}
				}

				// collect counts
				for (String e : p.getE().getWords()) {
					int indexE = enDict.getIndex(e);
					int subIndexE = p.getE().getIndexInDict(e);

					for (String f : p.getF().getWords()) {
						int indexF = foDict.getIndex(f);
						count[indexE][indexF] += t[indexE][indexF] / subTotal[subIndexE];
						total[indexF] += t[indexE][indexF] / subTotal[subIndexE];
					}
				}
			}

			// estimate probabilities
			for (int f = 0; f < foDict.size(); f++) {
				for (int e = 0; e < enDict.size(); e++) {
					t[e][f] = count[e][f] / total[f];
				}
			}

			printTransProbs();

			iter++;
			if (iter > MAX_ITER) {
				CONVERGE = true;
			}
		}
	}

	private void initCount() {
		count = new double[enDict.size()][foDict.size()];
	}

	private void initTotal() {
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
