from dataclasses import dataclass, field
from typing import Optional
from .component_type import ComponentType


@dataclass
class Component:
    component_name: Optional[str] = None
    component_type: Optional[ComponentType] = None
