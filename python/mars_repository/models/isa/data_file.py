from __future__ import annotations
from typing import Any, List, TypedDict
from .comment import Comment

class DataFile(TypedDict, total=False):
    comments: List[Comment]
    id: str
    name: str
    type: str
