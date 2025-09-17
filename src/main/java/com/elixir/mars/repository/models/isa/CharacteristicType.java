package com.elixir.mars.repository.models.isa;

import lombok.Data;
import lombok.experimental.FieldNameConstants;

@Data
@FieldNameConstants
public class CharacteristicType {
  public String annotationValue;
  public String termAccession;
  public String termSource;
}
