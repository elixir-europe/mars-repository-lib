package com.elixir.mars.repository.models.receipt;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Builder
@Data
public class MarsAccession {
    public String value;

    public List<MarsPath> path;
}
