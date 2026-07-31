from __future__ import annotations
from typing import List, TypedDict
from .mars_error import MarsError
from .mars_info import MarsInfo


class MarsMessage(TypedDict, total=False):
    errors: List[MarsError]
    info: List[MarsInfo]
