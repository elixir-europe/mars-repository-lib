from __future__ import annotations
from typing import Any, List, TypedDict
from .comment import Comment
from .person import Person
from .characteristic_category import CharacteristicCategory
from .materials import Materials
from .protocol import Protocol
from .process_sequence import ProcessSequence
from .assay import Assay

class Study(TypedDict, total=False):
    comments: List[Comment]
    publications: List[Any]
    people: List[Person]
    study_design_descriptors: List[Any]
    characteristic_categories: List[CharacteristicCategory]
    materials: Materials
    protocols: List[Protocol]
    process_sequence: List[ProcessSequence]
    assays: List[Assay]
    factors: List[Any]
    unit_categories: List[Any]
    identifier: str
    title: str
    description: str
    submission_date: str
    public_release_date: str
    filename: str
