package com.github.sofm.common.http.client.service;

import com.github.sofm.common.http.client.HttpMethod;
import com.github.sofm.common.http.client.HttpResult;
import java.util.Map;
import org.apache.http.impl.client.BasicCookieStore;

public interface HttpClient {

  /**
   * Download file
   *
   * @param url
   * @param storageFolder
   * @return
   */
  HttpResult download(String url, String storageFolder);

  /**
   * Post with String body
   *
   * @param url
   * @param headers
   * @param entity
   * @return
   */
  HttpResult post(String url, Map<String, String> headers, String entity);

  /**
   * Post with multiple params
   *
   * @param url
   * @param headers
   * @param params
   * @return
   */
  HttpResult post(String url, Map<String, String> headers, Map<String, String> params);

  /**
   * Simple query
   *
   * @param method
   * @param url
   * @param allowRedirect
   * @return
   */
  HttpResult query(HttpMethod method, String url, boolean allowRedirect);

  /**
   * Query with headers
   *
   * @param method
   * @param url
   * @param allowRedirect
   * @param headers
   * @return
   */
  HttpResult query(
      HttpMethod method, String url, boolean allowRedirect, Map<String, String> headers);

  /**
   * Query with headers, params
   *
   * @param method
   * @param url
   * @param allowRedirect
   * @param headers
   * @param params
   * @return
   */
  HttpResult query(
      HttpMethod method,
      String url,
      boolean allowRedirect,
      Map<String, String> headers,
      Map<String, String> params);

  /**
   * Query with headers, String
   *
   * @param method
   * @param url
   * @param allowRedirect
   * @param headers
   * @param entity
   * @return
   */
  HttpResult query(
      HttpMethod method,
      String url,
      boolean allowRedirect,
      Map<String, String> headers,
      String entity);

  HttpResult query(
      HttpMethod method,
      String url,
      boolean allowRedirect,
      Map<String, String> headers,
      Map<String, String> params,
      String entity,
      boolean isUseBasicAuthentication,
      String username,
      String password,
      BasicCookieStore cookieStore);

  /**
   * Query with hard timeout
   *
   * @param url
   * @param hardTimeout
   * @return
   */
  HttpResult query(String url, int hardTimeout);

  /**
   * Query with hard timeout & lazy load
   *
   * @param url
   * @param hardTimeout
   * @param isLazy
   * @return
   */
  HttpResult query(String url, int hardTimeout, boolean isLazy);

  /**
   * Upload file
   *
   * @param url
   * @param headers
   * @param params
   * @param filepath
   * @return
   */
  HttpResult upload(
      String url, Map<String, String> headers, Map<String, String> params, String filepath);
}
