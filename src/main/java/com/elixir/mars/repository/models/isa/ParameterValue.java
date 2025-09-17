package com.elixir.mars.repository.models.isa;

import lombok.Data;
import lombok.experimental.FieldNameConstants;

@Data
@FieldNameConstants
public class ParameterValue {
  public Category category;
  public Value value;
  public Unit unit;
}
