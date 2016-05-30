package vn.edu.vnu.uet.nlp.smt;

public class OnePositionAndTwoLengths {
	private int j;
	private int le;
	private int lf;

	private int hashCode = 0;

	public OnePositionAndTwoLengths(int j, int le, int lf) {
		this.j = j;
		this.le = le;
		this.lf = lf;
		generateHashCode();
	}

	public void generateHashCode() {
		StringBuilder tmp = new StringBuilder();
		tmp.append(j);
		tmp.append(",");
		tmp.append(le);
		tmp.append(",");
		tmp.append(lf);
		hashCode = tmp.toString().hashCode();
	}

	@Override
	public int hashCode() {
		if (hashCode == 0) {
			generateHashCode();
		}
		return hashCode;
	}

	@Override
	public boolean equals(Object obj) {
		final OnePositionAndTwoLengths key = (OnePositionAndTwoLengths) obj;
		return hashCode == key.hashCode;
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
