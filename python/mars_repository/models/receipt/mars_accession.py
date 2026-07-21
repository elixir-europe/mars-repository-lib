from dataclasses import dataclass, field
from typing import List
from .mars_path import MarsPath


@dataclass
class MarsAccession:
    value: str
    path: List[MarsPath] = field(default_factory=list)
