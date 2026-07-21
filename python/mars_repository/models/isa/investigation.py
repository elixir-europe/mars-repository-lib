from dataclasses import dataclass, field
from typing import Any, List, Optional
from .comment import Comment
from .person import Person
from .study import Study


@dataclass
class Investigation:
    ontology_source_references: Optional[List[Any]] = None
    comments: Optional[List[Comment]] = None
    publications: Optional[List[Any]] = None
    people: Optional[List[Person]] = None
    studies: Optional[List[Study]] = None
    identifier: Optional[str] = None
    title: Optional[str] = None
    description: Optional[str] = None
    submission_date: Optional[str] = None
    public_release_date: Optional[str] = None
    filename: Optional[str] = None
