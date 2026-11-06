package com.elixir.mars.repository.models.isa;

import lombok.Data;
import lombok.experimental.FieldNameConstants;

import java.util.ArrayList;

@Data
@FieldNameConstants
public class Unit {
    public String termSource;
    public String termAccession;
    public ArrayList<Object> comments;
}
