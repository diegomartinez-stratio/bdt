/*
 * Copyright (C) 2014 Stratio (http://stratio.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.stratio.qa.utils;

import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.cookie.Cookie;

import java.io.InputStream;
import java.util.List;

public class HttpResponse {

    private int statusCode;

    private String response;

    private List<Cookie> cookies;

    public HttpHeaders headers;

    private InputStream is;

    /**
     * Constructor of an HttpResponse.
     *
     * @param statusCode
     * @param response
     */
    public HttpResponse(Integer statusCode, String response, List<Cookie> cookies, HttpHeaders headers) {
        this.statusCode = statusCode;
        this.response = response;
        this.setCookies(cookies);
        this.headers = headers;
        this.is = null;
    }

    public HttpResponse(Integer statusCode, String response, List<Cookie> cookies, HttpHeaders headers, InputStream inputStream) {
        this.statusCode = statusCode;
        this.response = response;
        this.setCookies(cookies);
        this.headers = headers;
        this.is = inputStream;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(int status) {
        this.statusCode = status;
    }

    public String getResponse() {
        return response;
    }

    public void setResponse(String response) {
        this.response = response;
    }

    public List<Cookie> getCookies() {
        return cookies;
    }

    public void setCookies(List<Cookie> cookies) {
        this.cookies = cookies;
    }

    public HttpHeaders getResponseHeaders() {
        return headers;
    }

    public InputStream getInputStream() {
        return is;
    }

    public void setInputStream(InputStream is) {
        this.is = is;
    }
}