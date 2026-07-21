from dataclasses import dataclass, field
from typing import Optional
from .characteristic_type import CharacteristicType


@dataclass
class CharacteristicCategory:
    characteristic_type: Optional[CharacteristicType] = None
    id: Optional[str] = None
