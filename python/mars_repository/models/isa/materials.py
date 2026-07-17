from dataclasses import dataclass, field
from typing import Any, List, Optional
from .source import Source
from .sample import Sample
from .other_material import OtherMaterial


@dataclass
class Materials:
    sources: Optional[List[Source]] = None
    samples: Optional[List[Sample]] = None
    other_materials: Optional[List[OtherMaterial]] = None
