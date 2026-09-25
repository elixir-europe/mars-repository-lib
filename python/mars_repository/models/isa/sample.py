from __future__ import annotations
from typing import Any, List, TypedDict
from .derives_from import DerivesFrom
from .characteristic import Characteristic
from .factor_value import FactorValue

class Sample(TypedDict, total=False):
    derives_from: List[DerivesFrom]
    characteristics: List[Characteristic]
    factor_values: List[FactorValue]
    id: str
    name: str
