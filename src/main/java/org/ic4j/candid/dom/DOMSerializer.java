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

package org.ic4j.candid.dom;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

import org.ic4j.candid.parser.IDLType;
import org.ic4j.candid.parser.IDLValue;
import org.ic4j.candid.types.Label;
import org.ic4j.candid.types.Type;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.Text;

import org.ic4j.candid.CandidError;
import org.ic4j.candid.ObjectSerializer;
import org.ic4j.types.Principal;

public final class DOMSerializer extends DOMSerDeserBase implements ObjectSerializer {

	public static DOMSerializer create(IDLType idlType) {
		DOMSerializer serializer = new DOMSerializer();
		serializer.idlType = Optional.ofNullable(idlType);
		return serializer;
	}

	public static DOMSerializer create() {
		DOMSerializer serializer = new DOMSerializer();
		return serializer;
	}

	public DOMSerializer arrayItem(String arrayItem) {
		this.arrayItem = arrayItem;

		return this;
	}
	
	public void setIDLType(IDLType idlType)
	{
		this.idlType = Optional.ofNullable(idlType);
	}	

	@Override
	public IDLValue serialize(Object value) {
		if (value == null)
			return IDLValue.create(value);

		if (!Element.class.isAssignableFrom(value.getClass()))
			throw CandidError.create(CandidError.CandidErrorCode.CUSTOM,
					value.getClass().getName() + " is not assignable from " + Element.class.getName());

		this.namespace = Optional.ofNullable(((Element) value).getNamespaceURI());

		if (this.namespace.isPresent())
			this.isQualified = true;
		else
			this.isQualified = false;

		return this.getIDLValue(this.idlType, (Element) value);
	}

	IDLValue getPrimitiveIDLValue(Type type, Element value) {
		IDLValue result = IDLValue.create(null);

		if (value == null)
			return result;

		Text textNode = this.getTextNode(value);
		
		if(textNode == null)
			return result;

		String textValue = textNode.getTextContent();

		switch (type) {
		case BOOL:
			result = IDLValue.create(Boolean.valueOf(textValue), type);
			break;
		case INT:
			result = IDLValue.create(new BigInteger(textValue), type);
			break;
		case INT8:
			result = IDLValue.create(Byte.valueOf(textValue), type);
			break;
		case INT16:
			result = IDLValue.create(Short.valueOf(textValue), type);
			break;
		case INT32:
			result = IDLValue.create(Integer.valueOf(textValue), type);
			break;
		case INT64:
			result = IDLValue.create(Long.valueOf(textValue), type);
			break;
		case NAT:
			result = IDLValue.create(new BigInteger(textValue), type);
			break;
		case NAT8:
			result = IDLValue.create(Byte.valueOf(textValue), type);
			break;
		case NAT16:
			result = IDLValue.create(Short.valueOf(textValue), type);
			break;
		case NAT32:
			result = IDLValue.create(Integer.valueOf(textValue), type);
			break;
		case NAT64:
			result = IDLValue.create(Long.valueOf(textValue), type);
			break;
		case FLOAT32:
			result = IDLValue.create(Float.valueOf(textValue), type);
			break;
		case FLOAT64:
			result = IDLValue.create(Double.valueOf(textValue), type);
			break;
		case TEXT:
			result = IDLValue.create(textValue, type);
			break;
		case PRINCIPAL:
			result = IDLValue.create(Principal.fromString(textValue));
			break;
		case EMPTY:
			result = IDLValue.create(null, type);
		case NULL:
			result = IDLValue.create(null, type);
			break;
		}

		return result;
	}

	Type getPrimitiveType(Element value) {
		if (value == null)
			return Type.NULL;

		String type = null;
		if (this.isQualified) {
			if (value.hasAttributeNS(CANDID_NS, CANDID_TYPE_ATTR_NAME))
				type = value.getAttributeNS(CANDID_NS, CANDID_TYPE_ATTR_NAME);
		} else {
			if (value.hasAttribute(CANDID_TYPE_ATTR_NAME))
				type = value.getAttribute(CANDID_TYPE_ATTR_NAME);
		}

		if (type != null) {
			Type actualType = Type.from(type);

			if (actualType.isPrimitive())
				return actualType;
			else
				return Type.TEXT;
		}

		if (value.hasAttributeNS(XML_XSI_NS, XML_TYPE_ATTR_NAME)) {
			type = value.getAttributeNS(XML_XSI_NS, XML_TYPE_ATTR_NAME);

			switch (type) {
			case XSD_PREFIX + ":boolean":
				return Type.BOOL;
			case XSD_PREFIX + ":integer":
				return Type.INT;
			case XSD_PREFIX + ":byte":
				return Type.INT8;
			case XSD_PREFIX + ":short":
				return Type.INT16;
			case XSD_PREFIX + ":int":
				return Type.INT32;
			case XSD_PREFIX + ":long":
				return Type.INT64;
			case XSD_PREFIX + ":positiveInteger":
				return Type.NAT;
			case XSD_PREFIX + ":unsignedByte":
				return Type.NAT8;
			case XSD_PREFIX + ":unsignedShort":
				return Type.NAT16;
			case XSD_PREFIX + ":unsignedInt":
				return Type.NAT32;
			case XSD_PREFIX + ":unsignedLong":
				return Type.NAT64;
			case XSD_PREFIX + ":float":
				return Type.FLOAT32;
			case XSD_PREFIX + ":double":
				return Type.FLOAT64;
			case XSD_PREFIX + ":string":
				return Type.TEXT;
			case XSD_PREFIX + ":ID":
				return Type.PRINCIPAL;
			default:
				return Type.TEXT;

			}
		}

		return Type.TEXT;
	}

	IDLValue getArrayIDLValue(Optional<IDLType> expectedIdlType, Element value, String localName) {
		IDLType innerIdlType = IDLType.createType(Type.NULL);

		if (expectedIdlType.isPresent())
			innerIdlType = expectedIdlType.get().getInnerType();

		if (this.hasTextNode(value)) {
			if (innerIdlType == null)
				innerIdlType = IDLType.createType(Type.INT8);

			byte[] byteArray = Base64.getDecoder().decode(this.getTextNode(value).getTextContent());
			return IDLValue.create(byteArray, IDLType.createType(Type.VEC, innerIdlType));
		}

		if (value.getNodeType() == Node.ELEMENT_NODE) {
			List<Element> arrayElements = this.getArrayElements(value, localName);
			Object[] arrayValue = new Object[arrayElements.size()];

			int i = 0;
			for (Element arrayElement : arrayElements) {
				IDLValue item = this.getIDLValue(Optional.ofNullable(innerIdlType), arrayElement);

				arrayValue[i] = item.getValue();
				if (innerIdlType == null)
					innerIdlType = item.getIDLType();

				i++;
			}

			IDLType idlType;

			if (expectedIdlType.isPresent() && expectedIdlType.get().getInnerType() != null)
				idlType = expectedIdlType.get();
			else
				idlType = IDLType.createType(Type.VEC, innerIdlType);

			return IDLValue.create(arrayValue, idlType);
		}

		throw CandidError.create(CandidError.CandidErrorCode.CUSTOM,
				"Cannot convert class " + value.getClass().getName() + " to VEC");

	}

	IDLValue getIDLValue(Optional<IDLType> expectedIdlType, Element value) {
		// handle null values
		if (value == null)
			return IDLValue.create(value, Type.NULL);

		Type type;
		
		String typeAttribute = null;
		if (this.isQualified) {
			if (value.hasAttributeNS(CANDID_NS, CANDID_TYPE_ATTR_NAME))
				typeAttribute = value.getAttributeNS(CANDID_NS, CANDID_TYPE_ATTR_NAME);
		} else {
			if (value.hasAttribute(CANDID_TYPE_ATTR_NAME))
				typeAttribute = value.getAttribute(CANDID_TYPE_ATTR_NAME);
		}
		if (typeAttribute != null) {
			type = Type.from(typeAttribute);
		}else
			{
				if (this.hasTextNode(value))
				type = this.getPrimitiveType(value);
			else {
				MultiMap<QName, Element> elementMap = this.getFlatElements(value);
	
				QName itemQName = new QName(CANDID_NS, this.arrayItem);
				// check if it's array
				Collection<Element> items = elementMap.get(itemQName);
	
				if (items == null || items.isEmpty())
					type = Type.RECORD;
				else
					type = Type.VEC;
			}
		}
		
		if (expectedIdlType.isPresent())
			type = expectedIdlType.get().getType();

		// handle primitives

		if (type.isPrimitive())
			return this.getPrimitiveIDLValue(type, value);

		// handle arrays
		if (type == Type.VEC) {
			if (!expectedIdlType.isPresent())
				expectedIdlType = Optional.ofNullable(IDLType.createType(Type.VEC));

			return this.getArrayIDLValue(expectedIdlType, value, this.arrayItem);
		}

		// handle Objects
		if (type == Type.RECORD || type == Type.VARIANT) {
			MultiMap<QName, Element> elementMap = this.getFlatElements(value);

			Map<Label, Object> valueMap = new TreeMap<Label, Object>();
			Map<Label, IDLType> typeMap = new TreeMap<Label, IDLType>();
			Map<Label, IDLType> expectedTypeMap = new TreeMap<Label, IDLType>();

			if (expectedIdlType.isPresent())
				expectedTypeMap = expectedIdlType.get().getTypeMap();

			Iterator<QName> fieldQNames = elementMap.keySet().iterator();

			QName itemQName = new QName(CANDID_NS, this.arrayItem);
			while (fieldQNames.hasNext()) {
				QName qName = fieldQNames.next();
				String name = qName.getLocalPart();

				Collection<Element> items = elementMap.get(qName);

				IDLType expectedItemIdlType = null;

				IDLValue itemIdlValue;

				Label label;
				if (qName.equals(itemQName)) {
					// handle unnamed RECORDS and VARIANTS
					int i = 0;
					
					for(Element unnamedNode : items)
					{
						expectedItemIdlType = null;
						long id = i;
						
						String idAttribute = null;
						if (this.isQualified) {
							if (unnamedNode.hasAttributeNS(CANDID_NS, CANDID_ID_ATTR_NAME))
								idAttribute = unnamedNode.getAttributeNS(CANDID_NS, CANDID_ID_ATTR_NAME);
						} else {
							if (unnamedNode.hasAttribute(CANDID_ID_ATTR_NAME))
								idAttribute = unnamedNode.getAttribute(CANDID_ID_ATTR_NAME);
						}
						
						if(idAttribute != null)
						{
							try
							{
								id = Long.parseLong(idAttribute);
								
								if(id == i)
									i++;
							}catch(Exception e)
							{
								
							}
						}else
							i++;
						
						label = Label.createUnnamedLabel(id);
						
						if (expectedIdlType.isPresent() && expectedTypeMap != null) {

							expectedItemIdlType = expectedTypeMap.get(label);

							itemIdlValue = this.getIDLValue(Optional.ofNullable(expectedItemIdlType), unnamedNode);

						} else
							itemIdlValue = this.getIDLValue(Optional.ofNullable(expectedItemIdlType), unnamedNode);
						
						typeMap.put(label, itemIdlValue.getIDLType());
						valueMap.put(label, itemIdlValue.getValue());
					}
				} else {
					// handle named RECORDS and VARIANTS
					Element firstNode = items.iterator().next();

					if (this.isQualified) {
						if (firstNode.hasAttributeNS(CANDID_NS, CANDID_NAME_ATTR_NAME))
							name = firstNode.getAttributeNS(CANDID_NS, CANDID_NAME_ATTR_NAME);
					} else {
						if (value.hasAttribute(CANDID_NAME_ATTR_NAME))
							name = value.getAttribute(CANDID_NAME_ATTR_NAME);
					}

					label = Label.createNamedLabel((String) name);

					if (expectedIdlType.isPresent() && expectedTypeMap != null) {

						expectedItemIdlType = expectedTypeMap.get(label);

						if (expectedItemIdlType != null && expectedItemIdlType.getType() == Type.VEC)
							itemIdlValue = this.getArrayIDLValue(Optional.ofNullable(expectedItemIdlType), value, name);
						else if (items.size() == 1)
							itemIdlValue = this.getIDLValue(Optional.ofNullable(expectedItemIdlType), firstNode);
						else
							throw CandidError.create(CandidError.CandidErrorCode.CUSTOM,
									"Invalid number of " + name + " elements");
					} else {
						if (items.size() == 1)
							itemIdlValue = this.getIDLValue(Optional.ofNullable(expectedItemIdlType), firstNode);
						else
							itemIdlValue = this.getArrayIDLValue(Optional.ofNullable(IDLType.createType(Type.VEC)),
									value, name);
					}
					
					typeMap.put(label, itemIdlValue.getIDLType());
					valueMap.put(label, itemIdlValue.getValue());
				}
			}

			IDLType idlType = IDLType.createType(type, typeMap);
			IDLValue idlValue = IDLValue.create(valueMap, idlType);

			return idlValue;
		}

		if (type == Type.OPT) {
			if (expectedIdlType.isPresent()) {
				if (!value.hasChildNodes())
					return IDLValue.create(Optional.empty(), expectedIdlType.get());

				IDLValue itemIdlValue = this.getIDLValue(Optional.ofNullable(expectedIdlType.get().getInnerType()),
						value);

				return IDLValue.create(Optional.ofNullable(itemIdlValue.getValue()), expectedIdlType.get());
			} else {
				if (!value.hasChildNodes())
					return IDLValue.create(Optional.empty(), IDLType.createType(Type.OPT));

				IDLValue itemIdlValue = this.getIDLValue(Optional.ofNullable(IDLType.createType(Type.OPT)), value);

				return IDLValue.create(Optional.ofNullable(itemIdlValue.getValue()), IDLType.createType(Type.OPT));
			}
		}

		throw CandidError.create(CandidError.CandidErrorCode.CUSTOM, "Cannot convert type " + type.name());

	}

	MultiMap<QName, Element> getFlatElements(Element element) {

		MultiMap<QName, Element> elementMap = new MultiMap<QName, Element>();

		Node childNode = element.getFirstChild();
		if (childNode != null) {
			if (childNode.getNodeType() == Node.ELEMENT_NODE) {
				Element childElement = (Element) childNode;

				if (!this.isQualified || this.namespace.get() == childElement.getNamespaceURI()
						|| CANDID_NS == childElement.getNamespaceURI())
					elementMap.put(new QName(childElement.getNamespaceURI(), childElement.getLocalName()),
							childElement);
			}
			while (childNode.getNextSibling() != null) {
				childNode = childNode.getNextSibling();
				if (childNode.getNodeType() == Node.ELEMENT_NODE) {
					Element childElement = (Element) childNode;

					if (!this.isQualified || this.namespace.get() == childElement.getNamespaceURI()
							|| CANDID_NS == childElement.getNamespaceURI())
						elementMap.put(new QName(childElement.getNamespaceURI(), childElement.getLocalName()),
								childElement);
				}
			}
		}
		return elementMap;

	}

	List<Element> getArrayElements(Element element, String localName) {

		List<Element> elementSet = new ArrayList<Element>();
		Node childNode = element.getFirstChild();
		if (childNode != null) {
			if (childNode.getNodeType() == Node.ELEMENT_NODE) {
				Element childElement = (Element) childNode;

				if (!this.isQualified || this.namespace.get() == childElement.getNamespaceURI() || CANDID_NS == childElement.getNamespaceURI())
					if (childElement.getLocalName() == localName)
						elementSet.add(childElement);
			}
			while (childNode.getNextSibling() != null) {
				childNode = childNode.getNextSibling();
				if (childNode.getNodeType() == Node.ELEMENT_NODE) {
					Element childElement = (Element) childNode;

					if (!this.isQualified || this.namespace.get() == childElement.getNamespaceURI() || CANDID_NS == childElement.getNamespaceURI())
						if (childElement.getLocalName() == localName)
							elementSet.add(childElement);
				}
			}
		}
		return elementSet;
	}

	Text getTextNode(Element element) {
		Node childNode = element.getFirstChild();
		if (childNode != null) {
			if (childNode.getNodeType() == Node.TEXT_NODE) {
				return (Text) childNode;
			}
			while (childNode.getNextSibling() != null) {
				childNode = childNode.getNextSibling();
				if (childNode.getNodeType() == Node.TEXT_NODE) {
					return (Text) childNode;
				}
			}
		}
		return null;
	}

	boolean hasTextNode(Element element) {

		if (this.getTextNode(element) != null)
			return true;
		else
			return false;
	}

}
