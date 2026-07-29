from __future__ import annotations
from typing import List, TypedDict
from .mars_error import MarsError
from .mars_info import MarsInfo
from .mars_accession import MarsAccession


class MarsReceipt(TypedDict, total=False):
    target_repository: str
    errors: List[MarsError]
    info: List[MarsInfo]
    accessions: List[MarsAccession]
