package com.github.sofm.common.http.client.service;

import com.github.sofm.common.http.client.HttpMethod;
import com.github.sofm.common.http.client.HttpProps;
import com.github.sofm.common.http.client.HttpResult;
import com.github.sofm.common.util.ExceptionUtil;
import com.github.sofm.common.util.StringUtil;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.auth.AuthenticationException;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

@Slf4j
public class DefaultHttpClient implements HttpClient {

  private HttpProps httpProps;

  public DefaultHttpClient(HttpProps httpProps) {
    this.httpProps = httpProps;
  }

  @Override
  public HttpResult download(String url, String storageFolder) {
    HttpResult result = new HttpResult(httpProps.getDefaultHttpCode());
    CloseableHttpClient httpClient = null;
    HttpGet getMethod = new HttpGet(url);

    try (FileOutputStream fos = new FileOutputStream(storageFolder)) {
      httpClient = HttpClients.createDefault();
      HttpResponse response = httpClient.execute(getMethod);

      HttpEntity entity = response.getEntity();
      if (entity != null) {
        entity.writeTo(fos);
        result.setStatusCode(response.getStatusLine().getStatusCode());
      }
    } catch (Exception ex) {
      log.error("(download)url: {},  ex: {}", url, ExceptionUtil.getFullStackTrace(ex, true));
    } finally {
      destroy(httpClient, null);
    }

    return result;
  }

  @Override
  public HttpResult post(String url, Map<String, String> headers, String entity) {
    return query(HttpMethod.POST, url, false, headers, null, entity, false, null, null, null);
  }

  @Override
  public HttpResult post(String url, Map<String, String> headers, Map<String, String> params) {
    return query(HttpMethod.POST, url, false, headers, params, null, false, null, null, null);
  }

  @Override
  public HttpResult query(HttpMethod method, String url, boolean allowRedirect) {
    return query(method, url, allowRedirect, null);
  }

  @Override
  public HttpResult query(
      HttpMethod method, String url, boolean allowRedirect, Map<String, String> headers) {
    return query(method, url, allowRedirect, headers, null, null, false, null, null, null);
  }

  @Override
  public HttpResult query(
      HttpMethod method,
      String url,
      boolean allowRedirect,
      Map<String, String> headers,
      Map<String, String> params) {
    return query(method, url, allowRedirect, headers, params, null, false, null, null, null);
  }

  @Override
  public HttpResult query(
      HttpMethod method,
      String url,
      boolean allowRedirect,
      Map<String, String> headers,
      String entity) {
    return query(method, url, allowRedirect, headers, null, entity, false, null, null, null);
  }

  @Override
  public HttpResult query(String url, int hardTimeout) {
    return query(url, hardTimeout, false);
  }

  @Override
  public HttpResult query(String url, int hardTimeout, boolean isLazy) {
    HttpResult result = new HttpResult(httpProps.getDefaultHttpCode());
    CloseableHttpClient httpClient = null;
    final HttpGet getMethod = new HttpGet(url);
    TimerTask task =
        new TimerTask() {
          public void run() {
            getMethod.abort();
          }
        };
    (new Timer(true)).schedule(task, (hardTimeout * 1000));

    try {
      httpClient = HttpClients.createDefault();
      HttpResponse response = httpClient.execute(getMethod);
      result.setStatusCode(response.getStatusLine().getStatusCode());
      if (isLazy) {
        result.setBody(EntityUtils.toString(response.getEntity()));
      } else {
        result.setBody(response.getEntity().getContent().toString());
      }
    } catch (IOException ex) {
      log.error("(query)ex: {}", ExceptionUtil.getFullStackTrace(ex, true));
    } finally {
      destroy(httpClient, null);
    }

    return result;
  }

  @Override
  public HttpResult query(
      HttpMethod method,
      String url,
      boolean allowRedirect,
      Map<String, String> headers,
      Map<String, String> params,
      String entity,
      boolean isUseBasicAuthentication,
      String username,
      String password,
      BasicCookieStore cookieStore) {
    HttpResult result = new HttpResult(httpProps.getDefaultHttpCode());
    HttpClientContext context = HttpClientContext.create();
    context.setCookieStore(cookieStore);
    CloseableHttpClient httpclient = HttpClients.createDefault();
    CloseableHttpResponse response = null;

    try {
      if (method.equals(HttpMethod.HEAD)) {
        HttpHead request = new HttpHead(url);
        request.setConfig(getRequestConfig(allowRedirect));
        setBasicAuthentication(request, isUseBasicAuthentication, username, password);
        setHeaders(request, headers);
        response = httpclient.execute(request, context);
      } else if (method.equals(HttpMethod.GET) || method.equals(HttpMethod.DELETE)) {
        HttpRequestBase request;
        if (method.equals(HttpMethod.GET)) {
          request = new HttpGet(url);
        } else {
          request = new HttpDelete(url);
        }

        request.setConfig(getRequestConfig(allowRedirect));
        setBasicAuthentication(request, isUseBasicAuthentication, username, password);
        setHeaders(request, headers);
        response = httpclient.execute(request, context);
      } else {
        HttpEntityEnclosingRequestBase request;
        if (method.equals(HttpMethod.PUT)) {
          request = new HttpPut(url);
        } else {
          request = new HttpPost(url);
        }

        request.setConfig(getRequestConfig(allowRedirect));
        setBasicAuthentication(request, isUseBasicAuthentication, username, password);
        setHeaders(request, headers);
        if (params != null) {
          List<NameValuePair> parameters = new ArrayList<>();

          for (Map.Entry<String, String> entry : params.entrySet()) {
            parameters.add(new BasicNameValuePair(entry.getKey(), entry.getValue()));
          }

          request.setEntity(new UrlEncodedFormEntity(parameters, StandardCharsets.UTF_8.name()));
        }

        if (entity != null) {
          request.setEntity(new StringEntity(entity, StandardCharsets.UTF_8.name()));
        }

        response = httpclient.execute(request, context);
      }

      result.setStatusCode(response.getStatusLine().getStatusCode());
      if (response.getEntity() != null) {
        result.setBody(EntityUtils.toString(response.getEntity()));
        Multimap<String, String> responseHeaders = ArrayListMultimap.create();

        for (Header header : response.getAllHeaders()) {
          responseHeaders.put(header.getName(), header.getValue());
        }

        result.setHeaders(responseHeaders);
      } else {
        result.setBody("");
      }

    } catch (AuthenticationException | IOException ex) {
      log.error("(query)url: {}, ex: {}", url, ExceptionUtil.getFullStackTrace(ex, true));
    } finally {
      destroy(httpclient, response);
    }

    return result;
  }

  @Override
  public HttpResult upload(
      String url, Map<String, String> headers, Map<String, String> params, String filepath) {
    HttpResult result = new HttpResult(httpProps.getDefaultHttpCode());
    CloseableHttpClient closeableHttpClient = null;
    CloseableHttpResponse response = null;

    try {
      closeableHttpClient = HttpClients.createDefault();
      HttpPost request = new HttpPost(url);
      request.setConfig(getRequestConfig(false));
      setHeaders(request, headers);
      response = closeableHttpClient.execute(request);
      result.setStatusCode(response.getStatusLine().getStatusCode());
      result.setBody(StringUtil.toString(response.getEntity().getContent()));
    } catch (IOException ex) {
      log.error("(upload)url: {}, ex: {}", url, ExceptionUtil.getFullStackTrace(ex, true));
    } finally {
      destroy(closeableHttpClient, response);
    }

    return result;
  }

  private void destroy(CloseableHttpClient httpclient, CloseableHttpResponse response) {
    try {
      if (response != null) {
        response.close();
      }

      if (httpclient != null) {
        httpclient.close();
      }
    } catch (Exception ex) {
      log.error("(destroy)ex: {}", ExceptionUtil.getFullStackTrace(ex, true));
    }
  }

  private RequestConfig getRequestConfig(boolean allowRedirect) {
    return RequestConfig.custom()
        .setSocketTimeout(httpProps.getSocketTimeout())
        .setConnectTimeout(httpProps.getConnectTimeout())
        .setConnectionRequestTimeout(httpProps.getConnectRequestTimeout())
        .setRedirectsEnabled(allowRedirect)
        .build();
  }

  private void setBasicAuthentication(
      HttpRequestBase request, boolean isUseBasicAuthentication, String username, String password)
      throws AuthenticationException {
    if (isUseBasicAuthentication) {
      UsernamePasswordCredentials creds = new UsernamePasswordCredentials(username, password);
      request.addHeader(new BasicScheme().authenticate(creds, request, null));
    }
  }

  private void setHeaders(HttpRequestBase request, Map<String, String> headers) {
    if (headers != null) {
      for (Map.Entry<String, String> entry : headers.entrySet()) {
        request.setHeader(entry.getKey(), entry.getValue());
      }
    }
  }
}
