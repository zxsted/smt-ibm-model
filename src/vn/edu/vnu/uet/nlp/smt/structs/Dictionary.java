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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import vn.edu.vnu.uet.nlp.smt.utils.IConstants;

/**
 * @author tuanphong94
 *
 */
public class Dictionary implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = -2971140367051267737L;

	private Map<String, Integer> dict;
	private Map<Integer, String> reverseDict;

	private boolean isForeign;

	public Dictionary(String filename, boolean isForeign) {
		this.isForeign = isForeign;
		dict = new HashMap<String, Integer>();
		reverseDict = new HashMap<Integer, String>();

		if (isForeign) {
			dict.put(IConstants.NULLTOKEN, IConstants.NULLINDEX);
			reverseDict.put(IConstants.NULLINDEX, IConstants.NULLTOKEN);
		}

		BufferedReader br = null;

		try {
			br = Files.newBufferedReader(Paths.get(filename), StandardCharsets.UTF_8);
			int count = dict.size(); // 0 or 1
			for (String line; (line = br.readLine()) != null;) {
				if (line.isEmpty()) {
					continue;
				}

				String[] tokens = line.split("\\s+");
				for (String tok : tokens) {
					if (!dict.containsKey(tok)) {
						dict.put(tok, count);
						reverseDict.put(count, tok);
						count++;
					}
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void put(String word) {
		if (this.containsWord(word)) {
			return;
		} else {
			dict.put(word, dict.size());
			reverseDict.put(reverseDict.size(), word);
		}
	}

	public Dictionary(String enFile) {
		this(enFile, false);
	}

	public int getIndex(String word) {
		return dict.get(word);
	}

	public boolean containsWord(String word) {
		return dict.containsKey(word);
	}

	public String getWord(int index) {
		return reverseDict.get(index);
	}

	public boolean containsIndex(int index) {
		return reverseDict.containsKey(index);
	}

	public Map<String, Integer> getDict() {
		return dict;
	}

	public int size() {
		return dict.size();
	}

	public boolean isForeign() {
		return isForeign;
	}

	@Override
	public String toString() {
		return dict.toString();
	}
}
