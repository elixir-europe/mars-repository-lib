from __future__ import annotations
from typing import TypedDict

class Comment(TypedDict, total=False):
    name: str
    value: str
    id: str
