from dataclasses import dataclass, field
from typing import Any, List, Optional
from .measurement_type import MeasurementType
from .technology_type import TechnologyType
from .characteristic_category import CharacteristicCategory
from .materials import Materials
from .process_sequence import ProcessSequence
from .data_file import DataFile
from .comment import Comment


@dataclass
class Assay:
    measurement_type: Optional[MeasurementType] = None
    technology_type: Optional[TechnologyType] = None
    characteristic_categories: Optional[List[CharacteristicCategory]] = None
    materials: Optional[Materials] = None
    process_sequence: Optional[List[ProcessSequence]] = None
    data_files: Optional[List[DataFile]] = None
    comments: Optional[List[Comment]] = None
    unit_categories: Optional[List[Any]] = None
    id: Optional[str] = None
    filename: Optional[str] = None
    technology_platform: Optional[str] = None
