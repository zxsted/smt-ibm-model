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

import java.util.ArrayList;
import java.util.List;

public class Sentence {
	private List<Integer> content;

	public Sentence(int[] array) {
		content = new ArrayList<Integer>(array.length);

		for (int i = 0; i < array.length; i++) {
			content.add(array[i]);
		}
	}

	public int get(int index) {
		return content.get(index - 1);
	}

	public int length() {
		return content.size();
	}
}