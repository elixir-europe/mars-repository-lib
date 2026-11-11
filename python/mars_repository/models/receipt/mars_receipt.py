from __future__ import annotations
from typing import List, TypedDict
from .mars_assay_comment import MarsAssayComment
from .mars_error import MarsError
from .mars_info import MarsInfo
from .mars_accession import MarsAccession
from .mars_submission_status import MarsSubmissionStatus


class MarsReceipt(TypedDict, total=False):
    target_repository: str
    errors: List[MarsError]
    info: List[MarsInfo]
    accessions: List[MarsAccession]
    submission_status: MarsSubmissionStatus
    assay_comments: List[MarsAssayComment]
