from dataclasses import dataclass, field
from typing import Any, List, Optional
from .protocol_type import ProtocolType
from .parameter import Parameter
from .component import Component


@dataclass
class Protocol:
    parameters: Optional[List[Parameter]] = None
    components: Optional[List[Component]] = None
    id: Optional[str] = None
    name: Optional[str] = None
    protocol_type: Optional[ProtocolType] = None
    description: Optional[str] = None
    uri: Optional[str] = None
    version: Optional[str] = None
