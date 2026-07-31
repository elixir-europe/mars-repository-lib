from __future__ import annotations
from typing import Any, List, TypedDict
from .characteristic import Characteristic
from .derives_from import DerivesFrom

class OtherMaterial(TypedDict, total=False):
    characteristics: List[Characteristic]
    derives_from: List[DerivesFrom]
    id: str
    name: str
    type: str
