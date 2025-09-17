package com.elixir.mars.repository.models.isa;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import lombok.Data;
import lombok.experimental.FieldNameConstants;

@Data
@FieldNameConstants
public class DataFile {
  @JsonProperty("@id")
  public String id;

  public String name;
  public String type;
  public ArrayList<Comment> comments;
}
