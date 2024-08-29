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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.ic4j.candid.parser.ParserError.ParserErrorCode;
import org.ic4j.candid.parser.idl.type.IDLTypeGrammar;
import org.ic4j.candid.parser.idl.type.ParseException;
import org.ic4j.candid.parser.idl.type.SimpleNode;
import org.ic4j.candid.types.Label;
import org.ic4j.candid.types.Mode;
import org.ic4j.candid.types.Type;
import org.ic4j.IC4JLogging;

public class IDLParser {
	static final String TYPE_EXPRESSION_NODE_NAME = "TypeExpression";
	static final String SERVICE_EXPRESSION_NODE_NAME = "ServiceExpression";
	static final String IDENTIFIER_NODE_NAME = "Identifier";
	static final String SIMPLE_TYPE_NAME = "SimpleType";
	static final String VEC_TYPE_NAME = "VecType";
	static final String OPT_TYPE_NAME = "OptType";
	static final String RECORD_TYPE_NAME = "RecordType";
	static final String FUNC_TYPE_NAME = "FuncType";
	static final String SERVICE_TYPE_NAME = "ServiceType";
	
	static final String SERVICE_EXPRESSION_TYPE_NAME = "ServiceExpressionType";	
	static final String VARIANT_TYPE_NAME = "VariantType";
	static final String RECORD_SUB_TYPE_NAME = "RecordSubType";
	static final String VARIANT_SUB_TYPE_NAME = "VariantSubType";
	static final String FUNCTION_NODE_NAME = "Function";
	static final String ARGUMENTS_NODE_NAME = "Arguments";
	static final String ARGUMENT_NODE_NAME = "Argument";
	static final String NAMED_ARGUMENT_NODE_NAME = "NamedArgument";
	static final String QUERY_NODE_NAME = "Query";
	static final String ONEWAY_NODE_NAME = "Oneway";

	static final String BLOB_TYPE = "blob";

	static final IC4JLogging LOG = IC4JLogging.getIC4JLogger(IDLParser.class);

	IDLTypeGrammar parser;
	Map<String, IDLType> types = new HashMap<String, IDLType>();
	Map<String, IDLType> services = new HashMap<String, IDLType>();

	public IDLParser(Reader reader) {
		this.parser = new IDLTypeGrammar(reader);
	}

	public IDLParser(InputStream inputStream) {
		this.parser = new IDLTypeGrammar(inputStream);
	}

	public void parse() throws ParserError {
		try {
			SimpleNode node = this.parser.Start();

			// get types
			List<SimpleNode> typeNodes = this.getChildNodes(node, TYPE_EXPRESSION_NODE_NAME);
			
			Map<String, List<SimpleNode>> serviceExpressionTypes = new HashMap<String, List<SimpleNode>>();

			for (SimpleNode typeNode : typeNodes) {
				if (typeNode.jjtGetNumChildren() > 1) {
					if (IDENTIFIER_NODE_NAME.equals(typeNode.jjtGetChild(0).toString())) {
						String name = ((SimpleNode) typeNode.jjtGetChild(0)).jjtGetValue().toString();

						// handle service expression type
						if(SERVICE_EXPRESSION_TYPE_NAME.equals( typeNode.jjtGetChild(1).toString()))
						{
							serviceExpressionTypes.put(name, this.getChildNodes((SimpleNode)typeNode.jjtGetChild(1), null));
						}
						else
						{	
							IDLType idlType = this.getIDLType(((SimpleNode) typeNode.jjtGetChild(1)));
							
							if (idlType != null)
							{
								idlType.setName(name);
								this.types.put(name, idlType);
							}
						}
					}
				}
			}
			
			// find and complete missing types
			for(IDLType idlType : this.types.values())
				this.postProcessType(idlType);

			// get services

			List<SimpleNode> serviceNodes = this.getChildNodes(node, SERVICE_EXPRESSION_NODE_NAME);

			Integer id = 0;
			for (SimpleNode serviceNode : serviceNodes) {
				List<SimpleNode> functionNodes = this.getChildNodes(serviceNode, FUNCTION_NODE_NAME);
				
				if(functionNodes.isEmpty() && IDENTIFIER_NODE_NAME.equals(serviceNode.jjtGetChild(1).toString()))
				{	
					String name = ((SimpleNode) serviceNode.jjtGetChild(1)).jjtGetValue().toString();
					
					if(serviceExpressionTypes.containsKey(name))
					{
						functionNodes = serviceExpressionTypes.get(name);
					}
				}
				Map<String, IDLType> meths = new HashMap<String, IDLType>();

				for (SimpleNode functionNode : functionNodes) {
					List<IDLType> args = new ArrayList<IDLType>();
					List<IDLType> rets = new ArrayList<IDLType>();
					List<Mode> modes = new ArrayList<Mode>();

					String name = ((SimpleNode) functionNode.jjtGetChild(0)).jjtGetValue().toString();

					List<SimpleNode> argsNodes = this.getChildNodes((SimpleNode) functionNode.jjtGetChild(1),
							ARGUMENT_NODE_NAME);

					for (SimpleNode argsNode : argsNodes)
						args.add(this.getArgument(argsNode));

					List<SimpleNode> retsNodes = this.getChildNodes((SimpleNode) functionNode.jjtGetChild(2),
							ARGUMENT_NODE_NAME);

					for (SimpleNode retsNode : retsNodes)
						rets.add(this.getArgument(retsNode));

					List<SimpleNode> modeNodes = this.getChildNodes(functionNode, QUERY_NODE_NAME);

					if (modeNodes.size() > 0)
						modes.add(Mode.QUERY);

					modeNodes = this.getChildNodes(functionNode, ONEWAY_NODE_NAME);

					if (modeNodes.size() > 0)
						modes.add(Mode.ONEWAY);

					meths.put(name, IDLType.createType(args, rets, modes));
				}

				IDLType serviceType = IDLType.createType(meths);

				List<SimpleNode> serviceArgsNodes = this.getChildNodes(serviceNode, ARGUMENTS_NODE_NAME);

				if (serviceArgsNodes.size() > 0) {
					List<IDLType> args = new ArrayList<IDLType>();
					List<SimpleNode> argsNodes = this.getChildNodes(serviceArgsNodes.get(0), ARGUMENT_NODE_NAME);

					for (SimpleNode argsNode : argsNodes)
						args.add(this.getArgument(argsNode));

					serviceType.args = args;
				}

				if (IDENTIFIER_NODE_NAME.equals(serviceNode.jjtGetChild(0).toString())) {
					String name = ((SimpleNode) serviceNode.jjtGetChild(0)).jjtGetValue().toString();
					this.services.put(name, serviceType);
				} else {
					this.services.put(id.toString(), serviceType);
					id++;
				}

			}

		} catch (ParseException e) {
			throw ParserError.create(ParserErrorCode.PARSE, e);
		}
	}

	private IDLType getIDLType(SimpleNode typeNode) {
		switch (typeNode.toString()) {
		case IDENTIFIER_NODE_NAME:
			return this.getExistingType(typeNode);
		case SIMPLE_TYPE_NAME:
			return this.getSimpleType(typeNode);
		case VEC_TYPE_NAME:
			return this.getVecType(typeNode);
		case OPT_TYPE_NAME:
			return this.getOptType(typeNode);
		case RECORD_TYPE_NAME:
			return this.getRecordType(typeNode);
		case VARIANT_TYPE_NAME:
			return this.getVariantType(typeNode);
		case FUNC_TYPE_NAME:
			return this.getFuncType(typeNode);
		case SERVICE_TYPE_NAME:
			return this.getServiceType(typeNode);
		default:
			return null;
		}
	}

	private IDLType getExistingType(SimpleNode typeNode) {
		String name = typeNode.jjtGetValue().toString();

		IDLType idlType = this.types.get(name);

		if (idlType != null)
			idlType.setName(name);
		else {
			// set Reserved type, will try to find actual type in post processing
			idlType = IDLType.createType(Type.RESERVED);
			idlType.setName(name);
		}

		return idlType;
	}

	private IDLType getSimpleType(SimpleNode typeNode) {
		// handle blob
		if (BLOB_TYPE.equals(typeNode.jjtGetValue().toString()))
			return IDLType.createType(Type.VEC, IDLType.createType(Type.NAT8));

		return IDLType.createType(Type.from(typeNode.jjtGetValue().toString().toUpperCase()));
	}

	private IDLType getServiceType(SimpleNode typeNode) {
		Map<String, IDLType> meths = new HashMap<String, IDLType>();

		return IDLType.createType(meths);
	}

	private IDLType getFuncType(SimpleNode typeNode) {
		List<IDLType> args = new ArrayList<IDLType>();
		List<IDLType> rets = new ArrayList<IDLType>();
		List<Mode> modes = new ArrayList<Mode>();

		List<SimpleNode> argsNodes = this.getChildNodes((SimpleNode) typeNode.jjtGetChild(0), ARGUMENT_NODE_NAME);

		for (SimpleNode argsNode : argsNodes)
			args.add(this.getArgument(argsNode));

		List<SimpleNode> retsNodes = this.getChildNodes((SimpleNode) typeNode.jjtGetChild(1), ARGUMENT_NODE_NAME);

		for (SimpleNode retsNode : retsNodes)
			rets.add(this.getArgument(retsNode));

		List<SimpleNode> modeNodes = this.getChildNodes(typeNode, QUERY_NODE_NAME);

		if (modeNodes.size() > 0)
			modes.add(Mode.QUERY);

		modeNodes = this.getChildNodes(typeNode, ONEWAY_NODE_NAME);

		if (modeNodes.size() > 0)
			modes.add(Mode.ONEWAY);

		return IDLType.createType(args, rets, modes);
	}

	private IDLType getVecType(SimpleNode typeNode) {
		IDLType innerType = this.getIDLType((SimpleNode) typeNode.jjtGetChild(0));

		return IDLType.createType(Type.VEC, innerType);
	}

	private IDLType getOptType(SimpleNode typeNode) {
		IDLType innerType = this.getIDLType((SimpleNode) typeNode.jjtGetChild(0));

		return IDLType.createType(Type.OPT, innerType);
	}

	private IDLType getRecordType(SimpleNode typeNode) {
		Map<Label, IDLType> typeMap = new TreeMap<Label, IDLType>();
		List<SimpleNode> subTypeNodes = this.getChildNodes(typeNode, RECORD_SUB_TYPE_NAME);

		long id = 0;
		for (SimpleNode subTypeNode : subTypeNodes) {
			IDLType subType;
			Label label;

			if (subTypeNode.jjtGetNumChildren() > 1) {
				String labelName = ((SimpleNode) subTypeNode.jjtGetChild(0)).jjtGetValue().toString();
				
				labelName = this.normalizeLabelName(labelName);
				
				label = Label.createNamedLabel(labelName);
				SimpleNode subTypeNodeValue = (SimpleNode) subTypeNode.jjtGetChild(1);
				
				subType = this.getIDLType(subTypeNodeValue);
			} else {
				label = Label.createUnnamedLabel(id);
				id++;
				subType = this.getIDLType((SimpleNode) subTypeNode.jjtGetChild(0));
			}
			

			typeMap.put(label, subType);
		}

		return IDLType.createType(Type.RECORD, typeMap);
	}

	private IDLType getVariantType(SimpleNode typeNode) {
		Map<Label, IDLType> typeMap = new TreeMap<Label, IDLType>();
		List<SimpleNode> subTypeNodes = this.getChildNodes(typeNode, VARIANT_SUB_TYPE_NAME);

		for (SimpleNode subTypeNode : subTypeNodes) {
			IDLType subType = null;
			Label label;

			if (subTypeNode.jjtGetNumChildren() > 1) {
				String labelName = ((SimpleNode) subTypeNode.jjtGetChild(0)).jjtGetValue().toString();
				
				labelName = this.normalizeLabelName(labelName);
				
				label = Label.createNamedLabel(labelName);
				SimpleNode subTypeNodeValue = (SimpleNode) subTypeNode.jjtGetChild(1);
				
				subType = this.getIDLType(subTypeNodeValue);
			} else
				label = Label.createNamedLabel(((SimpleNode) subTypeNode.jjtGetChild(0)).jjtGetValue().toString());

			typeMap.put(label, subType);
		}

		return IDLType.createType(Type.VARIANT, typeMap);
	}
	
	String normalizeLabelName(String name)
	{
		if(name != null)
			name = name.replaceAll("\"", "");
		
		return name;
	}

	private IDLType getArgument(SimpleNode argNode) {
		String argName = null;
		IDLType argType = null;

		if (argNode.jjtGetNumChildren() == 1) {
			if (IDENTIFIER_NODE_NAME.equals(argNode.jjtGetChild(0).toString()))
				argName = ((SimpleNode) argNode.jjtGetChild(0)).jjtGetValue().toString();

			argType = this.getIDLType((SimpleNode) argNode.jjtGetChild(0));

			argType.setName(argName);
		} else if (argNode.jjtGetNumChildren() > 1) {
			if (IDENTIFIER_NODE_NAME.equals(argNode.jjtGetChild(0).toString()))
				argName = ((SimpleNode) argNode.jjtGetChild(0)).jjtGetValue().toString();

			if (NAMED_ARGUMENT_NODE_NAME.equals(argNode.jjtGetChild(1).toString()))
				argType = this.getIDLType((SimpleNode) argNode.jjtGetChild(1).jjtGetChild(0));

			if (argType != null)
				argType.setName(argName);
		}

		return argType;
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
	private void postProcessType(IDLType idlType)
	{
		if(idlType == null)
			return;
		
		Set<Label> valueSet = idlType.typeMap.keySet();
		for(Label idlSubTypeLabel : valueSet)
		{
			IDLType idlSubType = idlType.typeMap.get(idlSubTypeLabel);
			
			// find missing type
			if(idlSubType == null)
			{
				if(this.types.containsKey(idlSubTypeLabel.toString()))
					idlSubType = this.types.get(idlSubTypeLabel.toString());
			}
			else if(idlSubType.getType() == Type.RESERVED)
			{
				String idlSubTypeName = idlSubType.getName();
				idlSubType = this.types.get(idlSubTypeName);
			}			

			// find nested missing types
			if(idlSubType != null && !idlType.typeMap.containsKey(idlSubTypeLabel))
				this.postProcessType(idlSubType);	
			
			idlType.typeMap.put(idlSubTypeLabel, idlSubType);
		}
	}

	/**
	 * @return the types
	 */
	public Map<String, IDLType> getTypes() {
		return types;
	}

	/**
	 * @return the services
	 */
	public Map<String, IDLType> getServices() {
		return services;
	}
}
