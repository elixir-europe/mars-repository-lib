from dataclasses import dataclass, field
from typing import Any, List, Optional
from .characteristic import Characteristic
from .derives_from import DerivesFrom


@dataclass
class OtherMaterial:
    characteristics: Optional[List[Characteristic]] = None
    derives_from: Optional[List[DerivesFrom]] = None
    id: Optional[str] = None
    name: Optional[str] = None
    type: Optional[str] = None
