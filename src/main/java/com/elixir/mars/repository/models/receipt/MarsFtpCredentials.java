package com.elixir.mars.repository.models.receipt;

import lombok.Builder;
import lombok.Data;

/** FTP upload details returned when a submission is initiated. */
@Builder
@Data
public class MarsFtpCredentials {
    public String host;

    public String uploadPath;

    public String username;

    public String password;
}
