package com.elixir.mars.repository.models.receipt;

import java.util.List;
import lombok.Builder;
import lombok.Data;

/** Response returned when a repository submission is initiated. */
@Builder
@Data
public class MarsInitiateResponse {
    public String submissionId;

    public MarsFtpCredentials ftpCredentials;

    public List<MarsAssayComment> assayComments;
}
