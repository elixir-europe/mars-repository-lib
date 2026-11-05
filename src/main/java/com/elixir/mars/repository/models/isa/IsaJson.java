package com.elixir.mars.repository.models.isa;

import java.util.ArrayList;
import lombok.Data;
import lombok.experimental.FieldNameConstants;

/** The root of an ISA-JSON document, which is the investigation level. */
@Data
@FieldNameConstants
public class IsaJson {
  public String identifier;
  public String title;
  public String description;
  public String submissionDate;
  public String publicReleaseDate;
  public ArrayList<Object> ontologySourceReferences;
  public String filename;
  public ArrayList<Comment> comments;
  public ArrayList<Object> publications;
  public ArrayList<Person> people;
  public ArrayList<Study> studies;
}
