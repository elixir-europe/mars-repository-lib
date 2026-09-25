from __future__ import annotations
from typing import TypedDict
from .characteristic_type import CharacteristicType

class Category(TypedDict, total=False):
    characteristic_type: CharacteristicType
    id: str
