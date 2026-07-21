from dataclasses import dataclass, field
from typing import List, Optional
from .mars_error import MarsError
from .mars_info import MarsInfo
from .mars_accession import MarsAccession


@dataclass
class MarsReceipt:
    target_repository: str = ""
    errors: Optional[List[MarsError]] = None
    info: Optional[List[MarsInfo]] = None
    accessions: Optional[List[MarsAccession]] = None
