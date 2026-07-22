package com.elixir.mars.repository.models.isa;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.experimental.FieldNameConstants;

@Data
@FieldNameConstants
@JsonIgnoreProperties(ignoreUnknown = true)
public class Input {
  @JsonProperty("@id")
  public String id;
}
