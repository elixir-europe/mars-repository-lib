from dataclasses import dataclass, field
from typing import Optional


@dataclass
class Role:
    term_accession: Optional[str] = None
    term_source: Optional[str] = None
    annotation_value: Optional[str] = None
