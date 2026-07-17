from dataclasses import dataclass, field
from typing import List
from .mars_error import MarsError
from .mars_info import MarsInfo


@dataclass
class MarsMessage:
    errors: List[MarsError] = field(default_factory=list)
    info: List[MarsInfo] = field(default_factory=list)
