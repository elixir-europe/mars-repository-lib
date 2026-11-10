from __future__ import annotations

from typing import Any, List, TypedDict

from .comment import Comment
from .person import Person
from .study import Study


class IsaJson(TypedDict, total=False):
    identifier: str
    title: str
    description: str
    submission_date: str
    public_release_date: str
    ontology_source_references: List[Any]
    filename: str
    comments: List[Comment]
    publications: List[Any]
    people: List[Person]
    studies: List[Study]
