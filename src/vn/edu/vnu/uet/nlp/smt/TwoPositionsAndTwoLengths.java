package vn.edu.vnu.uet.nlp.smt;

public class TwoPositionsAndTwoLengths {
	private int i;
	private int j;
	private int le;
	private int lf;

	private int hashCode = 0;

	public TwoPositionsAndTwoLengths(final int i, final int j, final int le, final int lf) {
		this.i = i;
		this.j = j;
		this.le = le;
		this.lf = lf;
		generateHashCode();
	}

	public void generateHashCode() {
		StringBuilder tmp = new StringBuilder();
		tmp.append(i);
		tmp.append("|");
		tmp.append(j);
		tmp.append(",");
		tmp.append(le);
		tmp.append(",");
		tmp.append(lf);
		hashCode = tmp.toString().hashCode();
	}

	@Override
	public boolean equals(final Object obj) {
		final TwoPositionsAndTwoLengths key = (TwoPositionsAndTwoLengths) obj;
		return hashCode == key.hashCode;
	}

	@Override
	public int hashCode() {
		if (hashCode == 0) {
			generateHashCode();
		}
		return hashCode;
	}

	public int getI() {
		return i;
	}

	public int getJ() {
		return j;
	}

	public int getLe() {
		return le;
	}

	public int getLf() {
		return lf;
	}
}
