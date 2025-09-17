package com.elixir.mars.repository.models.receipt;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class MarsWhere {
  public String key;

  public String value;
}
