package vn.edu.vnu.uet.nlp.smt.structs;

import java.io.Serializable;

public class FertWord implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = -7819423512017604821L;
	private final int fert;
	private final int f;
	private int hashCode;

	public int getFert() {
		return fert;
	}

	public int getF() {
		return f;
	}

	public FertWord(final int fert, final int f) {
		this.fert = fert;
		this.f = f;
		generateHashCode();
	}

	@Override
	public boolean equals(final Object o) {
		final FertWord key = (FertWord) o;
		return hashCode == key.hashCode;
	}

	public void generateHashCode() {
		hashCode = (fert + "|" + f).hashCode();
	}

	@Override
	public int hashCode() {
		return hashCode;
	}
}
