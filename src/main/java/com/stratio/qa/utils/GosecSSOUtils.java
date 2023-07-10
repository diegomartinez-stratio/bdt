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

import java.io.*;
import java.net.URI;
import java.security.SecureRandom;
import java.util.*;
import java.security.cert.X509Certificate;
import java.util.stream.Collectors;
import javax.net.ssl.*;

import org.apache.http.*;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.LaxRedirectStrategy;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.ssl.SSLContexts;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GosecSSOUtils {
    private final Logger logger = LoggerFactory.getLogger(this.getClass().getCanonicalName());

    public String userName = System.getProperty("user", "admin");

    public String passWord = System.getProperty("passWord", "1234");

    public String tenant;

    public String ssoHost = System.getProperty("ssoHost", "sso.paas.labs.stratio.com");

    private String governance;

    public String managementHost = System.getProperty("managementHost", "/service/gosecmanagement");

    private String governanceProfileHost = System.getProperty("govProfileHost", "/service/dg-businessglossary-api/dictionary/user/profile");

    private String governanceKeosProfileHost = System.getProperty("govKeosProfileHost", "/dg-businessglossary-api/dictionary/user/profile");

    private Boolean verifyHost = true;

    public GosecSSOUtils(String ssHost, String userName, String passWord, String tenant, String gov) {
        this.ssoHost = ssHost;
        this.userName = userName;
        this.passWord = passWord;
        this.tenant = tenant;
        this.governance = gov;
    }

    /**
     * Compatibility
     */
    @Deprecated
    public HashMap<String, String> ssoTokenGenerator() throws Exception {
        return ssoTokenGenerator(true);
    }

    /**
     * This method provide tokens to be used to generate client cookie
     * @return cookieToken list of token generated
     * @throws Exception exception
     */
    public HashMap<String, String> ssoTokenGenerator(boolean addLogin) throws Exception {
        String protocol = "https://";
        HashMap<String, String> cookieToken = new HashMap<>();
        SSLContext sslContext = SSLContext.getInstance("SSL");
        // set up a TrustManager that trusts everything
        sslContext.init(null, ALL_TRUSTING_TRUST_MANAGER, new SecureRandom());
        HttpClientContext context = HttpClientContext.create();
        HttpGet httpGet = new HttpGet(protocol + ssoHost + "/login");
        if (!addLogin) {
            httpGet = new HttpGet(protocol + ssoHost);
        }
        HttpClientBuilder clientBuilder = HttpClientBuilder.create()
                .setSslcontext(sslContext)
                .setRedirectStrategy(new LaxRedirectStrategy())
                .setDefaultRequestConfig(RequestConfig.custom()
                .setCookieSpec(CookieSpecs.STANDARD).setCircularRedirectsAllowed(true).build());

        if (!this.verifyHost) {
            SSLConnectionSocketFactory scsf = new SSLConnectionSocketFactory(SSLContexts.custom().
                    loadTrustMaterial((chain, authType) -> true)
                    .build(), NoopHostnameVerifier.INSTANCE);
            clientBuilder.setSSLSocketFactory(scsf);
        }

        HttpClient client = clientBuilder.build();
        try {
            // 1. GET request to login endpoint --> URL with redirect
            HttpResponse firstResponse = client.execute(httpGet, context);
            Document doc = Jsoup.parse(getStringFromIS(firstResponse.getEntity().getContent()));
            Elements code = doc.select("[name=lt]");
            String loginCode = code.attr("value");
            String executionCode = doc.select("[name=execution]").attr("value");
            for (Header oneHeader : firstResponse.getAllHeaders()) {
                logger.debug(oneHeader.getName() + ":" + oneHeader.getValue());
            }

            URI redirect = context.getRedirectLocations().get(context.getRedirectLocations().size() - 1);

            // 2. POST request to URL with redirects
            HttpResponse secondResponse = postToLogin(client, redirect, context, loginCode, executionCode);

            // 3. POST request to accept cookies (to URL with redirects)
            postToLoginToAcceptTyC(client, secondResponse, redirect, context);

            // 4. GET request to obtain cookies
            cookieToken = getCookies(client, context);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            e.getStackTrace();
        }
        return cookieToken;
    }

    private HttpResponse postToLogin(HttpClient client, URI redirect, HttpClientContext context, String loginCode, String executionCode) throws IOException {
        List<NameValuePair> params = new ArrayList<>();
        params.add(new BasicNameValuePair("_eventId", "submit"));
        params.add(new BasicNameValuePair("submit", "LOGIN"));
        params.add(new BasicNameValuePair("username", userName));
        params.add(new BasicNameValuePair("password", passWord));

        if (tenant != null) {
            params.add(new BasicNameValuePair("tenant", tenant));
            params.add(new BasicNameValuePair("customFields[tenant]", tenant));
        }

        params.add(new BasicNameValuePair("lt", loginCode));
        params.add(new BasicNameValuePair("execution", executionCode));
        HttpPost httpPost = new HttpPost(redirect);
        httpPost.setEntity(new UrlEncodedFormEntity(params));
        HttpResponse secondResponse = client.execute(httpPost, context);
        for (Header oneHeader : secondResponse.getAllHeaders()) {
            logger.debug(oneHeader.getName() + ":" + oneHeader.getValue());
        }
        return secondResponse;
    }

    private void postToLoginToAcceptTyC(HttpClient client, HttpResponse secondResponse, URI redirect, HttpClientContext context) throws IOException {
        Document doc = Jsoup.parse(getStringFromIS(secondResponse.getEntity().getContent()));
        String executionCode = doc.select("[name=execution]").attr("value");
        String eventId = doc.select("[name=_eventId]").attr("value");
        List<NameValuePair> params = new ArrayList<>();
        if (eventId.equalsIgnoreCase("proceed")) {
            params.add(new BasicNameValuePair("_eventId", "proceed"));
            params.add(new BasicNameValuePair("continue", "Continue"));
        } else {
            params.add(new BasicNameValuePair("_eventId", "submit"));
            params.add(new BasicNameValuePair("submit", "Accept"));
        }
        params.add(new BasicNameValuePair("execution", executionCode));
        HttpPost httpPost = new HttpPost(redirect);
        httpPost.setEntity(new UrlEncodedFormEntity(params));
        client.execute(httpPost, context);
    }

    private HashMap<String, String> getCookies(HttpClient client, HttpClientContext context) throws IOException {
        HashMap<String, String> cookieToken = new HashMap<>();
        String protocol = "https://";
        HttpGet getRequest;
        if (governance != null) {
            if (ThreadProperty.get("isKeosEnv") != null && ThreadProperty.get("isKeosEnv").equals("true")) {
                getRequest = new HttpGet(protocol + ssoHost + governanceKeosProfileHost);
            } else {
                getRequest = new HttpGet(protocol + ssoHost + governanceProfileHost);
            }
        } else {
            getRequest = new HttpGet(protocol + ssoHost + managementHost);
        }
        client.execute(getRequest, context);
        for (Cookie oneCookie : context.getCookieStore().getCookies()) {
            logger.debug(oneCookie.getName() + ":" + oneCookie.getValue());
            cookieToken.put(oneCookie.getName(), oneCookie.getValue());
        }
        return cookieToken;
    }

    private static final TrustManager[] ALL_TRUSTING_TRUST_MANAGER = new TrustManager[]{
        new X509TrustManager() {
            public X509Certificate[] getAcceptedIssuers() {
                return null;
            }

            public void checkClientTrusted(X509Certificate[] certs, String authType) {
            }

            public void checkServerTrusted(X509Certificate[] certs, String authType) {
            }
        }
    };

    private String getStringFromIS(InputStream stream) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream))) {
            return reader.lines().collect(Collectors.joining("\n"));
        } catch (IOException e) {
            e.printStackTrace();
            return "";
        }

    }

    public void setVerifyHost(Boolean verifyHost) {
        this.verifyHost = verifyHost;
    }
}
