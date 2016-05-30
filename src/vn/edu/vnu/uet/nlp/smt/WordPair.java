package vn.edu.vnu.uet.nlp.smt;

public class WordPair {
	private final int e;
	private final int f;
	private int hashCode = 0;

	public int getE() {
		return e;
	}

	public int getF() {
		return f;
	}

	public WordPair(final int _e, final int _f) {
		e = _e;
		f = _f;
		generateHashCode();
	}

	@Override
	public boolean equals(final Object o) {
		final WordPair key = (WordPair) o;
		return hashCode == key.hashCode;
	}

	public void generateHashCode() {
		StringBuilder tmp = new StringBuilder();
		tmp.append(e);
		tmp.append("|");
		tmp.append(f);
		hashCode = tmp.toString().hashCode();

	}

	@Override
	public int hashCode() {
		if (hashCode == 0) {
			generateHashCode();
		}
		return hashCode;
	}
}
