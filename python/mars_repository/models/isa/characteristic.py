from __future__ import annotations
from typing import TypedDict
from .category import Category
from .value import Value
from .unit import Unit

class Characteristic(TypedDict, total=False):
    category: Category
    value: Value
    unit: Unit
