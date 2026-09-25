from __future__ import annotations
from typing import List, TypedDict
from .mars_path import MarsPath


class MarsAccession(TypedDict, total=False):
    value: str
    path: List[MarsPath]
