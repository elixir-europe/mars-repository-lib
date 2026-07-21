from dataclasses import dataclass, field
from typing import Optional


@dataclass
class PreviousProcess:
    id: Optional[str] = None
