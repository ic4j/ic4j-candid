# Test Plan

This project publishes a Java 8 compatible library and validates runtime behavior on newer LTS JDKs.

## Goals

- Keep the published jar compatible with Java 8.
- Run the automated test suite on JDK 8 first.
- Run the automated test suite on JDK 11 and JDK 21.
- Catch dependency or JAXB/Jackson regressions introduced by library upgrades.

## Supported Matrix

| Stage | JDK | Expected Result |
| :---- | :-- | :-------------- |
| Test suite | 8 | All tests pass |
| Test suite | 11 | All tests pass |
| Test suite | 21 | All tests pass |

## Local Verification

Use a Gradle runtime that supports the selected JDK. For this repository, Gradle 8.x is suitable for the Java 8, JDK 11, and JDK 21 legs, as long as `JAVA_HOME` points to a full JDK.

### Java 8

On macOS, avoid `java_home -v 1.8` if it resolves to the Apple plugin JRE. Point `JAVA_HOME` at a full JDK installation instead.

```bash
export JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-1.8.jdk/Contents/Home
gradle -PrunJavacc=true test --rerun-tasks
```

### JDK 11

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 11)
gradle -PrunJavacc=true test --rerun-tasks
```

### JDK 21

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
gradle -PrunJavacc=true test --rerun-tasks
```

## Notes

- The main jar is built with Java 8 compatibility enabled through Gradle `--release 8` when the build runs on JDK 9+.
- JavaCC regeneration is opt-in. Test runs from a clean checkout must enable it with `-PrunJavacc=true`; generated sources are written under `build/generated/sources/javacc`. To regenerate only after grammar changes, run `gradle -PrunJavacc=true javaccValue javaccType`.
- The build keeps both `jakarta.xml.bind` and `javax.xml.bind` APIs available so the serializer code and tests work on Java 8, 11, and 21.
- JAXB dependencies stay on the 3.0 line to avoid raising the main source-set baseline beyond Java 8.
- Test-only dependencies should be reviewed before major upgrades because some newer lines require Java 11+.