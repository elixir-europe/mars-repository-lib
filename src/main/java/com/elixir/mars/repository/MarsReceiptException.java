package com.elixir.mars.repository;

import java.util.List;
import java.util.Optional;

import com.elixir.mars.repository.models.receipt.MarsError;
import com.elixir.mars.repository.models.receipt.MarsPath;

import lombok.Getter;

public class MarsReceiptException extends RuntimeException {
    @Getter
    private final MarsError error;

    public MarsReceiptException(final String receiptErrorMessage) {
        error = MarsError.builder()
                .message(receiptErrorMessage)
                .build();
    }

    public MarsReceiptException(final Exception exception, final String receiptErrorMessage) {
        this.addSuppressed(exception);
        if (exception instanceof MarsReceiptException) {
            error = ((MarsReceiptException) exception).error;
        } else {
            error = MarsError.builder()
                    .message(receiptErrorMessage)
                    .build();
        }
    }

    public MarsReceiptException(final String receiptErrorMessage, final MarsPath... errorPath) {
        error = MarsError.builder()
                .message(receiptErrorMessage)
                .path(List.of(Optional.ofNullable(errorPath).orElse(new MarsPath[0])))
                .build();
    }

    public MarsReceiptException(
            final Exception exception,
            final String receiptErrorMessage,
            final MarsPath... errorPath) {
        this.addSuppressed(exception);
        if (exception instanceof MarsReceiptException) {
            error = ((MarsReceiptException) exception).error;
        } else {
            error = MarsError.builder()
                    .message(receiptErrorMessage)
                    .path(List.of(Optional.ofNullable(errorPath).orElse(new MarsPath[0])))
                    .build();
        }
    }
}
