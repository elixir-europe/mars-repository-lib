# mars-repository-lib

Library for target repositories of the [MARS framework](https://github.com/elixir-europe/MARS).

## Languages

| Language | Directory | Package | Version |
|---|---|---|---|
| Java 17 | `src/` | `com.elixir.mars:mars-repository` | 0.0.2-SNAPSHOT |
| Python ≥3.10 | `python/` | `mars-repository-py` | 0.0.2 |

## Java

Built with Gradle. Requires Java 17.

```sh
./gradlew build
```

## Python

Built with Hatchling. Requires Python 3.10+.

```sh
cd python
pip install -e .
```

```python
from mars_repository import build_mars_receipt, MarsReceipt
```
