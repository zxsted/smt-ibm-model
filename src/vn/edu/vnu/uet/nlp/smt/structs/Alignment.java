package vn.edu.vnu.uet.nlp.smt.structs;

public class Alignment {
	int[] a;
	int[] phi;
	double probability;
	String toString;
	int hashCode;

	public Alignment(int[] a, int[] phi) {
		this.a = a;
		this.phi = phi;

		generateString();
		generateHashCode();
	}

	private void generateHashCode() {
		hashCode = toString.hashCode();
	}

	private void generateString() {
		StringBuilder sb = new StringBuilder();
		for (int i : a) {
			sb.append(i + " ");
		}

		toString = sb.toString().trim();
	}

	public Alignment(int[] a, int[] phi, double probability) {
		this(a, phi);
		this.probability = probability;
	}

	public int[] getA() {
		return a;
	}

	public int[] getPhi() {
		return phi;
	}

	public void setProbability(double probability) {
		this.probability = probability;
	}

	public double getProbability() {
		return probability;
	}

	@Override
	public String toString() {
		return toString;
	}

	@Override
	public int hashCode() {
		return hashCode;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Alignment other = (Alignment) obj;
		if (hashCode != other.hashCode)
			return false;
		return true;
	}
}
