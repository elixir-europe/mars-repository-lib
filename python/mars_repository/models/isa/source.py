from dataclasses import dataclass, field
from typing import Any, List, Optional
from .characteristic import Characteristic


@dataclass
class Source:
    characteristics: Optional[List[Characteristic]] = None
    id: Optional[str] = None
    name: Optional[str] = None
