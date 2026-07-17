from dataclasses import dataclass, field
from typing import Any, List, Optional
from .derives_from import DerivesFrom
from .characteristic import Characteristic
from .factor_value import FactorValue


@dataclass
class Sample:
    derives_from: Optional[List[DerivesFrom]] = None
    characteristics: Optional[List[Characteristic]] = None
    factor_values: Optional[List[FactorValue]] = None
    id: Optional[str] = None
    name: Optional[str] = None
