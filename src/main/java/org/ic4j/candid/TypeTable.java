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

package org.ic4j.candid;

import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Optional;
import java.util.Queue;
import java.util.HashMap;
import java.util.Map;
import java.nio.charset.StandardCharsets;

import org.ic4j.candid.types.Meths;
import org.ic4j.candid.types.Mode;
import org.ic4j.candid.types.Opcode;
import org.ic4j.candid.types.Type;
import org.ic4j.candid.types.Label;
import org.ic4j.candid.parser.IDLType;

public final class TypeTable {
	// Raw value of the type description table
	List<List<Long>> table;

	// Value types for deserialization
	Queue<Long> types;

	// The front of current_type queue always points to the type of the value we are
	// deserializing.
	Deque<Long> currentType;
	private final Map<Integer, IDLType> resolvedTypes = new HashMap<Integer, IDLType>();

	TypeTable(List<List<Long>> table, Queue<Long> types, Deque<Long> currentType) {
		this.table = table;
		this.types = types;
		this.currentType = currentType;

	}

	// Parse the type table and return the remaining bytes
	public static TypeTableResponse fromBytes(byte[] input) {
		return fromBytes(input, DecodingLimits.DEFAULT);
	}

	public static TypeTableResponse fromBytes(byte[] input, DecodingLimits limits) {
		return fromBytes(input, new DecodingBudget(limits));
	}

	static TypeTableResponse fromBytes(byte[] input, DecodingBudget budget) {
		if (input.length > budget.limits.maxMessageBytes)
			throw CandidError.create(CandidError.CandidErrorCode.CUSTOM, "Message size limit exceeded");

		List<List<Long>> table = new ArrayList<List<Long>>();

		Queue<Long> types = new LinkedList<Long>();

		Bytes bytes = new Bytes(input);

		bytes.parseMagic();

		int len = budget.readTypeCount(bytes);

		for (int i = 0; i < len; i++) {
			List<Long> buf = new ArrayList<Long>();

			Integer ty = bytes.leb128ReadSigned();
			
			buf.add(ty.longValue());
			if (ty == Opcode.OPT.value || ty == Opcode.VEC.value) {
				ty = bytes.leb128ReadSigned();
				validateTypeRange(ty, len);
				buf.add(ty.longValue());
			} else if (ty == Opcode.RECORD.value || ty == Opcode.VARIANT.value) {
				Integer objLen = budget.readTypeCount(bytes);
				buf.add(objLen.longValue());

				Optional<Long> prevHash = Optional.empty();

				for (int j = 0; j < objLen; j++) {
					Long hash = bytes.leb128Read();
					if (hash > 0xffffffffL)
						throw CandidError.create(CandidError.CandidErrorCode.CUSTOM, "Field id exceeds unsigned 32-bit range");

					if (prevHash.isPresent()) {
						if (prevHash.get() >= hash)
							throw CandidError.create(CandidError.CandidErrorCode.CUSTOM,
									"Field id collision or not sorted");
					}

					prevHash = Optional.of(hash);
					buf.add(hash);
					ty = bytes.leb128ReadSigned();
					validateTypeRange(ty, len);
					buf.add(ty.longValue());
				}
			}
			else if (ty == Opcode.FUNC.value) {
				Integer objLen = budget.readTypeCount(bytes);
				
				buf.add(objLen.longValue());
				
				for (int j = 0; j < objLen; j++) {
					ty = bytes.leb128ReadSigned();
					validateTypeRange(ty, len);
					buf.add(ty.longValue());
				}
				
				objLen = budget.readTypeCount(bytes);
				
				buf.add(objLen.longValue());
				
				for (int j = 0; j < objLen; j++) {
					ty = bytes.leb128ReadSigned();
					validateTypeRange(ty, len);
					buf.add(ty.longValue());
				}
				
				objLen = budget.readTypeCount(bytes);
				
				buf.add(objLen.longValue());
				
				if(objLen > 1)
					throw CandidError.create(CandidError.CandidErrorCode.CUSTOM,
							String.format("Function annotation length should be at most 1, received value is %d", objLen));
				
				if(objLen == 1)
				{
					int mode = bytes.parseByte();
					
					if(mode != 1 && mode != 2)
						throw CandidError.create(CandidError.CandidErrorCode.CUSTOM,
								String.format("Unknown annotation %d", mode));					
					
					buf.add((long) mode);
				}
			}
			else if (ty == Opcode.SERVICE.value) {
				Integer objLen = budget.readTypeCount(bytes);
				
				buf.add(objLen.longValue());
				
				for (int j = 0; j < objLen; j++) {
					Integer nameLen = bytes.readLength();
					budget.reserveTypeEntries(nameLen);
					
					buf.add(nameLen.longValue());
					
					byte[] nameBytes =bytes.parseBytes(nameLen);
				
					for(byte nameByte : nameBytes)
						buf.add((long) nameByte);
					
					ty = bytes.leb128ReadSigned();
					validateTypeRange(ty, len);
					buf.add(ty.longValue());
				}
			}
			else {
				throw CandidError.create(CandidError.CandidErrorCode.CUSTOM,
						String.format("Unsupported op_code %d in type table", ty));
			}

			table.add(buf);
		}

		len = budget.readTypeCount(bytes);
		budget.reserveElements(len);

		for (int i = 0; i < len; i++) {
			Integer ty = bytes.leb128ReadSigned();
			validateTypeRange(ty, table.size());
			types.add(ty.longValue());
		}

		TypeTable typeTable = new TypeTable(table, types, new LinkedList<Long>());

		TypeTableResponse response = new TypeTableResponse();

		response.typeTable = typeTable;

		response.data = new byte[bytes.data.remaining()];
		bytes.data.get(response.data);

		return response;

	}

	static boolean isPrimitiveType(int ty) {
		return (ty < 0 && (ty >= -17 || ty == -24));
	}

	static void validateTypeRange(int ty, int len) {
		if ((ty >= 0 && ty < len) || isPrimitiveType(ty))
			return;
		else
			throw CandidError.create(CandidError.CandidErrorCode.CUSTOM, String.format("Unknown type %d", ty));
	}

	Long popCurrentType() {

		Long type = this.currentType.pop();

		if (type != null)
			return type;
		else
			throw CandidError.create(CandidError.CandidErrorCode.CUSTOM, "Empty current_type");

	}

	Long peekCurrentType() {

		Long type = this.currentType.peek();

		if (type != null)
			return type;
		else
			throw CandidError.create(CandidError.CandidErrorCode.CUSTOM, "Empty current_type");

	}

	Opcode rawValueToOpcode(int value) {
		int prevValue = value;
		if (value >= 0 && value < this.table.size())
			value = this.table.get(value).get(0).intValue();
		
		
		if(value == Opcode.FUNC.value)
		{
			List<Integer> args = new ArrayList<Integer>();
			List<Integer> rets = new ArrayList<Integer>();;
			List<Mode> modes = new ArrayList<Mode>();
			
			int i = 1;
			Integer objLen = this.table.get(prevValue).get(i++).intValue();
			
			
			for (int j = 0; j < objLen; j++) {
				Integer ty = this.table.get(prevValue).get(i++).intValue();
				args.add(ty);
			}
			
			objLen = this.table.get(prevValue).get(i++).intValue();
			
			for (int j = 0; j < objLen; j++) {
				Integer ty = this.table.get(prevValue).get(i++).intValue();
				rets.add(ty);
			}
			
			objLen = this.table.get(prevValue).get(i++).intValue();
			
			if(objLen == 1)
			{	
				Mode mode = Mode.from(this.table.get(prevValue).get(i++).intValue());
				
				modes.add(mode);
			}
			
			return Opcode.func(args, rets, modes);
		}
		
		if(value == Opcode.SERVICE.value)
		{
			List<Meths> meths = new ArrayList<Meths>();
			
			int i = 1;
			Integer objLen = this.table.get(prevValue).get(i++).intValue();
						
			for (int j = 0; j < objLen; j++) {
				Integer nameLen = this.table.get(prevValue).get(i++).intValue();
				
				byte[] nameBytes = new byte[nameLen];
				
				for (int k = 0; k < nameLen; k++) 
					nameBytes[k] = this.table.get(prevValue).get(i++).byteValue();
					
				String name = new String(nameBytes);	
				
				Integer ty = this.table.get(prevValue).get(i++).intValue();
				
				meths.add(new Meths(name, ty));
			}	
			
			return Opcode.service(meths);
		}		

		return Opcode.from(value);
	}

	// Pop type opcode from the front of currentType.
	// If the opcode is an index (>= 0), we push the corresponding entry from table,
	// to currentType queue, and pop the opcode from the front.

	// Same logic as parseType, but not poping the currentType queue.

	Opcode parseType() {
		Long op = this.popCurrentType();

		if (op >= 0 && op < this.table.size()) {
			List<Long> ty = this.table.get(op.intValue());	

			ListIterator<Long> it = ty.listIterator(ty.size());
			
			if(ty.get(0) == Opcode.FUNC.value || ty.get(0) == Opcode.SERVICE.value)
				this.currentType.push(ty.get(0));
			else
				while (it.hasPrevious())			
					this.currentType.push(it.previous());

			op = this.popCurrentType();
		}

		return Opcode.from(op.intValue());
	}

	// Same logic as parseType, but not poping the currentType queue.
	Opcode peekType() {
		Long op = this.peekCurrentType();

		int value = op.intValue();
		if (value >= 0)
			value = this.table.get(value).get(0).intValue();
		return Opcode.from(value);
	}

	IDLType resolveType(int reference, DecodingBudget budget, int depth) {
		validateTypeRange(reference, this.table.size());
		if (reference < 0)
			return IDLType.createType(Type.from(reference));
		IDLType cached = this.resolvedTypes.get(reference);
		if (cached != null)
			return cached;
		if (depth >= budget.limits.maxDepth)
			throw CandidError.create(CandidError.CandidErrorCode.CUSTOM, "Type nesting limit exceeded");

		List<Long> entry = this.table.get(reference);
		IDLType result = IDLType.createType(Type.from(entry.get(0).intValue()));
		this.resolvedTypes.put(reference, result);
		int position = 1;
		switch (result.getType()) {
		case OPT:
		case VEC:
			result.setInnerType(resolveType(entry.get(position).intValue(), budget, depth + 1));
			break;
		case RECORD:
		case VARIANT:
			int fields = entry.get(position++).intValue();
			for (int field = 0; field < fields; field++) {
				Label label = Label.createIdLabel(entry.get(position++));
				result.getTypeMap().put(label, resolveType(entry.get(position++).intValue(), budget, depth + 1));
			}
			break;
		case FUNC:
			int arguments = entry.get(position++).intValue();
			for (int argument = 0; argument < arguments; argument++)
				result.args.add(resolveType(entry.get(position++).intValue(), budget, depth + 1));
			int returns = entry.get(position++).intValue();
			for (int returned = 0; returned < returns; returned++)
				result.rets.add(resolveType(entry.get(position++).intValue(), budget, depth + 1));
			int modes = entry.get(position++).intValue();
			for (int mode = 0; mode < modes; mode++)
				result.modes.add(Mode.from(entry.get(position++).intValue()));
			break;
		case SERVICE:
			int methods = entry.get(position++).intValue();
			for (int method = 0; method < methods; method++) {
				byte[] name = new byte[entry.get(position++).intValue()];
				for (int character = 0; character < name.length; character++)
					name[character] = entry.get(position++).byteValue();
				IDLType function = resolveType(entry.get(position++).intValue(), budget, depth + 1);
				if (function.getType() != Type.FUNC)
					throw CandidError.create(CandidError.CandidErrorCode.CUSTOM, "Service method must reference a function");
				result.getMeths().put(new String(name, StandardCharsets.UTF_8), function);
			}
			break;
		default:
			throw CandidError.create(CandidError.CandidErrorCode.CUSTOM, "Unsupported type table entry");
		}
		return result;
	}

	// Check if currentType matches the provided type
	void checkType(Opcode expected) {
		Opcode wireType = this.parseType();

		if (wireType != expected)
			throw CandidError.create(CandidError.CandidErrorCode.CUSTOM, String
					.format("Type mismatch. Type on the wire: %d; Expected type: %d", wireType.value, expected.value));
	}

}
