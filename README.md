Shared Java library for repository integrations in the
[MARS framework](https://github.com/elixir-europe/MARS).

The library contains the common pieces that target repository services need when
they translate repository-specific responses back into a MARS receipt:

- ISA-JSON model classes used by repository services
- MARS receipt model classes

Repository service modules, such as ENA or BioSamples converters, should use this
library instead of keeping local copies of the receipt and ISA model code.

## Languages

| Language | Directory | Package | Version |
|---|---|---|---|
| Java 17 | `src/` | `com.elixir.mars:mars-repository` | 0.0.2 |
| Python ≥3.10 | `python/` | `mars-repository-py` | 0.0.2 |

## Java

Built with Gradle. Requires Java 17.

```sh
./gradlew build
```

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

### Creating a Receipt Provider

Target repository integrations typically extend `MarsReceiptProvider`, collect
repository accessions, and let the base class build the MARS receipt paths.

```java
public final class MarsEnaReceiptProvider extends MarsReceiptProvider {

  public MarsEnaReceiptProvider() {
    super("ena");
  }

  @Override
  public String convertMarsReceiptToJson() {
    return getMarsReceipt().toString();
  }

  public MarsReceipt convertReceiptToMars(final MyEnaReceipt receipt, final IsaJson isaJson) {
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

## License

This project is licensed under the MIT License. See [LICENSE](LICENSE).
