from __future__ import annotations
from typing import Any, List, TypedDict
from .executes_protocol import ExecutesProtocol
from .parameter_value import ParameterValue
from .previous_process import PreviousProcess
from .next_process import NextProcess
from .input import Input
from .output import Output

class ProcessSequence(TypedDict, total=False):
    executes_protocol: ExecutesProtocol
    parameter_values: List[ParameterValue]
    previous_process: PreviousProcess
    next_process: NextProcess
    inputs: List[Input]
    outputs: List[Output]
    id: str
    name: str
    performer: str
    date: str
