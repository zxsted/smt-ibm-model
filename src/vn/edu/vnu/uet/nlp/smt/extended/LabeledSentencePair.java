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

import java.util.HashSet;
import java.util.Set;

import vn.edu.vnu.uet.nlp.smt.structs.Sentence;
import vn.edu.vnu.uet.nlp.smt.structs.SentencePair;

public class LabeledSentencePair extends SentencePair {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1790292055601262028L;

	private Set<SingleAlignment> alignment;

	public LabeledSentencePair(Sentence e, Sentence f) {
		super(e, f);
		this.alignment = new HashSet<SingleAlignment>();
	}

	public LabeledSentencePair(Sentence e, Sentence f, Set<SingleAlignment> a) {
		super(e, f);
		this.alignment = a;
	}

	public Set<SingleAlignment> getAlignment() {
		return alignment;
	}

}
