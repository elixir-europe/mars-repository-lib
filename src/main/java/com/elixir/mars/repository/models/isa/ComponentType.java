package com.elixir.mars.repository.models.isa;

import lombok.Data;
import lombok.experimental.FieldNameConstants;

@Data
@FieldNameConstants
public class ComponentType {
  public String annotationValue;
  public String termSource;
  public String termAccession;
}
