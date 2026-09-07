# dfinity-agent
Java Candid for The Internet Computer (IC4J) is a collection of open-source native Java libraries designed to handle Candid types on the Internet Computer.
Full documentation <a href="https://docs.ic4j.com">
here</a>

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


# License

IC4J Candid is available under Apache License 2.0.

# Documentation

## Decoding Untrusted Input

Binary decoding uses per-message limits. Existing `IDLArgs.fromBytes` and
`IDLDeserialize.create` calls use `DecodingLimits.DEFAULT`:

| Limit | Default |
| :---- | ------: |
| Encoded message bytes | 16 MiB |
| Elements in one collection | 1,000,000 |
| Total decoded values / aggregate reserved elements | 2,000,000 each |
| Type-table budget | 100,000 |
| Value and type-resolution nesting depth | 128 |

The type-table budget counts definitions, fields, function parameters and results,
annotations, service methods and name bytes, and argument references. Aggregate
element reservations include top-level arguments, vector elements, record fields,
and variant alternatives. These are resource budgets, not exact heap-byte limits.
Limits are checked before collection allocation; exceeded budgets and malformed
lengths or type references raise `CandidError`. Recursive type references are
memoized per message. Callers should discard a decoder after a decoding failure.

For a different bounded workload, pass explicit limits:

```java
DecodingLimits limits = new DecodingLimits(4 * 1024 * 1024, 100000, 200000, 10000, 64);
IDLArgs decoded = IDLArgs.fromBytes(bytes, expectedTypes, limits);
```

Use `null` for `expectedTypes` when no expected schema is available. Low-level
callers can use `IDLDeserialize.create(bytes, limits)`. Keep transport request-size
and concurrency limits as well; these budgets do not bound total application memory.
Increasing the depth limit can exceed the JVM stack capacity.

Expected variant members are matched by field ID, never by their position in the
expected schema. Unknown selected members and payload-type mismatches are rejected.
POJO and both JAXB deserializers skip static, synthetic, final, and transient fields;
classes that previously relied on writing those fields must use mutable instance
fields or a custom deserializer instead.

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
| float32   | Float, Double | 
| float64   | Double | 
| text   | String | 
| opt   | Optional | 
| principal   | Principal | 
| vec   | array, List | 
| record   | Map, Class | 
| variant   | Map, Enum | 
| func   | Func | 
| service   | Service | 
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

<a href="https://search.maven.org/artifact/ic4j/ic4j-candid/0.8.5/jar">
https://search.maven.org/artifact/ic4j/ic4j-candid/0.8.5/jar
</a>

```
<dependency>
  <groupId>org.ic4j</groupId>
  <artifactId>ic4j-candid</artifactId>
	<version>0.8.5</version>
</dependency>
```

```
implementation 'org.ic4j:ic4j-candid:0.8.5'
```

## Dependencies

This this is using these open source libraries


### Jackson JSON Serializer and Deserializer
To manage Jackson objects.

### Java CC
To parse IC IDL Candid files

# Build

IC4J Candid 0.8.5 publishes Java 8 compatible bytecode. When the build runs on JDK 11 or newer, Gradle uses `--release 8` to keep the main artifact compatible with Java 8.

## JAXB Traversal Tuning (UBL / Large Object Graphs)

For JAXB to Candid conversion of very broad or deep XML models, traversal can be tuned with system properties.

Supported properties:

- `ic4j.candid.jaxb.maxDepth` (default: `64`)
- `ic4j.candid.jaxb.maxNodes` (default: `10000`)
- `ic4j.candid.jaxb.prune.packagePrefixes` (comma-separated package prefixes)
- `ic4j.candid.jaxb.prune.classNames` (comma-separated fully qualified class names)
- `ic4j.candid.jaxb.allow.packagePrefixes` (comma-separated package prefixes)
- `ic4j.candid.jaxb.allow.classNames` (comma-separated fully qualified class names)

Behavior notes:

- Cycles are already guarded using registry + in-progress tracking.
- If traversal exceeds budget or matches a prune rule, the branch is represented as Candid `reserved`.
- Allow rules override prune rules for exact class names or package prefixes.

Suggested presets:

- Conservative UBL baseline:
	- `-Dic4j.candid.jaxb.maxDepth=48`
	- `-Dic4j.candid.jaxb.maxNodes=5000`
- Aggressive pruning for generated schema packages:
	- `-Dic4j.candid.jaxb.prune.packagePrefixes=oasis.names.specification.ubl`
	- `-Dic4j.candid.jaxb.allow.classNames=com.example.MyEntryPointType`
- Keep defaults but cap worst-case expansion:
	- `-Dic4j.candid.jaxb.maxDepth=64`
	- `-Dic4j.candid.jaxb.maxNodes=10000`

Generated JavaCC parser sources are committed in the repository. Regenerate them only when you intentionally change the grammar files by running Gradle with `-PrunJavacc=true`.

Recommended validation matrix:

- Run the full test suite on JDK 8 first.
- Compile and package on JDK 11.
- Run the full test suite on JDK 11.
- Run the full test suite on JDK 21.

On macOS, make sure `JAVA_HOME` points to a full JDK for the Java 8 leg. `java_home -v 1.8` can resolve to the Apple plugin JRE, which is not sufficient for Gradle and JavaCC tasks.

The detailed test procedure is documented in `TESTING.md`.

