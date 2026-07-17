from dataclasses import dataclass, field
from typing import Optional


@dataclass
class Comment:
    name: str = ""
    value: str = ""
    id: Optional[str] = None
