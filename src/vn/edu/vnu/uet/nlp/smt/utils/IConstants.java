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
package vn.edu.vnu.uet.nlp.smt.utils;

/**
 * @author tuanphong94
 *
 */
public interface IConstants {
	static final String transProbsModelName = "trans_prob.smt";
	static final String alignmentModelName = "alignment.smt";
	static final String enDictName = "enDict.smt";
	static final String foDictName = "foDict.smt";
	static final String distortionModelName = "distortion.smt";
	static final String fertilityModelName = "fertility.smt";
	static final String nullInsertionModelName = "null.smt";

	static final String NULLTOKEN = "NUll";
	static final int NULLINDEX = 0;
}
