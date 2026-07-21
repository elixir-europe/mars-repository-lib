from dataclasses import dataclass, field
from typing import Optional
from .investigation import Investigation


@dataclass
class IsaJson:
    investigation: Optional[Investigation] = None
