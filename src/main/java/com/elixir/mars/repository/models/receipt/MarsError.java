package com.elixir.mars.repository.models.receipt;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Builder
@Data
public class MarsError {
    public MarsErrorType type;

    public String message;

    @JsonInclude(Include.NON_NULL)
    public List<MarsPath> path;
}
