from __future__ import annotations
from typing import Any, List, TypedDict
from .role import Role
from .comment import Comment

class Person(TypedDict, total=False):
    roles: List[Role]
    comments: List[Comment]
    id: str
    last_name: str
    first_name: str
    mid_initials: str
    email: str
    phone: str
    fax: str
    address: str
    affiliation: str
