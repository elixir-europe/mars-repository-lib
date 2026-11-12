from __future__ import annotations
from typing import List, TypedDict

from .mars_path import MarsPath


class MarsAssayComment(TypedDict, total=False):
    name: str
    value: str
    path: List[MarsPath]
