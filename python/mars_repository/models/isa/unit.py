from dataclasses import dataclass, field
from typing import Any, List, Optional


@dataclass
class Unit:
    term_source: Optional[str] = None
    term_accession: Optional[str] = None
    comments: Optional[List[Any]] = None
