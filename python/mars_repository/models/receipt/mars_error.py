from dataclasses import dataclass, field
from typing import List, Optional
from .mars_error_type import MarsErrorType
from .mars_path import MarsPath


@dataclass
class MarsError:
    type: MarsErrorType
    message: str
    path: Optional[List[MarsPath]] = None
