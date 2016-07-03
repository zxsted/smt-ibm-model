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

import java.util.Arrays;

public class ViterbiAlignment {
	int[] a;
	int[] phi;
	double probability;
	int hashCode;

	public ViterbiAlignment(int[] a, int[] phi) {
		this.a = a;
		this.phi = phi;

		generateHashCode();
	}

	private void generateHashCode() {
		hashCode = Arrays.hashCode(a);
	}

	public ViterbiAlignment(int[] a, int[] phi, double probability) {
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
		ViterbiAlignment other = (ViterbiAlignment) obj;
		if (hashCode != other.hashCode)
			return false;
		return true;
	}
}
