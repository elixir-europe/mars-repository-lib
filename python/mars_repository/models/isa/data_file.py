from dataclasses import dataclass, field
from typing import Any, List, Optional
from .comment import Comment


@dataclass
class DataFile:
    comments: Optional[List[Comment]] = None
    id: Optional[str] = None
    name: Optional[str] = None
    type: Optional[str] = None
