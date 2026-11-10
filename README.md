# MARS Repository Library
Shared library for repository integrations in the
[MARS framework](https://github.com/elixir-europe/MARS).

The library contains the common pieces that target repository services need when
they parse ISA-JSON and translate repository-specific responses back into a MARS receipt:

- ISA-JSON model classes
- MARS receipt model classes
- Helper classes or methods to map ISA fields to accessions
- Helper classes or methods to traverse the ISA graph

Repository service modules, such as ENA or BioSamples converters, should use this
library instead of keeping local copies of the receipt and ISA model code.

## Languages

| Language | Directory | Package | Version |
|---|---|---|---|
| Java 17 | `src/` | `com.elixir.mars:mars-repository` | 0.0.3 |
| Python ≥3.10 | `python/` | `mars-repository-py` | 0.0.3 |

Functionality between the two languages should be kept in sync for released versions.

## Java

Built with Gradle. Requires Java 17.

```sh
./gradlew build
```

The library is published to the Github Maven Repository, see
[here](https://docs.github.com/en/packages/working-with-a-github-packages-registry/working-with-the-apache-maven-registry)
for instructions on how to install.

### Publish Locally

MARS services can consume a local development version from your Maven local
repository:

```bash
./gradlew publishToMavenLocal
```

This publishes:

```text
com.elixir.mars:mars-repository:0.0.2
```

Then make sure the consuming service has `mavenLocal()` enabled and depends on
the same version:

```gradle
repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    implementation 'com.elixir.mars:mars-repository:0.0.2'
}
```

### Releasing a New Version

To release a new library version:

1. Update the version in `build.gradle` from the current `-SNAPSHOT` value to
   the release version, for example `0.0.2`.
2. Run the build to confirm the code still passes:

   ```bash
   ./gradlew build
   ```

3. Commit the version bump.
4. Create and push a git tag for the release, for example:

   ```bash
   git tag v0.0.2
   git push origin v0.0.2
   ```

5. After the release is published, bump `build.gradle` to the next
   `-SNAPSHOT` version, such as `0.0.3-SNAPSHOT`, and commit that change.
6. Create a release based on the tag on Github to produce a changelog.

### Creating a Receipt Provider

Target repository integrations typically extend `MarsReceiptProvider`, collect
repository accessions, and let the base class build the MARS receipt paths.

```java
public final class ExampleRepoReceiptProvider extends MarsReceiptProvider {

  public ExampleRepoReceiptProvider() {
    super("example");
  }

  @Override
  public String convertMarsReceiptToJson() {
    return getMarsReceipt().toString();
  }

  public MarsReceipt convertReceiptToMars(final ExampleRepoReceipt receipt, final IsaJson isaJson) {
    buildMarsReceipt(
        receipt.studyAccessionsMap,
        receipt.sampleAccessionsMap,
        receipt.sourceAccessionsMap,
        receipt.otherMaterialsAccessionsMap,
        receipt.dataFilesAccessionsMap,
        receipt.info,
        receipt.errors,
        isaJson);
    return getMarsReceipt();
  }
}
```

Use `ReceiptAccessionsMap` instances to describe which ISA-JSON field identifies
each object and which repository accession belongs to each field value. The base
provider can then produce `MarsAccession` entries pointing back to the relevant
study, source, sample, other material, or data file in the ISA-JSON document.

### Package Layout

```text
com.elixir.mars.repository
com.elixir.mars.repository.models.isa
com.elixir.mars.repository.models.receipt
```


## Python

Built with Hatchling. Requires Python 3.10+.

```sh
cd python
pip install -e .
```

See [`python/sample.py`](python/sample.py) for a complete example.

### Releasing a New Version

To release a new version of the Python library:

1. Update the `version` field in `python/pyproject.toml` and commit the version bump.
2. Create and push a Git tag for the version **prefixed with `py-*`**. For example:

   ```bash
   git tag py-v0.0.2
   git push origin py-v0.0.2
   ```

3. After the release is published, bump the version in `python/pyproject.toml` to the next
   development version (e.g. `0.0.3.dev0`) and commit that change so the package is ready for further development.
4. Create a release based on the tag on Github to produce a changelog.

## License

This project is licensed under the MIT License. See [LICENSE](LICENSE).
