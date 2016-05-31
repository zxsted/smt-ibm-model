package vn.edu.vnu.uet.nlp.smt.structs;

public class SentencePair {
	private Sentence sentE;
	private Sentence sentF;
	WordPair[][] wordPairs;

	public SentencePair(Sentence e, Sentence f) {
		this.sentE = e;
		this.sentF = f;

		initWordPairs();
	}

	private void initWordPairs() {
		wordPairs = new WordPair[sentE.length() + 1][sentF.length() + 1];

		for (int j = 1; j <= sentE.length(); j++) {
			int e = sentE.get(j);
			for (int i = 1; i <= sentF.length(); i++) {
				int f = sentF.get(i);
				WordPair ef = new WordPair(e, f);
				wordPairs[j][i] = ef;
			}
		}
	}

	public Sentence getE() {
		return sentE;
	}

	public Sentence getF() {
		return sentF;
	}

	public WordPair getWordPair(int j, int i) {
		return wordPairs[j][i];
	}
}
