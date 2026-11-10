from __future__ import annotations
from typing import TypedDict

class Role(TypedDict, total=False):
    term_accession: str
    term_source: str
    annotation_value: str
