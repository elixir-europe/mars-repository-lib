from __future__ import annotations
from typing import TypedDict
from .parameter_name import ParameterName

class Parameter(TypedDict, total=False):
    parameter_name: ParameterName
    id: str
