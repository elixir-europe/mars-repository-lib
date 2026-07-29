from __future__ import annotations
from typing import Any, List, TypedDict

class Unit(TypedDict, total=False):
    term_source: str
    term_accession: str
    comments: List[Any]
