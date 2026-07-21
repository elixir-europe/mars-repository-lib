from typing import List, Optional

from .models.receipt.mars_error import MarsError
from .models.receipt.mars_path import MarsPath


class MarsReceiptException(RuntimeError):
    def __init__(
        self,
        receipt_error_message: str,
        error_path: Optional[List[MarsPath]] = None,
        exception: Optional[Exception] = None,
    ):
        if exception is not None:
            self.__cause__ = exception

        if isinstance(exception, MarsReceiptException):
            self.error = exception.error
        elif error_path is not None:
            self.error = MarsError(
                message=receipt_error_message,
                type=None,
                path=error_path,
            )
        else:
            self.error = MarsError(
                message=receipt_error_message,
                type=None,
                path=None,
            )

        super().__init__(receipt_error_message)
