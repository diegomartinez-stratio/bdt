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

import com.jayway.jsonpath.PathNotFoundException;
import com.stratio.qa.utils.ThreadProperty;
import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;
import io.cucumber.datatable.DataTable;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.apache.commons.lang.time.DateUtils;
import org.assertj.core.api.Assertions;
import org.hjson.JsonArray;
import org.hjson.JsonValue;

import javax.crypto.spec.SecretKeySpec;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.lang.reflect.InvocationTargetException;
import java.security.Key;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.stratio.qa.assertions.Assertions.assertThat;

/**
 * Generic Miscellaneous Specs.
 *
 * @see <a href="MiscSpec-annotations.html">Miscellaneous Steps &amp; Matching Regex</a>
 */
public class MiscSpec extends BaseGSpec {

    public static final int DEFAULT_TIMEOUT = 1000;

    /**
     * Generic constructor.
     *
     * @param spec object
     */
    public MiscSpec(CommonG spec) {
        this.commonspec = spec;

    }

    /**
     * Save value for future use.
     * <p>
     * If element is a jsonpath expression (i.e. $.fragments[0].id), it will be
     * applied over the last httpResponse.
     * <p>
     * If element is a jsonpath expression preceded by some other string
     * (i.e. ["a","b",,"c"].$.[0]), it will be applied over this string.
     * This will help to save the result of a jsonpath expression evaluated over
     * previous stored variable.
     *
     * @param position position from a search result
     * @param element  key in the json response to be saved
     * @param envVar   thread environment variable where to store the value
     * @throws IllegalAccessException    exception
     * @throws IllegalArgumentException  exception
     * @throws SecurityException         exception
     * @throws NoSuchFieldException      exception
     * @throws ClassNotFoundException    exception
     * @throws InstantiationException    exception
     * @throws InvocationTargetException exception
     * @throws NoSuchMethodException     exception
     */
    @Given("^I save element (in position '(.+?)' in )?'(.+?)'( in environment variable '(.+?)')?( in file '(.+?)')?$")
    public void saveElementEnvironmentFile(String position, String element, String envVar, String file) throws Exception {
        Pattern pattern = Pattern.compile("^((.*)(\\.)+)(\\$.*)$");
        Matcher matcher = pattern.matcher(element);
        String json;
        String parsedElement;

        if (matcher.find()) {
            json = matcher.group(2);
            parsedElement = matcher.group(4);
        } else {
            json = commonspec.getResponse().getResponse();
            parsedElement = element;
        }

        String value = commonspec.getJSONPathString(json, parsedElement, position);

        if (envVar != null) {
            ThreadProperty.set(envVar, value.replaceAll("\n", ""));
        }
        if (file != null) {
            writeInFile(value.replaceAll("\n", ""), file);
        }
    }

    @Deprecated
    public void saveElementEnvironment(String position, String element, String envVar) throws Exception {
        saveElementEnvironmentFile(position, element, envVar, null);
    }

    /**
     * Save value for future use.
     *
     * @param value  value to be saved
     * @param envVar thread environment variable where to store the value
     */
    @Given("^I save '(.+?)' in variable '(.+?)'$")
    public void saveInEnvironment(String value, String envVar) {
        ThreadProperty.set(envVar, value);
    }

    /**
     * Wait seconds.
     *
     * @param seconds
     * @throws InterruptedException
     */
    @When("^I wait '(\\d+)' seconds?$")
    public void idleWait(Integer seconds) throws InterruptedException {
        Thread.sleep(seconds * DEFAULT_TIMEOUT);
    }

    /**
     * Sort elements in envVar by a criteria and order.
     *
     * @param envVar   Environment variable to be sorted
     * @param criteria alphabetical,...
     * @param order    ascending or descending
     */
    @When("^I sort elements in '(.+?)' by '(.+?)' criteria in '(.+?)' order$")
    public void sortElements(String envVar, String criteria, String order) {

        String value = ThreadProperty.get(envVar);
        JsonArray jsonArr = JsonValue.readHjson(value).asArray();

        List<JsonValue> jsonValues = new ArrayList<JsonValue>();
        for (int i = 0; i < jsonArr.size(); i++) {
            jsonValues.add(jsonArr.get(i));
        }

        Comparator<JsonValue> comparator;
        switch (criteria) {
            case "alphabetical":
                commonspec.getLogger().debug("Alphabetical criteria selected.");
                comparator = new Comparator<JsonValue>() {
                    public int compare(JsonValue json1, JsonValue json2) {
                        int res = String.CASE_INSENSITIVE_ORDER.compare(json1.toString(), json2.toString());
                        if (res == 0) {
                            res = json1.toString().compareTo(json2.toString());
                        }
                        return res;
                    }
                };
                break;
            default:
                commonspec.getLogger().debug("No criteria selected.");
                comparator = null;
        }

        if ("ascending".equals(order)) {
            Collections.sort(jsonValues, comparator);
        } else {
            Collections.sort(jsonValues, comparator.reversed());
        }

        ThreadProperty.set(envVar, jsonValues.toString());
    }

    /**
     * Checks if an exception has been thrown.
     *
     * @param exception    : "IS NOT" | "IS"
     * @param clazz
     * @param exceptionMsg
     */
    @Then("^an exception '(.+?)' thrown( with class '(.+?)')?( and message like '(.+?)')?")
    public void assertExceptionNotThrown(String exception, String clazz, String exceptionMsg)
            throws ClassNotFoundException {
        List<Exception> exceptions = commonspec.getExceptions();
        if ("IS NOT".equals(exception)) {
            assertThat(exceptions).as("Captured exception list is not empty").isEmpty();
        } else {
            assertThat(exceptions).as("Captured exception list is empty").isNotEmpty();
            Exception ex = exceptions.get(exceptions.size() - 1);
            if (clazz != null && exceptionMsg != null) {
                assertThat(ex.toString()).as("Unexpected last exception class").contains(clazz);
                assertThat(ex.toString()).as("Unexpected last exception message").contains(exceptionMsg);
            } else if (clazz != null) {
                assertThat(exceptions.get(exceptions.size() - 1).getClass().getSimpleName()).as("Unexpected last exception class").isEqualTo(clazz);
            }

            commonspec.getExceptions().clear();
        }
    }

    /**
     * Check if expression defined by JSOPath (http://goessner.net/articles/JsonPath/index.html)
     * match in JSON stored in a environment variable.
     *
     * @param envVar environment variable where JSON is stored
     * @param table  data table in which each row stores one expression
     */
    @Then("^'(.+?)' matches the following cases:$")
    public void matchWithExpresion(String envVar, DataTable table) throws Exception {
        String jsonString = ThreadProperty.get(envVar);

        for (List<String> row : table.cells()) {
            String expression = row.get(0);
            String condition = row.get(1);
            String result = row.get(2);
            try {
                String value = commonspec.getJSONPathString(jsonString, expression, null);
                commonspec.evaluateJSONElementOperation(value, condition, result);
            } catch (PathNotFoundException e) {
                if (!condition.equals("PathNotFound")) {
                    throw e;
                }
            }
        }
    }

    /*
     * Check value stored in environment variable "is|matches|is higher than|is lower than|contains||does not contain|is different from" to value provided
     *
     * @param envVar
     * @param value
     *
     */
    @Then("^'(.+)' (is|matches|is higher than|is higher than or equal to|is lower than|is lower than or equal to|contains|does not contain|is different from) '(.+)'$")
    public void checkValue(String envVar, String operation, String value) throws Exception {
        switch (operation.toLowerCase()) {
            case "is":
                Assertions.assertThat(envVar).as("Values are not equal.").isEqualTo(value);
                break;
            case "matches":
                Assertions.assertThat(envVar).as("Values are different.").matches(value);
                break;
            case "is higher than":
                if (envVar.matches("^-?\\d+$") && value.matches("^-?\\d+$")) {
                    Assertions.assertThat(Integer.parseInt(envVar)).as("First value is not higher than second one.").isGreaterThan(Integer.parseInt(value));
                } else {
                    throw new Exception("A number should be provided in order to perform a valid comparison.");
                }
                break;
            case "is higher than or equal to":
                if (envVar.matches("^-?\\d+$") && value.matches("^-?\\d+$")) {
                    Assertions.assertThat(Integer.parseInt(envVar)).as("First value is not higher than or equal to second one.").isGreaterThanOrEqualTo(Integer.parseInt(value));
                } else {
                    throw new Exception("A number should be provided in order to perform a valid comparison.");

                }
                break;
            case "is lower than":
                if (envVar.matches("^-?\\d+$") && value.matches("^-?\\d+$")) {
                    Assertions.assertThat(Integer.parseInt(envVar)).as("First value is not lower than second one.").isLessThan(Integer.parseInt(value));
                } else {
                    throw new Exception("A number should be provided in order to perform a valid comparison.");
                }
                break;
            case "is lower than or equal to":
                if (envVar.matches("^-?\\d+$") && value.matches("^-?\\d+$")) {
                    Assertions.assertThat(Integer.parseInt(envVar)).as("First value is not lower than or equal to second one.").isLessThanOrEqualTo(Integer.parseInt(value));
                } else {
                    throw new Exception("A number should be provided in order to perform a valid comparison.");
                }
                break;
            case "contains":
                Assertions.assertThat(envVar).as("Second value is not contained in first one.").contains(value);
                break;
            case "does not contain":
                Assertions.assertThat(envVar).as("Second value is contained in first one.").doesNotContain(value);
                break;
            case "is different from":
                Assertions.assertThat(envVar).as("Both values are equal.").isNotEqualTo(value);
                break;
            default:
                throw new Exception("Not a valid comparison. Valid ones are: is | matches | is higher than | is higher than or equal to | is lower than | is lower than or equal to | contains | does not contain | is different from");
        }
    }

    @Given("^I set tenant variables$")
    public void setTenantVariables() throws Exception {
        String ccTenant = System.getProperty("CC_TENANT", "NONE");
        String dcosTenant = System.getProperty("DCOS_TENANT");
        if (dcosTenant == null) {
            throw new Exception("DCOS_TENANT is null");
        }
        String zkTenant = System.getProperty("ZK_TENANT") != null ? System.getProperty("ZK_TENANT") : System.getProperty("DCOS_TENANT");
        String xdTenant = System.getProperty("XD_TENANT") != null ? System.getProperty("XD_TENANT") : System.getProperty("DCOS_TENANT");
        String pgTenant = System.getProperty("PG_TENANT") != null ? System.getProperty("PG_TENANT") : System.getProperty("DCOS_TENANT");
        String elasticTenant = System.getProperty("ELASTIC_TENANT") != null ? System.getProperty("ELASTIC_TENANT") : System.getProperty("DCOS_TENANT");
        String kafkaTenant = System.getProperty("KAFKA_TENANT") != null ? System.getProperty("KAFKA_TENANT") : System.getProperty("DCOS_TENANT");
        String sparkTenant = System.getProperty("SPARK_TENANT") != null ? System.getProperty("SPARK_TENANT") : System.getProperty("DCOS_TENANT");
        String pgXLTenant = System.getProperty("PGD_TENANT") != null ? System.getProperty("PGD_TENANT") : System.getProperty("DCOS_TENANT");
        String schemaRegistryTenant = System.getProperty("SCHEMA_REGISTRY_TENANT") != null ? System.getProperty("SCHEMA_REGISTRY_TENANT") : System.getProperty("DCOS_TENANT");
        String restProxyTenant = System.getProperty("REST_PROXY_TENANT") != null ? System.getProperty("REST_PROXY_TENANT") : System.getProperty("DCOS_TENANT");
        String govTenant = System.getProperty("GOV_TENANT") != null ? System.getProperty("GOV_TENANT") : System.getProperty("DCOS_TENANT");
        String cassandraTenant = System.getProperty("CASSANDRA_TENANT") != null ? System.getProperty("CASSANDRA_TENANT") : System.getProperty("DCOS_TENANT");
        String igniteTenant = System.getProperty("IGNITE_TENANT") != null ? System.getProperty("IGNITE_TENANT") : System.getProperty("DCOS_TENANT");
        String etcdTenant = System.getProperty("ETCD_TENANT") != null ? System.getProperty("ETCD_TENANT") : System.getProperty("DCOS_TENANT");
        String k8sTenant = System.getProperty("K8S_TENANT") != null ? System.getProperty("K8S_TENANT") : System.getProperty("DCOS_TENANT");
        String arangoTenant = System.getProperty("ARANGO_TENANT") != null ? System.getProperty("ARANGO_TENANT") : System.getProperty("DCOS_TENANT");
        String kibanaTenant = System.getProperty("KIBANA_TENANT") != null ? System.getProperty("KIBANA_TENANT") : System.getProperty("DCOS_TENANT");
        String hdfsTenant = System.getProperty("HDFS_TENANT") != null ? System.getProperty("HDFS_TENANT") : System.getProperty("DCOS_TENANT");
        String spartaTenant = System.getProperty("SPARTA_TENANT") != null ? System.getProperty("SPARTA_TENANT") : System.getProperty("DCOS_TENANT");
        ThreadProperty.set("CC_TENANT", ccTenant);
        ThreadProperty.set("XD_TENANT", xdTenant);
        ThreadProperty.set("ZK_TENANT", zkTenant);
        ThreadProperty.set("PG_TENANT", pgTenant);
        ThreadProperty.set("ELASTIC_TENANT", elasticTenant);
        ThreadProperty.set("KAFKA_TENANT", kafkaTenant);
        ThreadProperty.set("SPARK_TENANT", sparkTenant);
        ThreadProperty.set("PGD_TENANT", pgXLTenant);
        ThreadProperty.set("SCHEMA_REGISTRY_TENANT", schemaRegistryTenant);
        ThreadProperty.set("REST_PROXY_TENANT", restProxyTenant);
        ThreadProperty.set("GOV_TENANT", govTenant);
        ThreadProperty.set("CASSANDRA_TENANT", cassandraTenant);
        ThreadProperty.set("IGNITE_TENANT", igniteTenant);
        ThreadProperty.set("ETCD_TENANT", etcdTenant);
        ThreadProperty.set("K8S_TENANT", k8sTenant);
        ThreadProperty.set("ARANGO_TENANT", arangoTenant);
        ThreadProperty.set("KIBANA_TENANT", kibanaTenant);
        ThreadProperty.set("HDFS_TENANT", hdfsTenant);
        ThreadProperty.set("SPARTA_TENANT", spartaTenant);
        commonspec.getLogger().debug("Tenant variables: XD --> {}, ZK --> {}, CC --> {}, PG --> {}, ELASTIC --> {}, KAFKA --> {}, SPARK --> {}, PGD --> {}, SCHEMA REG --> {}, REST PROXY --> {}," +
                        "GOVERNANCE --> {}, CASSANDRA --> {}, IGNITE --> {}, ETCD --> {}, K8S --> {}, ARANGO --> {}, KIBANA --> {}, HDFS --> {}, SPARTA --> {}",
                xdTenant, zkTenant, ccTenant, pgTenant, elasticTenant, kafkaTenant, sparkTenant, pgXLTenant, schemaRegistryTenant, restProxyTenant, govTenant, cassandraTenant, igniteTenant,
                etcdTenant, k8sTenant, arangoTenant, kibanaTenant, hdfsTenant, spartaTenant);
    }

    @Given("^I set gosec API variables$")
    public void setGosecVariables() throws Exception {
        String gosecVersion = ThreadProperty.get("gosec-management_version");
        if (gosecVersion == null) {
            commonspec.getLogger().warn("gosec-management_version has not been defined");
        } else {
            String[] gosecVersionArray = gosecVersion.split("\\.");
            if (gosecVersionArray.length != 3) {
                commonspec.getLogger().warn("gosec-management_version must have X.X.X format");
            } else {
                if (Integer.parseInt(gosecVersionArray[0]) >= 1 && (Integer.parseInt(gosecVersionArray[1]) > 1 || (Integer.parseInt(gosecVersionArray[1]) == 1 && Integer.parseInt(gosecVersionArray[2]) >= 1))) { //Gosec version >= 1.1.1
                    ThreadProperty.set("API_USER", "/api/user?id=");
                    ThreadProperty.set("API_GROUP", "/api/group?id=");
                    ThreadProperty.set("API_POLICY", "/api/policy?id=");
                    ThreadProperty.set("API_TAG", "/api/policy/tag?id=");
                    ThreadProperty.set("API_USERS", "/api/users");
                    ThreadProperty.set("API_GROUPS", "/api/groups");
                    ThreadProperty.set("API_POLICIES", "/api/policies");
                    ThreadProperty.set("API_TAGS", "/api/policies/tags");
                    ThreadProperty.set("API_SERVICE", "/api/service");
                } else {
                    ThreadProperty.set("API_USER", "/api/user/");
                    ThreadProperty.set("API_GROUP", "/api/group/");
                    ThreadProperty.set("API_POLICY", "/api/policy/");
                    ThreadProperty.set("API_TAG", "/api/policy/tag/");
                    ThreadProperty.set("API_USERS", "/api/user");
                    ThreadProperty.set("API_GROUPS", "/api/group");
                    ThreadProperty.set("API_POLICIES", "/api/policy");
                    ThreadProperty.set("API_TAGS", "/api/policy/tag");
                    ThreadProperty.set("API_SERVICE", "/api/service");
                }
            }
        }
    }

    /**
     * Convert provided date into timestamp
     *
     * @param date     : provided date (2020-08-08, 2020-08-08 10:10:10, etc)
     * @param format   : provided format (yyyy-MM-dd, yyyy-MM-dd HH:mm:ss, etc) - Format should match with provided date/time
     * @param timezone : Timezone GMT,GMT+1,GMT-1... (OPTIONAL) if not provided, UTC (GMT)
     * @param envVar   : environment variable where timestamp is saved
     * @throws Exception
     */
    @When("^I convert date '(.+?)' in format '(.+?)'( with timezone '(.+?)')? into timestamp and save the value in environment variable '(.+?)'$")
    public void convertDateToTimestamp(String date, String format, String timezone, String envVar) throws Exception {

        SimpleDateFormat isoFormat = new SimpleDateFormat(format);
        if (timezone != null) {
            isoFormat.setTimeZone(TimeZone.getTimeZone(timezone));
        } else {
            isoFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        }
        try {
            Date d = isoFormat.parse(date);
            GregorianCalendar calendar = new GregorianCalendar(TimeZone.getTimeZone("Europe/Madrid"));
            calendar.setTime(d);
            ThreadProperty.set(envVar, Long.toString(calendar.getTimeInMillis()));
            commonspec.getLogger().debug("Your date in timestamp: {} with timezone: {}", calendar.getTimeInMillis(), timezone);
        } catch (AssertionError e) {
            commonspec.getLogger().error("Error converting date {}, check date and format", date);
            throw e;
        }
    }

    /**
     * Get current date and time and save into environment variable
     *
     * @param envVar : environment variable where date time is saved
     */
    @Given("^I get current date and time and save the value in environment variable '(.+?)'$")
    public void getCurrentDateTime(String envVar) {
        DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        Date date = new Date(); //now
        dateFormat.setTimeZone(TimeZone.getTimeZone("Europe/Madrid"));
        ThreadProperty.set(envVar, dateFormat.format(date));
        commonspec.getLogger().debug("Current Date: {}", dateFormat.format(date));
    }

    /**
     * Get current date and time plus provided minutes and save into environment variable
     *
     * @param envVar        : environment variable where date time is saved
     * @param addMinuteTime : added minutes
     */
    @Given("^I get date and time with '(.+?)' more minutes from now and save the value in environment variable '(.+?)'$")
    public void getOneMoreMinute(int addMinuteTime, String envVar) {
        DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        Date targetTime = new Date(); //now
        targetTime = DateUtils.addMinutes(targetTime, addMinuteTime); //add minute
        dateFormat.setTimeZone(TimeZone.getTimeZone("Europe/Madrid"));
        ThreadProperty.set(envVar, dateFormat.format(targetTime));
        commonspec.getLogger().debug("Date adding {} minutes : {}", addMinuteTime, dateFormat.format(targetTime));
    }

    /**
     * Get current date and time plus provided seconds and save into environment variable
     *
     * @param envVar        : environment variable where date time is saved
     * @param addSecondsTime : added seconds
     */
    @Given("^I get date and time with '(.+?)' more seconds from now and save the value in environment variable '(.+?)'$")
    public void getOneMoreSecond(int addSecondsTime, String envVar) {
        DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        Date targetTime = new Date(); //now
        targetTime = DateUtils.addSeconds(targetTime, addSecondsTime); //add seconds
        dateFormat.setTimeZone(TimeZone.getTimeZone("Europe/Madrid"));
        ThreadProperty.set(envVar, dateFormat.format(targetTime));
        commonspec.getLogger().debug("Date adding {} seconds : {}", addSecondsTime, dateFormat.format(targetTime));
    }


    /**
     * Get JWT for the provided data
     *
     * @param tenant    : environment tenant
     * @param userId    : login userId
     * @param mail      : users email
     * @param groups    : List of groups
     * @param issuer    : Issuer to sign the token
     * @param secretKey : Vault secretKey to sign the token
     * @param envVar    : local variable where JWT will be saved
     */
    @Given("^I get JWT for tenant:'(.+?)',userId:'(.+?)',mail:'(.+?)'(,groups:'(.+?)')? with issuer:'(.+?)' and secretKey:'(.+?)' and save the value in environment variable '(.+?)'$")
    public void getJwtFromData(String tenant, String userId, String mail, String groups, String issuer, String secretKey, String envVar) {
        List<String> myList = groups == null ? new ArrayList<>() : new ArrayList<>(Arrays.asList(groups.split(",")));

        //Set JWT start and end dates
        Date startDate = new GregorianCalendar(2021, Calendar.JANUARY, 01).getTime();
        Date endDate = new GregorianCalendar(2099, Calendar.JANUARY, 01).getTime();

        //The JWT signature algorithm we will be using to sign the token
        SignatureAlgorithm signatureAlgorithm = SignatureAlgorithm.HS256;

        //secretKey should be given as Base64URL encoded
        Key signingKey = new SecretKeySpec(secretKey.getBytes(), signatureAlgorithm.getJcaName());

        try {
            JwtBuilder builder = Jwts.builder().setNotBefore(startDate)
                    .claim("cn", userId)
                    .claim("tenant", tenant)
                    .claim("groups", myList)
                    .claim("mail", mail)
                    .setExpiration(endDate)
                    .setIssuer(issuer)
                    .claim("uid", userId)
                    .signWith(signatureAlgorithm, signingKey);

            //Save token on env variable
            ThreadProperty.set(envVar, builder.compact());
            commonspec.getLogger().debug("Your JWT: {}", builder.compact());
        } catch (AssertionError e) {
            commonspec.getLogger().error("Error creating JWT, check data");
            throw e;
        }
    }

    /**
     * Get JWT for the provided data using privateKey
     *
     * @param tenant  : environment tenant
     * @param userId  : login userId
     * @param mail    : users email
     * @param groups  : List of groups
     * @param issuer  : Issuer to sign the token
     * @param privKey : Vault privateKey to sign the token
     * @param envVar  : local variable where JWT will be saved
     */
    @Given("^I get JWT for tenant:'(.+?)',userId:'(.+?)',mail:'(.+?)'(,groups:'(.+?)')? with issuer:'(.+?)' and privateKey:'(.+?)' and save the value in environment variable '(.+?)'$")
    public void getJwtUsingPrivateKey(String tenant, String userId, String mail, String groups, String issuer, String privKey, String envVar) throws NoSuchAlgorithmException, InvalidKeySpecException, IOException {
        List<String> myList = groups == null ? new ArrayList<>() : new ArrayList<>(Arrays.asList(groups.split(",")));

        //Set JWT start and end dates
        Date startDate = new GregorianCalendar(2021, Calendar.JANUARY, 01).getTime();
        Date endDate = new GregorianCalendar(2099, Calendar.JANUARY, 01).getTime();

        //The JWT signature algorithm we will be using to sign the token
        SignatureAlgorithm signatureAlgorithm = SignatureAlgorithm.RS256;

        //change text newline to new lines in provided String
        privKey = privKey.replaceAll("newline", "\n");

        // Read in the key into a String
        StringBuilder pkcs8Lines = new StringBuilder();
        BufferedReader rdr = new BufferedReader(new StringReader(privKey));
        String line;
        while ((line = rdr.readLine()) != null) {
            pkcs8Lines.append(line);
        }

        // Remove the "BEGIN" and "END" lines, as well as any whitespace
        String pkcs8Pem = pkcs8Lines.toString();
        pkcs8Pem = pkcs8Pem.replace("-----BEGIN PRIVATE KEY-----", "");
        pkcs8Pem = pkcs8Pem.replace("-----END PRIVATE KEY-----", "");
        pkcs8Pem = pkcs8Pem.replaceAll("\\s+", "");

        // Base64 decode the result
        byte[] pkcs8EncodedBytes = Base64.getDecoder().decode(pkcs8Pem);

        // extract the private key
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(pkcs8EncodedBytes);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        PrivateKey privateKey = kf.generatePrivate(keySpec);

        try {
            JwtBuilder builder = Jwts.builder().setNotBefore(startDate)
                    .claim("cn", userId)
                    .claim("tenant", tenant)
                    .claim("groups", myList)
                    .claim("mail", mail)
                    .setExpiration(endDate)
                    .setIssuer(issuer)
                    .claim("uid", userId)
                    .signWith(signatureAlgorithm, privateKey);

            //Save token on env variable
            ThreadProperty.set(envVar, builder.compact());
            commonspec.getLogger().debug("Your JWT: {}", builder.compact());
        } catch (AssertionError e) {
            commonspec.getLogger().error("Error creating JWT, check data");
            throw e;
        }
    }
}
