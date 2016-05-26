package vn.edu.vnu.uet.nlp.smt;

public class SentencePair {
	private Sentence e;
	private Sentence f;

	public SentencePair(Sentence _e, Sentence _f) {
		e = _e;
		f = _f;
	}

	public Sentence getE() {
		return e;
	}

	public Sentence getF() {
		return f;
	}
}
