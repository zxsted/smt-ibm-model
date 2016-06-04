package vn.edu.vnu.uet.nlp.smt.structs;

import java.io.Serializable;

import vn.edu.vnu.uet.nlp.smt.utils.Utils;

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
		Utils.generateTwoIntegersHashCode(fert, f);
	}

	public FertWord(final int fert, final int f, final int hashCode) {
		this.fert = fert;
		this.f = f;

		this.hashCode = hashCode;
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		final FertWord other = (FertWord) obj;
		return hashCode == other.hashCode;
	}

	@Override
	public int hashCode() {
		return hashCode;
	}
}
