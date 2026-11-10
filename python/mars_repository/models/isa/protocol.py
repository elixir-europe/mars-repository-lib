from __future__ import annotations
from typing import Any, List, TypedDict
from .protocol_type import ProtocolType
from .parameter import Parameter
from .component import Component

class Protocol(TypedDict, total=False):
    parameters: List[Parameter]
    components: List[Component]
    id: str
    name: str
    protocol_type: ProtocolType
    description: str
    uri: str
    version: str
