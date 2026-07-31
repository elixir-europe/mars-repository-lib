from __future__ import annotations
from typing import TypedDict
from .component_type import ComponentType

class Component(TypedDict, total=False):
    component_name: str
    component_type: ComponentType
