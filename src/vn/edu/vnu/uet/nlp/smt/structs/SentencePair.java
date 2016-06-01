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

/**
 * @author tuanphong94
 *
 */
public class SentencePair {
	private Sentence sentE;
	private Sentence sentF;
	WordPair[][] wordPairs;

	public SentencePair(Sentence e, Sentence f) {
		this.sentE = e;
		this.sentF = f;

		initWordPairs();
	}

	private void initWordPairs() {
		wordPairs = new WordPair[sentE.length() + 1][sentF.length() + 1];
		int iStart = 1;
		if (sentF.isForeign()) {
			iStart = 0;
		}

		for (int j = 1; j <= sentE.length(); j++) {
			int e = sentE.get(j);
			for (int i = iStart; i <= sentF.length(); i++) {
				int f = sentF.get(i);
				WordPair ef = new WordPair(e, f);
				wordPairs[j][i] = ef;
			}
		}
	}

	public Sentence getE() {
		return sentE;
	}

	public Sentence getF() {
		return sentF;
	}

	public WordPair getWordPair(int j, int i) {
		return wordPairs[j][i];
	}
}
