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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;

import org.ic4j.candid.parser.IDLType;
import org.w3c.dom.Document;

public abstract class DOMSerDeserBase {
	public static final String CANDID_NS = "http://ic4j.org/candid";
	public static final String XML_XSD_NS = "http://www.w3.org/2001/XMLSchema";
	public static final String XML_XSI_NS = "http://www.w3.org/2001/XMLSchema-instance";
	public static final String XSD_PREFIX = "xsd";
	public static final String XSI_PREFIX = "xsi";
	public static final String CANDID_PREFIX = "candid";
	public static final String CANDID_TYPE_ATTR_NAME = "type";
	public static final String CANDID_NAME_ATTR_NAME = "name";
	public static final String CANDID_ID_ATTR_NAME = "id";
	public static final String XML_TYPE_ATTR_NAME = "type";
	public static final String ARRAY_ITEM_NAME = "item";
	
	Optional<IDLType> idlType = Optional.empty();
	Optional<String> namespace = Optional.empty();
	Optional<Document> document = Optional.empty();
	
	String arrayItem = ARRAY_ITEM_NAME;

	boolean isQualified = true;
	
	class MultiMap<K, V>
	{
	    private Map<K, Collection<V>> map = new TreeMap<>();
	 
	    public void put(K key, V value)
	    {
	        if (map.get(key) == null) {
	            map.put(key, new ArrayList<V>());
	        }
	 
	        map.get(key).add(value);
	    }
	 
	 
	    public Collection<V> get(Object key) {
	        return map.get(key);
	    }
	 
	    public Set<K> keySet() {
	        return map.keySet();
	    }
	 
	    public Set<Map.Entry<K, Collection<V>>> entrySet() {
	        return map.entrySet();
	    }
	 
	    public Collection<Collection<V>> values() {
	        return map.values();
	    }
	 
	    public boolean containsKey(Object key) {
	        return map.containsKey(key);
	    }
	 
	    public int size()
	    {
	        int size = 0;
	        for (Collection<V> value: map.values()) {
	            size += value.size();
	        }
	        return size;
	    }
	 
	    public boolean isEmpty() {
	        return map.isEmpty();
	    }
	 
	}
	
	class QName extends javax.xml.namespace.QName implements Comparable
	{

		public QName(String namespaceURI, String localPart) {
			super(namespaceURI, localPart);
		}

		@Override
		public int compareTo(Object o) {
			QName other = (QName)o;
			
			String resultA = (this.getNamespaceURI() == null ? "" : this.getNamespaceURI()) + this.getLocalPart();
			String resultB = (other.getNamespaceURI() == null ? "" : other.getNamespaceURI()) + other.getLocalPart();

			return resultA.compareTo(resultB);
		}
		
	}
}
