from __future__ import annotations
from typing import Any, List, TypedDict
from .characteristic import Characteristic

class Source(TypedDict, total=False):
    characteristics: List[Characteristic]
    id: str
    name: str
