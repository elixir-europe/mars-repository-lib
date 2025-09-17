package com.elixir.mars.repository.models.isa;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.experimental.FieldNameConstants;

@Data
@FieldNameConstants
@JsonIgnoreProperties(ignoreUnknown = true)
public class ParameterName {
  public String annotationValue;
  public String termAccession;
  public String termSource;
}
