package vn.edu.vnu.uet.nlp.smt.structs;

public class Alignment {
	int[] a;
	int[] phi;

	public Alignment(int[] a, int[] phi) {
		this.a = a;
		this.phi = phi;
	}

	public int[] getA() {
		return a;
	}

	public int[] getPhi() {
		return phi;
	}
}
