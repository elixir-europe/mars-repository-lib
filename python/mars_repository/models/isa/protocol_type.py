from __future__ import annotations
from typing import TypedDict

class ProtocolType(TypedDict, total=False):
    annotation_value: str
    term_accession: str
    term_source: str
