from __future__ import annotations
from typing import Any, List, TypedDict
from .source import Source
from .sample import Sample
from .other_material import OtherMaterial

class Materials(TypedDict, total=False):
    sources: List[Source]
    samples: List[Sample]
    other_materials: List[OtherMaterial]
