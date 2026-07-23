# mars-repository-lib

Shared Java library for repository integrations in the
[MARS framework](https://github.com/elixir-europe/MARS).

The library contains the common pieces that target repository services need when
they translate repository-specific responses back into a MARS receipt:

- ISA-JSON model classes used by repository services
- MARS receipt model classes
- `MarsReceiptProvider`, the base class for building MARS-compatible receipts
- `ReceiptAccessionsMap`, a small mapping object for connecting ISA-JSON fields
  to repository accessions
- `MarsReceiptException`, a receipt-aware runtime exception for conversion errors

Repository service modules, such as ENA or BioSamples converters, should use this
library instead of keeping local copies of the receipt and ISA model code.

## Requirements

- Java 17
- Gradle wrapper included in this repository

## Build

```bash
./gradlew build
```

## Publish Locally

MARS services can consume a local development version from your Maven local
repository:

```bash
./gradlew publishToMavenLocal
```

This publishes:

```text
com.elixir.mars:mars-repository:0.0.2-SNAPSHOT
```

Then make sure the consuming service has `mavenLocal()` enabled and depends on
the same version:

```gradle
repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    implementation 'com.elixir.mars:mars-repository:0.0.2-SNAPSHOT'
}
```

## Creating a Receipt Provider

Target repository integrations typically extend `MarsReceiptProvider`, collect
repository accessions, and let the base class build the MARS receipt paths.

```java
public final class EnaReceiptProvider extends MarsReceiptProvider {

  public EnaReceiptProvider() {
    super("ena");
  }

  @Override
  public String convertMarsReceiptToJson() {
    return getMarsReceipt().toString();
  }
}
```

Use `ReceiptAccessionsMap` instances to describe which ISA-JSON field identifies
each object and which repository accession belongs to each field value. The base
provider can then produce `MarsAccession` entries pointing back to the relevant
study, source, sample, other material, or data file in the ISA-JSON document.

## Package Layout

```text
com.elixir.mars.repository
com.elixir.mars.repository.models.isa
com.elixir.mars.repository.models.receipt
```

## License

This project is licensed under the MIT License. See [LICENSE](LICENSE).
