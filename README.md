# dfinity-agent
Java Candid for The Internet Computer (IC4J) is a set of native Java libraries to manage the Internet Computer Candid types.

<a href="https://dfinity.org/">
https://dfinity.org/
</a>

The code is implementation of the Internet Computer Interface protocol 

<a href="https://sdk.dfinity.org/docs/interface-spec/index.html">
https://sdk.dfinity.org/docs/interface-spec/index.html
</a>

and it's using Dfinity Rust agent as an inspiration, using similar package structures and naming conventions.

<a href="https://github.com/dfinity/agent-rs">
https://github.com/dfinity/agent-rs
</a>

Currently we support query and update (call) operations with primitive types, arrays, option and principal type. Early access to variant and record types. 


0.5.6
Added support for Java POJO serialization and deserialization

0.5.7
Added support for JSON(Jackson), XML(DOM) serialization and deserialization and JDBC(ResultSet) serialization


# License

IC4J Candid is available under Apache License 2.0.

# Documentation

## Supported type mapping between Java and Candid

| Candid      | Java    |
| :---------- | :---------- | 
| bool   | Boolean | 
| int| BigInteger   | 
| int8   | Byte | 
| int16   | Short | 
| int32   | Integer | 
| int64   | Long | 
| nat| BigInteger   | 
| nat8   | Byte | 
| nat16   | Short | 
| nat32   | Integer | 
| nat64   | Long |
| float32   | Float | 
| float64   | Double | 
| text   | String | 
| opt   | Optional | 
| principal   | Principal | 
| vec   | array | 
| record   | Map | 
| variant   | Map | 
| null   |Null | 


## POJO Java class with Candid annotations

```
import java.math.BigInteger;

import org.ic4j.candid.annotations.Field;
import org.ic4j.candid.annotations.Name;
import org.ic4j.candid.types.Type;

public class Pojo {
	@Field(Type.BOOL)
	@Name("bar")
	public Boolean bar;

	@Field(Type.INT)
	@Name("foo")
	public BigInteger foo;
}
```

```
Pojo pojoValue = new Pojo();
				
pojoValue.bar = new Boolean(false);
pojoValue.foo = BigInteger.valueOf(43); 
				
```

## JSON (Jackson) serialization and deserialization

Use JacksonSerializer to serialize Jackson JsonNode or Jackson compatible Pojo class to Candid

```
JsonNode jsonValue;
IDLType idlType;

IDLValue idlValue = IDLValue.create(jsonValue, JacksonSerializer.create(idlType));
List<IDLValue> args = new ArrayList<IDLValue>();
args.add(idlValue);

IDLArgs idlArgs = IDLArgs.create(args);

byte[] buf = idlArgs.toBytes();
```

Use JacksonDeserializer to deserialize Candid to Jackson JsonNode or Jackson compatible Pojo class

```
JsonNode jsonResult = IDLArgs.fromBytes(buf).getArgs().get(0)
	.getValue(JacksonDeserializer.create(idlValue.getIDLType()), JsonNode.class);
```

## XML (DOM) serialization and deserialization

Use DOMSerializer to serialize DOM Node to Candid

```
Node domValue;

IDLValue idlValue = IDLValue.create(domValue,DOMSerializer.create());
List<IDLValue> args = new ArrayList<IDLValue>();
args.add(idlValue);

IDLArgs idlArgs = IDLArgs.create(args);

byte[] buf = idlArgs.toBytes();
```

Use DOMDeserializer to deserialize Candid to DOM Node

```
DOMDeserializer domDeserializer = DOMDeserializer.create(idlValue.getIDLType()).rootElement("http://scaleton.com/dfinity/candid","data");
			
Node domResult = IDLArgs.fromBytes(buf).getArgs().get(0).getValue(domDeserializer, Node.class);
```

## JDBC (ResultSet) serialization

Use JDBCSerializer to serialize JDBC ResultSet to Candid

```
ResultSet result = statement.executeQuery(sql);
			
IDLValue idlValue = IDLValue.create(result, JDBCSerializer.create());
List<IDLValue> args = new ArrayList<IDLValue>();
args.add(idlValue);

IDLArgs idlArgs = IDLArgs.create(args);

byte[] buf = idlArgs.toBytes();
```

# Downloads / Accessing Binaries

To add Java IC4J Candid library to your Java project use Maven or Gradle import from Maven Central.

<a href="https://search.maven.org/artifact/com.scaleton.dfinity/dfinity-agent/0.5.7/jar">
https://search.maven.org/artifact/com.scaleton.dfinity/dfinity-agent/0.5.7/jar
</a>

```
<dependency>
  <groupId>org.ic4j</groupId>
  <artifactId>candid</artifactId>
  <version>0.6.0</version>
</dependency>
```

```
implementation 'org.ic4j:candid:0.6.0'
```

## Dependencies

This this is using these open source libraries


### Jackson JSON Serializer and Deserializer
To manage Jackson objects.

# Build

You need JDK 8+ to build IC4J Candid.

