from __future__ import annotations
from typing import Any, List, TypedDict
from .comment import Comment
from .person import Person
from .study import Study

class Investigation(TypedDict, total=False):
    ontology_source_references: List[Any]
    comments: List[Comment]
    publications: List[Any]
    people: List[Person]
    studies: List[Study]
    identifier: str
    title: str
    description: str
    submission_date: str
    public_release_date: str
    filename: str
