# Maven

Commands and techniques for Maven projects (`pom.xml`). The alert's `manifest_path` names the POM Dependabot scanned; in a multi-module build, the version may be managed in a parent POM or an imported BOM rather than in that file.

Use `./mvnw` in place of `mvn` when the repository has a Maven wrapper.

## Trace (step 3)

Show every path to the artifact in a module, with the resolved version:

```bash
mvn -pl <module> -am dependency:tree -Dincludes=<groupId>:<artifactId>
```

Find where the version comes from:

```bash
mvn -pl <module> help:effective-pom -Doutput=effective-pom.xml
```

Search the effective POM for the artifact. If its version is not in any project POM, it comes from an imported BOM (for example `spring-boot-dependencies` or `quarkus-bom`) or from a parent POM.

## Research options (step 4)

- Available versions: `mvn versions:display-dependency-updates -pl <module>`, or the artifact's page on Maven Central.
- Whether a newer BOM includes the fix: read the BOM's POM for the release you would move to, or its release notes.
- Required Java version: the artifact's release notes, or the `maven-compiler-plugin` release or target in its POM.

## Apply (step 4)

Follow the project's rule on where versions live (step 1). The usual order:

1. **Version managed in the project**: change it where it is managed, usually a parent POM's `dependencyManagement` section or the property it uses, rather than in a module POM.
2. **Version from an imported BOM**: if a newer patch release of the BOM includes the fixed version, bump the BOM property (for example `spring-boot.version` or `quarkus.platform.version`).
3. **Pin**: otherwise add an explicit entry for the artifact in the `dependencyManagement` section of the POM that imports the BOM, above the BOM import (Maven uses the first declaration it finds). Add a comment naming the alert or GHSA ID, so the pin can be removed when the BOM catches up:

   ```xml
   <!-- Pin for GHSA-xxxx-xxxx-xxxx (Dependabot alert 123). Remove when the BOM includes 2.18.3 or later. -->
   <dependency>
   	<groupId>com.example</groupId>
   	<artifactId>example-lib</artifactId>
   	<version>2.18.3</version>
   </dependency>
   ```

Maven versions are exact unless written as a range (`[1.0,2.0)`); do not introduce ranges.

## Verify (step 5)

Rerun the `dependency:tree` command from the trace step for every affected module, and confirm each copy is at or above the patched version.

## Without Dependabot (step 2)

The OWASP Dependency-Check plugin reports known vulnerabilities: `mvn org.owasp:dependency-check-maven:check`. Its first run downloads the vulnerability database and can take a long time.

## Checks (step 6)

Build and test the affected modules and the modules that depend on them, as CI does, for example `mvn -pl <module> -am install`. Run the project's format or style check if CI runs one. Tests that use Testcontainers need Docker running.
