from dataclasses import dataclass, field
from typing import Optional


@dataclass
class ParameterName:
    annotation_value: Optional[str] = None
    term_accession: Optional[str] = None
    term_source: Optional[str] = None
