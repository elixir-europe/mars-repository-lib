from __future__ import annotations
from typing import TypedDict


class MarsFtpCredentials(TypedDict, total=False):
    host: str
    upload_path: str
    username: str
    password: str
