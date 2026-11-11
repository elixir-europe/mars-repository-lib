package com.elixir.mars.repository.models.receipt;

import lombok.Builder;
import lombok.Data;

/** Repository-provided comment to add to an assay in ISA-JSON. */
@Builder
@Data
public class MarsAssayComment {
    public String name;

    public String value;
}
