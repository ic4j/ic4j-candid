/*
 * Copyright 2021 Exilor Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
*/

package org.ic4j.candid.types;

import org.apache.commons.lang3.ArrayUtils;

public enum Type {
	NULL, BOOL, NAT, INT, NAT8, NAT16, NAT32, NAT64, INT8, INT16, INT32, INT64, FLOAT32, FLOAT64, TEXT, RESERVED, EMPTY,
	OPT, VEC, RECORD, VARIANT, FUNC, SERVICE, PRINCIPAL;

	
	public boolean isPrimitive()
	{
		Type[] primitives = {NULL,BOOL,NAT,INT,NAT8,NAT16,NAT32,NAT64,INT8,INT16,INT32,INT64,FLOAT32,FLOAT64,TEXT,RESERVED,EMPTY,PRINCIPAL};
		
		return ArrayUtils.contains(primitives, this);
	}	

}
