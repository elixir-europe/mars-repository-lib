from dataclasses import dataclass, field
from typing import Any, List, Optional
from .role import Role
from .comment import Comment


@dataclass
class Person:
    roles: Optional[List[Role]] = None
    comments: Optional[List[Comment]] = None
    id: Optional[str] = None
    last_name: Optional[str] = None
    first_name: Optional[str] = None
    mid_initials: Optional[str] = None
    email: Optional[str] = None
    phone: Optional[str] = None
    fax: Optional[str] = None
    address: Optional[str] = None
    affiliation: Optional[str] = None
