package com.elixir.mars.repository.models.isa;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.experimental.FieldNameConstants;

@Data
@FieldNameConstants
public class NextProcess {
  @JsonProperty("@id")
  public String id;
}
