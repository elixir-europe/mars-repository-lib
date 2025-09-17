package com.elixir.mars.repository.models.isa;

import lombok.Data;
import lombok.experimental.FieldNameConstants;

@Data
@FieldNameConstants
public class Role {
  public String termAccession;
  public String termSource;
  public String annotationValue;
}
