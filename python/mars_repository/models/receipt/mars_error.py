from __future__ import annotations
from typing import List, TypedDict
from .mars_error_type import MarsErrorType
from .mars_path import MarsPath


class MarsError(TypedDict, total=False):
    type: MarsErrorType
    message: str
    path: List[MarsPath]
