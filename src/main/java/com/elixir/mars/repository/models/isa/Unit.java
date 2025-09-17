package com.elixir.mars.repository.models.isa;

import java.util.ArrayList;
import lombok.Data;
import lombok.experimental.FieldNameConstants;

@Data
@FieldNameConstants
public class Unit {
  public String termSource;
  public String termAccession;
  public ArrayList<Object> comments;
}
