from dataclasses import dataclass, field
from typing import Optional


@dataclass
class MarsInfo:
    name: Optional[str] = None
    message: str = ""
