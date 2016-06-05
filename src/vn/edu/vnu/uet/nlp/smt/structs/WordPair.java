/*******************************************************************************
 * Copyright [2016] [Nguyen Tuan Phong]
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package vn.edu.vnu.uet.nlp.smt.structs;

import java.io.Serializable;

import vn.edu.vnu.uet.nlp.smt.utils.Utils;

/**
 * @author tuanphong94
 *
 */
public class WordPair implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = -2546261036800247864L;

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
		this.hashCode = Utils.generateTwoIntegersHashCode(e, f);
	}

	public WordPair(final int e, final int f, final int hashCode) {
		this.e = e;
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
		final WordPair other = (WordPair) obj;
		return hashCode == other.hashCode;
	}

	@Override
	public int hashCode() {
		return hashCode;
	}
}
