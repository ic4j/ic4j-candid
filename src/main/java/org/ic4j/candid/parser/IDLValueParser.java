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

package org.ic4j.candid.parser;

import java.io.InputStream;
import java.io.Reader;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.ic4j.candid.parser.ParserError.ParserErrorCode;
import org.ic4j.candid.parser.idl.value.IDLValueGrammar;
import org.ic4j.candid.parser.idl.value.ParseException;
import org.ic4j.candid.parser.idl.value.SimpleNode;
import org.ic4j.candid.types.Label;

import org.ic4j.candid.types.Type;
import org.ic4j.IC4JLogging;

public class IDLValueParser {
	static final String IDENTIFIER_NODE_NAME = "Identifier";
	
	static final String STRING_NAME = "String";
	static final String INTEGER_NAME = "Integer";
	static final String FLOAT_NAME = "Float";
	static final String BOOLEAN_NAME = "Boolean";
	static final String NULL_NAME = "Null";
	static final String SIMPLE_VALUE_NAME = "SimpleValue";
	
	static final String BLOB_VALUE_NAME = "BlobValue";
	static final String VEC_VALUE_NAME = "VecValue";
	static final String OPT_VALUE_NAME = "OptValue";
	static final String RECORD_VALUE_NAME = "RecordValue";
	static final String VARIANT_VALUE_NAME = "VariantValue";
	static final String RECORD_SUB_VALUE_NAME = "RecordSubValue";
	static final String VARIANT_SUB_VALUE_NAME = "VariantSubValue";
	static final String ARGUMENTS_NODE_NAME = "Arguments";
	static final String VALUE_NODE_NAME = "Value";

	static IC4JLogging LOG = IC4JLogging.getIC4JLogger(IDLValueParser.class);
	IDLValueGrammar idlValueGrammar;
	List<IDLValue> args = new ArrayList<IDLValue>();

	public IDLValueParser(Reader reader) {
		this.idlValueGrammar = new IDLValueGrammar(reader);
	}

	public IDLValueParser(InputStream inputStream) {
		this.idlValueGrammar = new IDLValueGrammar(inputStream);
	}

	public void parse() throws ParserError {
		try {
			SimpleNode node = this.idlValueGrammar.Start();

			// get values
			List<SimpleNode> valueNodes = this.getChildNodes((SimpleNode) node.jjtGetChild(0).jjtGetChild(0),
					VALUE_NODE_NAME);

			for (SimpleNode valueNode : valueNodes) {

					IDLValue idlValue = this.getIDLValue(((SimpleNode) valueNode.jjtGetChild(0)));

					if (idlValue != null) {
						this.args.add(idlValue);
					}
			}

		} catch (ParseException e) {
			throw ParserError.create(ParserErrorCode.PARSE, e);
		}
	}

	private IDLValue getIDLValue(SimpleNode valueNode) {
		switch (valueNode.toString()) {
		case SIMPLE_VALUE_NAME:
			return this.getSimpleValue(valueNode);
		case BLOB_VALUE_NAME:
			return this.getBlobValue(valueNode);			
		case VEC_VALUE_NAME:
			return this.getVecValue(valueNode);
		case OPT_VALUE_NAME:
			return this.getOptValue(valueNode);
		case RECORD_VALUE_NAME:
			return this.getRecordValue(valueNode);
		case VARIANT_VALUE_NAME:
			return this.getVariantValue(valueNode);
		default:
			return null;
		}
	}

	private IDLValue getSimpleValue(SimpleNode node) {
		SimpleNode valueNode = (SimpleNode) node.jjtGetChild(0);
		String type = valueNode.toString();
		
		String valueString = null;
		
		if(valueNode.jjtGetValue() != null)
			valueString = valueNode.jjtGetValue().toString();
		
		Object value;
		
		switch(type)
		{
		case INTEGER_NAME:
			value = new BigInteger(valueString);
			break;
		case FLOAT_NAME:
			value = new Double(valueString);
			break;	
		case BOOLEAN_NAME:
			value = new Boolean(valueString);
			break;
		case NULL_NAME:
			value = null;
			break;			
		default:
			if(valueString != null)
				value = valueString.substring(1, valueString.length() - 1);
			else
			value = null;
		}
		
		if (node.jjtGetNumChildren() > 1)
		{
			SimpleNode typeNode = (SimpleNode) node.jjtGetChild(1);
			IDLType idlType =  IDLType.createType(Type.from(typeNode.jjtGetValue().toString().toUpperCase()));
			
			return IDLValue.create(value, idlType);
		}
		else
			return IDLValue.create(value);
		  		
	}
	
	private IDLValue getBlobValue(SimpleNode node) {
		SimpleNode valueNode = (SimpleNode) node.jjtGetChild(0);
		
		byte[] blob = null;
		
		if(valueNode.jjtGetValue() != null)
			blob = valueNode.jjtGetValue().toString().getBytes();

		IDLType idlType = IDLType.createType(Type.VEC, Type.NAT8);

		return IDLValue.create(blob, idlType);
	}	

	private IDLValue getVecValue(SimpleNode node) {
		
		List<SimpleNode> valueNodes = this.getChildNodes((SimpleNode) node.jjtGetChild(0),
				VALUE_NODE_NAME);
		
		IDLType innerType = IDLType.createType(Type.NULL);
		
		List<Object> arrayList = new ArrayList<Object>();
		
		for (SimpleNode valueNode : valueNodes) {
			IDLValue idlValue = this.getIDLValue(((SimpleNode) valueNode.jjtGetChild(0)));
			
			arrayList.add(idlValue.getValue());
			innerType = idlValue.getIDLType();
		}	

		IDLType idlType = IDLType.createType(Type.VEC, innerType);

		return IDLValue.create(arrayList.toArray(), idlType);
	}

	private IDLValue getOptValue(SimpleNode node) {
		SimpleNode valueNode = (SimpleNode) node.jjtGetChild(0);
		
		IDLValue innerValue = this.getIDLValue((SimpleNode) valueNode.jjtGetChild(0));
		
		IDLType idlType = IDLType.createType(Type.OPT, innerValue.getIDLType());

		return IDLValue.create(innerValue.value, idlType);
	}

	private IDLValue getRecordValue(SimpleNode node) {				
		Map<Label, IDLType> typeMap = new TreeMap<Label, IDLType>();
		Map<Label, Object> valueMap = new TreeMap<Label, Object>();
		
		if(node.jjtGetNumChildren() == 0)
			return IDLValue.create(valueMap, IDLType.createType(Type.RECORD, typeMap));
		
		SimpleNode valueNode = (SimpleNode) node.jjtGetChild(0);			
		
		List<SimpleNode> subValueNodes = this.getChildNodes(valueNode, RECORD_SUB_VALUE_NAME);

		long id = 0;
		for (SimpleNode subValueNode : subValueNodes) {
			IDLValue subValue;
			Label label;

			if (subValueNode.jjtGetNumChildren() > 1) {
				String labelName = ((SimpleNode) subValueNode.jjtGetChild(0)).jjtGetValue().toString();
				if(INTEGER_NAME.equals(subValueNode.jjtGetChild(0).toString()))
					label = Label.createIdLabel(Long.valueOf(labelName));
				else
				{
					labelName = this.normalizeLabelName(labelName);

					label = Label.createNamedLabel(labelName);
				}										

				SimpleNode subValueNodeValue = (SimpleNode) subValueNode.jjtGetChild(1);

				subValue = this.getIDLValue(subValueNodeValue);
			} else {
				label = Label.createUnnamedLabel(id);
				id++;
				subValue = this.getIDLValue((SimpleNode) subValueNode.jjtGetChild(0));
			}

			typeMap.put(label, subValue.idlType);
			valueMap.put(label, subValue.value.orElse(null));
		}

		return IDLValue.create(valueMap, IDLType.createType(Type.RECORD, typeMap));
	}

	private IDLValue getVariantValue(SimpleNode node) {			
		Map<Label, IDLType> typeMap = new TreeMap<Label, IDLType>();
		Map<Label, Object> valueMap = new TreeMap<Label, Object>();
		
		if(node.jjtGetNumChildren() == 0)
			return IDLValue.create(valueMap, IDLType.createType(Type.VARIANT, typeMap));
		
		SimpleNode valueNode = (SimpleNode) node.jjtGetChild(0);
		
		List<SimpleNode> subValueNodes = this.getChildNodes(valueNode, VARIANT_SUB_VALUE_NAME);

		for (SimpleNode subValueNode : subValueNodes) {
			IDLValue subValue = null;
			Label label;

			if (subValueNode.jjtGetNumChildren() > 1) {
				String labelName = ((SimpleNode) subValueNode.jjtGetChild(0)).jjtGetValue().toString();
				if(INTEGER_NAME.equals(subValueNode.jjtGetChild(0).toString()))
					label = Label.createIdLabel(Long.valueOf(labelName));
				else
				{
					labelName = this.normalizeLabelName(labelName);

					label = Label.createNamedLabel(labelName);
				}
				SimpleNode subValueNodeValue = (SimpleNode) subValueNode.jjtGetChild(1);

				subValue = this.getIDLValue(subValueNodeValue);
			} else
				label = Label.createNamedLabel(((SimpleNode) subValueNode.jjtGetChild(0)).jjtGetValue().toString());

			typeMap.put(label, subValue.idlType);
			valueMap.put(label, subValue.value.orElse(null));
		}

		return IDLValue.create(valueMap, IDLType.createType(Type.VARIANT, typeMap));
	}

	String normalizeLabelName(String name) {
		if (name != null)
			name = name.replaceAll("\"", "");

		return name;
	}

	private List<SimpleNode> getChildNodes(SimpleNode node, String name) {
		List<SimpleNode> childNodes = new LinkedList<>();
		for (int i = 0; i < node.jjtGetNumChildren(); i++) {
			SimpleNode childNode = (SimpleNode) node.jjtGetChild(i);

			if (name != null) {
				if (name.equals(childNode.toString()))
					childNodes.add(childNode);
			} else
				childNodes.add(childNode);
		}

		return childNodes;
	}

	// add types for null values

	/**
	 * @return the args
	 */
	public IDLArgs getArgs() {
		return IDLArgs.create(this.args);
	}

}
