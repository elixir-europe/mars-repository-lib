package com.elixir.mars.repository.models.receipt;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/** Repository-provided comment to add to an assay in ISA-JSON. */
@Builder
@Data
public class MarsAssayComment {
    public String name;

    public String value;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public List<MarsPath> path;
}
