package com.github.sofm.common.http.client;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@AllArgsConstructor
@Slf4j
@Data
public class HttpProps {

  private Integer socketTimeout;

  private Integer connectTimeout;

  private Integer connectRequestTimeout;

  private Integer defaultHttpCode;

  public HttpProps() {
    if (socketTimeout == null) {
      socketTimeout = 30000;
    }

    if (connectTimeout == null) {
      connectTimeout = 30000;
    }

    if (connectRequestTimeout == null) {
      connectRequestTimeout = 30000;
    }

    if (defaultHttpCode == null) {
      defaultHttpCode = 500;
    }
  }
}
