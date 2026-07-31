from __future__ import annotations
from typing import TypedDict

class Value(TypedDict, total=False):
    annotation_value: str
    term_source: str
    term_accession: str
