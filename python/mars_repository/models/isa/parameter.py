from dataclasses import dataclass, field
from typing import Optional
from .parameter_name import ParameterName


@dataclass
class Parameter:
    parameter_name: Optional[ParameterName] = None
    id: Optional[str] = None
