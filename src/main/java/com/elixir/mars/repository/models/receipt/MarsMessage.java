package com.elixir.mars.repository.models.receipt;

import lombok.Builder;
import lombok.Builder.Default;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Builder
@Data
public class MarsMessage {
    @Default
    public List<MarsError> errors = new ArrayList<>();

    @Default
    public List<MarsInfo> info = new ArrayList<>();
}
