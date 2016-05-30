package vn.edu.vnu.uet.nlp.smt;

public class WordPair {
	private final int e;
	private final int f;
	private int hashCode;

	public int getE() {
		return e;
	}

	public int getF() {
		return f;
	}

	public WordPair(final int e, final int f) {
		this.e = e;
		this.f = f;
		generateHashCode();
	}

	@Override
	public boolean equals(final Object o) {
		final WordPair key = (WordPair) o;
		return hashCode == key.hashCode;
	}

	public void generateHashCode() {
		hashCode = (e + "|" + f).hashCode();
	}

	@Override
	public int hashCode() {
		return hashCode;
	}
}
