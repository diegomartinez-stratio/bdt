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

package com.stratio.qa.specs;

import io.netty.handler.codec.http.cookie.Cookie;
import com.stratio.qa.utils.GosecSSOUtils;
import com.stratio.qa.utils.ThreadProperty;
import cucumber.api.java.en.Given;
import io.netty.handler.codec.http.cookie.DefaultCookie;
import org.json.JSONObject;
import com.stratio.qa.assertions.Assertions;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.testng.Assert;
import java.util.concurrent.Future;
import org.asynchttpclient.Response;


/**
 * Keos Specs.
 *
 * @see <a href="KeosSpec-annotations.html">Keos Steps</a>
 */
public class KeosSpec extends BaseGSpec {
    /**
     * Generic constructor.
     *
     * @param spec object
     */
    public KeosSpec(CommonG spec) {
        this.commonspec = spec;
    }

    /**
     * Generate token to authenticate in gosec SSO in Keos
     *
     * @param token_type       : token type: [governance, discovery] or null
     * @param ssoHost          : current sso host
     * @param userName         : username
     * @param password         : password
     * @param tenant           : tenant
     * @param hostVerifier     : whether to verify host or not
     * @param pathWithoutLogin : whether to remove /login or keep it
     * @throws Exception exception
     */
    @Given("^I set sso( governance| discovery| intelligence)? keos token using host '(.+?)' with user '(.+?)', password '(.+?)' and tenant '(.+?)'( without host name verification)?( without login path)?$")
    public void setGoSecSSOCookieKeos(String token_type, String ssoHost, String userName, String password, String tenant, String hostVerifier, String pathWithoutLogin) throws Exception {
        GosecSSOUtils ssoUtils = new GosecSSOUtils(ssoHost, userName, password, tenant, token_type);
        ssoUtils.setVerifyHost(hostVerifier == null);
        HashMap<String, String> ssoCookies = ssoUtils.ssoTokenGenerator(pathWithoutLogin == null);

        String[] tokenList = {"user", "_oauth2_proxy", "stratio-cookie"};
        switch (String.valueOf(token_type).trim()) {
            case "governance":
                tokenList = new String[]{"user", "_oauth2_proxy", "stratio-cookie", "stratio-governance-auth"};
                break;
            case "discovery":
                Assert.assertTrue(ThreadProperty.has("discovery_sso_cookie_name"),
                        "Discovery SSO Cookie name must be setup first in envVar 'discovery_sso_cookie_name'");
                tokenList = new String[] {ThreadProperty.get("discovery_sso_cookie_name")};
                break;
            case "intelligence":
                tokenList = new String[]{"jupyterhub-session-id", "jupyterhub-hub-login"};
                break;
            default:
                break;
        }

        List<Cookie> cookiesAtributes = commonspec.addSsoToken(ssoCookies, tokenList);
        commonspec.setCookies(cookiesAtributes);

        if (ssoCookies.get("stratio-governance-auth") != null) {
            ThreadProperty.set("keosGovernanceAuthCookie", ssoCookies.get("stratio-governance-auth"));
        }

        if (ssoCookies.get("_oauth2_proxy") != null) {
            ThreadProperty.set("oauth2ProxyCookie", ssoCookies.get("_oauth2_proxy"));
        }

        if (ssoCookies.get("stratio-cookie") != null) {
            ThreadProperty.set("stratioCookie", ssoCookies.get("stratio-cookie"));
        }

        if (ssoCookies.get("user") != null) {
            ThreadProperty.set("user", ssoCookies.get("user"));
        }

        if ("discovery".equals(String.valueOf(token_type).trim())) {
            Assert.assertNotNull(ssoCookies.get(ThreadProperty.get("discovery_sso_cookie_name")),
                    "Discovery cookie was not found on SSO response");
            ThreadProperty.set(ThreadProperty.get("discovery_sso_cookie_name"),
                    ssoCookies.get(ThreadProperty.get("discovery_sso_cookie_name")));
        }

        if ("intelligence".equals(String.valueOf(token_type).trim())) {
            for (String cookie : tokenList) {
                Assert.assertNotNull(ssoCookies.get(cookie), "SSO cookie " + cookie + " is not defined after login");
                ThreadProperty.set(cookie, ssoCookies.get(cookie));
            }
        }

        this.commonspec.getLogger().debug("Cookies to set:");
        for (Cookie cookie : cookiesAtributes) {
            this.commonspec.getLogger().debug("\t" + cookie.name() + ":" + cookie.value());
        }
    }

    /**
     * Convert descriptor to k8s-json-schema
     *
     * @param descriptor : descriptor to be converted to k8s-json-schema
     * @param envVar     : environment variable where to store json
     * @throws Exception exception     *
     */
    @Given("^I convert descriptor '(.+?)' to k8s-json-schema( and save it in variable '(.+?)')?( and save it in file '(.+?)')?")
    public void convertDescriptorToK8sJsonSchema(String descriptor, String descriptorAttrs, String envVar, String fileName) throws Exception {
        JSONObject jsonSchema = new JSONObject();
        jsonSchema.put("descriptor", new JSONObject(descriptorAttrs));
        jsonSchema.put("deployment", commonspec.parseJSONSchema(new JSONObject(descriptor)));
        if (envVar != null) {
            ThreadProperty.set(envVar, jsonSchema.toString());
        }
        if (fileName != null) {
            File tempDirectory = new File(System.getProperty("user.dir") + "/target/test-classes/");
            String absolutePathFile = tempDirectory.getAbsolutePath() + "/" + fileName;
            commonspec.getLogger().debug("Creating file {} in 'target/test-classes'", absolutePathFile);
            // Note that this Writer will delete the file if it exists
            Writer out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(absolutePathFile), StandardCharsets.UTF_8));
            try {
                out.write(jsonSchema.toString());
            } catch (Exception e) {
                commonspec.getLogger().error("Custom file {} hasn't been created:\n{}", absolutePathFile, e.toString());
            } finally {
                out.close();
            }
        }
    }


    @Given("^I download k8s universe version '(.+?)'")
    public void downloadK8sUniverse(String version) throws Exception {
        String command = "wget -P target/test-classes http://qa.int.stratio.com/repository/paas/kubernetes-universe/descriptors/kubernetes-universe-descriptors-" + version + ".zip";

        // Execute command
        commonspec.runLocalCommand(command);

        Assertions.assertThat(commonspec.getCommandExitStatus()).as("Error downloading kubernetes universe version: " + version).isEqualTo(0);
    }


    @Given("^I upload k8s universe '(.+?)'")
    public void uploadK8sUniverse(String universeFile) throws Exception {
        // Check file exists
        File rules = new File(universeFile);
        Assertions.assertThat(rules.exists()).as("File: " + universeFile + " does not exist.").isTrue();

        // Obtain endpoint
        String endPointUpload = ThreadProperty.get("KEOS_CCT_UNIVERSE_SERVICE_INGRESS_PATH") + "/v1/descriptors";

        // Obtain URL
        String restURL = "https://" + System.getProperty("KEOS_CLUSTER_NAME") + ":443" + endPointUpload;

        // Form query parameters
        String headers = "-H \"accept: */*\" -H \"Content-Type: multipart/form-data\"";
        String forms = "-F \"file=@" + universeFile + ";type=application/zip\"";

        String cookie = "-H \"Cookie:_oauth2_proxy=" + ThreadProperty.get("oauth2ProxyCookie") + "\"";
        String command = "curl -X PUT -k " + cookie + " \"" + restURL + "\" " + headers + " " + forms;

        // Execute command
        commonspec.runLocalCommand(command);

        Assertions.assertThat(commonspec.getCommandExitStatus()).isEqualTo(0);
        Assertions.assertThat(commonspec.getCommandResult()).as("Not possible to upload universe: " + commonspec.getCommandResult()).doesNotContain("Error");

    }

    /**
     * Obtain metabase id for the current user and set ups metabase token headers
     * @param host : discovery host
     * @param path : discovery session endpoint
     * @throws Exception
     */
    @Given("^I obtain metabase id for current user in host '(.+?)' with endpoint '(.+?)'$")
    public void saveMetabaseCurrentUserCookieKeos(String host, String path) throws Exception {
        RestSpec restSpec = new RestSpec(commonspec);
        restSpec.setupRestClient("securely", host, ":443");
        Future<Response> response = commonspec.generateRequest("GET", false, null, null, path, "", null, "");

        String metabase_session = "";
        for (String header_value : response.get().getHeaders("Set-Cookie")) {
            if (header_value.startsWith("metabase.SESSION")) {
                metabase_session = header_value.split(";")[0].split("=")[1];
            }
        }
        Assert.assertNotEquals(metabase_session, "", "Error obtaining Metabase Session");

        Cookie cookie = new DefaultCookie("metabase.SESSION", metabase_session);
        cookie.setDomain("");
        cookie.setPath("");
        cookie.setWrap(false);
        cookie.setMaxAge(99999L);
        cookie.setSecure(false);
        cookie.setHttpOnly(false);
        List cookieList = new ArrayList();
        cookieList.add(cookie);
        cookieList.add(this.commonspec.getCookies().get(0));
        commonspec.setCookies(cookieList);

        HashMap<String, String> headers = new HashMap<String, String>();
        headers.put("X-Metabase-Session", metabase_session);
        commonspec.setHeaders(headers);
    }
}
