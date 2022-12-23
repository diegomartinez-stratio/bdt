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

import com.datastax.driver.core.ColumnDefinitions;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.spi.json.GsonJsonProvider;
import com.jayway.jsonpath.spi.mapper.GsonMappingProvider;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import io.netty.handler.codec.http.cookie.DefaultCookie;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.cookie.Cookie;
import com.stratio.qa.clients.cct.CctMarathonServiceApiClient;
import com.stratio.qa.clients.cct.ConfigurationApiClient;
import com.stratio.qa.clients.cct.DeployApiClient;
import com.stratio.qa.clients.k8s.KubernetesClient;
import com.stratio.qa.clients.marathon.MarathonApiClient;
import com.stratio.qa.clients.marathon.MarathonUtils;
import com.stratio.qa.clients.mesos.MesosApiClient;
import com.stratio.qa.clients.mesos.MesosUtils;
import com.stratio.qa.conditions.Conditions;
import com.stratio.qa.utils.*;
import io.cucumber.datatable.DataTable;
import cucumber.api.Scenario;
import org.apache.commons.collections.IteratorUtils;
import org.apache.commons.io.FileUtils;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.Condition;
import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.BoundRequestBuilder;
import org.asynchttpclient.Realm;
import org.asynchttpclient.Response;
import org.hjson.JsonObject;
import org.hjson.JsonType;
import org.hjson.JsonValue;
import org.json.JSONArray;
import org.json.JSONObject;
import org.ldaptive.SearchResult;
import org.openqa.selenium.*;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.interactions.internal.Locatable;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.stratio.qa.assertions.Assertions.assertThat;
import static org.testng.Assert.fail;

import java.text.ParseException;
import java.util.Date;

import org.everit.json.schema.loader.SchemaLoader;
import org.testng.Assert;

public class CommonG {

    private static final long DEFAULT_CURRENT_TIME = 1000L;

    private static final int DEFAULT_SLEEP_TIME = 1500;

    private final Logger logger = LoggerFactory.getLogger(ThreadProperty.get("class"));

    private RemoteWebDriver driver = null;

    private String browserName = null;

    private PreviousWebElements previousWebElements = null;

    private String parentWindow = "";

    private AsyncHttpClient client;

    private HttpResponse response;

    private ResultSet previousCassandraResults;

    private DBCursor previousMongoResults;

    private List<JSONObject> previousElasticsearchResults;

    private List<Map<String, String>> previousCSVResults;

    private String resultsType = "";

    private Set<org.openqa.selenium.Cookie> seleniumCookies = new HashSet<org.openqa.selenium.Cookie>();

    private Map<String, String> headers = new HashMap<>();

    private String restHost;

    private String restPort;

    private String webHost;

    private String webPort;

    private int commandExitStatus;

    private String commandResult;

    private String commandResultError;

    private String restProtocol;

    private Optional<SearchResult> previousLdapResults;

    private Map<String, List<String>> previousSqlResult = null;

    MarathonApiClient marathonClient;

    MarathonUtils marathonUtils;

    MesosApiClient mesosApiClient;

    MesosUtils mesosUtils;

    CctMarathonServiceApiClient cctMarathonServiceClient;

    ConfigurationApiClient configurationApiClient;

    DeployApiClient deployApiClient;

    KubernetesClient kubernetesClient;

    /**
     * Checks if a given string matches a regular expression or contains a string
     *
     * @param expectedMessage message used for comparing
     * @return boolean
     */
    public static Pattern matchesOrContains(String expectedMessage) {
        Pattern pattern;
        if (expectedMessage.startsWith("regex:")) {
            String regex = expectedMessage.substring(expectedMessage.indexOf("regex:") + 6);
            pattern = Pattern.compile(regex);
        } else {
            pattern = Pattern.compile(Pattern.quote(expectedMessage));
        }
        return pattern;
    }

    /**
     * Get the common remote connection.
     *
     * @return RemoteConnection
     */
    public RemoteSSHConnection getRemoteSSHConnection() {
        return RemoteSSHConnectionsUtil.getLastRemoteSSHConnection();
    }

    /**
     * Set the remote connection.
     */
    @Deprecated
    public void setRemoteSSHConnection(RemoteSSHConnection remoteSSHConnection) {
        setRemoteSSHConnection(remoteSSHConnection, "default");
    }

    /**
     * Set the remote connection.
     */
    public void setRemoteSSHConnection(RemoteSSHConnection remoteSSHConnection, String sshConnectionId) {
        RemoteSSHConnectionsUtil.getRemoteSSHConnectionsMap().put(sshConnectionId, remoteSSHConnection);
        RemoteSSHConnectionsUtil.setLastRemoteSSHConnectionId(sshConnectionId);
        RemoteSSHConnectionsUtil.setLastRemoteSSHConnection(remoteSSHConnection);
    }

    /**
     * Get the common REST host.
     *
     * @return String
     */
    public String getRestHost() {
        return this.restHost;
    }

    /**
     * Set the REST host.
     *
     * @param restHost api host
     */
    public void setRestHost(String restHost) {
        this.restHost = restHost;
    }

    /**
     * Get the common REST port.
     *
     * @return String
     */
    public String getRestPort() {
        return this.restPort;
    }

    /**
     * Set the REST port.
     *
     * @param restPort api port
     */
    public void setRestPort(String restPort) {
        this.restPort = restPort;
    }

    /**
     * Get the common WEB host.
     *
     * @return String
     */
    public String getWebHost() {
        return this.webHost;
    }

    /**
     * Set the WEB host.
     *
     * @param webHost host where app is running
     */
    public void setWebHost(String webHost) {
        this.webHost = webHost;
    }

    /**
     * Get the common WEB port.
     *
     * @return String
     */
    public String getWebPort() {
        return this.webPort;
    }

    /**
     * Set the WEB port.
     *
     * @param webPort port where app is running
     */
    public void setWebPort(String webPort) {
        this.webPort = webPort;
    }

    /**
     * Get the common logger.
     *
     * @return Logger
     */
    public Logger getLogger() {
        return this.logger;
    }

    /**
     * Get the exception list.
     *
     * @return List(Exception)
     */
    public List<Exception> getExceptions() {
        return ExceptionList.INSTANCE.getExceptions();
    }

    /**
     * Get the textFieldCondition list.
     *
     * @return List(Exception)
     */
    public Condition<WebElement> getTextFieldCondition() {
        return Conditions.INSTANCE.getTextFieldCondition();
    }

    /**
     * Get the cassandra utils.
     *
     * @return CassandraUtils
     */
    public CassandraUtils getCassandraClient() {
        return CassandraUtil.INSTANCE.getCassandraUtils();
    }

    /**
     * Get the elasticSearch utils.
     *
     * @return ElasticSearchUtils
     */
    public ElasticSearchUtils getElasticSearchClient() {
        return ElasticSearchUtil.INSTANCE.getElasticSearchUtils();
    }

    public OpenSearchUtils getOpenSearchClient() {
        return OpenSearchUtil.INSTANCE.getOpenSearchUtils();
    }

    /**
     * Get the MongoDB utils.
     *
     * @return MongoDBUtils
     */
    public MongoDBUtils getMongoDBClient() {
        return MongoDBUtil.INSTANCE.getMongoDBUtils();
    }

    /**
     * Get the remoteWebDriver.
     *
     * @return RemoteWebDriver
     */
    public RemoteWebDriver getDriver() {
        return driver;
    }

    /**
     * Set the remoteDriver.
     *
     * @param driver driver to be used for testing
     */
    public void setDriver(RemoteWebDriver driver) {
        this.driver = driver;
    }

    /**
     * Get the browser name.
     *
     * @return String
     */
    public String getBrowserName() {
        return browserName;
    }

    /**
     * Set the browser name.
     *
     * @param browserName browser to be used for testing
     */
    public void setBrowserName(String browserName) {
        this.browserName = browserName;
    }

    /**
     * Looks for webelements inside a selenium context. This search will be made
     * by id, name and xpath expression matching an {@code locator} value
     *
     * @param method        class of element to be searched
     * @param element       webElement searched in selenium context
     * @param expectedCount integer. Expected number of elements.
     * @param scenario      Cucumber Scenario
     * @return List(WebElement)
     * @throws IllegalAccessException   exception
     * @throws IllegalArgumentException exception
     * @throws SecurityException        exception
     * @throws NoSuchFieldException     exception
     * @throws ClassNotFoundException   exception
     */
    public List<WebElement> locateElement(String method, String element,
                                          Integer expectedCount, Scenario scenario) throws ClassNotFoundException, NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {

        List<WebElement> wel = null;

        if ("id".equals(method)) {
            logger.debug("Locating {} by id", element);
            wel = this.getDriver().findElements(By.id(element));
        } else if ("name".equals(method)) {
            logger.debug("Locating {} by name", element);
            wel = this.getDriver().findElements(By.name(element));
        } else if ("class".equals(method)) {
            logger.debug("Locating {} by class", element);
            wel = this.getDriver().findElements(By.className(element));
        } else if ("xpath".equals(method)) {
            logger.debug("Locating {} by xpath", element);
            wel = this.getDriver().findElements(By.xpath(element));
        } else if ("css".equals(method)) {
            wel = this.getDriver().findElements(By.cssSelector(element));
        } else {
            fail("Unknown search method: " + method);
        }

        if (expectedCount != -1) {
            PreviousWebElements pwel = new PreviousWebElements(wel);
            assertThat(this, scenario, pwel).as("Element count doesnt match").hasSize(expectedCount);
        }

        return wel;
    }

    /**
     * Capture a snapshot or an evidence in the driver
     *
     * @param driver driver used for testing
     * @param type   type
     * @return String
     */
    public String captureEvidence(WebDriver driver, String type, Scenario scenario) {
        return captureEvidence(driver, type, "", scenario);
    }

    /**
     * Capture a snapshot or an evidence in the driver
     *
     * @param driver driver used for testing
     * @param type   type
     * @param suffix suffix
     * @return String
     */
    public String captureEvidence(WebDriver driver, String type, String suffix, Scenario scenario) {
        String testSuffix = System.getProperty("TESTSUFFIX");
        String dir = "./target/executions/";
        if (testSuffix != null) {
            dir = dir + testSuffix + "/";
        }

        String clazz = ThreadProperty.get("class");
        String currentBrowser = ThreadProperty.get("browser");
        String currentData = ThreadProperty.get("dataSet");

        if (!currentData.equals("")) {
            currentData = currentData
                    .replaceAll("[\\\\|\\/|\\|\\s|:|\\*]", "_");
        }

        if (!"".equals(currentData)) {
            currentData = "-" + HashUtils.doHash(currentData);
        }

        Timestamp ts = new Timestamp(new java.util.Date().getTime());
        String outputFile = dir + clazz + "/"
                + ThreadProperty.get("feature") + "." + ThreadProperty.get("scenario") + "/" + currentBrowser +
                currentData + ts.toString() + suffix;

        outputFile = outputFile.replaceAll(" ", "_");

        if (type.endsWith("htmlSource")) {
            if (type.equals("framehtmlSource")) {
                boolean isFrame = (Boolean) ((JavascriptExecutor) driver)
                        .executeScript("return window.top != window.self");

                if (isFrame) {
                    outputFile = outputFile + "frame.html";
                } else {
                    outputFile = "";
                }
            } else if (type.equals("htmlSource")) {
                driver.switchTo().defaultContent();
                outputFile = outputFile + ".html";
            }

            if (!outputFile.equals("")) {
                String source = driver.getPageSource();

                File fout = new File(outputFile);
                boolean dirs = fout.getParentFile().mkdirs();

                try (FileOutputStream fos = new FileOutputStream(fout, true)) {
                    Writer out = new OutputStreamWriter(fos, StandardCharsets.UTF_8);
                    PrintWriter writer = new PrintWriter(out, false);
                    writer.append(source);
                    writer.close();
                    out.close();
                } catch (IOException e) {
                    logger.error("Exception on evidence capture", e);
                }
            }

        } else if ("screenCapture".equals(type)) {
            outputFile = outputFile + ".png";
            File file = null;
            driver.switchTo().defaultContent();
            ((Locatable) driver.findElement(By.tagName("body")))
                    .getCoordinates().inViewPort();

            if (currentBrowser.startsWith("chrome")
                    || currentBrowser.startsWith("droidemu")
                    || (System.getProperty("SELENIUM_GRID") == null && currentBrowser.contains("chrome"))) {
                Actions actions = new Actions(driver);
                actions.keyDown(Keys.CONTROL).sendKeys(Keys.HOME).perform();
                actions.keyUp(Keys.CONTROL).perform();

                file = chromeFullScreenCapture(driver);
            } else {
                file = ((TakesScreenshot) driver)
                        .getScreenshotAs(OutputType.FILE);
            }
            try {
                FileUtils.copyFile(file, new File(outputFile));
                addPngFileToReport(file, scenario);
            } catch (IOException e) {
                logger.error("Exception on copying browser screen capture", e);
            }
        }

        return outputFile;

    }

    private File adjustLastCapture(Integer newTrailingImageHeight,
                                   List<File> capture) {
        // cuts last image just in case it dupes information
        Integer finalHeight = 0;
        Integer finalWidth = 0;
        File trailingImage = capture.get(capture.size() - 1);
        capture.remove(capture.size() - 1);

        BufferedImage oldTrailingImage;
        File temp = null;
        try {
            oldTrailingImage = ImageIO.read(trailingImage);
            BufferedImage newTrailingImage;
            if ((oldTrailingImage.getHeight() == newTrailingImageHeight)) {
                newTrailingImage = oldTrailingImage;
            } else {
                newTrailingImage = new BufferedImage(oldTrailingImage.getWidth(), oldTrailingImage.getHeight() - newTrailingImageHeight, BufferedImage.TYPE_INT_RGB);
            }

            newTrailingImage.createGraphics().drawImage(oldTrailingImage, 0,
                    0 - newTrailingImageHeight, null);

            File newTrailingImageF = File.createTempFile("tmpnewTrailingImage",
                    ".png");
            newTrailingImageF.deleteOnExit();

            ImageIO.write(newTrailingImage, "png", newTrailingImageF);

            capture.add(newTrailingImageF);

            finalWidth = ImageIO.read(capture.get(0)).getWidth();
            for (File cap : capture) {
                finalHeight += ImageIO.read(cap).getHeight();
            }

            BufferedImage img = new BufferedImage(finalWidth, finalHeight,
                    BufferedImage.TYPE_INT_RGB);

            Integer y = 0;
            BufferedImage tmpImg = null;
            for (File cap : capture) {
                tmpImg = ImageIO.read(cap);
                img.createGraphics().drawImage(tmpImg, 0, y, null);
                y += tmpImg.getHeight();
            }

            long ts = System.currentTimeMillis() / DEFAULT_CURRENT_TIME;

            temp = File.createTempFile("chromecap" + ts, ".png");
            temp.deleteOnExit();
            ImageIO.write(img, "png", temp);

        } catch (IOException e) {
            logger.error("Cant read image", e);
        }
        return temp;
    }

    private File chromeFullScreenCapture(WebDriver driver) {
        driver.switchTo().defaultContent();
        // scroll loop n times to get the whole page if browser is chrome
        ArrayList<File> capture = new ArrayList<File>();

        Boolean atBottom = false;
        Integer windowSize = ((Long) ((JavascriptExecutor) driver)
                .executeScript("return document.documentElement.scrollHeight"))
                .intValue();
        Integer accuScroll = 0;
        Integer newTrailingImageHeight = 0;
        try {
            while (!atBottom) {

                Thread.sleep(DEFAULT_SLEEP_TIME);
                capture.add(((TakesScreenshot) driver)
                        .getScreenshotAs(OutputType.FILE));

                ((JavascriptExecutor) driver).executeScript("if(window.screen)"
                        + " {window.scrollBy(0," + getDocumentHeight(driver) + ");};");

                accuScroll += getDocumentHeight(driver);

                if (windowSize <= accuScroll) {
                    atBottom = true;
                }
            }

        } catch (InterruptedException e) {
            logger.error("Interrupted waits among scrolls", e);
        }

        newTrailingImageHeight = accuScroll - windowSize;
        return adjustLastCapture(newTrailingImageHeight, capture);
    }

    private Integer getDocumentHeight(WebDriver driver) {
        return ((Long) ((JavascriptExecutor) driver)
                .executeScript("return document.documentElement.clientHeight"))
                .intValue();
    }

    /**
     * Add png file to cucumber report (it's embed on scenario)
     *
     * @param pngFile Screenshot file
     */
    private void addPngFileToReport(File pngFile, Scenario scenario) {
        if (scenario != null) {
            try {
                BufferedImage bImage = ImageIO.read(pngFile);
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                ImageIO.write(bImage, "png", bos);
                scenario.embed(bos.toByteArray(), "image/png");
            } catch (IOException e) {
                logger.error("Error adding screenshot in cucumber report", e);
            }
        }
    }

    /**
     * Returns the previous webElement
     *
     * @return List(WebElement)
     */
    public PreviousWebElements getPreviousWebElements() {
        return previousWebElements;
    }

    /**
     * Set the previous webElement
     */
    public void setPreviousWebElements(PreviousWebElements previousWebElements) {
        this.previousWebElements = previousWebElements;
    }

    /**
     * Returns the parentWindow
     *
     * @return String
     */
    public String getParentWindow() {
        return this.parentWindow;
    }

    /**
     * Sets the parentWindow
     */
    public void setParentWindow(String windowHandle) {
        this.parentWindow = windowHandle;

    }

    // COPIED FROM COMMON.JAVA
    public AsyncHttpClient getClient() {
        return client;
    }

    public void setClient(AsyncHttpClient client) {
        this.client = client;
    }

    public HttpResponse getResponse() {
        return response;
    }

    public void setResponse(String endpoint, Response response) throws IOException {
        Integer statusCode = response.getStatusCode();
        String httpResponse = response.getResponseBody();
        List<Cookie> cookies = response.getCookies();
        HttpHeaders headers = response.getHeaders();
        this.response = new HttpResponse(statusCode, httpResponse, cookies, headers);
    }

    /**
     * Returns the information contained in file passed as parameter
     *
     * @param baseData path to file to be read
     * @param type     type of information, it can be: json|string
     * @return String
     */
    public String retrieveData(String baseData, String type) {
        String result;

        InputStream stream = getClass().getClassLoader().getResourceAsStream(baseData);

        Writer writer = new StringWriter();
        char[] buffer = new char[1024];
        Reader reader;

        if (stream == null) {
            this.getLogger().error("File does not exist: {}", baseData);
            return "ERR! File not found: " + baseData;
        }

        try {
            reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8));
            int n;
            while ((n = reader.read(buffer)) != -1) {
                writer.write(buffer, 0, n);
            }
        } catch (Exception readerexception) {
            this.getLogger().error(readerexception.getMessage());
        } finally {
            try {
                stream.close();
            } catch (Exception closeException) {
                this.getLogger().error(closeException.getMessage());
            }
        }
        String text = writer.toString();

        if ("json".equals(type) || "scim".equals(type)) {
            String std = text.replace("\r", "").replace("\n", ""); // make sure we have unix style text regardless of the input
            result = JsonValue.readHjson(std).toString();
        } else {
            result = text;
        }
        return result;
    }


    /**
     * Returns the information modified
     *
     * @param data          string containing the information
     * @param type          type of information, it can be: json|string
     * @param modifications modifications to apply with a format:
     *                      WHERE,ACTION,VALUE
     *                      <p>
     *                      {@code
     *                      DELETE: Delete the key in json or string in current value
     *                      in case of DELETE action modifications is |key1|DELETE|N/A|
     *                      and with json {"key1":"value1","key2":{"key3":null}}
     *                      returns {"key2":{"key3":null}}
     *                      Example 2:
     *                      {"key1":"val1", "key2":"val2"} -> | key1 | DELETE | N/A | -> {"key2":"val2"}
     *                      "mystring" -> | str | DELETE | N/A | -> "mying"
     *                      }
     *                      <p>
     *                      {@code
     *                      ADD: Add new key to json or append string to current value.
     *                      in case of ADD action is  |N/A|ADD|&config=config|,
     *                      and with data  username=username&password=password
     *                      returns username=username&password=password&config=config
     *                      Example 2:
     *                      {"key1":"val1", "key2":"val2"} -> | key3 | ADD | val3 | -> {"key1":"val1", "key2":"val2", "key3":"val3"}
     *                      "mystring" -> | N/A | ADD | new | -> "mystringnew"
     *                      }
     *                      <p>
     *                      {@code
     *                      UPDATE: Update value in key or modify part of string.
     *                      in case of UPDATE action is |username=username|UPDATE|username=NEWusername|,
     *                      and with data username=username&password=password
     *                      returns username=NEWusername&password=password
     *                      Example 2:
     *                      {"key1":"val1", "key2":"val2"} -> | key1 | UPDATE | newval1 | -> {"key1":"newval1", "key2":"val2"}
     *                      "mystring" -> | str | UPDATE | mod | -> "mymoding"
     *                      }
     *                      <p>
     *                      {@code
     *                      PREPEND: Prepend value to key value or to string
     *                      in case of PREPEND action is |username=username|PREPEND|key1=value1&|,
     *                      and with data username=username&password=password
     *                      returns key1=value1&username=username&password=password
     *                      Example 2:
     *                      {"key1":"val1", "key2":"val2"} -> | key1 | PREPEND | new | -> {"key1":"newval1", "key2":"val2"}
     *                      "mystring" -> | N/A | PREPEND | new | -> "newmystring"
     *                      }
     *                      <p>
     *                      {@code
     *                      REPLACE: Update value in key or modify part of string.
     *                      in case of REPLACE action is |key2.key3|REPLACE|lu->REPLACE|
     *                      and with json {"key1":"value1","key2":{"key3":"value3"}}
     *                      returns {"key1":"value1","key2":{"key3":"vaREPLACEe3"}}
     *                      the  format is (WHERE,  ACTION,  CHANGE FROM -> TO).
     *                      REPLACE replaces a string or its part per other string
     *                      }
     *                      <p>
     *                      {@code
     *                      if modifications has fourth argument, the replacement is effected per special json object
     *                      the format is:
     *                      (WHERE,   ACTION,    CHANGE_TO, JSON_TYPE),
     *                      WHERE is the key, ACTION is REPLACE,
     *                      CHANGE_TO is the new value of the key,
     *                      JSON_TYPE is the type of jason object,
     *                      there are 5 special cases of json object replacements:
     *                      array|object|number|boolean|null
     *                      }
     *                      <p>
     *                      {@code
     *                      example1: |key2.key3|REPLACE|5|number|
     *                      with json {"key1":"value1","key2":{"key3":"value3"}}
     *                      returns {"key1":"value1","key2":{"key3":5}}
     *                      in this case it replaces value of key3
     *                      per jason number
     *                      }<p>
     *                      {@code
     *                      example2: |key2.key3|REPLACE|{}|object|
     *                      with json  {"key1":"value1","key2":{"key3":"value3"}}
     *                      returns  {"key1":"value1","key2":{"key3":{}}}
     *                      in this case it replaces per empty json object
     *                      }<p>
     *                      {@code
     *                      APPEND: Append value to key value or to string
     *                      {"key1":"val1", "key2":"val2"} -> | key1 | APPEND | new | -> {"key1":"val1new", "key2":"val2"}
     *                      "mystring" -> | N/A | APPEND | new | -> "mystringnew"
     *                      }
     * @return String
     * @throws Exception
     */
    public String modifyData(String data, String type, DataTable modifications) throws Exception {
        String modifiedData = data;
        String typeJsonObject = "";
        String nullValue = "";

        JSONArray jArray;
        JSONObject jObject;
        Double jNumber;
        Long jLong;
        Boolean jBoolean;
        boolean array = false;

        if ("json".equals(type) || "gov".equals(type) || "servicegov".equals(type) || "scim".equals(type)) {
            LinkedHashMap jsonAsMap = new LinkedHashMap();
            for (int i = 0; i < modifications.cells().size(); i++) {
                String composeKey = modifications.cells().get(i).get(0);
                String operation = modifications.cells().get(i).get(1);
                String newValue = modifications.cells().get(i).get(2);

                if (modifications.cells().get(0).size() == 4) {
                    typeJsonObject = modifications.cells().get(i).get(3);
                }

                if (modifiedData.startsWith("[") && modifiedData.endsWith("]")) {
                    modifiedData = "{\"content\":" + modifiedData + "}";
                    array = true;
                } else {
                    JsonObject object = new JsonObject(JsonValue.readHjson(modifiedData).asObject());
                    removeNulls(object);
                    modifiedData = JsonValue.readHjson(object.toString()).toString();
                }

                switch (operation.toUpperCase()) {
                    case "DELETE":
                        if (array) {
                            composeKey = "$.content" + composeKey.substring(1);
                        }
                        jsonAsMap = JsonPath.parse(modifiedData).delete(composeKey).json();
                        break;
                    case "ADD":
                        if (array) {
                            composeKey = "$.content" + composeKey.substring(1);
                        }
                        // Get the last key
                        String newKey;
                        String newComposeKey;
                        if (composeKey.contains(".")) {
                            newKey = composeKey.substring(composeKey.lastIndexOf('.') + 1);
                            newComposeKey = composeKey.substring(0, composeKey.lastIndexOf('.'));
                        } else {
                            newKey = composeKey;
                            newComposeKey = "$";
                        }

                        if ("array".equals(typeJsonObject)) {
                            jArray = new JSONArray();
                            if (!"[]".equals(newValue)) {
                                jArray = new JSONArray(newValue);
                            }
                            jsonAsMap = JsonPath.parse(modifiedData).put(newComposeKey, newKey, jArray).json();
                            break;
                        } else if ("object".equals(typeJsonObject)) {
                            jObject = new JSONObject();
                            if (!"{}".equals(newValue)) {
                                jObject = new JSONObject(newValue);
                            }
                            jsonAsMap = JsonPath.parse(modifiedData).put(newComposeKey, newKey, jObject).json();
                            break;
                        } else if ("string".equals(typeJsonObject)) {
                            jsonAsMap = JsonPath.parse(modifiedData).put(newComposeKey, newKey, newValue).json();
                            break;
                        } else if ("number".equals(typeJsonObject)) {
                            jNumber = new Double(newValue);
                            jsonAsMap = JsonPath.parse(modifiedData).put(newComposeKey, newKey, jNumber).json();
                            break;
                        } else if ("long".equals(typeJsonObject)) {
                            jLong = new Long(newValue);
                            jsonAsMap = JsonPath.parse(modifiedData).set(composeKey, jLong).json();
                            break;
                        } else if ("boolean".equals(typeJsonObject)) {
                            jBoolean = new Boolean(newValue);
                            jsonAsMap = JsonPath.parse(modifiedData).put(newComposeKey, newKey, jBoolean).json();
                            break;
                        } else if ("null".equals(typeJsonObject)) {
                            nullValue = JsonPath.parse(modifiedData).put(newComposeKey, newKey, null).jsonString();
                            break;
                        } else {
                            String replaceValue = JsonPath.parse(modifiedData).read(composeKey);
                            String toBeReplaced = newValue.split("->")[0];
                            String replacement = newValue.split("->")[1];
                            newValue = replaceValue.replace(toBeReplaced, replacement);
                            jsonAsMap = JsonPath.parse(modifiedData).put(newComposeKey, newKey, newValue).json();
                            break;
                        }
//                        jsonAsMap = JsonPath.parse(modifiedData).put(newComposeKey, newKey, newValue).json();
//                        break;
                    case "UPDATE":
                        if (array) {
                            composeKey = "$.content" + composeKey.substring(1);
                        }
                        jsonAsMap = JsonPath.parse(modifiedData).set(composeKey, newValue).json();
                        break;
                    case "APPEND":
                        if (array) {
                            composeKey = "$.content" + composeKey.substring(1);
                        }
                        String appendValue = JsonPath.parse(modifiedData).read(composeKey);
                        jsonAsMap = JsonPath.parse(modifiedData).set(composeKey, appendValue + newValue).json();
                        break;
                    case "PREPEND":
                        if (array) {
                            composeKey = "$.content" + composeKey.substring(1);
                        }
                        String prependValue = JsonPath.parse(modifiedData).read(composeKey);
                        jsonAsMap = JsonPath.parse(modifiedData).set(composeKey, newValue + prependValue).json();
                        break;
                    case "REPLACE":
                        if (array) {
                            composeKey = "$.content" + composeKey.substring(1);
                        }
                        if ("array".equals(typeJsonObject)) {
                            jArray = new JSONArray();
                            if (!"[]".equals(newValue)) {
                                jArray = new JSONArray(newValue);
                            }
                            jsonAsMap = JsonPath.parse(modifiedData).set(composeKey, jArray).json();
                            break;
                        } else if ("object".equals(typeJsonObject)) {
                            jObject = new JSONObject();
                            if (!"{}".equals(newValue)) {
                                jObject = new JSONObject(newValue);
                            }
                            jsonAsMap = JsonPath.parse(modifiedData).set(composeKey, jObject).json();
                            break;
                        } else if ("string".equals(typeJsonObject)) {
                            jsonAsMap = JsonPath.parse(modifiedData).set(composeKey, newValue).json();
                            break;
                        } else if ("number".equals(typeJsonObject)) {
                            jNumber = new Double(newValue);
                            jsonAsMap = JsonPath.parse(modifiedData).set(composeKey, jNumber).json();
                            break;
                        } else if ("long".equals(typeJsonObject)) {
                            jLong = new Long(newValue);
                            jsonAsMap = JsonPath.parse(modifiedData).set(composeKey, jLong).json();
                            break;
                        } else if ("boolean".equals(typeJsonObject)) {
                            jBoolean = new Boolean(newValue);
                            jsonAsMap = JsonPath.parse(modifiedData).set(composeKey, jBoolean).json();
                            break;
                        } else if ("null".equals(typeJsonObject)) {
                            nullValue = JsonPath.parse(modifiedData).set(composeKey, null).jsonString();
                            break;
                        } else {
                            String replaceValue = JsonPath.parse(modifiedData).read(composeKey);
                            String toBeReplaced = newValue.split("->")[0];
                            String replacement = newValue.split("->")[1];
                            newValue = replaceValue.replace(toBeReplaced, replacement);
                            jsonAsMap = JsonPath.parse(modifiedData).set(composeKey, newValue).json();
                            break;
                        }
                    case "ADDTO":
                        if (array) {
                            composeKey = "$.content" + composeKey.substring(1);
                        }
                        if ("array".equals(typeJsonObject)) {
                            jArray = new JSONArray();
                            if (!"[]".equals(newValue)) {
                                jArray = new JSONArray(newValue);
                            }
                            jsonAsMap = JsonPath.parse(modifiedData).add(composeKey, jArray).json();
                            break;
                        } else if ("object".equals(typeJsonObject)) {
                            jObject = new JSONObject();
                            if (!"{}".equals(newValue)) {
                                jObject = new JSONObject(newValue);
                            }
                            jsonAsMap = JsonPath.parse(modifiedData).add(composeKey, jObject).json();
                            break;
                        } else if ("string".equals(typeJsonObject)) {
                            jsonAsMap = JsonPath.parse(modifiedData).add(composeKey, newValue).json();
                            break;
                        } else if ("number".equals(typeJsonObject)) {
                            jNumber = new Double(newValue);
                            jsonAsMap = JsonPath.parse(modifiedData).add(composeKey, jNumber).json();
                            break;
                        } else if ("long".equals(typeJsonObject)) {
                            jLong = new Long(newValue);
                            jsonAsMap = JsonPath.parse(modifiedData).set(composeKey, jLong).json();
                            break;
                        } else if ("boolean".equals(typeJsonObject)) {
                            jBoolean = new Boolean(newValue);
                            jsonAsMap = JsonPath.parse(modifiedData).add(composeKey, jBoolean).json();
                            break;
                        } else if ("null".equals(typeJsonObject)) {
                            nullValue = JsonPath.parse(modifiedData).add(composeKey, null).jsonString();
                            break;
                        } else {
                            // TO-DO: understand  newValue.split("->")[0];  and  newValue.split("->")[1];
                            break;
                        }
                    case "HEADER":
                        if (array) {
                            composeKey = "$.content" + composeKey.substring(1);
                        }
                        this.headers.put(composeKey, newValue);
                        jsonAsMap = JsonPath.parse(modifiedData).json();
                        break;
                    default:
                        throw new Exception("Modification type does not exist: " + operation);
                }

                modifiedData = new JSONObject(jsonAsMap).toString();
                if (!"".equals(nullValue)) {
                    modifiedData = nullValue;
                }
                modifiedData = modifiedData.replaceAll("\"TO_BE_NULL\"", "null");
            }
        } else {
            for (int i = 0; i < modifications.cells().size(); i++) {
                String value = modifications.cells().get(i).get(0);
                String operation = modifications.cells().get(i).get(1);
                String newValue = modifications.cells().get(i).get(2);

                switch (operation.toUpperCase()) {
                    case "DELETE":
                        modifiedData = modifiedData.replace(value, "");
                        break;
                    case "ADD":
                    case "APPEND":
                        modifiedData = modifiedData + newValue;
                        break;
                    case "UPDATE":
                    case "REPLACE":
                        modifiedData = modifiedData.replace(value, newValue);
                        break;
                    case "PREPEND":
                        modifiedData = newValue + modifiedData;
                        break;
                    case "HEADER":
                        this.headers.put(value, newValue);
                        break;
                    default:
                        throw new Exception("Modification type does not exist: " + operation);
                }
            }
        }

        if (array) {
            modifiedData = modifiedData.substring(11, modifiedData.length() - 1);
        }

        return modifiedData;
    }

    /**
     * Eliminates null occurrences, replacing them with "TO_BE_NULL"
     *
     * @param object JsonObject containing json where to replace null ocurrences
     * @return JsonObject
     */
    public JsonObject removeNulls(JsonObject object) {
        for (int j = 0; j < object.names().size(); j++) {
            if (JsonType.OBJECT.equals(object.get(object.names().get(j)).getType())) {
                removeNulls(object.get(object.names().get(j)).asObject());
            } else {
                if (object.get(object.names().get(j)).isNull()) {
                    object.set(object.names().get(j), "TO_BE_NULL");
                }
            }
        }
        return object;
    }

    /**
     * Generates the request based on the type of request, the end point, the data and type passed
     *
     * @param requestType type of request to be sent
     * @param secure      type of protocol
     * @param user        user to be used in request
     * @param password    password to be used in request
     * @param endPoint    end point to sent the request to
     * @param data        to be sent for PUT/POST requests
     * @param type        type of data to be sent (json|string)
     * @param codeBase64  XXX
     * @throws Exception exception
     */
    @Deprecated
    public Future<Response> generateRequest(String requestType, boolean secure, String user, String password, String endPoint, String data, String type, String codeBase64) throws Exception {
        return generateRequest(requestType, secure, user, password, endPoint, data, type);
    }

    /**
     * Generates the request based on the type of request, the end point, the data and type passed
     *
     * @param requestType type of request to be sent
     * @param secure      type of protocol
     * @param user        user to be used in request
     * @param password    password to be used in request
     * @param endPoint    end point to sent the request to
     * @param data        to be sent for PUT/POST requests
     * @param type        type of data to be sent (json|string|gov|scim|servicegov)
     * @throws Exception exception
     */
    public Future<Response> generateRequest(String requestType, boolean secure, String user, String password, String endPoint, String data, String type) throws Exception {

        String protocol = this.getRestProtocol();
        Future<Response> response = null;
        BoundRequestBuilder request;
        Realm realm = null;
        String govTenant = System.getProperty("GOV_TENANT") != null ? System.getProperty("GOV_TENANT") : "NONE";
        String govRolesID = System.getProperty("GOV_ROLESID");
        String govUserID = System.getProperty("GOV_USERID") != null ? System.getProperty("GOV_USERID") : "admin";

        if ("servicegov".equals(type)) {
            govRolesID = System.getProperty("GOV_SERVICE_ROLESID") != null ? System.getProperty("GOV_SERVICE_ROLESID") : "Service";
            govUserID = System.getProperty("GOV_SERVICE_USERID") != null ? System.getProperty("GOV_SERVICE_USERID") : "admin";
            type = "gov";
        }

        if (this.getRestHost() == null) {
            throw new Exception("Rest host has not been set");
        }

        if (this.getRestPort() == null) {
            throw new Exception("Rest port has not been set");
        }

        if (this.getRestProtocol() == null) {
            protocol = "http://";
        }

        String restURL = protocol + this.getRestHost() + this.getRestPort();

        // Setup user and password for requests
        if (user != null) {
            realm = new Realm.Builder(user, password)
                    .setUsePreemptiveAuth(true)
                    .setScheme(Realm.AuthScheme.BASIC)
                    .build();
        }

        switch (requestType.toUpperCase()) {
            case "GET":
                request = this.getClient().prepareGet(restURL + endPoint);

                if ("json".equals(type)) {
                    request = request.setHeader("Content-Type", "application/json; charset=UTF-8");
                } else if ("string".equals(type)) {
                    this.getLogger().debug("Sending request as: {}", type);
                    request = request.setHeader("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
                } else if ("gov".equals(type)) {
                    request = request.setHeader("Content-Type", System.getProperty("GOV_HEADER_CONTENT_TYPE", "application/json; charset=UTF-8"));
                    request = request.setHeader("Accept", "application/json");
                    request = request.setHeader("X-TenantID", govTenant);
                    request = request.setHeader("X-RolesID", govRolesID);
                    request = request.setHeader("X-UserID", govUserID);
                } else if ("scim".equals(type)) {
                    request = request.setHeader("Content-Type", "application/scim+json");
                    request = request.setHeader("Accept", "application/scim+json");
                }

                if (this.getResponse() != null) {
                    this.getLogger().debug("Reusing coookies: {}", this.getResponse().getCookies());
                    request = request.setCookies(this.getResponse().getCookies());
                }

                for (Cookie cook : this.getCookies()) {
                    request = request.addCookie(cook);
                }

                if (this.getSeleniumCookies().size() > 0) {
                    for (org.openqa.selenium.Cookie cookie : this.getSeleniumCookies()) {
                        Cookie cookieToAdd = new DefaultCookie(cookie.getName(), cookie.getValue());
                        cookieToAdd.setDomain(cookie.getDomain());
                        cookieToAdd.setPath(cookie.getPath());
                        cookieToAdd.setWrap(false);
                        cookieToAdd.setMaxAge(99);
                        cookieToAdd.setSecure(false);
                        cookieToAdd.setHttpOnly(false);
                        request.addCookie(cookieToAdd);
                    }
                }

                if (!this.headers.isEmpty()) {
                    for (Map.Entry<String, String> header : headers.entrySet()) {
                        request = request.setHeader(header.getKey(), header.getValue());
                    }
                }

                if (user != null) {
                    request = request.setRealm(realm);
                }

                response = request.execute();
                break;

            case "DELETE":
                if (data == "") {
                    request = this.getClient().prepareDelete(restURL + endPoint);
                    if ("gov".equals(type)) {
                        request = request.setHeader("Content-Type", "application/json; charset=UTF-8");
                        request = request.setHeader("Accept", "application/json");
                        request = request.setHeader("X-TenantID", govTenant);
                        request = request.setHeader("X-RolesID", govRolesID);
                        request = request.setHeader("X-UserID", govUserID);
                    }
                } else {
                    request = this.getClient().prepareDelete(restURL + endPoint).setBody(data);
                    if ("json".equals(type)) {
                        request = request.setHeader("Content-Type", "application/json; charset=UTF-8");
                    } else if ("string".equals(type)) {
                        this.getLogger().debug("Sending request as: {}", type);
                        request = request.setHeader("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
                    } else if ("gov".equals(type)) {
                        request = request.setHeader("Content-Type", System.getProperty("GOV_HEADER_CONTENT_TYPE", "application/json; charset=UTF-8"));
                        request = request.setHeader("Accept", "application/json");
                        request = request.setHeader("X-TenantID", govTenant);
                        request = request.setHeader("X-RolesID", govRolesID);
                        request = request.setHeader("X-UserID", govUserID);
                    } else if ("scim".equals(type)) {
                        request = request.setHeader("Content-Type", "application/scim+json");
                        request = request.setHeader("Accept", "application/scim+json");
                    }
                }
                if (this.getSeleniumCookies().size() > 0) {
                    for (org.openqa.selenium.Cookie cookie : this.getSeleniumCookies()) {
                        Cookie cookieToAdd = new DefaultCookie(cookie.getName(), cookie.getValue());
                        cookieToAdd.setDomain(cookie.getDomain());
                        cookieToAdd.setPath(cookie.getPath());
                        cookieToAdd.setWrap(false);
                        cookieToAdd.setMaxAge(99);
                        cookieToAdd.setSecure(false);
                        cookieToAdd.setHttpOnly(false);
                        request.addCookie(cookieToAdd);
                    }
                }

                for (Cookie cook : this.getCookies()) {
                    request = request.addCookie(cook);
                }

                if (!this.headers.isEmpty()) {
                    for (Map.Entry<String, String> header : headers.entrySet()) {
                        request = request.setHeader(header.getKey(), header.getValue());
                    }
                }

                if (user != null) {
                    request = request.setRealm(realm);
                }

                if (data == "") {
                    response = request.execute();
                } else {
                    response = this.getClient().executeRequest(request.build());
                }
                break;
            case "POST":
                if (data == null) {
                    Exception missingFields = new Exception("Missing fields in request.");
                    throw missingFields;
                } else {
                    request = this.getClient().preparePost(restURL + endPoint).setBody(data);
                    if ("json".equals(type)) {
                        request = request.setHeader("Content-Type", "application/json; charset=UTF-8");
                    } else if ("string".equals(type)) {
                        this.getLogger().debug("Sending request as: {}", type);
                        request = request.setHeader("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
                    } else if ("gov".equals(type)) {
                        request = request.setHeader("Content-Type", System.getProperty("GOV_HEADER_CONTENT_TYPE", "application/json; charset=UTF-8"));
                        request = request.setHeader("Accept", "application/json");
                        request = request.setHeader("X-TenantID", govTenant);
                        request = request.setHeader("X-RolesID", govRolesID);
                        request = request.setHeader("X-UserID", govUserID);
                    } else if ("scim".equals(type)) {
                        request = request.setHeader("Content-Type", "application/scim+json");
                        request = request.setHeader("Accept", "application/scim+json");
                    }

                    if (this.getResponse() != null) {
                        request = request.setCookies(this.getResponse().getCookies());
                    }

                    if (this.getSeleniumCookies().size() > 0) {
                        for (org.openqa.selenium.Cookie cookie : this.getSeleniumCookies()) {
                            Cookie cookieToAdd = new DefaultCookie(cookie.getName(), cookie.getValue());
                            cookieToAdd.setDomain(cookie.getDomain());
                            cookieToAdd.setPath(cookie.getPath());
                            cookieToAdd.setWrap(false);
                            cookieToAdd.setMaxAge(99);
                            cookieToAdd.setSecure(false);
                            cookieToAdd.setHttpOnly(false);
                            request.addCookie(cookieToAdd);
                        }
                    }

                    for (Cookie cook : this.getCookies()) {
                        request = request.addCookie(cook);
                    }

                    if (!this.headers.isEmpty()) {
                        for (Map.Entry<String, String> header : headers.entrySet()) {
                            request = request.setHeader(header.getKey(), header.getValue());
                        }
                    }

                    if (user != null) {
                        request = request.setRealm(realm);
                    }

                    request.setRequestTimeout(180000);
                    response = this.getClient().executeRequest(request.build());
                    break;
                }
            case "PUT":
                if (data == null) {
                    Exception missingFields = new Exception("Missing fields in request.");
                    throw missingFields;
                } else {
                    request = this.getClient().preparePut(restURL + endPoint).setBody(data);
                    if ("json".equals(type)) {
                        request = request.setHeader("Content-Type", "application/json; charset=UTF-8");
                    } else if ("string".equals(type)) {
                        request = request.setHeader("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
                    } else if ("gov".equals(type)) {
                        request = request.setHeader("Content-Type", System.getProperty("GOV_HEADER_CONTENT_TYPE", "application/json; charset=UTF-8"));
                        request = request.setHeader("Accept", "application/json");
                        request = request.setHeader("X-TenantID", govTenant);
                        request = request.setHeader("X-RolesID", govRolesID);
                        request = request.setHeader("X-UserID", govUserID);
                    } else if ("scim".equals(type)) {
                        request = request.setHeader("Content-Type", "application/scim+json");
                        request = request.setHeader("Accept", "application/scim+json");
                    }

                    if (this.getResponse() != null) {
                        request = request.setCookies(this.getResponse().getCookies());
                    }

                    if (this.getSeleniumCookies().size() > 0) {
                        for (org.openqa.selenium.Cookie cookie : this.getSeleniumCookies()) {
                            Cookie cookieToAdd = new DefaultCookie(cookie.getName(), cookie.getValue());
                            cookieToAdd.setDomain(cookie.getDomain());
                            cookieToAdd.setPath(cookie.getPath());
                            cookieToAdd.setWrap(false);
                            cookieToAdd.setMaxAge(99);
                            cookieToAdd.setSecure(false);
                            cookieToAdd.setHttpOnly(false);
                            request.addCookie(cookieToAdd);
                        }
                    }

                    for (Cookie cook : this.getCookies()) {
                        request = request.addCookie(cook);
                    }

                    if (!this.headers.isEmpty()) {
                        for (Map.Entry<String, String> header : headers.entrySet()) {
                            request = request.setHeader(header.getKey(), header.getValue());
                        }
                    }

                    if (user != null) {
                        request = request.setRealm(realm);
                    }

                    response = this.getClient().executeRequest(request.build());
                    break;
                }
            case "PATCH":
                if (data == null) {
                    Exception missingFields = new Exception("Missing fields in request.");
                    throw missingFields;
                } else {
                    request = this.getClient().preparePatch(restURL + endPoint).setBody(data);
                    if ("json".equals(type)) {
                        request = request.setHeader("Content-Type", "application/json; charset=UTF-8");
                    } else if ("string".equals(type)) {
                        request = request.setHeader("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
                    } else if ("gov".equals(type)) {
                        request = request.setHeader("Content-Type", System.getProperty("GOV_HEADER_CONTENT_TYPE", "application/json; charset=UTF-8"));
                        request = request.setHeader("Accept", "application/json");
                        request = request.setHeader("X-TenantID", govTenant);
                        request = request.setHeader("X-RolesID", govRolesID);
                        request = request.setHeader("X-UserID", govUserID);
                    } else if ("scim".equals(type)) {
                        request = request.setHeader("Content-Type", "application/scim+json");
                        request = request.setHeader("Accept", "application/scim+json");
                    }

                    if (this.getResponse() != null) {
                        request = request.setCookies(this.getResponse().getCookies());
                    }

                    if (this.getSeleniumCookies().size() > 0) {
                        for (org.openqa.selenium.Cookie cookie : this.getSeleniumCookies()) {
                            Cookie cookieToAdd = new DefaultCookie(cookie.getName(), cookie.getValue());
                            cookieToAdd.setDomain(cookie.getDomain());
                            cookieToAdd.setPath(cookie.getPath());
                            cookieToAdd.setWrap(false);
                            cookieToAdd.setMaxAge(99);
                            cookieToAdd.setSecure(false);
                            cookieToAdd.setHttpOnly(false);
                            request.addCookie(cookieToAdd);
                        }
                    }

                    for (Cookie cook : this.getCookies()) {
                        request = request.addCookie(cook);
                    }

                    if (!this.headers.isEmpty()) {
                        for (Map.Entry<String, String> header : headers.entrySet()) {
                            request = request.setHeader(header.getKey(), header.getValue());
                        }
                    }

                    if (user != null) {
                        request = request.setRealm(realm);
                    }

                    response = this.getClient().executeRequest(request.build());
                    break;
                }
            case "CONNECT":
            case "HEAD":
            case "OPTIONS":
            case "REQUEST":
            case "TRACE":
                throw new Exception("Operation not implemented: " + requestType);
            default:
                throw new Exception("Operation not valid: " + requestType);
        }
        return response;
    }


    /**
     * Generates the request based on the type of request, the end point, the data and type passed
     *
     * @param requestType type of request to be sent
     * @param secure      type of protocol
     * @param endPoint    end point to sent the request to
     * @param data        to be sent for PUT/POST requests
     * @param type        type of data to be sent (json|string)
     * @throws Exception exception
     */
    @Deprecated
    public Future<Response> generateRequest(String requestType, boolean secure, String endPoint, String data, String
            type, String codeBase64) throws Exception {
        return generateRequest(requestType, false, null, null, endPoint, data, type, "");
    }


    /**
     * Saves the value in the attribute in class extending CommonG.
     *
     * @param element attribute in class where to store the value
     * @param value   value to be stored
     * @throws NoSuchFieldException      exception
     * @throws SecurityException         exception
     * @throws IllegalArgumentException  exception
     * @throws IllegalAccessException    exception
     * @throws InstantiationException    exception
     * @throws ClassNotFoundException    exception
     * @throws NoSuchMethodException     exception
     * @throws InvocationTargetException exception
     */

    public void setPreviousElement(String element, String value) throws
            NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException, InstantiationException, ClassNotFoundException, NoSuchMethodException, InvocationTargetException {
        Reflections reflections = new Reflections("com.stratio");
        Set classes = reflections.getSubTypesOf(CommonG.class);

        Object pp = (classes.toArray())[0];
        String qq = (pp.toString().split(" "))[1];
        Class<?> c = Class.forName(qq);

        Field ff = c.getDeclaredField(element);
        ff.setAccessible(true);
        ff.set(null, value);
    }

    public ResultSet getCassandraResults() {
        return previousCassandraResults;
    }

    public void setCassandraResults(ResultSet results) {
        this.previousCassandraResults = results;
    }

    public DBCursor getMongoResults() {
        return previousMongoResults;
    }

    public void setMongoResults(DBCursor results) {
        this.previousMongoResults = results;
    }

    public List<JSONObject> getElasticsearchResults() {
        return previousElasticsearchResults;
    }

    public void setElasticsearchResults(List<JSONObject> results) {
        this.previousElasticsearchResults = results;
    }

    public List<Map<String, String>> getCSVResults() {
        return previousCSVResults;
    }

    public void setCSVResults(List<Map<String, String>> results) {
        this.previousCSVResults = results;
    }

    public String getResultsType() {
        return resultsType;
    }

    public void setResultsType(String resultsType) {
        this.resultsType = resultsType;
    }

    public Set<org.openqa.selenium.Cookie> getSeleniumCookies() {
        return seleniumCookies;
    }

    public void setSeleniumCookies(Set<org.openqa.selenium.Cookie> cookies) {
        this.seleniumCookies = cookies;
    }

    public boolean cookieExists(String cookieName) {
        if (this.getSeleniumCookies() != null && this.getSeleniumCookies().size() != 0) {
            for (org.openqa.selenium.Cookie cookie : this.getSeleniumCookies()) {
                if (cookie.getName().contains(cookieName)) {
                    return true;
                }
            }
        }
        return false;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public void setHeaders(Map<String, String> headers) {
        this.headers = headers;
    }

    public void clearHeaders() {
        setHeaders(new HashMap<>());
    }

    /**
     * Checks the different results of a previous query to CSV file
     *
     * @param expectedResults A DataTable Object with all data needed for check the results. The DataTable must contains at least 2 columns:
     *                        a) A field column from the result
     *                        b) Occurrences column (Integer type)
     *                        <p>
     *                        Example:
     *                        |latitude| longitude|place     |occurrences|
     *                        |12.5    |12.7      |Valencia  |1           |
     *                        |2.5     | 2.6      |Stratio   |0           |
     *                        |12.5    |13.7      |Sevilla   |1           |
     *                        IMPORTANT: All columns must exist
     * @throws Exception exception
     */
    public void resultsMustBeCSV(DataTable expectedResults) throws Exception {
        if (getCSVResults() != null) {
            //Map for cucumber expected results
            List<Map<String, String>> resultsListExpected = new ArrayList<Map<String, String>>();
            Map<String, String> resultsCucumber;

            for (int e = 1; e < expectedResults.cells().size(); e++) {
                resultsCucumber = new HashMap<String, String>();

                for (int i = 0; i < expectedResults.cells().get(0).size(); i++) {
                    resultsCucumber.put(expectedResults.cells().get(0).get(i), expectedResults.cells().get(e).get(i));

                }
                resultsListExpected.add(resultsCucumber);
            }
            getLogger().debug("Expected Results: " + resultsListExpected.toString());

            getLogger().debug("Obtained Results: " + getCSVResults().toString());

            //First, we check that the number of rows are equals
            assertThat(resultsListExpected.size()).overridingErrorMessage("The number of rows of expected result is %s but the csv file contains %s", resultsListExpected.size(), getCSVResults().size()).isEqualTo(getCSVResults().size());
            //Then we check the CSV content
            for (int i = 0; i < resultsListExpected.size(); i++) {
                Map<String, String> expectedRow = resultsListExpected.get(i);
                Map<String, String> obtainedRow = getCSVResults().get(i);
                //First we check the number of columns
                assertThat(expectedRow.size()).overridingErrorMessage("The number columns of row %s has to be %s but was %s", i, expectedRow.size(), obtainedRow.size()).isEqualTo(obtainedRow.size());
                //Check the headers values
                assertThat(expectedRow.keySet()).overridingErrorMessage("The headers do not match").isEqualTo(obtainedRow.keySet());
                //Now, we are going to check the values
                Set<String> keys = expectedRow.keySet();
                for (String key : keys) {
                    if (expectedRow.get(key).contains("regex") || expectedRow.get(key).contains("not_check") || expectedRow.get(key).contains("not_empty")) {
                        if (expectedRow.get(key).contains("regex-timestamp")) {
                            String[] format = expectedRow.get(key).split("_");
                            assertThat(true).overridingErrorMessage("The values of key %s and %s line are not a valid timestamp", expectedRow.get(key), i).isEqualTo(isThisDateValid(obtainedRow.get(key), format[1]));
                        }
                        if (expectedRow.get(key).contains("regex-uuid")) {
                            assertThat(true).overridingErrorMessage("The values of key %s and %s line are not an UIDD", expectedRow.get(key), i).isEqualTo(isUUID(obtainedRow.get(key)));
                        }
                        if (expectedRow.get(key).contains("not_empty")) {
                            assertThat(false).overridingErrorMessage("The values of key %s and %s line are empty", expectedRow.get(key), i).isEqualTo(obtainedRow.get(key).isEmpty());
                        }
                    } else {
                        assertThat(expectedRow.get(key)).overridingErrorMessage("The values of key %s and %s line are not equals", expectedRow.get(key), i).isEqualTo(obtainedRow.get(key));
                    }
                }
            }
        } else {
            throw new Exception("You must execute a query before trying to get results");
        }
    }

    /**
     * Check if a string is a UUID
     *
     * @param uuid - UUID value
     * @return true if it is a UUID or false if it is not an UUID
     */
    private boolean isUUID(String uuid) {
        try {
            UUID.fromString(uuid);
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    /**
     * Check is a String is a valid timestamp format
     *
     * @param dateToValidate
     * @param dateFromat
     * @return true/false
     */
    private boolean isThisDateValid(String dateToValidate, String dateFromat) {
        if (dateToValidate == null) {
            return false;
        }
        SimpleDateFormat sdf = new SimpleDateFormat(dateFromat);
        sdf.setLenient(false);
        try {
            //if not valid, it will throw ParseException
            Date date = sdf.parse(dateToValidate);
        } catch (ParseException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    /**
     * Checks the different results of a previous query to Cassandra database
     *
     * @param expectedResults A DataTable Object with all data needed for check the results. The DataTable must contains at least 2 columns:
     *                        a) A field column from the result
     *                        b) Occurrences column (Integer type)
     *                        <p>
     *                        Example:
     *                        |latitude| longitude|place     |occurrences|
     *                        |12.5    |12.7      |Valencia  |1           |
     *                        |2.5     | 2.6      |Stratio   |0           |
     *                        |12.5    |13.7      |Sevilla   |1           |
     *                        IMPORTANT: All columns must exist
     * @throws Exception exception
     */
    public void resultsMustBeCassandra(DataTable expectedResults) throws Exception {
        if (getCassandraResults() != null) {
            //Map for query results
            ColumnDefinitions columns = getCassandraResults().getColumnDefinitions();
            List<Row> rows = getCassandraResults().all();

            List<Map<String, Object>> resultsListObtained = new ArrayList<Map<String, Object>>();
            Map<String, Object> results;

            for (int i = 0; i < rows.size(); i++) {
                results = new HashMap<String, Object>();
                for (int e = 0; e < columns.size(); e++) {
                    results.put(columns.getName(e), rows.get(i).getObject(e));

                }
                resultsListObtained.add(results);

            }
            getLogger().debug("Results: " + resultsListObtained.toString());
            //Map for cucumber expected results
            List<Map<String, Object>> resultsListExpected = new ArrayList<Map<String, Object>>();
            Map<String, Object> resultsCucumber;

            for (int e = 1; e < expectedResults.cells().size(); e++) {
                resultsCucumber = new HashMap<String, Object>();

                for (int i = 0; i < expectedResults.cells().get(0).size(); i++) {
                    resultsCucumber.put(expectedResults.cells().get(0).get(i), expectedResults.cells().get(e).get(i));

                }
                resultsListExpected.add(resultsCucumber);
            }
            getLogger().debug("Expected Results: " + resultsListExpected.toString());

            //Comparisons
            int occurrencesObtained = 0;
            int iterations = 0;
            int occurrencesExpected = 0;
            String nextKey;
            for (int e = 0; e < resultsListExpected.size(); e++) {
                iterations = 0;
                occurrencesObtained = 0;
                occurrencesExpected = Integer.parseInt(resultsListExpected.get(e).get("occurrences").toString());

                for (int i = 0; i < resultsListObtained.size(); i++) {

                    Iterator<String> it = resultsListExpected.get(0).keySet().iterator();

                    while (it.hasNext()) {
                        nextKey = it.next();
                        if (!nextKey.equals("occurrences")) {
                            if (resultsListObtained.get(i).get(nextKey).toString().equals(resultsListExpected.get(e).get(nextKey).toString())) {
                                iterations++;
                            }

                        }

                        if (iterations == resultsListExpected.get(0).keySet().size() - 1) {
                            occurrencesObtained++;
                            iterations = 0;
                        }
                    }

                    iterations = 0;
                }
                assertThat(occurrencesExpected).overridingErrorMessage("In row " + e + " have been found "
                        + occurrencesObtained + " results and " + occurrencesExpected + " were expected").isEqualTo(occurrencesObtained);

            }
        } else {
            throw new Exception("You must execute a query before trying to get results");
        }
    }


    /**
     * Checks the different results of a previous query to Mongo database
     *
     * @param expectedResults A DataTable Object with all data needed for check the results. The DataTable must contains at least 2 columns:
     *                        a) A field column from the result
     *                        b) Occurrences column (Integer type)
     *                        <p>
     *                        Example:
     *                        |latitude| longitude|place     |occurrences|
     *                        |12.5    |12.7      |Valencia  |1           |
     *                        |2.5     | 2.6      |Stratio   |0           |
     *                        |12.5    |13.7      |Sevilla   |1           |
     *                        IMPORTANT: All columns must exist
     * @throws Exception exception
     */
    public void resultsMustBeMongo(DataTable expectedResults) throws Exception {
        if (getMongoResults() != null) {
            //Map for cucumber expected results
            List<Map<String, Object>> resultsListExpected = new ArrayList<Map<String, Object>>();
            Map<String, Object> resultsCucumber;

            for (int e = 1; e < expectedResults.cells().size(); e++) {
                resultsCucumber = new HashMap<String, Object>();

                for (int i = 0; i < expectedResults.cells().get(0).size(); i++) {
                    resultsCucumber.put(expectedResults.cells().get(0).get(i), expectedResults.cells().get(e).get(i));

                }
                resultsListExpected.add(resultsCucumber);
            }
            getLogger().debug("Expected Results: " + resultsListExpected.toString());

            //Comparisons
            int occurrencesObtained = 0;
            int iterations = 0;
            int occurrencesExpected = 0;
            String nextKey;
            for (int e = 0; e < resultsListExpected.size(); e++) {
                iterations = 0;
                occurrencesObtained = 0;
                occurrencesExpected = Integer.parseInt(resultsListExpected.get(e).get("occurrences").toString());

                String resultsListObtained = "[";
                DBCursor cursor = getMongoResults();
                while (cursor.hasNext()) {

                    DBObject row = cursor.next();

                    resultsListObtained = resultsListObtained + row.toString();
                    if (cursor.hasNext()) {
                        resultsListObtained = ", " + resultsListObtained;
                    }

                    Iterator<String> it = resultsListExpected.get(0).keySet().iterator();

                    while (it.hasNext()) {
                        nextKey = it.next();
                        if (!nextKey.equals("occurrences")) {
                            if (row.get(nextKey).toString().equals(resultsListExpected.get(e).get(nextKey).toString())) {
                                iterations++;
                            }
                        }

                        if (iterations == resultsListExpected.get(0).keySet().size() - 1) {
                            occurrencesObtained++;
                            iterations = 0;
                        }
                    }
                    iterations = 0;
                    if (cursor.hasNext()) {
                        resultsListObtained = resultsListObtained + ",";
                    }
                }

                resultsListObtained = resultsListObtained + "]";
                getLogger().debug("Results: " + resultsListObtained);

                assertThat(occurrencesExpected).overridingErrorMessage("In row " + e + " have been found "
                        + occurrencesObtained + " results and " + occurrencesExpected + " were expected").isEqualTo(occurrencesObtained);
            }

        } else {
            throw new Exception("You must execute a query before trying to get results");
        }
    }

    /**
     * Checks the different results of a previous query to Elasticsearch database
     *
     * @param expectedResults A DataTable Object with all data needed for check the results. The DataTable must contains at least 2 columns:
     *                        a) A field column from the result
     *                        b) Occurrences column (Integer type)
     *                        <p>
     *                        Example:
     *                        |latitude| longitude|place     |occurrences|
     *                        |12.5    |12.7      |Valencia  |1           |
     *                        |2.5     | 2.6      |Stratio   |0           |
     *                        |12.5    |13.7      |Sevilla   |1           |
     *                        IMPORTANT: All columns must exist
     * @throws Exception exception
     */
    public void resultsMustBeElasticsearch(DataTable expectedResults) throws Exception {
        if (getElasticsearchResults() != null) {
            List<List<String>> expectedResultList = expectedResults.cells();
            //Check size
            assertThat(expectedResultList.size() - 1).overridingErrorMessage(
                            "Expected number of columns to be" + (expectedResultList.size() - 1)
                                    + "but was " + previousElasticsearchResults.size())
                    .isEqualTo(previousElasticsearchResults.size());
            List<String> columnNames = expectedResultList.get(0);
            for (int i = 0; i < previousElasticsearchResults.size(); i++) {
                for (int j = 0; j < columnNames.size(); j++) {
                    assertThat(expectedResultList.get(i + 1).get(j)).overridingErrorMessage("In row " + i + "and "
                            + "column " + j
                            + "have "
                            + "been "
                            + "found "
                            + expectedResultList.get(i + 1).get(j) + " results and " + previousElasticsearchResults.get(i).get(columnNames.get(j)).toString() + " were "
                            + "expected").isEqualTo(previousElasticsearchResults.get(i).get(columnNames.get(j)).toString());
                }
            }
        } else {
            throw new Exception("You must execute a query before trying to get results");
        }
    }

    /**
     * Runs a command locally
     *
     * @param command command used to be run locally
     */
    public void runLocalCommand(String command) throws Exception {
        runLocalCommand(command, -1);
    }

    /**
     * Runs a command locally
     *
     * @param command command used to be run locally
     * @param timeout max time in seconds that the command is allowed to run
     */
    public void runLocalCommand(String command, int timeout) throws Exception {
        String result = "";
        String line;
        Process p;
        boolean aborted = false;
        try {
            p = Runtime.getRuntime().exec(new String[]{"/bin/sh", "-c", command});
            if (timeout > 0) {
                if (!p.waitFor(timeout, TimeUnit.SECONDS)) {
                    this.commandExitStatus = 124;
                    this.commandResult = "The command took too long to finish";
                    aborted = true;
                }
            } else {
                p.waitFor();
            }
        } catch (java.io.IOException e) {
            this.commandExitStatus = 1;
            this.commandResult = "Error";
            return;
        }

        if (!aborted) {
            BufferedReader input = new BufferedReader(new InputStreamReader(p.getInputStream()));
            while ((line = input.readLine()) != null) {
                if (result.isEmpty()) {
                    result += line;
                } else {
                    result += "\n" + line;
                }
            }
            input.close();

            StringBuilder sbError = new StringBuilder();
            BufferedReader error = new BufferedReader(new InputStreamReader(p.getErrorStream()));
            while ((line = error.readLine()) != null) {
                sbError.append(line).append("\n");
            }
            error.close();

            this.commandResult = result;
            this.commandExitStatus = p.exitValue();
            this.commandResultError = sbError.toString();
        }

        p.destroy();

        if (p.isAlive()) {
            p.destroyForcibly();
        }

    }

    public int getCommandExitStatus() {
        return commandExitStatus;
    }

    public void setCommandExitStatus(int commandExitStatus) {
        this.commandExitStatus = commandExitStatus;
    }

    public String getCommandResult() {
        return commandResult;
    }

    public void setCommandResult(String commandResult) {
        this.commandResult = commandResult;
    }

    public String getCommandResultError() {
        return commandResultError;
    }

    public String getRestProtocol() {
        return restProtocol;
    }

    /**
     * Set the REST host.
     *
     * @param restProtocol api protocol "http or https"
     */
    public void setRestProtocol(String restProtocol) {
        this.restProtocol = restProtocol;
    }

    public List<Cookie> getCookies() {
        return CookiesUtils.getCookies();
    }

    public void setCookies(List<Cookie> cookies) {
        CookiesUtils.setCookies(cookies);
    }

    public void clearCookies() {
        CookiesUtils.setCookies(new ArrayList<>());
    }

    /**
     * Parse jsonpath expression from a given string.
     * <p>
     * If the string is json we can obtain its keys using ~ symbol.
     * <p>
     * If position is not null and the result of jsonpath expression is an array,
     * then this function will return the element at the given position at the array.
     * <p>
     * If position is null, it will return the result of the jsonpath evaluation as string.
     *
     * @param jsonString string to be parsed
     * @param expr       jsonpath expression
     * @param position   position from a search result
     */
    public String getJSONPathString(String jsonString, String expr, String position) {

        String value;

        if (expr.contains(".~")) {
            this.getLogger().debug("Expression referred to json keys");
            Pattern pattern = Pattern.compile("^(.*?).~(.*?)$");
            Matcher matcher = pattern.matcher(expr);
            String aux = null;
            String op = null;
            if (matcher.find()) {
                aux = matcher.group(1);
                op = matcher.group(2);
            }
            LinkedHashMap auxData = JsonPath.parse(jsonString).read(aux);
            JSONObject json = new JSONObject(auxData);
            List<String> keys = IteratorUtils.toList(json.keys());
            List<String> stringKeys = new ArrayList<String>();
            if (op.equals("")) {
                for (String key : keys) {
                    stringKeys.add("\"" + key + "\"");
                }
                value = stringKeys.toString();
            } else {
                Pattern patternOp = Pattern.compile("^\\[(-?\\d+)\\]$");
                Matcher matcherOp = patternOp.matcher(op);
                Integer index = null;
                Boolean isNegative = false;
                if (matcherOp.find()) {
                    if (matcherOp.group(1).contains("-")) {
                        isNegative = true;
                    }
                    index = Integer.parseInt(matcherOp.group(1).replace("-", ""));
                }
                if (isNegative) {
                    value = keys.get(keys.size() - index);
                } else {
                    value = keys.get(index);
                }

            }
        } else {
            String result = JsonValue.readHjson(jsonString).toString();
            Object data = JsonPath.parse(result).read(expr);
            if (position != null) {
                JSONArray jsonArray = new JSONArray(data.toString());
                value = jsonArray.get(Integer.parseInt(position)).toString();
            } else {
                if (data instanceof LinkedHashMap) {
                    value = (new JSONObject((LinkedHashMap) data)).toString();
                } else {
                    value = data.toString();
                }
            }
        }
        return value;
    }


    /**
     * Remove a subelement in a JsonPath
     *
     * @param jsonString String of the json
     * @param expr       regex to be removed
     */

    public String removeJSONPathElement(String jsonString, String expr) {

        Configuration conf = Configuration.builder().jsonProvider(new GsonJsonProvider()).mappingProvider(new GsonMappingProvider()).build();
        DocumentContext context = JsonPath.using(conf).parse(jsonString);
        context.delete(expr);
        return context.jsonString();
    }

    /**
     * The function searches over the array by certain field value,
     * and replaces occurences with the parameter provided.
     *
     * @param jsonString Original json object
     * @param key        Key to search
     * @param value      Value to replace key with
     */
    public String replaceJSONPathElement(String jsonString, String key, String value) {
        return JsonPath.parse(jsonString).set(key, value).jsonString();
    }

    /**
     * Evaluate an expression.
     * <p>
     * Object o could be a string or a list.
     *
     * @param o         object to be evaluated
     * @param condition condition to compare
     * @param result    expected result
     */
    public void evaluateJSONElementOperation(Object o, String condition, String result) throws Exception {

        if (o instanceof String) {
            String value = (String) o;
            switch (condition) {
                case "equal":
                    assertThat(value).as("Evaluate JSONPath does not match with proposed value").isEqualTo(result);
                    break;
                case "not equal":
                    assertThat(value).as("Evaluate JSONPath match with proposed value").isNotEqualTo(result);
                    break;
                case "contains":
                    assertThat(value).as("Evaluate JSONPath does not contain proposed value").contains(result);
                    break;
                case "does not contain":
                    assertThat(value).as("Evaluate JSONPath contain proposed value").doesNotContain(result);
                    break;
                case "size":
                    JsonValue jsonObject = JsonValue.readHjson(value);
                    if (jsonObject.isArray()) {
                        assertThat(jsonObject.asArray()).as("Keys size does not match").hasSize(Integer.parseInt(result));
                    } else {
                        Assertions.fail("Expected array for size operation check");
                    }
                    break;
                default:
                    Assertions.fail("Not implemented condition");
                    break;
            }
        } else if (o instanceof List) {
            List<String> keys = (List<String>) o;
            switch (condition) {
                case "contains":
                    assertThat(keys).as("Keys does not contain that name").contains(result);
                    break;
                case "size":
                    assertThat(keys).as("Keys size does not match").hasSize(Integer.parseInt(result));
                    break;
                default:
                    Assertions.fail("Operation not implemented for JSON keys");
            }
        }

    }

    public void runCommandAndGetResult(String command) throws Exception {
        getRemoteSSHConnection().runCommand(command);
        setCommandResult(getRemoteSSHConnection().getResult());
    }

    public String updateMarathonJson(String json) {
        if (json.contains("uris")) {
            return removeJSONPathElement(removeJSONPathElement(removeJSONPathElement(json, "$.versionInfo"), "$.version"), "$.uris.*");
        } else {
            return removeJSONPathElement(removeJSONPathElement(json, "$.versionInfo"), "$.version");
        }
    }

    public void runCommandLoggerAndEnvVar(int exitStatus, String envVar, Boolean local) {
        List<String> logOutput = Arrays.asList(this.getCommandResult().split("\n"));
        StringBuffer log = new StringBuffer();
        int logLastLines;
        try {
            logLastLines = Integer.parseInt(System.getProperty("BDT_LOCAL_PROCESS_LOG_LINES", "25"));
        } catch (NumberFormatException e) {
            logger.warn("Unable to cast value: " + System.getProperty("BDT_LOCAL_PROCESS_LOG_LINES", "25"));
            logLastLines = 25;
        }

        if (logOutput.size() < logLastLines) {
            logLastLines = logOutput.size();
        }
        for (String s : logOutput.subList(logOutput.size() - logLastLines, logOutput.size())) {
            log.append(s).append("\n");
        }

        if (envVar != null) {
            if (this.getRemoteSSHConnection() != null && !local) {
                ThreadProperty.set(envVar, this.getRemoteSSHConnection().getResult().trim());
            } else {
                ThreadProperty.set(envVar, this.getCommandResult().trim());
            }
        }
        if (this.getCommandExitStatus() != exitStatus) {
            if (System.getProperty("logLevel", "") != null && System.getProperty("logLevel", "").equalsIgnoreCase("debug")) {
                if (!("".equals(this.getCommandResult()))) {
                    this.getLogger().debug("Command complete stdout:\n{}", this.getCommandResult());
                }
            } else {
                this.getLogger().error("Command last {} lines stdout:", logLastLines);
                this.getLogger().error("{}", log);
            }
            if (commandResultError != null && !commandResultError.isEmpty()) {
                this.getLogger().error("Command complete stderr:\n{}", commandResultError);
            }
        } else {
            if (!("".equals(this.getCommandResult()))) {
                this.getLogger().debug("Command complete stdout:\n{}", this.getCommandResult());
            }
        }
    }

    /**
     * Get the LDAP utils.
     *
     * @return LdapUtils
     */
    public LdapUtils getLdapUtils() {
        return LdapUtil.INSTANCE.getldapUtils();
    }

    /**
     * *
     *
     * @return the previously searched for LDAP result
     */
    public Optional<SearchResult> getPreviousLdapResults() {
        return this.previousLdapResults;
    }

    /**
     * Store a LDAP search result
     *
     * @param result the LDAP SearchResult obtained from an LDAP query
     */
    public void setPreviousLdapResults(SearchResult result) {
        this.previousLdapResults = Optional.of(result);
    }

    /**
     * Method to convert one json to yaml file - backup&restore functionality
     * <p>
     * File will be placed on path /target/test-classes
     */
    public String asYaml(String jsonStringFile) throws IOException {

        InputStream stream = getClass().getClassLoader().getResourceAsStream(jsonStringFile);

        Writer writer = new StringWriter();
        char[] buffer = new char[1024];
        Reader reader;

        if (stream == null) {
            this.getLogger().error("File does not exist: {}", jsonStringFile);
            throw new FileNotFoundException("ERR! File not found: " + jsonStringFile);
        }

        try {
            reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8));
            int n;
            while ((n = reader.read(buffer)) != -1) {
                writer.write(buffer, 0, n);
            }
        } catch (Exception readerexception) {
            this.getLogger().error(readerexception.getMessage());
        } finally {
            try {
                stream.close();
            } catch (Exception closeException) {
                this.getLogger().error(closeException.getMessage());
            }
        }
        String text = writer.toString();

        String std = text.replace("\r", "").replace("\n", ""); // make sure we have unix style text regardless of the input

        // parse JSON
        JsonNode jsonNodeTree = new ObjectMapper().readTree(std);
        // save it as YAML
        String jsonAsYaml = new YAMLMapper().writeValueAsString(jsonNodeTree);
        return jsonAsYaml;
    }

    /**
     * Method to convert one yaml to json file - for operator test
     * <p>
     * File will be placed on path /target/test-classes
     */
    public String convertYamlToJson(String yaml) {
        return convertYamlStringToJson(retrieveData(yaml, "yaml"));
    }

    /**
     * Method to convert one yaml to json file - for operator test
     */
    public String convertYamlStringToJson(String yaml) {
        try {
            ObjectMapper yamlReader = new ObjectMapper(new YAMLFactory());
            Object obj = yamlReader.readValue(yaml, Object.class);
            ObjectMapper jsonWriter = new ObjectMapper();
            return jsonWriter.writerWithDefaultPrettyPrinter().writeValueAsString(obj);
        } catch (IOException ex) {
            logger.error("Yaml file can't be converted to Json file", ex);
        }
        return null;
    }

    /**
     * Connect to JDBC secured/not secured database
     *
     * @param database database connection string
     * @param host     database host
     * @param port     database port
     * @param user     database user
     * @param password database password
     * @param ca       trusted certificate authorities (.crt)
     * @param crt:     server certificate
     * @param key:     server private key
     * @throws Exception exception     *
     */
    public void connectToPostgreSQLDatabase(String encryption, String trans, String database, String host, String port, String user, String
            password, Boolean secure, String ca, String crt, String key) throws SQLException {
        Properties props = new Properties();
        if (port.startsWith("[")) {
            port = port.substring(1, port.length() - 1);
        }
        if (user != null) {
            props.setProperty("user", user);
        }
        if (!secure) {
            if (password != null) {
                props.setProperty("password", password);
            } else {
                props.setProperty("password", "stratio");
            }
            if (encryption != null) {
                props.setProperty("ssl", "true");
                props.setProperty("sslmode", "prefer");
            }
            if (trans != null) {
                props.setProperty("prepareThreshold", "0");
            }
        } else {
            if (user != null) {
                props.setProperty("user", user);
            }
            if (ca != null) {
                props.setProperty("sslrootcert", ca);
            }
            if (crt != null) {
                props.setProperty("sslcert", crt);
            }
            if (key != null) {
                props.setProperty("sslkey", key);
            }
            props.setProperty("password", "null");
            props.setProperty("ssl", "true");
            props.setProperty("sslmode", "verify-full");
            if (trans != null) {
                props.setProperty("prepareThreshold", "0");
            }
        }
        try {
            JDBCConnection.setConnection(DriverManager.getConnection("jdbc:postgresql://" + host + ":" + port + "/" + database, props));
        } catch (SQLException se) {
            // log the exception
            this.getLogger().error(se.getMessage());
            // re-throw the exception
            throw se;
        }
    }

    /*
     * @return connection object
     *
     */
    public Connection getConnection() {
        return JDBCConnection.getConnection();
    }


    /**
     * Generate deployment json from schema
     *
     * @param schema schema obtained from deploy-api
     * @return JSONObject   deployment json
     */
    public JSONObject parseJSONSchema(JSONObject schema) throws Exception {
        JSONObject json = new JSONObject();
        String name = "";
        JSONObject jsonSchema = schema;

        // Check if key 'parameters' exists
        if (jsonSchema.has("parameters")) {
            jsonSchema = jsonSchema.getJSONObject("parameters");
        }

        // Check if key 'properties' exists
        if (jsonSchema.has("properties")) {
            // Obtain properties
            jsonSchema = jsonSchema.getJSONObject("properties");
        }

        // Obtain all keys and iterate through them
        Iterator<?> keys = jsonSchema.keys();
        while (keys.hasNext()) {
            // Obtain key
            String key = keys.next().toString();
            // Obtain value of key
            JSONObject element = jsonSchema.getJSONObject(key);
            // Check if value contain properties
            // If it DOESN'T CONTAIN properties
            if (!element.has("properties")) {
                // Check if it has default value
                if (element.has("default")) {
                    // Add element with the default value assigned
                    json.put(key, element.get("default"));
                    // If it doesn't have default value, we assign a default value depending on the type
                } else {
                    switch (element.getString("type")) {
                        case "string":
                            json.put(key, "");
                            break;
                        case "boolean":
                            json.put(key, false);
                            break;
                        case "number":
                        case "integer":
                            json.put(key, 0);
                            break;
                        case "array":
                            json.put(key, new ArrayList<>());
                            break;
                        case "RollingUpdate":
                            json.put(key, "");
                            break;
                        default:
                            Assertions.fail("type not expected");
                    }
                }

                // If it CONTAINS properties
            } else {
                // Check if it has default value
                if (element.has("default")) {
                    // Add element with the default value assigned
                    json.put(key, element.get("default"));
                } else {
                    // Recursive call, keep evaluating json
                    json.put(key, parseJSONSchema(element));
                }
            }
        }

        return json;
    }

    /**
     * Check json matches schema
     *
     * @param schema schema obtained from deploy-api
     * @param json   json to be checked
     * @return boolean whether the json matches the schema or not
     */
    public boolean matchJsonToSchema(JSONObject schema, JSONObject json) throws Exception {
        SchemaLoader.builder()
                .useDefaults(true)
                .schemaJson(schema)
                .build()
                .load()
                .build()
                .validate(json);
        return true;
    }

    /**
     * Get service status
     *
     * @param service name of the service to be checked
     * @param cluster URI of the cluster
     * @return String   normalized service status
     * @throws Exception exception     *
     */
    public String retrieveServiceStatus(String service, String cluster) throws Exception {
        String status = "";
        String endPoint = "/service/deploy-api/deploy/status/service?service=" + service;
        String element = "$.status";
        Future response;

        this.setRestProtocol("https://");
        this.setRestHost(cluster);
        this.setRestPort(":443");

        response = this.generateRequest("GET", true, null, null, endPoint, null, "json");
        this.setResponse("GET", (Response) response.get());
        assertThat(this.getResponse().getStatusCode()).as("It hasn't been possible to obtain status for service: " + service).isEqualTo(200);

        String json = this.getResponse().getResponse();

        String value = this.getJSONPathString(json, element, null);

        switch (value) {
            case "0":
                status = "deploying";
                break;
            case "1":
                status = "suspended";
                break;
            case "2":
                status = "running";
                break;
            case "3":
                status = "delayed";
                break;
            default:
                throw new Exception("Unknown service status code");
        }

        return status;
    }

    /**
     * Get service health status
     *
     * @param service name of the service to be checked
     * @param cluster URI of the cluster
     * @return String   normalized service health status
     * @throws Exception exception     *
     */
    public String retrieveHealthServiceStatus(String service, String cluster) throws Exception {
        String health = "";
        String endPoint = "/service/deploy-api/deploy/status/service?service=" + service;
        String element = "$.healthy";
        Future response;

        this.setRestProtocol("https://");
        this.setRestHost(cluster);
        this.setRestPort(":443");

        response = this.generateRequest("GET", true, null, null, endPoint, null, "json");
        this.setResponse("GET", (Response) response.get());
        assertThat(this.getResponse().getStatusCode()).as("It hasn't been possible to obtain health status for service: " + service).isEqualTo(200);

        String json = this.getResponse().getResponse();

        String value = this.getJSONPathString(json, element, null);

        switch (value) {
            case "0":
                health = "unhealthy";
                break;
            case "1":
                health = "healthy";
                break;
            case "2":
                health = "unknown";
                break;
            default:
                throw new Exception("Unknown service health status code");
        }

        return health;
    }

    /**
     * Executes the command specified in remote system
     *
     * @param command         command to be run locally
     * @param sshConnectionId ssh connection id
     * @param exitStatus      command exit status
     * @param envVar          environment variable name
     * @param timeout         max time in seconds that the command is allowed to run
     * @throws Exception exception
     **/
    public void executeCommand(String command, String sshConnectionId, Integer exitStatus, String envVar, Integer timeout) throws Exception {
        if (exitStatus == null) {
            exitStatus = 0;
        }
        timeout = (timeout == null) ? -1 : timeout;
        RemoteSSHConnection remoteSSHConnection = sshConnectionId != null ? RemoteSSHConnectionsUtil.getRemoteSSHConnectionsMap().get(sshConnectionId) : getRemoteSSHConnection();
        command = "set -o pipefail && alias grep='grep --color=never' && " + command;
        remoteSSHConnection.runCommand(command, timeout);
        setCommandResult(remoteSSHConnection.getResult());
        setCommandExitStatus(remoteSSHConnection.getExitStatus());
        runCommandLoggerAndEnvVar(exitStatus, envVar, Boolean.FALSE);

        Assertions.assertThat(remoteSSHConnection.getExitStatus()).isEqualTo(exitStatus);
    }

    @Deprecated
    public void executeCommand(String command, Integer exitStatus, String envVar) throws Exception {
        executeCommand(command, null, exitStatus, envVar, -1);
    }

    @Deprecated
    public void executeCommand(String command, String sshConnectionId, Integer exitStatus, String envVar) throws Exception {
        executeCommand(command, sshConnectionId, exitStatus, envVar, -1);
    }

    public void connectToCrossdataDatabase(boolean security, String host, String port, String keystore_path, String keystore_pwd, String truststore_path, String truststore_pwd, String user, String password, boolean pagination) throws Exception {
        String jdbcConnection = "jdbc:crossdata://Server=" + host + ":" + port + ";UID=" + user + ";PAGINATION=" + pagination;

        if (security) {
            Assert.assertNotNull(keystore_path, "Keystore path is mandatory when security is enabled");
            Assert.assertNotNull(keystore_pwd, "Keystore password is mandatory when security is enabled");
            Assert.assertNotNull(truststore_path, "Truststore path is mandatory when security is enabled");
            Assert.assertNotNull(truststore_pwd, "Truststore password is mandatory when security is enabled");
            jdbcConnection = jdbcConnection + ";SSL=true;KEYSTORE=" + keystore_path +
                    ";KEYSTORE_PWD=" + keystore_pwd + ";TRUSTSTORE=" + truststore_path + ";TRUSTSTORE_PWD=" + truststore_pwd;
        }

        if (password != null) {
            jdbcConnection = jdbcConnection + ";PWD=" + password;
        }

        try {
            Class.forName("com.stratio.jdbc.core.jdbc4.StratioDriver");
            JDBCConnection.setConnection(DriverManager.getConnection(jdbcConnection));
        } catch (SQLException se) {
            // log the exception
            this.getLogger().error(se.getMessage());
            // re-throw the exception
            throw se;
        }
    }

    public void connectToCustomDatabase(String database, String jdbcConnection, String user, String password) throws Exception {
        String driver;
        switch (database) {
            case "oracle":
                driver = "oracle.jdbc.OracleDriver";
                break;
            case "db2":
                driver = "com.ibm.db2.jcc.DB2Driver";
                break;
            case "sqlserver":
                driver = "com.microsoft.sqlserver.jdbc.SQLServerDriver";
                break;
            case "saphana":
                driver = "com.sap.db.jdbc.Driver";
                break;
            default:
                driver = "unknown";
        }

        logger.info("Connecting to {} using driver {}", jdbcConnection, driver);
        try {
            Class.forName(driver);
            JDBCConnection.setConnection(DriverManager.getConnection(jdbcConnection, user, password));
        } catch (SQLException se) {
            // log the exception
            this.getLogger().error(se.getMessage());
            // re-throw the exception
            throw se;
        }
    }

    public void setCCTConnection(String tenantOrig, String loginInfo) throws Exception {
        if (ThreadProperty.get("EOS_ACCESS_POINT") == null && ThreadProperty.get("KEOS_OAUTH2_PROXY_HOST") == null) {
            fail("KEOS_OAUTH2_PROXY_HOST and EOS_ACCESS_POINT variable are not set. Check @keos / @dcos annotation is working properly.");
        }
        if (ThreadProperty.get("isKeosEnv") != null && ThreadProperty.get("isKeosEnv").equals("true")) {
            setCCTConnectionKeos(tenantOrig, loginInfo);
        } else {
            setCCTConnectionDCOS(tenantOrig, loginInfo);
        }
    }

    private void setCCTConnectionDCOS(String tenantOrig, String loginInfo) throws Exception {
        String tenant = tenantOrig;
        String user;
        String password;

        if (tenantOrig == null) {
            tenant = ThreadProperty.get("DCOS_TENANT");
        }

        if (loginInfo == null) {
            user = ThreadProperty.get("DCOS_USER");
            password = ThreadProperty.get("DCOS_PASSWORD") != null ? ThreadProperty.get("DCOS_PASSWORD") : System.getProperty("DCOS_PASSWORD");
        } else {
            user = loginInfo.split(":")[0];
            password = loginInfo.split(":")[1];
        }

        // Set sso token
        DcosSpec dcosSpec = new DcosSpec(this);
        dcosSpec.setGoSecSSOCookie(null, null, ThreadProperty.get("EOS_ACCESS_POINT"), user, password, tenant, null, null);

        // Securely send requests
        this.setRestProtocol("https://");
        this.setRestHost(ThreadProperty.get("EOS_ACCESS_POINT"));
        this.setRestPort(":443");
    }

    private void setCCTConnectionKeos(String tenantOrig, String loginInfo) throws Exception {
        String tenant = tenantOrig;
        String user;
        String password;

        if (tenantOrig == null) {
            tenant = ThreadProperty.get("KEOS_TENANT");
        }

        if (loginInfo == null) {
            user = ThreadProperty.get("KEOS_USER");
            password = ThreadProperty.get("KEOS_PASSWORD") != null ? ThreadProperty.get("KEOS_PASSWORD") : System.getProperty("KEOS_PASSWORD");
        } else {
            user = loginInfo.split(":")[0];
            password = loginInfo.split(":")[1];
        }

        // Set sso token
        KeosSpec keosSpec = new KeosSpec(this);
        keosSpec.setGoSecSSOCookieKeos(null, ThreadProperty.get("KEOS_OAUTH2_PROXY_HOST"), user, password, tenant, null, null);

        // Securely send requests
        this.setRestProtocol("https://");
        this.setRestHost(ThreadProperty.get("KEOS_OAUTH2_PROXY_HOST"));
        this.setRestPort(":443");
    }

    public Map<String, List<String>> getPreviousSqlResult() {
        return previousSqlResult;
    }

    public void setPreviousSqlResult(Map<String, List<String>> previousSqlResult) {
        this.previousSqlResult = previousSqlResult;
    }

    /**
     * Get the Vault utils.
     *
     * @return VaultUtils
     */
    public VaultUtils getVaultUtils() {
        return VaultUtil.INSTANCE.getVaultUtils();
    }

    /**
     * Get /etc/hosts management utils.
     *
     * @return ETCHOSTSManagementUtils
     */
    public ETCHOSTSManagementUtils getETCHOSTSManagementUtils() {
        return ETCHOSTSManagementUtil.INSTANCE.getETCHOSTSManagementUtils();
    }

    /**
     * Get Kafka utils.
     *
     * @return KafkaSecUtils
     */
    public KafkaSecUtils getKafkaSecUtils() {
        return KafkaSecUtil.INSTANCE.getKafkaSecUtils();
    }

    /**
     * Get Zookeeper utils.
     *
     * @return ZookeeperSecUtils
     */
    public ZookeeperSecUtils getZookeeperSecUtils() {
        return ZookeeperSecUtil.INSTANCE.getZookeeperSecUtils();
    }

    /**
     * Get HDFS utils.
     *
     * @return HDFSSecUtils
     */
    public HDFSSecUtils getHDFSSecUtils() {
        return HDFSSecUtil.INSTANCE.getHDFSSecUtils();
    }

    public void initClients() {
        marathonClient = MarathonApiClient.getInstance(this);
        marathonUtils = new MarathonUtils(marathonClient);
        mesosApiClient = MesosApiClient.getInstance(this);
        mesosUtils = new MesosUtils(mesosApiClient);
        cctMarathonServiceClient = CctMarathonServiceApiClient.getInstance(this);
        configurationApiClient = ConfigurationApiClient.getInstance(this);
        deployApiClient = DeployApiClient.getInstance(this);
    }

    public void initKubernetesClient() {
        kubernetesClient = KubernetesClient.getInstance();
    }

    /**
     * Obtain cookies from previous request
     *
     * @param ssoCookies
     * @param tokenList
     * @return
     */
    public List<Cookie> addSsoToken(HashMap<String, String> ssoCookies, String[] tokenList) {
        List<Cookie> cookiesAttributes = new ArrayList<>();
        for (String tokenKey : tokenList) {
            Cookie cookieToAdd = new DefaultCookie(tokenKey, ssoCookies.get(tokenKey) != null ? ssoCookies.get(tokenKey) : "");
            cookieToAdd.setDomain(null);
            cookieToAdd.setPath(null);
            cookieToAdd.setWrap(false);
            cookieToAdd.setMaxAge(99);
            cookieToAdd.setSecure(false);
            cookieToAdd.setHttpOnly(false);
            cookiesAttributes.add(cookieToAdd);
        }
        return cookiesAttributes;
    }

    /**
     * Check if host is reachable
     *
     * @param host Host
     * @param timeOut timeout
     * @return True/False
     */
    public boolean isHostReachable(String host, int timeOut) throws Exception {
        int timeoutAux = timeOut;
        while (timeoutAux > 0) {
            try {
                return InetAddress.getByName(host).isReachable(timeoutAux * 1000);
            } catch (UnknownHostException e) {
                timeoutAux--;
                if (timeoutAux > 0) {
                    getLogger().warn("UnknownHostException returned after {} seconds, it will try again in 1 second", timeOut - timeoutAux);
                    Thread.sleep(1000);
                } else {
                    return false;
                }
            }
        }
        return false;
    }
}
