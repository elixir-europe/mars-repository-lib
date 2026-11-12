from __future__ import annotations
from typing import List, TypedDict
from .mars_assay_comment import MarsAssayComment
from .mars_ftp_credentials import MarsFtpCredentials


class MarsInitiateResponse(TypedDict, total=False):
    submission_id: str
    ftp_credentials: MarsFtpCredentials
    assay_comments: List[MarsAssayComment]
