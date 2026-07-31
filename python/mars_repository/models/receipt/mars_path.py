from __future__ import annotations
from typing import TypedDict
from .mars_where import MarsWhere


class MarsPath(TypedDict, total=False):
    key: str
    where: MarsWhere
