from __future__ import annotations
from typing import TypedDict
from .investigation import Investigation

class IsaJson(TypedDict, total=False):
    investigation: Investigation
