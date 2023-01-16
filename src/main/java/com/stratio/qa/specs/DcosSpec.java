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

import com.auth0.jwt.JWTSigner;
import com.jayway.jsonpath.JsonPath;
import io.netty.handler.codec.http.cookie.Cookie;
import io.netty.handler.codec.http.cookie.DefaultCookie;
import org.asynchttpclient.Response;
import com.stratio.qa.utils.GosecSSOUtils;
import com.stratio.qa.utils.RemoteSSHConnection;
import com.stratio.qa.utils.RemoteSSHConnectionsUtil;
import com.stratio.qa.utils.ThreadProperty;
import com.stratio.qa.utils.vaultansible.VaultAnsible;
import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Paths;
import org.assertj.core.api.Assertions;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.Future;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.stratio.qa.assertions.Assertions.assertThat;
import static org.testng.Assert.fail;

/**
 * Generic DC/OS Specs.
 *
 * @see <a href="DcosSpec-annotations.html">DCOS Steps &amp; Matching Regex</a>
 */
public class DcosSpec extends BaseGSpec {

    private final Logger logger = LoggerFactory.getLogger(DcosSpec.class);

    String descriptorPath = "/stratio_volume/descriptor.json";

    String vaultResponsePath = "/stratio_volume/vault_response";

    /**
     * Generic constructor.
     *
     * @param spec object
     */
    public DcosSpec(CommonG spec) {
        this.commonspec = spec;
    }

    /**
     * Authenticate in a DCOS cluster
     *
     * @param remoteHost remote host
     * @param email      email for JWT singing
     * @param user       remote user
     * @param password   (required if pemFile null)
     * @throws Exception exception
     */
    @Given("^I authenticate to DCOS cluster '(.+?)' using email '(.+?)' with user '(.+?)' and password '(.+?)'( over SSH port '(.+?)')?$")
    public void authenticateDCOSpassword(String remoteHost, String email, String user, String password, String remotePort) throws Exception {
        authenticateDCOS(remoteHost, email, user, password, null, remotePort);
    }

    /**
     * Authenticate in a DCOS cluster
     *
     * @param remoteHost : remote host
     * @param email      : email for JWT singing
     * @param user       : remote user
     * @param pemFile    : (required if password null)
     * @throws Exception exception
     */
    @Given("^I authenticate to DCOS cluster '(.+?)' using email '(.+?)' with user '(.+?)' and pem file '(.+?)'( over SSH port '(.+?)')?$")
    public void authenticateDCOSpem(String remoteHost, String email, String user, String pemFile, String remotePort) throws Exception {
        authenticateDCOS(remoteHost, email, user, null, pemFile, remotePort);
    }

    /**
     * Authenticate in a DCOS cluster
     *
     * @param remoteHost : remote host
     * @param email      : email for JWT singing
     * @param user       : remote user
     * @param password   : (required if pemFile null)
     * @param pemFile    : (required if password null)
     * @throws Exception exception
     */
    private void authenticateDCOS(String remoteHost, String email, String user, String password, String pemFile, String remotePort) throws Exception {
        commonspec.setRemoteSSHConnection(new RemoteSSHConnection(user, password, remoteHost, remotePort, pemFile), remoteHost + "_" + user);
        commonspec.getRemoteSSHConnection().runCommand("sudo cat /var/lib/dcos/dcos-oauth/auth-token-secret");
        String DCOSsecret = commonspec.getRemoteSSHConnection().getResult().trim();
        setDCOSCookie(DCOSsecret, email);
    }

    /**
     * Set DCOS cookie for future requests
     * @param DCOSsecret    : DCOS secret to be used
     * @param email         : email to log in DCOS
     * @throws Exception
     */
    public void setDCOSCookie(String DCOSsecret, String email) throws Exception {
        final JWTSigner signer = new JWTSigner(DCOSsecret);
        final HashMap<String, Object> claims = new HashMap<>();
        claims.put("uid", email);
        final String jwt = signer.sign(claims);
        Cookie cookie = new DefaultCookie("dcos-acs-auth-cookie", jwt);
        cookie.setDomain("");
        cookie.setPath("");
        cookie.setWrap(false);
        cookie.setMaxAge(99999);
        cookie.setSecure(false);
        cookie.setHttpOnly(false);
        List<Cookie> cookieList = new ArrayList<>();
        cookieList.add(cookie);
        commonspec.setCookies(cookieList);
        ThreadProperty.set("dcosAuthCookie", jwt);
    }

    /**
     * Generate token to authenticate in gosec SSO
     *
     * @param ssoHost  : current sso host
     * @param userName : username
     * @param passWord : password
     * @throws Exception exception
     */
    @Given("^I( do not)? set sso( governance)? token using host '(.+?)' with user '(.+?)' and password '(.+?)'( and tenant '(.+?)')?( without host name verification)?( without login path)?$")
    public void setGoSecSSOCookie(String set, String gov, String ssoHost, String userName, String passWord, String tenant, String hostVerifier, String pathWithoutLogin) throws Exception {
        if (set == null) {
            if (System.getProperty("ADFS") != null && System.getProperty("ADFS").equalsIgnoreCase("yes") && userName.contains("stratiobigdata.onmicrosoft.com")) {

                HashMap<String, String> cookieTokenADFS = new HashMap<>();

                String adfsCookie = this.readOneLineFile(System.getProperty("user.dir") + "/dcos-acs-auth-cookie-" + userName);
                ThreadProperty.set("dcosAuthCookie", adfsCookie);
                cookieTokenADFS.put("dcos-acs-auth-cookie", adfsCookie);
                ArrayList<String> adfsTokenList = new ArrayList<>();
                adfsTokenList.add("dcos-acs-auth-cookie");

                if (gov != null) {
                    String adfsGovCookie = this.readOneLineFile(System.getProperty("user.dir") + "/stratio-governance-auth-" + userName);
                    ThreadProperty.set("dcosGovernanceAuthCookie", adfsGovCookie);
                    cookieTokenADFS.put("stratio-governance-auth", adfsGovCookie);
                    adfsTokenList.add("stratio-governance-auth");
                }

                if (hostVerifier != null || pathWithoutLogin != null) {
                    String adfsRocketCookie = this.readOneLineFile(System.getProperty("user.dir") + "/user-" + userName);
                    ThreadProperty.set("dcosUserCookie", adfsRocketCookie);
                    cookieTokenADFS.put("user", adfsRocketCookie);
                    adfsTokenList.add("user");
                }

                String[] adfsTokenArray = adfsTokenList.toArray(new String[adfsTokenList.size()]);
                List<Cookie> cookiesAtributesADFS = this.commonspec.addSsoToken(cookieTokenADFS, adfsTokenArray);
                commonspec.setCookies(cookiesAtributesADFS);

            } else {

                GosecSSOUtils ssoUtils = new GosecSSOUtils(ssoHost, userName, passWord, tenant, gov);
                ssoUtils.setVerifyHost(hostVerifier == null);
                HashMap<String, String> ssoCookies = ssoUtils.ssoTokenGenerator(pathWithoutLogin == null);

                String[] tokenList = {"user", "dcos-acs-auth-cookie"};
                if (gov != null) {
                    tokenList = new String[]{"user", "dcos-acs-auth-cookie", "stratio-governance-auth"};
                }
                List<Cookie> cookiesAtributes = this.commonspec.addSsoToken(ssoCookies, tokenList);

                this.commonspec.getLogger().debug("Cookies to set:");
                for (String cookie : tokenList) {
                    this.commonspec.getLogger().debug("\t" + cookie + ":" + ssoCookies.get(cookie));
                }

                if (ssoCookies.get("dcos-acs-auth-cookie") != null) {
                    ThreadProperty.set("dcosAuthCookie", ssoCookies.get("dcos-acs-auth-cookie"));
                }

                if (ssoCookies.get("stratio-governance-auth") != null) {
                    ThreadProperty.set("dcosGovernanceAuthCookie", ssoCookies.get("stratio-governance-auth"));
                }
                if (ssoCookies.get("user") != null) {
                    ThreadProperty.set("dcosUserCookie", ssoCookies.get("user"));
                }
                commonspec.setCookies(cookiesAtributes);

            }
        }
    }

    /**
     * Read a one-line file
     *
     * @param fileName
     * @return File contents (one line)
     * @throws IOException
     */
    private String readOneLineFile(String fileName) throws IOException {
        StringBuilder sb = new StringBuilder();
        try {
            BufferedReader br = Files.newBufferedReader(Paths.get(fileName), StandardCharsets.UTF_8);
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
        } catch (IOException e) {
            throw new NoSuchFileException("File '" + fileName + "' not found.\n" + e.toString());
        }

        return sb.toString();
    }

    /**
     * Generate token to authenticate in gosec SSO with Discovery
     *
     * @param ssoHost  : current sso host
     * @param userName : username
     * @param passWord : password
     * @param discoveryCookie : Cookie for discovery
     * @throws Exception exception
     */
    @Given("I set sso discovery token using host '(.+?)' with user '(.+?)' and password '(.+?)'( and tenant '(.+?)')?( without host name verification)? with cookie name '(.+?)'$")
    public void setGoSecSSOCookieforDiscovery(String ssoHost, String userName, String passWord, String tenant, String hostVerifier, String discoveryCookie) throws Exception {
        if (System.getProperty("ADFS") != null && System.getProperty("ADFS").equalsIgnoreCase("yes") && userName.contains("stratiobigdata.onmicrosoft.com")) {

            HashMap<String, String> cookieTokenADFS = new HashMap<>();

            String adfsCookie = this.readOneLineFile(System.getProperty("user.dir") + "/discovery-auth-cookie-" + userName);
            ThreadProperty.set(discoveryCookie, adfsCookie);
            cookieTokenADFS.put(discoveryCookie, adfsCookie);
            String[] adfsTokenList = {discoveryCookie};

            List<Cookie> cookiesAtributesADFS = this.commonspec.addSsoToken(cookieTokenADFS, adfsTokenList);
            commonspec.setCookies(cookiesAtributesADFS);

        } else {

            GosecSSOUtils ssoUtils = new GosecSSOUtils(ssoHost, userName, passWord, tenant, null);
            ssoUtils.setVerifyHost(hostVerifier == null);
            HashMap<String, String> ssoCookies = ssoUtils.ssoTokenGenerator();
            String[] tokenList = new String[]{discoveryCookie};
            List<Cookie> cookiesAtributes = this.commonspec.addSsoToken(ssoCookies, tokenList);
            this.commonspec.getLogger().debug("Discovery Cookie to set:");
            for (String cookie : tokenList) {
                this.commonspec.getLogger().debug("\t" + cookie + ":" + ssoCookies.get(cookie));
            }
            ThreadProperty.set(discoveryCookie, ssoCookies.get(discoveryCookie));
            commonspec.setCookies(cookiesAtributes);

        }
    }

    /**
     * Generate token to authenticate in gosec SSO for Intelligence
     *
     * @param ssoHost  : current sso host
     * @param userName : username
     * @param passWord : password
     * @throws Exception exception
     */
    @Given("I set sso intelligence token using host '(.+?)' with user '(.+?)' and password '(.+?)'( and tenant '(.+?)')?( without host name verification)?( without login path)?$")
    public void setGoSecSSOCookieForIntelligence(String ssoHost, String userName, String passWord, String tenant, String hostVerifier, String pathWithoutLogin) throws Exception {
        String[] tokenList = new String[]{"jupyterhub-session-id", "jupyterhub-hub-login"};

        GosecSSOUtils ssoUtils = new GosecSSOUtils(ssoHost, userName, passWord, tenant, null);
        ssoUtils.setVerifyHost(hostVerifier == null);
        HashMap<String, String> ssoCookies = ssoUtils.ssoTokenGenerator(pathWithoutLogin == null);
        List<Cookie> cookiesAttributes = this.commonspec.addSsoToken(ssoCookies, tokenList);

        this.commonspec.getLogger().debug("Intelligence Cookies to set:");
        for (String cookie : tokenList) {
            this.commonspec.getLogger().debug("\t" + cookie + ":" + ssoCookies.get(cookie));
            ThreadProperty.set(cookie, ssoCookies.get(cookie));
        }
        commonspec.setCookies(cookiesAttributes);
    }

    /**
     * Checks if there are any unused nodes in the cluster and returns the IP of one of them.
     * REQUIRES A PREVIOUSLY-ESTABLISHED SSH CONNECTION TO DCOS-CLI TO WORK
     *
     * @param hosts     : list of IPs that will be investigated
     * @param envVar    : environment variable name
     * @throws Exception
     */
    @Given("^I save the IP of an unused node in hosts '(.+?)' in the in environment variable '(.+?)'?$")
    public void getUnusedNode(String hosts, String envVar) throws Exception {
        Set<String> hostList = new HashSet(Arrays.asList(hosts.split(",")));

        //Get the list of currently used hosts
        commonspec.executeCommand("dcos task | awk '{print $2}'", null, 0, null);
        String results = commonspec.getRemoteSSHConnection().getResult();
        Set<String> usedHosts = new HashSet(Arrays.asList(results.replaceAll("\r", "").split("\n")));

        //We get the nodes not being used
        hostList.removeAll(usedHosts);

        if (hostList.size() == 0) {
            throw new IllegalStateException("No unused nodes in the cluster.");
        } else {
            //Pick the first available node
            ThreadProperty.set(envVar, hostList.iterator().next());
        }
    }


    /**
     * Check if all task of a service are correctly distributed in all datacenters of the cluster
     *
     * @param serviceList : all task deployed in the cluster separated by a semicolumn.
     * @throws Exception
     */
    @Given("^services '(.*?)' are splitted correctly in datacenters$")
    public void checkServicesDistributionMultiDataCenter(String serviceList) throws Exception {
        commonspec.executeCommand("dcos node --json >> aux.txt", null, 0, null);
        commonspec.executeCommand("cat aux.txt", null, 0, null);
        checkDataCentersDistribution(serviceList.split(","), obtainsDataCenters(commonspec.getRemoteSSHConnection().getResult()).split(";"));
        commonspec.executeCommand("rm -rf aux.txt", null, 0, null);
    }

    /**
     * Check if all task of a service are correctly distributed in all datacenters of the cluster
     *
     * @param serviceList    : all task deployed in the cluster separated by a semicolumn.
     * @param dataCentersIps : all ips of the datacenters to be checked
     *                       Example: ip_1_dc1, ip_2_dc1;ip_3_dc2,ip_4_dc2
     * @throws Exception
     */
    @Given("^services '(.+?)' are splitted correctly in datacenters '(.+?)'$")
    public void checkServicesDistributionMultiDataCenterPram(String serviceList, String dataCentersIps) throws Exception {
        checkDataCentersDistribution(serviceList.split(","), dataCentersIps.split(";"));
    }

    public void checkDataCentersDistribution(String[] serviceListArray, String[] dataCentersIpsArray) throws Exception {
        int[] results = new int[dataCentersIpsArray.length];
        int div = serviceListArray.length / dataCentersIpsArray.length;
        int resto = serviceListArray.length % dataCentersIpsArray.length;

        for (int i = 0; i < serviceListArray.length; i++) {
            commonspec.executeCommand("dcos task | grep " + serviceListArray[i] + " | awk '{print $2}'", null, 0, null);
            String service_ip = commonspec.getRemoteSSHConnection().getResult();
            for (int x = 0; x < dataCentersIpsArray.length; x++) {
                if (dataCentersIpsArray[x].toLowerCase().contains(service_ip.toLowerCase())) {
                    results[x] = results[x] + 1;
                }
            }
        }

        int sum = 0;
        for (int i = 0; i < results.length; i++) {
            if (resto > 0) {
                assertThat(results[i]).as("Services in datacenter should be: " + div + " or " + (div + 1)).isBetween(div - 1, div + 2);
            } else {
                assertThat(results[i]).as("Services in datacenter should be: " + div + " and it is: " + results[i]).isEqualTo(div);
            }

            sum = sum + results[i];
        }

        assertThat(sum).as("There are less services: " + sum + " than expected: " + serviceListArray.length).isEqualTo(serviceListArray.length);
    }

    /**
     * Obtain datacenters avaialable in cluster
     *
     * @param jsonString : json where to look for datacenters
     * @return
     */
    public String obtainsDataCenters(String jsonString) {
        Map<String, String> datacentersDistribution = new HashMap<String, String>();
        JSONArray jsonArray = new JSONArray(jsonString);
        for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject object = jsonArray.getJSONObject(i);
            String ip = object.getString("hostname");
            String datacenter = ((JSONObject) object.get("attributes")).getString("dc");
            String existValue = datacentersDistribution.get(datacenter);
            if (existValue == null) {
                datacentersDistribution.put(datacenter, ip);
            } else {
                datacentersDistribution.put(datacenter, datacentersDistribution.get(datacenter) + "," + ip);
            }
        }
        String result = "";
        for (String ips : datacentersDistribution.keySet()) {
            String key = ips;
            String value = datacentersDistribution.get(key);
            result = result + ";" + value;
        }
        return result.substring(1);
    }

    /**
     * Convert jsonSchema to json
     *
     * @param jsonSchema : jsonSchema to be converted to json
     * @param envVar     : environment variable where to store json
     * @throws Exception exception     *
     */
    @Given("^I convert jsonSchema '(.+?)' to json( and save it in variable '(.+?)')?( and save it in file '(.+?)')?")
    public void convertJSONSchemaToJSON(String jsonSchema, String envVar, String fileName) throws Exception {
        String json = commonspec.parseJSONSchema(new JSONObject(jsonSchema)).toString();
        if (envVar != null) {
            ThreadProperty.set(envVar, json);
        }
        if (fileName != null) {
            File tempDirectory = new File(System.getProperty("user.dir") + "/target/test-classes/");
            String absolutePathFile = tempDirectory.getAbsolutePath() + "/" + fileName;
            commonspec.getLogger().debug("Creating file {} in 'target/test-classes'", absolutePathFile);
            // Note that this Writer will delete the file if it exists
            Writer out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(absolutePathFile), StandardCharsets.UTF_8));
            try {
                out.write(json);
            } catch (Exception e) {
                commonspec.getLogger().error("Custom file {} hasn't been created:\n{}", absolutePathFile, e.toString());
            } finally {
                out.close();
            }
        }
    }

    /**
     * Check if json is validated against a schema
     *
     * @param json   : json to be validated against schema
     * @param schema : schema to be validated against
     * @throws Exception exception     *
     */
    @Given("^json (.+?) matches schema (.+?)$")
    public void jsonMatchesSchema(String json, String schema) throws Exception {
        JSONObject jsonschema = new JSONObject(schema);
        JSONObject jsondeploy = new JSONObject(json);

        commonspec.matchJsonToSchema(jsonschema, jsondeploy);
    }

    /**
     * A PUT request over the body value.
     *
     * @param key       : key to be added
     * @param value     : value to be added
     * @param service   : service to add key-value pair to
     * @throws Exception
     */
    @Then("^I add a new DCOS label with key '(.+?)' and value '(.+?)' to the service '(.+?)'?$")
    public void sendAppendRequest(String key, String value, String service) throws Exception {
        String[] serviceArray = service.split("/");
        String serviceName = serviceArray[serviceArray.length - 1];

        commonspec.runCommandAndGetResult("touch /dcos/" + serviceName + ".json && dcos marathon app show " + service + " > /dcos/" + serviceName + ".json");
        commonspec.runCommandAndGetResult("cat /dcos/" + serviceName + ".json");

        String configFile = commonspec.getRemoteSSHConnection().getResult();
        String myValue = commonspec.getJSONPathString(configFile, ".labels", "0");
        String myJson = commonspec.updateMarathonJson(commonspec.removeJSONPathElement(configFile, "$.labels"));

        String newValue = myValue.replaceFirst("\\{", "{\"" + key + "\": \"" + value + "\", ");
        newValue = "\"labels\":" + newValue;
        String myFinalJson = myJson.replaceFirst("\\{", "{" + newValue.replace("\\n", "\\\\n") + ",");
        if (myFinalJson.contains("uris")) {
            String test = myFinalJson.replaceAll("\"uris\"", "\"none\"");
            commonspec.runCommandAndGetResult("echo '" + test + "' > /dcos/final" + serviceName + ".json");
        } else {
            commonspec.runCommandAndGetResult("echo '" + myFinalJson + "' > /dcos/final" + serviceName + ".json");
        }
        commonspec.runCommandAndGetResult("dcos marathon app update " + service + " < /dcos/final" + serviceName + ".json");

        commonspec.setCommandExitStatus(commonspec.getRemoteSSHConnection().getExitStatus());
    }

    /**
     * Set a environment variable in marathon and deploy again.
     *
     * @param key       : key to be added
     * @param value     : value to be added
     * @param service   : service to add key-value pair to
     * @throws Exception
     */
    @Then("^I modify marathon environment variable '(.+?)' with value '(.+?)' for service '(.+?)'?$")
    public void setMarathonProperty(String key, String value, String service) throws Exception {
        commonspec.runCommandAndGetResult("touch " + service + "-env.json && dcos marathon app show " + service + " > /dcos/" + service + "-env.json");
        commonspec.runCommandAndGetResult("cat /dcos/" + service + "-env.json");

        String configFile = commonspec.getRemoteSSHConnection().getResult();
        String myJson1 = commonspec.replaceJSONPathElement(configFile, key, value);
        String myJson4 = commonspec.updateMarathonJson(myJson1);
        String myJson = myJson4.replaceAll("\"uris\"", "\"none\"");

        commonspec.runCommandAndGetResult("echo '" + myJson + "' > /dcos/final" + service + "-env.json");
        commonspec.runCommandAndGetResult("dcos marathon app update " + service + " < /dcos/final" + service + "-env.json");
        commonspec.setCommandExitStatus(commonspec.getRemoteSSHConnection().getExitStatus());
    }

    /**
     * Obtain metabase id for user
     * @param user      : user to obtain metabase id from
     * @param password  : password for user
     * @param url       : endpoint to retrieve info from
     * @throws Exception
     */
    @Then("^I obtain metabase id for user '(.+?)' and password '(.+?)' in endpoint '(.+?)' and save in context cookies$")
    public void saveMetabaseCookie(String user, String password, String url) throws Exception {
        if (System.getProperty("ADFS") != null && System.getProperty("ADFS").equalsIgnoreCase("yes") && user.contains("stratiobigdata.onmicrosoft.com")) {

            HashMap<String, String> cookieTokenADFS = new HashMap<>();

            String adfsCookie = this.readOneLineFile(System.getProperty("user.dir") + "/metabase-" + user);
            cookieTokenADFS.put("metabase.SESSION", adfsCookie);
            String[] adfsTokenList = {"metabase.SESSION"};

            List<Cookie> cookiesAtributesADFS = this.commonspec.addSsoToken(cookieTokenADFS, adfsTokenList);
            commonspec.getCookies().add(cookiesAtributesADFS.get(0));

        } else {
            String command = "curl -X POST -k -H 'Cookie: " + this.commonspec.getCookies().get(0) + " ' -H \"Content-Type: application/json\" -d '{\"username\": \"" + user + "\", \"password\": \"" + password + "\"}' " + url;
            commonspec.runLocalCommand(command);
            commonspec.runCommandLoggerAndEnvVar(0, null, Boolean.TRUE);
            Assertions.assertThat(commonspec.getCommandExitStatus()).isEqualTo(0);
            String result = JsonPath.parse(commonspec.getCommandResult().trim()).read("$.id");
            Cookie cookie = new DefaultCookie("metabase.SESSION_ID", result);
            cookie.setDomain("");
            cookie.setPath("");
            cookie.setWrap(false);
            cookie.setMaxAge(99999L);
            cookie.setSecure(false);
            cookie.setHttpOnly(false);
            ArrayList<Cookie> cookieList = new ArrayList<>();
            cookieList.add(cookie);
            cookieList.add(this.commonspec.getCookies().get(0));
            this.commonspec.setCookies(cookieList);
        }
    }


    /**
     * Check if a role of a service complies the established constraints
     *
     * @param role        : name of role of a service or scheduler
     * @param service     : name of service of exhibitor
     * @param instance    : name of instance of a service, for scheduler is id
     * @param constraints : all stablished contraints separated by a semicolumn.
     *                    Example: constraint1,constraint2,...
     * @throws Exception
     */
    @Then("^The role '(.+?)' of the service '(.+?)' with instance '(.+?)' complies the constraints '(.+?)'$")
    public void checkComponentConstraints(String role, String service, String instance, String constraints) throws Exception {
        checkComponentConstraint(role, service, instance, constraints.split(","));
    }

    /***
     * Check contraint in component
     *
     * @param role          : role
     * @param service       : service name
     * @param instance      : instance name
     * @param constraints   : expected constraints
     * @throws Exception
     */
    public void checkComponentConstraint(String role, String service, String instance, String[] constraints) throws Exception {
        for (int i = 0; i < constraints.length; i++) {
            String[] elements = constraints[i].split(":");
            Assertions.assertThat(elements.length).overridingErrorMessage("Error while parsing constraints. The constraint's format is ATRIBUTE:CONSTRAINT:VALOR or ATRIBUTE:CONSTRAINT").isIn(2, 3);
            Assertions.assertThat(elements[1]).overridingErrorMessage("Error while parsing constraints. Constraints should be CLUSTER, UNIQUE, LIKE, UNLIKE, GROUP_BY, MAX_PER or IS").isIn("UNIQUE", "CLUSTER", "GROUP_BY", "LIKE", "UNLIKE", "MAX_PER", "IS");
            if (elements.length == 2) {
                Assertions.assertThat(elements[1]).overridingErrorMessage("Error while parsing constraints. The constraint's format " + elements[1] + " is ATRIBUTE:CONSTRAINT:VALOR").isIn("UNIQUE", "CLUSTER", "GROUP_BY");
                if (!elements[1].equals("GROUP_BY")) {
                    checkConstraint(role, service, instance, elements[0], elements[1], null);
                }
            } else {
                Assertions.assertThat(elements[1]).overridingErrorMessage("Error while parsing constraints. The constraint's format " + elements[1] + " is ATRIBUTE:CONSTRAINT").isNotEqualTo("UNIQUE");
                checkConstraint(role, service, instance, elements[0], elements[1], elements[2]);
            }
        }
    }

    /**
     *
     * @param role          : role
     * @param service       : service name
     * @param instance      : instance name
     * @param tag           : tag
     * @param constraint    : expected contraint
     * @param value         : value
     * @throws Exception
     */
    public void checkConstraint(String role, String service, String instance, String tag, String constraint, String value) throws Exception {
        RestSpec restspec = new RestSpec(commonspec);
        MiscSpec miscspec = new MiscSpec(commonspec);
        if (role.equals("scheduler")) {
            restspec.sendRequestTimeout(100, 5, "GET", "/service/marathon/v2/apps" + instance, null, null, "app");
            miscspec.saveElementEnvironment(null, "$.app.tasks[0]", "marathon_answer");
            Assertions.assertThat(ThreadProperty.get("marathon_answer")).overridingErrorMessage("Error while parsing constraints. The instance " + instance + " of the service " + service + " isn't deployed").isNotEmpty();
        } else {
            restspec.sendRequestTimeout(100, 5, "GET", "/exhibitor/exhibitor/v1/explorer/node-data?key=/datastore/" + service + "/" + instance + "/plan-v2-json&_=", null, null, "str");
            miscspec.saveElementEnvironment(null, "$.str", "exhibitor_answer");
            Assertions.assertThat(ThreadProperty.get("exhibitor_answer")).overridingErrorMessage("Error while parsing constraints. The instance " + instance + " of the service " + service + " isn't deployed").isNotEmpty();
        }
        CommandExecutionSpec commandexecutionspec = new CommandExecutionSpec(commonspec);
        if (tag.equals("hostname")) {
            if (role.equals("scheduler")) {
                commandexecutionspec.executeLocalCommand("echo '" + ThreadProperty.get("marathon_answer") + "' | jq .host | sed 's/\"//g'", "0", "elementsConstraint");
            } else {
                selectElements(role, service, "agent_hostname");
            }
            String[] hostnames = ThreadProperty.get("elementsConstraint").split("\n");
            checkConstraintType(role, instance, tag, constraint, value, hostnames);
        } else {
            if (role.equals("scheduler")) {
                commandexecutionspec.executeLocalCommand("echo '" + ThreadProperty.get("marathon_answer") + "' | jq .slaveId | sed 's/\"//g'", "0", "elementsConstraint");
            } else {
                selectElements(role, service, "slaveid");
            }
            String[] slavesid = ThreadProperty.get("elementsConstraint").split("\n");
            String[] valor = new String[slavesid.length];
            for (int i = 0; i < slavesid.length; i++) {
                restspec.sendRequestTimeout(100, 5, "GET", "/mesos/slaves?slave_id=" + slavesid[i], null, null, "slaves");
                miscspec.saveElementEnvironment(null, "$.slaves[0].attributes", "mesos_answer");
                commandexecutionspec.executeLocalCommand("echo '" + ThreadProperty.get("mesos_answer") + "' | jq '." + tag + "' | sed 's/^.\\|.$//g'", "0", "valortag");
                valor[i] = ThreadProperty.get("valortag");
            }
            checkConstraintType(role, instance, tag, constraint, value, valor);
        }
    }

    /**
     * @param role   : name of role of a service
     * @param IP     : Ip of the machine from which you want to save the nodes
     * @param envVar : environment variable where to store nodes
     * @throws Exception
     */
    @When("^I save nodes '(.+?)' that are in machine '(.+?)' in environment variable '(.+?)'$")
    public void saveNodes(String role, String IP, String envVar) throws Exception {
        selectElements(role, "pbd", "agent_hostname", IP, envVar);
    }

    /**
     * @param role        : name of role of a service
     * @param envVar      : environment variable where before you save nodes
     * @param envVar2     : environment variable when you want to check slave nodes
     * @param sTimeout    : Same RestSpec.sendRequest
     * @param sWait       : Same RestSpec.sendRequest
     * @param requestType : Same RestSpec.sendRequest
     * @param endPoint    : Same RestSpec.sendRequest
     * @param status      : Same RestSpec.sendRequest
     * @throws Exception
     */
    @Then("^I check status of nodes '(.+?)' using environment variable '(.+?)'(,'(.+?)')? in less than '(\\d+?)' seconds, checking each '(\\d+?)' seconds, I send a '(.+?)' request to '(.+?)' checking status '(.+?)' of nodes$")
    public void checkProxyNodesStatus(String role, String envVar, String envVar2, String sTimeout, String sWait, String requestType, String endPoint, String status) throws Exception {
        Integer timeout = Integer.parseInt(sTimeout);
        Integer wait = Integer.parseInt(sWait);
        String estadoNodo;

        RestSpec restspec = new RestSpec(commonspec);

        //datanodes
        selectElements(role, "pbd", "name");
        String[] dataNodes = ThreadProperty.get("elementsConstraint").split("\n");

        //Si tenemos algún DataNode caído, chequeamos los datanodeSlave de ese dataNode
        if (role.contains("datanode_slave") && envVar2 != null) {

            for (int i = 0; i < dataNodes.length; i++) {
                if (dataNodes[i].split("_")[1].contains(ThreadProperty.get(envVar2).split("_")[1])) {
                    estadoNodo = status;
                } else {
                    estadoNodo = "RUNNING";
                }
                restspec.sendRequestTimeout(timeout, wait, requestType, endPoint, null, null, dataNodes[i] + "\",\"role\":\"" + role + "\",\"status\":\"" + estadoNodo);
            }

        } else {
            for (int i = 0; i < dataNodes.length; i++) {
                if (dataNodes[i].contains(ThreadProperty.get(envVar)) && !ThreadProperty.get(envVar).isEmpty()) {
                    estadoNodo = status;
                } else {
                    estadoNodo = "RUNNING";
                }
                restspec.sendRequestTimeout(timeout, wait, requestType, endPoint, null, null, dataNodes[i] + "\",\"role\":\"" + role + "\",\"status\":\"" + estadoNodo);
            }
        }
    }

    /**
     *
     * @param role
     * @param service
     * @param element
     * @param elementValue
     * @param envValue
     * @throws Exception
     */
    public void selectElements(String role, String service, String element, String elementValue, String envValue) throws Exception {
        CommandExecutionSpec commandexecutionspec = new CommandExecutionSpec(commonspec);
        commandexecutionspec.executeLocalCommand("echo '" + ThreadProperty.get("exhibitor_answer") + "' | jq '.phases[] | .[] | .steps[] | .[] | select(.role==\"" + role + "\") | select(.status==\"RUNNING\") | select(." + element + "==\"" + elementValue + "\").name' | sed 's/\"//g'", "0", envValue);
    }

    /**
     *
     * @param role
     * @param service
     * @param element
     * @throws Exception
     */
    public void selectElements(String role, String service, String element) throws Exception {
        CommandExecutionSpec commandexecutionspec = new CommandExecutionSpec(commonspec);
        commandexecutionspec.executeLocalCommand("echo '" + ThreadProperty.get("exhibitor_answer") + "' | jq '.phases[] | .[] | .steps[] | .[] | select(.role==\"" + role + "\") | select(.status==\"RUNNING\")." + element + "' | sed 's/\"//g'", "0", "elementsConstraint");
    }

    /**
     *
     * @param role
     * @param instance
     * @param tag
     * @param constraint
     * @param value
     * @param elements
     * @throws Exception
     */
    public void checkConstraintType(String role, String instance, String tag, String constraint, String value, String[] elements) throws Exception {
        Pattern p = value != null ? Pattern.compile(value) : null;
        Matcher m;
        switch (constraint) {
            case "UNIQUE":
                for (int i = 0; i < elements.length; i++) {
                    for (int j = i + 1; j < elements.length; j++) {
                        Assertions.assertThat(elements[i]).overridingErrorMessage("The role " + role + " of the instance " + instance + " doesn't complies the established constraint " + tag + ":" + constraint).isNotEqualTo(elements[j]);
                    }
                }
                break;
            case "CLUSTER":
                if (value == null) {
                    for (int i = 0; i < elements.length; i++) {
                        for (int j = i + 1; j < elements.length; j++) {
                            Assertions.assertThat(elements[i]).overridingErrorMessage("The role " + role + " of the instance " + instance + " doesn't complies the established constraint " + tag + ":" + constraint).isEqualTo(elements[j]);
                        }
                    }
                } else {
                    checkConstraintClusterValueIs(role, instance, tag, constraint, value, elements);
                }
                break;
            case "LIKE":
                for (int i = 0; i < elements.length; i++) {
                    m = p.matcher(elements[i]);
                    Assertions.assertThat(m.find()).overridingErrorMessage("The role " + role + " of the instance " + instance + " doesn't complies the established constraint " + tag + ":" + constraint + ":" + value).isEqualTo(true);
                }
                break;
            case "UNLIKE":
                for (int i = 0; i < elements.length; i++) {
                    m = p.matcher(elements[i]);
                    Assertions.assertThat(m.find()).overridingErrorMessage("The role " + role + " of the instance " + instance + " doesn't complies the established constraint " + tag + ":" + constraint + ":" + value).isEqualTo(false);
                }
                break;
            case "IS":
                checkConstraintClusterValueIs(role, instance, tag, constraint, value, elements);
                break;
            case "MAX_PER":
                Map<String, Integer> diferent = new HashMap<String, Integer>();
                int count;
                for (int i = 0; i < elements.length; i++) {
                    if (!diferent.containsKey(elements[i])) {
                        diferent.put(elements[i], 1);
                    } else {
                        count = diferent.get(elements[i]);
                        count = count + 1;
                        diferent.put(elements[i], count);
                    }
                }
                Iterator it = diferent.keySet().iterator();
                while (it.hasNext()) {
                    Assertions.assertThat(diferent.get(it.next())).overridingErrorMessage("The role " + role + " of the instance " + instance + " doesn't complies the established constraint " + tag + ":" + constraint + ":" + value).isLessThanOrEqualTo(Integer.parseInt(value));
                }
                break;
            case "GROUP_BY":
                ArrayList<String> dif = new ArrayList<>();
                dif.add(elements[0]);
                boolean ok;
                for (int i = 1; i < elements.length; i++) {
                    ok = false;
                    for (int j = 0; j < dif.size(); j++) {
                        if (elements[i].equals(dif.get(j))) {
                            ok = true;
                            j = dif.size();
                        }
                    }
                    if (!ok) {
                        dif.add(elements[i]);
                    }
                }
                Assertions.assertThat(dif.size()).overridingErrorMessage("The role " + role + " of the instance " + instance + " doesn't complies the established constraint " + tag + ":" + constraint + ":" + value).isLessThanOrEqualTo(Integer.parseInt(value));
                break;
            default:
                commonspec.getExceptions().add(new Exception("Error while parsing constraints. Constraints should be CLUSTER, UNIQUE, LIKE, UNLIKE, GROUP_BY, UNIQUE, LIKE, UNLIKE, GROUP_BY, MAX_PER or IS"));
        }
    }

    /**
     *
     * @param role
     * @param instance
     * @param tag
     * @param constraint
     * @param value
     * @param elements
     * @throws Exception
     */
    public void checkConstraintClusterValueIs(String role, String instance, String tag, String constraint, String value, String[] elements) throws Exception {
        for (int i = 0; i < elements.length; i++) {
            for (int j = i + 1; j < elements.length; j++) {
                Assertions.assertThat(elements[i]).overridingErrorMessage("The role " + role + " of the instance " + instance + " doesn't complies the established constraint " + tag + ":" + constraint + ":" + value).isEqualTo(elements[j]);
                Assertions.assertThat(elements[i]).overridingErrorMessage("The role " + role + " of the instance " + instance + " doesn't complies the established constraint " + tag + ":" + constraint + ":" + value).isEqualTo(value);
            }
        }
    }

    /**
     * Obtains info from file passed as a parameter based on the jq expression passed as a parameter
     *
     * @param jqExpression    : jq expression to retrieve required information
     * @param absolutPathFile : absolut path to file to retrieve information from
     * @param envVar          : environment variable where to store retrieved information
     * @throws Exception
     */
    @Given("^I obtain '(.+?)' from json file '(.+?)' and save it in environment variable '(.+?)'$")
    public void obtainInfoFromFile(String jqExpression, String absolutPathFile, String envVar) throws Exception {
        String prefix = "set -o pipefail && cat ";

        try {
            commonspec.getRemoteSSHConnection().runCommand("test -f " + absolutPathFile);
        } catch (Exception e) {
            commonspec.getLogger().debug("SSH connection not opened.");
            throw new Exception("SSH connection not opened.");
        }

        if (commonspec.getRemoteSSHConnection().getExitStatus() != 0) {
            commonspec.getLogger().debug("File passed as parameter: " + absolutPathFile + ", does not exist in remote system");
            throw new Exception("File: " + absolutPathFile + " does not exist in remote system.");
        }

        commonspec.getRemoteSSHConnection().runCommand(prefix + absolutPathFile + " | " + jqExpression);
        if (commonspec.getRemoteSSHConnection().getExitStatus() != 0) {
            commonspec.getLogger().debug("Problem with jq expression passed as parameter.");
            throw new Exception("Error obtaining info from json file: " + commonspec.getRemoteSSHConnection().getResult());
        }

        if (commonspec.getRemoteSSHConnection().getResult().equals("null")) {
            commonspec.getLogger().debug("jq expression passed as parameter returns null.");
            throw new Exception("Info obtained from json file with jq: " + jqExpression + ", is null.");
        }

        ThreadProperty.set(envVar, commonspec.getRemoteSSHConnection().getResult());
    }

    /**
     * Obtains required information from descriptor file
     *
     * @param info   : information required from descriptor.json in bootstrap system
     * @param envVar : environment variable where to store retrieved information
     * @throws Exception
     */
    @Given("^I obtain '(MASTERS|NODES|PRIV_NODES|PUBLIC_NODES|PUBLIC_NODE|GOSEC_NODES|ID|DNS_SEARCH|INTERNAL_DOMAIN|ARTIFACT_REPO|DOCKER_REGISTRY|EXTERNAL_DOCKER_REGISTRY|REALM|KDC_HOST|KDC_PORT|KADMIN_HOST|KADMIN_PORT|LDAP_HOST|VAULT_HOST|IP|ADMIN_USER|TENANT|ACCESS_POINT|LDAP_URL|LDAP_PORT|LDAP_USER_DN|LDAP_GROUP_DN|LDAP_BASE|LDAP_ADMIN_GROUP)' from descriptor and save it in environment variable '(.+?)'$")
    public void obtainInfoFromDescriptor(String info, String envVar) throws Exception {
        String jqExpression = "";

        switch (info) {
            case "MASTERS":
                jqExpression = "jq -crM '.nodes[] | select(.role ?== \"master\") | .networking[0].ip' | paste -sd \",\" -";
                break;
            case "NODES":
                jqExpression = "jq -crM '.nodes[] | select(.role ?== \"agent\") | .networking[0].ip' | paste -sd \",\" -";
                break;
            case "PRIV_NODES":
                jqExpression = "jq -crM '.nodes[] | select((.role ?== \"agent\") and .public ?== false) | .networking[0].ip' | paste -sd \",\" -";
                break;
            case "PUBLIC_NODES":
                jqExpression = "jq -crM '.nodes[] | select((.role ?== \"agent\") and .public ?== true) | .networking[0].ip' | paste -sd \",\" -";
                break;
            case "PUBLIC_NODE":
                jqExpression = "jq -cM '.nodes[] | select((.role ?== \"agent\") and .public ?== true) | .networking[0].ip' | jq -crMs '.[0]'";
                break;
            case "GOSEC_NODES":
                jqExpression = "jq -crM '.nodes[] | select(.role ?== \"gosec\") | .networking[0].ip' | paste -sd \",\" -";
                break;
            case "ID":
                jqExpression = "jq -crM .id";
                break;
            case "DNS_SEARCH":
                jqExpression = "jq -crM .dnsSearch";
                break;
            case "INTERNAL_DOMAIN":
                jqExpression = "jq -crM .internalDomain";
                break;
            case "ARTIFACT_REPO":
                jqExpression = "jq -crM .artifactRepository";
                break;
            case "DOCKER_REGISTRY":
                jqExpression = "jq -crM .dockerRegistry";
                break;
            case "EXTERNAL_DOCKER_REGISTRY":
                jqExpression = "jq -crM .externalDockerRegistry";
                break;
            case "REALM":
                jqExpression = "jq -crM .security.kerberos.realm";
                break;
            case "KDC_HOST":
                jqExpression = "jq -crM .security.kerberos.kdcHost";
                break;
            case "KDC_PORT":
                jqExpression = "jq -crM .security.kerberos.kdcPort";
                break;
            case "KADMIN_HOST":
                jqExpression = "jq -crM .security.kerberos.kadminHost";
                break;
            case "KADMIN_PORT":
                jqExpression = "jq -crM .security.kerberos.kadminPort";
                break;
            case "LDAP_HOST":
                jqExpression = "jq -crM .security.ldap.url";
                break;
            case "VAULT_HOST":
                jqExpression = "jq -crM '.nodes[] | select(.role ?== \"gosec\") | .networking[0].ip' | paste -sd \",\" - | cut -d, -f1";
                break;
            case "IP":
                jqExpression = "jq -crM '.nodes[] | select(.role ?== \"master\") | .networking[0].ip' | paste -sd \",\" - | cut -d, -f1";
                break;
            case "ADMIN_USER":
                jqExpression = "jq -crM .security.ldap.adminUserUuid";
                break;
            case "TENANT":
                jqExpression = "jq -crM .security.tenantSSODefault | sed 's/null/NONE/g'";
                break;
            case "ACCESS_POINT":
                jqExpression = "jq -crM .proxyAccessPointURL | sed 's/https:\\/\\///g'";
                break;
            case "LDAP_URL":
                jqExpression = "jq -crM .security.ldap.url";
                break;
            case "LDAP_PORT":
                jqExpression = "jq -crM .security.ldap.port";
                break;
            case "LDAP_USER_DN":
                jqExpression = "jq -crM .security.ldap.userDn";
                break;
            case "LDAP_GROUP_DN":
                jqExpression = "jq -crM .security.ldap.groupDN";
                break;
            case "LDAP_BASE":
                jqExpression = "jq -crM .security.ldap.ldapBase";
                break;
            case "LDAP_ADMIN_GROUP":
                jqExpression = "jq -crM .security.ldap.adminrouterAuthorizedGroup";
                break;
            default:
                break;
        }

        obtainInfoFromFile(jqExpression, this.descriptorPath, envVar);

    }

    /**
     * Obtain info from ETCD
     *
     * @throws Exception
     */
    public void obtainBasicInfoFromETCD() throws Exception {
        String localVaultResponseFile = null;
        String localVaultResponseFilePath = null;
        String localCaTrustFilePath = null;

        String bootstrap_ip = ThreadProperty.get("BOOTSTRAP_IP");
        String bootstrap_user = null;
        String bootstrap_pem = null;
        String remotePort;

        // Check if needed parameters have been passed
        if (bootstrap_ip == null) {
            bootstrap_ip = System.getProperty("BOOTSTRAP_IP");

            if (bootstrap_ip == null) {
                throw new Exception("BOOTSTRAP_IP variable needs to be provided in order to obtain information from system.");
            }
            ThreadProperty.set("BOOTSTRAP_IP", bootstrap_ip);
        }
        String bootstrap_ipSystem = System.getProperty("BOOTSTRAP_IP");
        if (bootstrap_ipSystem == null) {
            System.setProperty("BOOTSTRAP_IP", bootstrap_ip);
        }

        bootstrap_user = ThreadProperty.get("CLUSTER_SSH_USER") != null ? ThreadProperty.get("CLUSTER_SSH_USER") : System.getProperty("REMOTE_USER");
        assertThat(bootstrap_user).as("REMOTE_USER variable needs to be provided in order to obtain information from system.").isNotNull();
        ThreadProperty.set("CLUSTER_SSH_USER", bootstrap_user);

        String remote_user = System.getProperty("REMOTE_USER");
        if (remote_user == null) {
            System.setProperty("REMOTE_USER", bootstrap_user);
        }

        bootstrap_pem = ThreadProperty.get("CLUSTER_SSH_PEM_PATH") != null ? ThreadProperty.get("CLUSTER_SSH_PEM_PATH") : System.getProperty("PEM_FILE_PATH");
        assertThat(bootstrap_pem).as("PEM_FILE_PATH variable needs to be provided in order to obtain information from system.").isNotNull();
        ThreadProperty.set("CLUSTER_SSH_PEM_PATH", bootstrap_pem);

        String pem_file_path = System.getProperty("PEM_FILE_PATH");
        if (pem_file_path == null) {
            System.setProperty("PEM_FILE_PATH", bootstrap_pem);
        }

        localVaultResponseFile = "vault_response_" + bootstrap_ip;
        localVaultResponseFilePath = "./target/test-classes/" + localVaultResponseFile;
        localCaTrustFilePath = "./target/test-classes/ca_test.crt";

        if (System.getProperty("EOS_NEW_SSH_PORT") != null) {
            remotePort = System.getProperty("EOS_NEW_SSH_PORT");
        } else {
            remotePort = "22";
        }

        // Open connection to bootstrap
        commonspec.getLogger().debug("Openning connection to bootstrap to obtain descriptor file: " + descriptorPath);
        commonspec.setRemoteSSHConnection(new RemoteSSHConnection(bootstrap_user, null, bootstrap_ip, remotePort, bootstrap_pem), "bootstrap_connection");

        // Make local copy of vault response file
        commonspec.getLogger().debug("Copying vault_response file to: " + localVaultResponseFilePath);
        commonspec.getRemoteSSHConnection().copyFrom(vaultResponsePath, localVaultResponseFilePath);

        // Make local copy of CA
        String caTrust = (System.getProperty("EOS_CA_TRUST") != null) ? System.getProperty("EOS_CA_TRUST") : "/stratio_volume/cas_trusted/ca.crt";
        commonspec.getLogger().debug("Copying ca_trust file to: " + localCaTrustFilePath);
        commonspec.getRemoteSSHConnection().copyFrom(caTrust, localCaTrustFilePath);

        // Close connection to bootstrap system
        commonspec.getRemoteSSHConnection().closeConnection();
        RemoteSSHConnectionsUtil.getRemoteSSHConnectionsMap().remove("bootstrap_connection");
        RemoteSSHConnectionsUtil.setLastRemoteSSHConnectionId(null);
        RemoteSSHConnectionsUtil.setLastRemoteSSHConnection(null);

        // Save content of vault response file in memory to speed up process
        String response = commonspec.retrieveData(localVaultResponseFile, "json");

        if (ThreadProperty.get("configuration_api_id") == null) {
            fail("configuration_api_id variable is not set. Check configuration-api is installed and @dcos annotation is working properly.");
        }

        // Set sso token
        String dcosPassword = ThreadProperty.get("DCOS_PASSWORD") != null ? ThreadProperty.get("DCOS_PASSWORD") : System.getProperty("DCOS_PASSWORD");
        setGoSecSSOCookie(null, null, ThreadProperty.get("EOS_ACCESS_POINT"), ThreadProperty.get("DCOS_USER"), dcosPassword, ThreadProperty.get("DCOS_TENANT"), null, null);
        // Securely send requests
        commonspec.setRestProtocol("https://");
        commonspec.setRestHost(ThreadProperty.get("EOS_ACCESS_POINT"));
        commonspec.setRestPort(":443");

        // Obtain configuration-api endpoint
        String path = "/dcs/v1/fabric";
        String endpoint = "/service/" + ThreadProperty.get("configuration_api_id") + "/etcd?path=" + path;

        Future<Response> responseETCD = commonspec.generateRequest("GET", false, null, null, endpoint, "", null);
        commonspec.setResponse("GET", responseETCD.get());

        if (commonspec.getResponse().getStatusCode() != 200) {
            logger.error("Obtain info from ETCD: " + endpoint + " failed with status code: " + commonspec.getResponse().getStatusCode() + " and response: " + commonspec.getResponse().getResponse());
            throw new Exception("Obtain info from ETCD: " + endpoint + " failed with status code: " + commonspec.getResponse().getStatusCode() + " and response: " + commonspec.getResponse().getResponse());
        }

        String etcdInfo = commonspec.getResponse().getResponse();

        obtainJSONInfoAndExpose(etcdInfo, "$.eos.internalDomain", "EOS_INTERNAL_DOMAIN", null);
        obtainJSONInfoAndExpose(etcdInfo, "$.eos.dockerRegistry", "DOCKER_REGISTRY", null);
        obtainJSONInfoAndExpose(etcdInfo, "$.eos.externalDockerRegistry", "EXTERNAL_DOCKER_REGISTRY", null);
        obtainJSONInfoAndExpose(etcdInfo, "$.eos.artifactRepository", "ARTIFACT_REPOSITORY", null);
        obtainJSONInfoAndExpose(etcdInfo, "$.eos.proxyAccessPointURL", "EOS_ACCESS_POINT", null);
        obtainJSONInfoAndExpose(etcdInfo, "$.eos.consulDatacenter", "CONSUL_DATACENTER", null);
        obtainJSONInfoAndExpose(etcdInfo, "$.globals.sso.ssoTenantDefault", "DCOS_TENANT", null);
        obtainJSONInfoAndExpose(etcdInfo, "$.globals.kerberos.realm", "EOS_REALM", null);

        obtainJSONInfoAndExpose(etcdInfo, "$.globals.kerberos.kdcHost", "KDC_HOST", null);
        obtainJSONInfoAndExpose(etcdInfo, "$.globals.kerberos.kdcPort", "KDC_PORT", null);
        obtainJSONInfoAndExpose(etcdInfo, "$.globals.kerberos.kadminHost", "KADMIN_HOST", null);
        obtainJSONInfoAndExpose(etcdInfo, "$.globals.kerberos.kadminPort", "KADMIN_PORT", null);

        if (System.getProperty("ADFS") != null && System.getProperty("ADFS").equalsIgnoreCase("yes") && System.getProperty("DCOS_USER") != null) {
            ThreadProperty.set("DCOS_USER", System.getProperty("DCOS_USER"));
        } else {
            obtainJSONInfoAndExpose(etcdInfo, "$.globals.ldap.adminUserUuid", "DCOS_USER", null);
        }
        obtainJSONInfoAndExpose(etcdInfo, "$.globals.ldap.url", "LDAP_URL", null);
        obtainJSONInfoAndExpose(etcdInfo, "$.globals.ldap.port", "LDAP_PORT", null);
        obtainJSONInfoAndExpose(etcdInfo, "$.globals.ldap.userDn", "LDAP_USER_DN", null);
        obtainJSONInfoAndExpose(etcdInfo, "$.globals.ldap.groupDN", "LDAP_GROUP_DN", null);
        obtainJSONInfoAndExpose(etcdInfo, "$.globals.ldap.ldapBase", "LDAP_BASE", null);
        obtainJSONInfoAndExpose(etcdInfo, "$.globals.ldap.adminrouterAuthorizedGroup", "LDAP_ADMIN_GROUP", null);
        obtainJSONInfoAndExpose(etcdInfo, "$.globals.vault.vaultHost", "EOS_VAULT_HOST_INTERNAL", null);
        obtainJSONInfoAndExpose(etcdInfo, "$.globals.vault.vaultPort", "VAULT_PORT", null);

        String[] schemaVersion = ThreadProperty.get("EOS_SCHEMA_VERSION").split("\\.");
        if (Integer.parseInt(schemaVersion[0]) > 0 && Integer.parseInt(schemaVersion[1]) > 3) {
            obtainJSONInfoAndExpose(etcdInfo, "$.cluster_info.descriptor.id", "EOS_CLUSTER_ID", null);
            obtainJSONInfoAndExpose(etcdInfo, "$.cluster_info.descriptor.dnsSearch", "EOS_DNS_SEARCH", null);
            obtainJSONInfoAndExpose(etcdInfo, "$.cluster_info.descriptor.nodes[?(@.role == \"master\")].networking[0].ip", "DCOS_IP", "0");
            obtainJSONInfoAndExpose(etcdInfo, "$.cluster_info.descriptor.nodes[?(@.role == \"agent\" && @.public == true)].networking[0].ip", "PUBLIC_NODE", "0");
            obtainJSONInfoAndExpose(etcdInfo, "$.cluster_info.descriptor.nodes[?(@.role == \"gosec\")].networking[0].ip", "VAULT_HOST", "0");
            obtainJSONInfoAndExpose(etcdInfo, "$.globals.overlayNetwork.addressPool", "ADDRESS_POOL", null);
        }

        ThreadProperty.set("EOS_VAULT_HOST", ThreadProperty.get("VAULT_HOST")); // Retrocompatibily, it will be removed in next version
        ThreadProperty.set("EOS_VAULT_PORT", ThreadProperty.get("VAULT_PORT")); // Retrocompatibily, it will be removed in next version

        obtainJSONInfo(response, "ROOT_TOKEN", "VAULT_TOKEN");

    }

    /**
     * Obtain info from json and expose in thread variable
     *
     * @param json          : json to look for info in
     * @param jqExpression  : jq expression to obtain specific info from json
     * @param envVar        : thread variable where to expose value
     * @param position      : position in value obtained with jq
     */
    public void obtainJSONInfoAndExpose(String json, String jqExpression, String envVar, String position) {
        String value = "";
        try {
            value = commonspec.getJSONPathString(json, jqExpression, position).replaceAll("\\[", "").replaceAll("\\]", "").replaceAll("\"", "");
        } catch (Exception e) {
            if (envVar == "PUBLIC_NODE" || envVar == "DCOS_TENANT") {
                value = null;
            } else {
                throw e;
            }
        }

        switch (envVar) {
            case "DCOS_TENANT":
                if (value == null) {
                    value = "NONE";
                }
                break;
            case "EOS_ACCESS_POINT":
                value = value.replaceAll("https://", "");
                break;
            default:
                break;
        }

        if (!(envVar == "PUBLIC_NODE" && value == null)) {
            ThreadProperty.set(envVar, value);
        }
    }


    /**
     * Obtains ssh user and pem from daedalus system:
     * CLUSTER_SSH_USER, CLUSTER_SSH_PEM, CLUSTER_SSH_PEM_PATH
     *
     * @throws Exception
     */
    public void obtainBasicInfoFromWorkspace() throws Exception {

        String clusterName = System.getProperty("EOS_CLUSTER_ID");
        if (clusterName == null) {
            commonspec.getLogger().info("Info cannot be retrieved from workspace without EOS_CLUSTER_ID. BOOTSTRAP_IP, REMOTE_USER and PEM_FILE_PATH need to be provided.");
            return;
        }

        String daedalusSystem = "daedalus-workspaces.int.stratio.com";
        String workspaceName = "daedalus-workspace-" + clusterName;
        String workspaceURL = "http://" + daedalusSystem + "/" + workspaceName + ".tgz";

        // Download workspace
        String commandWget = "wget -T 20 -t 1 " + workspaceURL;
        commonspec.runLocalCommand(commandWget);
        if (commonspec.getCommandExitStatus() != 0) {
            String bootstrap_ip = System.getProperty("BOOTSTRAP_IP");
            String remote_user = System.getProperty("REMOTE_USER");
            String pem_file_path = System.getProperty("PEM_FILE_PATH");
            if (bootstrap_ip == null || remote_user == null || pem_file_path == null) {
                throw new Exception("Not possible to download workspace tgz.");
            }
            commonspec.getLogger().info("Info cannot be retrieved from workspace. Trying old way");
            return;
        }

        // Untar workspace
        CommandExecutionSpec commandExecutionSpec = new CommandExecutionSpec(commonspec);
        String commandUntar = "tar -C ./target/test-classes/ -xvf " + workspaceName + ".tgz";
        commandExecutionSpec.executeLocalCommand(commandUntar, null, null);

        // Clean
        String commandRmTgz = "rm " + workspaceName + ".tgz";
        commandExecutionSpec.executeLocalCommand(commandRmTgz, null, null);

        // Obtain and export values
        String daedalusJson = commonspec.retrieveData(workspaceName + "/daedalus.json", "json");
        String varClusterSSHUser = "CLUSTER_SSH_USER";
        obtainJSONInfo(daedalusJson, "CLUSTER_SSH_USER", varClusterSSHUser);

        String varClusterBootstrapIP = "BOOTSTRAP_IP";
        obtainJSONInfo(daedalusJson, "BOOTSTRAP_IP", varClusterBootstrapIP);

        String varClusterSSHPemPath = "CLUSTER_SSH_PEM_PATH";
        ThreadProperty.set(varClusterSSHPemPath, "./target/test-classes/" + workspaceName + "/key");

        // Obtain secrets
        String daedalusSecretsPass = System.getProperty("EOS_DAEDALUS_SECRETS_PASS");
        if (daedalusSecretsPass == null) {
            commonspec.getLogger().info("Secrets cannot be retrieved from workspace without EOS_DAEDALUS_SECRETS_PASS. DCOS_PASSWORD,LDAP_USER,LDAP_PASSWORD,DCOS_TENANT1,DCOS_TENANT1_OWNER_USER,DCOS_TENANT1_OWNER_PASSWORD,DCOS_TENANT1_OWNER_GROUP,DCOS_TENANT1_OWNER_POLICY_NAME,DCOS_TENANT2,DCOS_TENANT2_OWNER_USER,DCOS_TENANT2_OWNER_PASSWORD,DCOS_TENANT2_OWNER_GROUP and DCOS_TENANT2_OWNER_POLICY_NAME need to be provided.");
            return;
        }

        String daedalusSecrets = new String(Files.readAllBytes(Paths.get(getClass().getClassLoader().getResource(workspaceName + "/secrets.yml").getFile())));
        String daedalusSecretsPassword = System.getProperty("EOS_DAEDALUS_SECRETS_PASS");

        byte[] decryptedSecrets = VaultAnsible.decrypt(daedalusSecrets.getBytes(), daedalusSecretsPassword);
        String decryptedString = new String(decryptedSecrets, StandardCharsets.UTF_8);

        LinkedHashMap<String, Object> yamlMap = (LinkedHashMap<String, Object>) new Yaml().load(decryptedString);
        LinkedHashMap<String, Object> yamlMapSecrets = (LinkedHashMap<String, Object>) yamlMap.get("secrets");

        // LDAP secrets
        LinkedHashMap<String, Object> yamlMapSecretsLdap = (LinkedHashMap<String, Object>) yamlMapSecrets.get("ldap");
        LinkedHashMap<String, Object> yamlMapSecretsLdapAdmin = (LinkedHashMap<String, Object>) yamlMapSecretsLdap.get("admin");
        LinkedHashMap<String, Object> yamlMapSecretsLdapSuperAdmin = (LinkedHashMap<String, Object>) yamlMapSecretsLdap.get("super_admin");
        if (System.getProperty("ADFS") != null && System.getProperty("ADFS").equalsIgnoreCase("yes") && System.getProperty("DCOS_PASSWORD") != null) {
            ThreadProperty.set("DCOS_PASSWORD", System.getProperty("DCOS_PASSWORD"));
        } else {
            ThreadProperty.set("DCOS_PASSWORD", yamlMapSecretsLdapAdmin.get("pass").toString());
        }
        ThreadProperty.set("LDAP_USER", yamlMapSecretsLdapSuperAdmin.get("user").toString());
        ThreadProperty.set("LDAP_PASSWORD", yamlMapSecretsLdapSuperAdmin.get("pass").toString());

        // There can be custers without tenants info in secrets.yml
        try {
            // Tenants secrets
            LinkedHashMap<String, Object> yamlTenants = (LinkedHashMap<String, Object>) yamlMapSecrets.get("qa_tenants");

            yamlTenants.keySet().forEach(tenant -> {
                LinkedHashMap<String, Object> yamlMapSecretsTenant = (LinkedHashMap<String, Object>) yamlTenants.get(tenant);
                LinkedHashMap<String, Object> yamlMapSecretsTenantOwner = (LinkedHashMap<String, Object>) yamlMapSecretsTenant.get("owner");
                ThreadProperty.set("DCOS_" + tenant.toUpperCase(), yamlMapSecretsTenant.get("id").toString());
                ThreadProperty.set("DCOS_" + tenant.toUpperCase() + "_OWNER_USER", yamlMapSecretsTenantOwner.get("user").toString());
                ThreadProperty.set("DCOS_" + tenant.toUpperCase() + "_OWNER_PASSWORD", yamlMapSecretsTenantOwner.get("pass").toString());
                ThreadProperty.set("DCOS_" + tenant.toUpperCase() + "_OWNER_GROUP", yamlMapSecretsTenant.get("group").toString());
                ThreadProperty.set("DCOS_" + tenant.toUpperCase() + "_OWNER_POLICY_NAME", yamlMapSecretsTenantOwner.get("policy_name").toString());
            });
        } catch (NullPointerException npe) {
            commonspec.getLogger().info("Tenants secrets not stored in workspace.");
        }
    }

    /**
     * Obtains basic information for tests from descriptor file:
     * EOS_CLUSTER_ID, EOS_DNS_SEARCH, EOS_INTERNAL_DOMAIN, DCOS_IP, DCOS_USER, DCOS_TENANT, VAULT_TOKEN
     * LDAP_URL, LDAP_PORT, LDAP_USER_DN, LDAP_GROUP_DN, LDAP_BASE, LDAP_ADMIN_GROUP
     *
     * @throws Exception
     */
    @Given("^I( force to)? obtain basic information from bootstrap$")
    public void obtainBasicInfoFromDescriptor(String force) throws Exception {
        String localDescriptorFile = null;
        String localDescriptorFilePath = null;
        String localVaultResponseFile = null;
        String localVaultResponseFilePath = null;
        String localCaTrustFilePath = null;
        String remotePort;

        // General values
        String varClusterID = "EOS_CLUSTER_ID";
        String varSchemaVersion = "EOS_SCHEMA_VERSION";
        String varClusterDomain = "EOS_DNS_SEARCH";
        String varInternalDomain = "EOS_INTERNAL_DOMAIN";
        String varIp = "DCOS_IP";
        String varAdminUser = "DCOS_USER";
        String varTenant = "DCOS_TENANT";
        String varVaultHost = "VAULT_HOST";
        String varVaultPort = "EOS_VAULT_PORT";
        String varVaultToken = "VAULT_TOKEN";
        String varPublicNode = "PUBLIC_NODE";
        String varAccessPoint = "EOS_ACCESS_POINT";
        String varRealm = "EOS_REALM";
        String varAddressPool = "ADDRESS_POOL";
        String varDockerRegistry = "DOCKER_REGISTRY";
        String varExternalDockerRegistry = "EXTERNAL_DOCKER_REGISTRY";
        String varArtifactRepository = "ARTIFACT_REPOSITORY";
        String varConsulDatacenter = "CONSUL_DATACENTER";

        String varKdcHost = "KDC_HOST";
        String varKdcPort = "KDC_PORT";
        String varKAdminHost = "KADMIN_HOST";
        String varKAdminPort = "KADMIN_PORT";

        // LDAP values
        String varLDAPurl = "LDAP_URL";
        String varLDAPport = "LDAP_PORT";
        String varLDAPuserDn = "LDAP_USER_DN";
        String varLDAPgroupDn = "LDAP_GROUP_DN";
        String varLDAPbase = "LDAP_BASE";
        String varLDAPadminGroup = "LDAP_ADMIN_GROUP";

        String bootstrap_ip = ThreadProperty.get("BOOTSTRAP_IP");
        String bootstrap_user = null;
        String bootstrap_pem = null;

        // Check if needed parameters have been passed
        if (bootstrap_ip == null) {
            bootstrap_ip = System.getProperty("BOOTSTRAP_IP");

            if (bootstrap_ip == null) {
                throw new Exception("BOOTSTRAP_IP variable needs to be provided in order to obtain information from system.");
            }
            ThreadProperty.set("BOOTSTRAP_IP", bootstrap_ip);
        }
        String bootstrap_ipSystem = System.getProperty("BOOTSTRAP_IP");
        if (bootstrap_ipSystem == null) {
            System.setProperty("BOOTSTRAP_IP", bootstrap_ip);
        }

        bootstrap_user = ThreadProperty.get("CLUSTER_SSH_USER") != null ? ThreadProperty.get("CLUSTER_SSH_USER") : System.getProperty("REMOTE_USER");
        assertThat(bootstrap_user).as("REMOTE_USER variable needs to be provided in order to obtain information from system.").isNotNull();
        ThreadProperty.set("CLUSTER_SSH_USER", bootstrap_user);

        String remote_user = System.getProperty("REMOTE_USER");
        if (remote_user == null) {
            System.setProperty("REMOTE_USER", bootstrap_user);
        }

        bootstrap_pem = ThreadProperty.get("CLUSTER_SSH_PEM_PATH") != null ? ThreadProperty.get("CLUSTER_SSH_PEM_PATH") : System.getProperty("PEM_FILE_PATH");
        assertThat(bootstrap_pem).as("PEM_FILE_PATH variable needs to be provided in order to obtain information from system.").isNotNull();
        ThreadProperty.set("CLUSTER_SSH_PEM_PATH", bootstrap_pem);

        String pem_file_path = System.getProperty("PEM_FILE_PATH");
        if (pem_file_path == null) {
            System.setProperty("PEM_FILE_PATH", bootstrap_pem);
        }

        localDescriptorFile = "descriptor_" + bootstrap_ip + ".json";
        localDescriptorFilePath = "./target/test-classes/" + localDescriptorFile;
        localVaultResponseFile = "vault_response_" + bootstrap_ip;
        localVaultResponseFilePath = "./target/test-classes/" + localVaultResponseFile;
        localCaTrustFilePath = "./target/test-classes/ca_test.crt";

        String[] vars = {varClusterID, varClusterDomain, varInternalDomain, varIp, varAdminUser, varTenant, varVaultHost, varVaultToken, varPublicNode, varAccessPoint, varRealm, varAddressPool, varLDAPurl, varLDAPport, varLDAPuserDn, varLDAPgroupDn, varLDAPbase, varLDAPadminGroup};
        boolean bootstrapInfoObtained = true;

        // Check if info have been retrieved previously
        if (force != null) {
            bootstrapInfoObtained = false;
        } else {
            for (String var : vars) {
                if (ThreadProperty.get(var) == null) {
                    bootstrapInfoObtained = false;
                    break;
                }
            }
        }

        if (System.getProperty("EOS_NEW_SSH_PORT") != null) {
            remotePort = System.getProperty("EOS_NEW_SSH_PORT");
        } else {
            remotePort = "22";
        }

        if (!bootstrapInfoObtained) {
            commonspec.getLogger().debug("Openning connection to bootstrap to obtain descriptor file: " + descriptorPath);
            commonspec.setRemoteSSHConnection(new RemoteSSHConnection(bootstrap_user, null, bootstrap_ip, remotePort, bootstrap_pem), "bootstrap_connection");

            commonspec.getLogger().debug("Copying descriptor file to: " + localDescriptorFilePath);
            commonspec.getRemoteSSHConnection().copyFrom(descriptorPath, localDescriptorFilePath);

            commonspec.getLogger().debug("Copying vault_response file to: " + localVaultResponseFilePath);
            commonspec.getRemoteSSHConnection().copyFrom(vaultResponsePath, localVaultResponseFilePath);

            String caTrust = (System.getProperty("EOS_CA_TRUST") != null) ? System.getProperty("EOS_CA_TRUST") : "/stratio_volume/cas_trusted/ca.crt";
            commonspec.getLogger().debug("Copying ca_trust file to: " + localCaTrustFilePath);
            commonspec.getRemoteSSHConnection().copyFrom(caTrust, localCaTrustFilePath);

            // Close connection to bootstrap system
            commonspec.getRemoteSSHConnection().closeConnection();
            RemoteSSHConnectionsUtil.getRemoteSSHConnectionsMap().remove("bootstrap_connection");
            RemoteSSHConnectionsUtil.setLastRemoteSSHConnectionId(null);
            RemoteSSHConnectionsUtil.setLastRemoteSSHConnection(null);

            // Save content of files in memory to speed up process
            String descriptor = commonspec.retrieveData(localDescriptorFile, "json");
            String response = commonspec.retrieveData(localVaultResponseFile, "json");

            obtainJSONInfo(descriptor, "ID", varClusterID);
            obtainJSONInfo(descriptor, "SCHEMA_VERSION", varSchemaVersion);
            obtainJSONInfo(descriptor, "DNS_SEARCH", varClusterDomain);
            obtainJSONInfo(descriptor, "INTERNAL_DOMAIN", varInternalDomain);
            obtainJSONInfo(descriptor, "IP", varIp);
            if (System.getProperty("ADFS") != null && System.getProperty("ADFS").equalsIgnoreCase("yes") && System.getProperty("DCOS_USER") != null) {
                ThreadProperty.set("DCOS_USER", System.getProperty("DCOS_USER"));
            } else {
                obtainJSONInfo(descriptor, "ADMIN_USER", varAdminUser);
            }
            obtainJSONInfo(descriptor, "TENANT", varTenant);
            obtainJSONInfo(descriptor, "VAULT_HOST", varVaultHost);
            ThreadProperty.set("EOS_VAULT_HOST", ThreadProperty.get(varVaultHost)); // Retrocompatibily, it will be removed in next version
            ThreadProperty.set(varVaultPort, "8200");
            ThreadProperty.set("EOS_VAULT_PORT", ThreadProperty.get(varVaultPort)); // Retrocompatibily, it will be removed in next version
            obtainJSONInfo(descriptor, "REALM", varRealm);
            obtainJSONInfo(descriptor, "ACCESS_POINT", varAccessPoint);
            obtainJSONInfo(descriptor, "LDAP_URL", varLDAPurl);
            obtainJSONInfo(descriptor, "LDAP_PORT", varLDAPport);
            obtainJSONInfo(descriptor, "LDAP_USER_DN", varLDAPuserDn);
            obtainJSONInfo(descriptor, "LDAP_GROUP_DN", varLDAPgroupDn);
            obtainJSONInfo(descriptor, "LDAP_BASE", varLDAPbase);
            obtainJSONInfo(descriptor, "LDAP_ADMIN_GROUP", varLDAPadminGroup);
            obtainJSONInfo(descriptor, "PUBLIC_NODE", varPublicNode);
            obtainJSONInfo(descriptor, "ADDRESS_POOL", varAddressPool);
            obtainJSONInfo(descriptor, "DOCKER_REGISTRY", varDockerRegistry);
            obtainJSONInfo(descriptor, "EXTERNAL_DOCKER_REGISTRY", varExternalDockerRegistry);
            obtainJSONInfo(descriptor, "ARTIFACT_REPOSITORY", varArtifactRepository);
            obtainJSONInfo(descriptor, "CONSUL_DATACENTER", varConsulDatacenter);

            obtainJSONInfo(descriptor, "KDC_HOST", varKdcHost);
            obtainJSONInfo(descriptor, "KDC_PORT", varKdcPort);
            obtainJSONInfo(descriptor, "KADMIN_HOST", varKAdminHost);
            obtainJSONInfo(descriptor, "KADMIN_PORT", varKAdminPort);

            obtainJSONInfo(response, "ROOT_TOKEN", varVaultToken);
        } else {
            commonspec.getLogger().debug("Basic information from bootstrap was previously obtained");
        }
    }


    /**
     * Obtains info from a json stored in a variable and expose it in a thread variable
     *
     * @param json      : json where to get info from
     * @param info      : info to be obtained from json
     * @param envVar    : thread environment variable where to expose obtained info
     */
    public void obtainJSONInfo(String json, String info, String envVar) {
        String jqExpression = "";
        String position = null;

        switch (info) {
            case "MASTERS":
                jqExpression = "$.nodes[?(@.role == \"master\")].networking[0].ip";
                break;
            case "NODES":
                jqExpression = "$.nodes[?(@.role == \"agent\")].networking[0].ip";
                break;
            case "PRIV_NODES":
                jqExpression = "$.nodes[?(@.role == \"agent\" && @.public == false)].networking[0].ip";
                break;
            case "PUBLIC_NODES":
                jqExpression = "$.nodes[?(@.role == \"agent\" && @.public == true)].networking[0].ip";
                break;
            case "PUBLIC_NODE":
                jqExpression = "$.nodes[?(@.role == \"agent\" && @.public == true)].networking[0].ip";
                position = "0";
                break;
            case "GOSEC_NODES":
                jqExpression = "$.nodes[?(@.role == \"gosec\")].networking[0].ip";
                break;
            case "ID":
                jqExpression = "$.id";
                break;
            case "SCHEMA_VERSION":
                jqExpression = "$.schemaVersion";
                break;
            case "DNS_SEARCH":
                jqExpression = "$.dnsSearch";
                break;
            case "INTERNAL_DOMAIN":
                jqExpression = "$.internalDomain";
                break;
            case "ARTIFACT_REPOSITORY":
                jqExpression = "$.artifactRepository";
                break;
            case "DOCKER_REGISTRY":
                jqExpression = "$.dockerRegistry";
                break;
            case "EXTERNAL_DOCKER_REGISTRY":
                jqExpression = "$.externalDockerRegistry";
                break;
            case "REALM":
                jqExpression = "$.security.kerberos.realm";
                break;
            case "KDC_HOST":
                jqExpression = "$.security.kerberos.kdcPort";
                break;
            case "KDC_PORT":
                jqExpression = "$.security.kerberos.kdcHost";
                break;
            case "KADMIN_HOST":
                jqExpression = "$.security.kerberos.kadminHost";
                break;
            case "KADMIN_PORT":
                jqExpression = "$.security.kerberos.kadminPort";
                break;
            case "LDAP_HOST":
                jqExpression = "$.security.ldap.url";
                break;
            case "VAULT_HOST":
                jqExpression = "$.nodes[?(@.role == \"gosec\")].networking[0].ip";
                position = "0";
                break;
            case "IP":
                jqExpression = "$.nodes[?(@.role == \"master\")].networking[0].ip";
                position = "0";
                break;
            case "ADMIN_USER":
                jqExpression = "$.security.ldap.adminUserUuid";
                break;
            case "TENANT":
                jqExpression = "$.security.tenantSSODefault";
                break;
            case "ACCESS_POINT":
                jqExpression = "$.proxyAccessPointURL";
                break;
            case "LDAP_URL":
                jqExpression = "$.security.ldap.url";
                break;
            case "LDAP_PORT":
                jqExpression = "$.security.ldap.port";
                break;
            case "LDAP_USER_DN":
                jqExpression = "$.security.ldap.userDn";
                break;
            case "LDAP_GROUP_DN":
                jqExpression = "$.security.ldap.groupDN";
                break;
            case "LDAP_BASE":
                jqExpression = "$.security.ldap.ldapBase";
                break;
            case "LDAP_ADMIN_GROUP":
                jqExpression = "$.security.ldap.adminrouterAuthorizedGroup";
                break;
            case "ROOT_TOKEN":
                jqExpression = "$.root_token";
                break;
            case "ADDRESS_POOL":
                jqExpression = "$.security.overlayNetwork.addressPool";
                break;
            case "CONSUL_DATACENTER":
                jqExpression = "$.consulDatacenter";
                break;
            case "CLUSTER_SSH_USER":
                jqExpression = "$.infra.ssh_user";
                break;
            case "BOOTSTRAP_IP":
                jqExpression = "$.infra.bootstrap_ip";
                break;
            default:
                break;
        }

        String value = "";
        try {
            value = commonspec.getJSONPathString(json, jqExpression, position).replaceAll("\\[", "").replaceAll("\\]", "").replaceAll("\"", "");
        } catch (Exception e) {
            if (info == "PUBLIC_NODE" || info == "TENANT") {
                value = null;
            } else {
                throw e;
            }
        }

        switch (info) {
            case "TENANT":
                if (value == null) {
                    value = "NONE";
                }
                break;
            case "ACCESS_POINT":
                value = value.replaceAll("https://", "");
                break;
            default:
                break;
        }

        if (!(info == "PUBLIC_NODE" && value == null)) {
            ThreadProperty.set(envVar, value);
        }

    }

    /**
     * Obtain service info from marathon
     *
     * @throws Exception
     */
    @Given("^I get services info from marathon( forcefully)?")
    public void getServicesInfoFromMarathon(String force) throws Exception {
        if (ThreadProperty.get("marathonVariables") == null || force != null) {
            getServicesInfoFromMarathonImpl();
        }
    }

    /**
     * Using marathon API, get all services deployed in cluster and set CCT variables
     *
     * @throws Exception
     */
    private void getServicesInfoFromMarathonImpl() throws Exception {
        if (System.getProperty("DCOS_PASSWORD") == null && ThreadProperty.get("DCOS_PASSWORD") == null) {
            throw new Exception("DCOS_PASSWORD should be defined when we use @dcos annotation");
        } else if (System.getProperty("DCOS_PASSWORD") == null) {
            System.setProperty("DCOS_PASSWORD", ThreadProperty.get("DCOS_PASSWORD"));
        } else {
            ThreadProperty.set("DCOS_PASSWORD", System.getProperty("DCOS_PASSWORD"));
        }
        String marathonEndPoint = "/service/marathon/v2/apps";
        // Set sso token
        String dcosPassword = ThreadProperty.get("DCOS_PASSWORD") != null ? ThreadProperty.get("DCOS_PASSWORD") : System.getProperty("DCOS_PASSWORD");
        setGoSecSSOCookie(null, null, ThreadProperty.get("EOS_ACCESS_POINT"), ThreadProperty.get("DCOS_USER"), dcosPassword, ThreadProperty.get("DCOS_TENANT"), null, null);
        // Securely send requests
        commonspec.setRestProtocol("https://");
        commonspec.setRestHost(ThreadProperty.get("EOS_ACCESS_POINT"));
        commonspec.setRestPort(":443");
        // Invoke marathon API
        Future<Response> response = commonspec.generateRequest("GET", false, null, null, marathonEndPoint, "", null);
        commonspec.setResponse(marathonEndPoint, response.get());
        if (commonspec.getResponse().getStatusCode() != 200) {
            throw new Exception("Error in marathon request. Response code: " + commonspec.getResponse().getStatusCode());
        }
        // Save versions
        List<String> appsToSaveVersion = Arrays.asList("gosec-management", "dyplon-http", "gosec-identities-daas", "gosec-services-daas", "command-center", "cct-deploy-api", "cct-universe", "cct-marathon-services", "cct-configuration-api", "gosec-management-baas", "gosec-management-ui");
        JSONObject marathonAnswer = new JSONObject(commonspec.getResponse().getResponse());
        JSONArray marathonApps = (JSONArray) marathonAnswer.get("apps");
        for (Object oApp : marathonApps) {
            if (oApp instanceof JSONObject) {
                JSONObject jsonApp = (JSONObject) oApp;
                try {
                    String serviceName = jsonApp.getString("id").replace("/command-center/", "");
                    String dockerImage = jsonApp.getJSONObject("container").getJSONObject("docker").getString("image");
                    String dockerImageName = dockerImage.substring(dockerImage.lastIndexOf("/") + 1, dockerImage.lastIndexOf(":"));
                    String dockerImageVersion = dockerImage.substring(dockerImage.lastIndexOf(":") + 1);
                    if (appsToSaveVersion.contains(dockerImageName)) {
                        ThreadProperty.set(dockerImageName + "_version", dockerImageVersion);
                        commonspec.getLogger().debug(dockerImageName + " - " + dockerImageVersion);
                        switch (dockerImageName) {
                            case "cct-marathon-services":
                                ThreadProperty.set("cct-marathon-services_id", serviceName);
                                break;
                            case "cct-universe":
                                ThreadProperty.set("cct-universe_id", serviceName);
                                break;
                            case "cct-deploy-api":
                                ThreadProperty.set("deploy_api_id", serviceName);
                                break;
                            case "command-center":
                                ThreadProperty.set("cct_ui_id", serviceName);
                                break;
                            case "cct-configuration-api":
                                ThreadProperty.set("configuration_api_id", serviceName);
                                break;
                            default:
                                break;
                        }
                    }
                } catch (Exception e) {
                    if (jsonApp.get("id") != JSONObject.NULL) {
                        commonspec.getLogger().debug("Error obtaining container in service with id: " + jsonApp.getString("id"));
                    }
                }
            }
        }
        ThreadProperty.set("marathonVariables", "true");
    }
}
