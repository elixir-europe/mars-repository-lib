package com.elixir.mars.repository.models.receipt;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Builder
@Data
public class MarsReceipt {
    public String targetRepository;

    public List<MarsError> errors;

    public List<MarsInfo> info;

    public List<MarsAccession> accessions;
}
