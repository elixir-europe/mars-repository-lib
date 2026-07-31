from __future__ import annotations
from typing import TypedDict

class ComponentType(TypedDict, total=False):
    annotation_value: str
    term_source: str
    term_accession: str
