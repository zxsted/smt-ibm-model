package vn.edu.vnu.uet.nlp.smt.extended;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import gnu.trove.map.hash.TObjectDoubleHashMap;
import vn.edu.vnu.uet.nlp.smt.ibm.IBMModel1;
import vn.edu.vnu.uet.nlp.smt.structs.Dictionary;
import vn.edu.vnu.uet.nlp.smt.structs.Sentence;
import vn.edu.vnu.uet.nlp.smt.structs.WordPair;

public abstract class IBMModelExtendAbstract extends IBMModel1 {

	double delta = 1E-4;

	List<LabeledSentencePair> labeledSentPairs;
	Set<WordPair> alignedWordPairs;

	// constructor for reusing
	public IBMModelExtendAbstract(String model) {
		super(model);
	}

	// constructor for training
	public IBMModelExtendAbstract(String enFile, String foFile, String labeledData) throws IOException {
		super(enFile, foFile);

		initLabeledSentPairs(labeledData);
		// initAlignedWordPairs();

		delta = (double) 1 / enDict.size();
	}

	protected double computeFMeasureOnLabeledData(List<Set<SingleAlignment>> guessedAlignment) {
		int numGuessed = 0, numGold = 0, numMutual = 0;

		for (int index = 0; index < guessedAlignment.size(); index++) {
			Set<SingleAlignment> guessed = guessedAlignment.get(index);
			Set<SingleAlignment> gold = labeledSentPairs.get(index).getAlignment();

			for (SingleAlignment a : guessed) {
				if (gold.contains(a)) {
					numMutual++;
				}
			}

			numGuessed += guessed.size();
			numGold += gold.size();
		}

		double precision = (double) numMutual / numGuessed;
		double recall = (double) numMutual / numGold;

		return 2 * precision * recall / (precision + recall);
	}

	protected void nomalizeT() {
		initTotalT();
		Set<WordPair> keySet = t.keySet();
		for (WordPair ef : keySet) {
			totalT[ef.getF()] += t.get(ef);
		}
		for (WordPair ef : keySet) {
			t.put(ef, t.get(ef) / totalT[ef.getF()]);
		}
	}

	protected List<Set<SingleAlignment>> predictAlignmentForLabeledData() {
		List<Set<SingleAlignment>> result = new ArrayList<Set<SingleAlignment>>();

		for (LabeledSentencePair pair : labeledSentPairs) {
			result.add(predictAlignment(pair));
		}

		return result;
	}

	private Set<SingleAlignment> predictAlignment(LabeledSentencePair pair) {
		Set<SingleAlignment> result = new HashSet<SingleAlignment>();

		int lf = pair.getF().length();
		int le = pair.getE().length();

		for (int j = 1; j <= le; j++) {
			double max_prob = 0.0;
			int max_i = 0;
			for (int i = iStart; i <= lf; i++) {
				double new_prob = getTransProb(pair.getE().get(j), pair.getF().get(i));
				if (new_prob > max_prob) {
					max_i = i;
					max_prob = new_prob;
				}
			}

			if (max_i > 0) {
				result.add(new SingleAlignment(max_i, j));
			}
		}

		return result;
	}

	protected TObjectDoubleHashMap<WordPair> computeTFromLabeledData() {
		TObjectDoubleHashMap<WordPair> result = new TObjectDoubleHashMap<WordPair>();
		TObjectDoubleHashMap<WordPair> count = new TObjectDoubleHashMap<WordPair>();
		TObjectDoubleHashMap<Integer> total = new TObjectDoubleHashMap<Integer>();

		for (LabeledSentencePair pair : labeledSentPairs) {
			Set<SingleAlignment> set = pair.getAlignment();
			for (SingleAlignment a : set) {
				WordPair ef = pair.getWordPair(a.getTrg(), a.getSrc());
				int f = ef.getF();
				if (count.containsKey(ef)) {
					count.put(ef, count.get(ef) + 1);
				} else {
					count.put(ef, 1);
				}

				if (total.containsKey(f)) {
					total.put(f, total.get(f) + 1);
				} else {
					total.put(f, 1);
				}
			}
		}

		Set<WordPair> keySet = count.keySet();
		for (WordPair ef : keySet) {
			double value = count.get(ef) / total.get(ef.getF());
			result.put(ef, value);
		}

		return result;
	}

	@SuppressWarnings("unused")
	private void initAlignedWordPairs() {
		alignedWordPairs = new HashSet<WordPair>();
		for (LabeledSentencePair pair : labeledSentPairs) {
			Set<SingleAlignment> set = pair.getAlignment();
			for (SingleAlignment a : set) {
				alignedWordPairs.add(pair.getWordPair(a.getTrg(), a.getSrc()));
			}
		}
	}

	private void initLabeledSentPairs(String labeledData) throws IOException {
		labeledSentPairs = new ArrayList<LabeledSentencePair>();
		int errCnt = 0;
		BufferedReader br = Files.newBufferedReader(Paths.get(labeledData), StandardCharsets.UTF_8);
		for (String foLine; (foLine = br.readLine()) != null;) {
			if (foLine.isEmpty()) {
				continue;
			}

			String enLine = br.readLine();
			if (enLine.isEmpty()) {
				continue;
			}

			String alignLine = br.readLine();
			if (alignLine.isEmpty()) {
				continue;
			}

			try {
				int[] foArray = buildIndexArray(foLine, foDict);
				int[] enArray = buildIndexArray(enLine, enDict);

				Sentence enSent = new Sentence(enArray);
				Sentence foSent = new Sentence(foArray);
				Set<SingleAlignment> align = buildAlignment(alignLine, foArray.length, enArray.length);
				if (align.isEmpty()) {
					continue;
				}

				labeledSentPairs.add(new LabeledSentencePair(enSent, foSent, align));
			} catch (Exception e) {
				errCnt++;
			}
		}

		System.out.println("Labeled data has " + errCnt + " error pairs.");
	}

	private Set<SingleAlignment> buildAlignment(String alignLine, int srcLength, int trgLength) {
		Set<SingleAlignment> align = new HashSet<SingleAlignment>();
		String[] toks = alignLine.split("\\s+");

		for (String tok : toks) {
			int index = tok.lastIndexOf(':');
			if (index < 0) {
				continue;
			}
			String srcs = tok.substring(0, index);
			String trgs = tok.substring(index + 1);

			String[] src = srcs.split(",");
			String[] trg = trgs.split(",");

			if (src.length > 1 && trg.length > 1) {
				return null;
			}

			for (String s : src) {
				if (s.isEmpty()) {
					continue;
				}
				int first = Integer.parseInt(s) + 1;
				if (first <= 0 || first > srcLength) {
					continue;
				}

				for (String t : trg) {
					if (t.isEmpty()) {
						continue;
					}
					int second = Integer.parseInt(t) + 1;
					if (second <= 0 || second > trgLength) {
						continue;
					}

					align.add(new SingleAlignment(first, second));
				}
			}
		}

		return align;
	}

	private int[] buildIndexArray(String line, Dictionary dict) {
		String[] toks = line.split("\\s+");
		int[] result = new int[toks.length];
		for (int i = 0; i < toks.length; i++) {
			String tok = toks[i];
			int index = tok.lastIndexOf(':');
			int manual_i = Integer.parseInt(tok.substring(index + 1));
			if (manual_i != i) {
				return null;
			}
			String word = tok.substring(0, index);
			if (dict.containsWord(word)) {
				result[i] = dict.getIndex(word);
			} else {
				// result[i] = -1;
				dict.put(word);
				result[i] = dict.getIndex(word);
			}
		}

		return result;
	}

	@Override
	public void printDataInfo() {
		super.printDataInfo();
		if (labeledSentPairs != null) {
			System.out.println("Number of labeled sentence pairs: " + labeledSentPairs.size());
		}
		if (alignedWordPairs != null) {
			System.out.println("Number of aligned word pairs: " + alignedWordPairs.size());
		}
	}
}
