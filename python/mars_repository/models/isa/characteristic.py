from dataclasses import dataclass, field
from typing import Optional
from .category import Category
from .value import Value
from .unit import Unit


@dataclass
class Characteristic:
    category: Optional[Category] = None
    value: Optional[Value] = None
    unit: Optional[Unit] = None
