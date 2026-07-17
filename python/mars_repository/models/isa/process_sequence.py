from dataclasses import dataclass, field
from typing import Any, List, Optional
from .executes_protocol import ExecutesProtocol
from .parameter_value import ParameterValue
from .previous_process import PreviousProcess
from .next_process import NextProcess
from .input import Input
from .output import Output


@dataclass
class ProcessSequence:
    executes_protocol: Optional[ExecutesProtocol] = None
    parameter_values: Optional[List[ParameterValue]] = None
    previous_process: Optional[PreviousProcess] = None
    next_process: Optional[NextProcess] = None
    inputs: Optional[List[Input]] = None
    outputs: Optional[List[Output]] = None
    id: Optional[str] = None
    name: Optional[str] = None
    performer: Optional[str] = None
    date: Optional[str] = None
