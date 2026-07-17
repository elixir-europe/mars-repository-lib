from dataclasses import dataclass, field
from typing import Any, List, Optional
from .comment import Comment
from .person import Person
from .characteristic_category import CharacteristicCategory
from .materials import Materials
from .protocol import Protocol
from .process_sequence import ProcessSequence
from .assay import Assay


@dataclass
class Study:
    comments: Optional[List[Comment]] = None
    publications: Optional[List[Any]] = None
    people: Optional[List[Person]] = None
    study_design_descriptors: Optional[List[Any]] = None
    characteristic_categories: Optional[List[CharacteristicCategory]] = None
    materials: Optional[Materials] = None
    protocols: Optional[List[Protocol]] = None
    process_sequence: Optional[List[ProcessSequence]] = None
    assays: Optional[List[Assay]] = None
    factors: Optional[List[Any]] = None
    unit_categories: Optional[List[Any]] = None
    identifier: Optional[str] = None
    title: Optional[str] = None
    description: Optional[str] = None
    submission_date: Optional[str] = None
    public_release_date: Optional[str] = None
    filename: Optional[str] = None
