from dataclasses import dataclass, field
from typing import Optional
from .mars_where import MarsWhere


@dataclass
class MarsPath:
    key: str
    where: Optional[MarsWhere] = None
