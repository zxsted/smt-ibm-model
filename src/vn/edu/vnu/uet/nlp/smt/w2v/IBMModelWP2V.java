package vn.edu.vnu.uet.nlp.smt.w2v;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import vn.edu.vnu.uet.nlp.smt.ibm.IBMModelAbstract;
import vn.edu.vnu.uet.nlp.smt.structs.Dictionary;
import vn.edu.vnu.uet.nlp.smt.structs.Sentence;
import vn.edu.vnu.uet.nlp.smt.structs.SentencePair;
import vn.edu.vnu.uet.nlp.smt.structs.WordPair;

public class IBMModelWP2V extends IBMModelAbstract {
	private WordPOSDistances wordDis;
	protected List<SentencePair> wpSentPairs;

	Dictionary enWPDict;
	Dictionary foWPDict;

	public IBMModelWP2V(String targetFile, String sourceFile, boolean usingNull) {
		super(targetFile, sourceFile, usingNull);
	}

	public IBMModelWP2V(String model) {
		super(model);
	}

	public IBMModelWP2V(String targetFile, String sourceFile, String trgWPFile, String srcWPFile, String trgW2V,
			String srcW2V, String weightFile) throws Exception {
		this(targetFile, sourceFile, false);

		enWPDict = new Dictionary(trgWPFile);
		foWPDict = new Dictionary(srcWPFile);
		initWPSentPairs(trgWPFile, srcWPFile);

		Set<WordPair> set = new HashSet<WordPair>();

		for (SentencePair p : wpSentPairs) {
			for (int j = 1; j <= p.getE().length(); j++) {
				for (int i = iStart; i <= p.getF().length(); i++) {
					WordPair ef = p.getWordPair(j, i);
					set.add(ef);
				}
			}
		}

		wordDis = new WordPOSDistances(srcW2V, trgW2V, weightFile, foWPDict, enWPDict, set);

	}

	private void initWPSentPairs(String enFile, String foFile) {
		wpSentPairs = new ArrayList<SentencePair>();

		try {
			BufferedReader enBr = Files.newBufferedReader(Paths.get(enFile), StandardCharsets.UTF_8);
			BufferedReader foBr = Files.newBufferedReader(Paths.get(foFile), StandardCharsets.UTF_8);

			String enLine, foLine;

			// int count = 0;
			while ((enLine = enBr.readLine()) != null && (foLine = foBr.readLine()) != null) {
				String[] enLineWords = enLine.split("\\s+");
				String[] foLineWords = foLine.split("\\s+");

				if (enLineWords.length > MAX_LENGTH || foLineWords.length > MAX_LENGTH
						|| enLineWords.length + foLineWords.length < 2) {
					continue;
				}

				int[] enArray = new int[enLineWords.length];
				int[] foArray = new int[foLineWords.length];

				for (int i = 0; i < enArray.length; i++) {
					enArray[i] = enWPDict.getIndex(enLineWords[i]);
				}

				for (int i = 0; i < foArray.length; i++) {
					foArray[i] = foWPDict.getIndex(foLineWords[i]);
				}

				wpSentPairs.add(new SentencePair(new Sentence(enArray), new Sentence(foArray, usingNull)));
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	@Override
	public void train() {
		printDataInfo();

		System.out.print("Initializing IBM Model 1...");
		long ss = System.currentTimeMillis();
		initTransProbs();
		initCountT();
		initTotalT();
		long ee = System.currentTimeMillis();
		long initTime = ee - ss;
		System.out.println(" [" + initTime + " ms]");

		System.out.println("Start training IBM Model 1...");
		try {
			mainLoop();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	protected void mainLoop() throws IOException {
		int eCnt = 0;
		for (int iter = 1; iter <= MAX_ITER_1; iter++) {
			System.out.print("Iteration " + iter);

			long start = System.currentTimeMillis();

			if (iter > 1) {
				initCountT();
				initTotalT();
			}

			for (int pInd = 0; pInd < sentPairs.size(); pInd++) {
				SentencePair p = sentPairs.get(pInd);
				SentencePair wp = wpSentPairs.get(pInd);

				if (p.getE().length() != wp.getE().length() || p.getF().length() != wp.getF().length()) {
					eCnt++;
					continue;
				}

				int le = p.getE().length();
				int lf = p.getF().length();
				double subTotal;

				for (int j = 1; j <= le; j++) {
					subTotal = 0;

					// compute normalization
					for (int i = iStart; i <= lf; i++) {
						WordPair ef = p.getWordPair(j, i);
						WordPair wef = wp.getWordPair(j, i);
						subTotal += t.get(ef) * wordDis.getFactor(wef);
					}

					// collect counts
					for (int i = iStart; i <= lf; i++) {
						int f = p.getF().get(i);
						WordPair ef = p.getWordPair(j, i);
						WordPair wef = wp.getWordPair(j, i);
						double c = t.get(ef) * wordDis.getFactor(wef) / subTotal;

						if (countT.containsKey(ef)) {
							countT.put(ef, countT.get(ef) + c);
						} else {
							countT.put(ef, c);
						}

						totalT[f] += c;
					}
				}
			}

			// estimate probabilities
			Set<WordPair> keySet = countT.keySet();

			for (WordPair ef : keySet) {
				double value = countT.get(ef) / totalT[ef.getF()];
				t.put(ef, value);
			}

			long end = System.currentTimeMillis();
			long time = end - start;

			System.out.println(" [" + time + " ms]");
		}

		System.out.println(eCnt);
	}

}
