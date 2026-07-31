from __future__ import annotations
from typing import Any, List, TypedDict
from .measurement_type import MeasurementType
from .technology_type import TechnologyType
from .characteristic_category import CharacteristicCategory
from .materials import Materials
from .process_sequence import ProcessSequence
from .data_file import DataFile
from .comment import Comment

class Assay(TypedDict, total=False):
    measurement_type: MeasurementType
    technology_type: TechnologyType
    characteristic_categories: List[CharacteristicCategory]
    materials: Materials
    process_sequence: List[ProcessSequence]
    data_files: List[DataFile]
    comments: List[Comment]
    unit_categories: List[Any]
    id: str
    filename: str
    technology_platform: str
