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
package vn.edu.vnu.uet.nlp.smt.extended;

import vn.edu.vnu.uet.nlp.smt.utils.Utils;

public class SingleAlignment {
	int src;
	int trg;

	private int hashCode;

	public SingleAlignment(int src, int trg) {
		super();
		this.src = src;
		this.trg = trg;
		hashCode = Utils.generateTwoIntegersHashCode(src, trg);
	}

	public int getSrc() {
		return src;
	}

	public int getTrg() {
		return trg;
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
		SingleAlignment other = (SingleAlignment) obj;
		if (other.hashCode() != obj.hashCode())
			return false;
		return true;
	}

}
