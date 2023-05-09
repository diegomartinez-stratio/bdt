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

import com.stratio.qa.exceptions.SuppressableException;
import com.stratio.qa.utils.StepException;
import com.stratio.qa.utils.ThreadProperty;
import cucumber.api.Result;
import cucumber.api.Scenario;
import cucumber.api.java.After;
import cucumber.api.java.Before;
import java.io.File;

import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.AsyncHttpClientConfig;
import org.asynchttpclient.DefaultAsyncHttpClientConfig;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.remote.CommandInfo;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.HttpCommandExecutor;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.remote.http.HttpClient;
import org.openqa.selenium.remote.internal.ApacheHttpClient;
import org.openqa.selenium.remote.internal.HttpClientFactory;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

import static org.asynchttpclient.Dsl.asyncHttpClient;
import static org.asynchttpclient.Dsl.config;
import static org.testng.Assert.fail;

public class HookGSpec extends BaseGSpec {

    public static final int ORDER_0 = 0;

    public static final int ORDER_10 = 10;

    public static final int ORDER_20 = 20;

    public static final int PAGE_LOAD_TIMEOUT = 120;

    public static final int IMPLICITLY_WAIT = 10;

    public static final int SCRIPT_TIMEOUT = 30;

    private static final String importantTAG = "@important";

    private static final String notImportantTAG = "@notimportant";

    private static final String alwaysTAG = "@always";

    private static final String continueOnPreviousFeatureFailureTAG = "@continueOnPreviousFeatureFailure";

    private static boolean prevScenarioFailed = false;

    private static boolean importantScenarioHasFail = false;

    private static final String quietasdefault = System.getProperty("quietasdefault", "true");

    public static boolean loggerEnabled = true;

    public static final int ORDER_30 = 30;

    public static String featureUri = "";

    /**
     * Default constructor.
     *
     * @param spec
     */
    public HookGSpec(CommonG spec) {
        this.commonspec = spec;
    }

    /**
     * Clean the exception list.
     */
    @Before(order = ORDER_30, value = "@cypress")
    public void cypressSetup() throws Exception {
        String checkCypressCommand = "npm list cypress";
        commonspec.getLogger().info("Checking if Cypress is installed and updated...");
        commonspec.runLocalCommand(checkCypressCommand);
        if (commonspec.getCommandExitStatus() == 0) {
            commonspec.getLogger().info("Cypress already installed and updated.");
            return;
        }

        // TODO: remove postinstall command when https://github.com/michaelleeallen/mocha-junit-reporter/pull/143 is merged (see it in qa-unified-test project)
        String installCypressCommand = "npm install --loglevel=error --depth=0 --registry=http://niquel.int.stratio.com/repository/publicnpm/ --legacy-peer-deps && npm run postinstall --if-present";
        commonspec.getLogger().info("Cypress not installed or updated, proceeding...");
        commonspec.runLocalCommand(installCypressCommand);
        commonspec.getLogger().info(commonspec.getCommandResult() + "\n" + commonspec.getCommandResultError());
        commonspec.getLogger().info("Checking...");
        commonspec.runLocalCommand(checkCypressCommand);
        if (commonspec.getCommandExitStatus() != 0) {
            commonspec.getLogger().error("Cypress not installed, check your npm package.json file to be sure that Cypress is included.");
            throw new Exception("Cypress not installed, check your npm package.json file to be sure that Cypress is included.");
        }
        commonspec.getLogger().info("Cypress installed successfully.");
    }

    @Before(order = ORDER_0)
    public void globalSetup() {
        commonspec.getExceptions().clear();
        StepException.INSTANCE.setException(null);
    }


    @After
    public void watch_this_tagged_scenario(Scenario scenario) throws Exception {
        loggerEnabled = true;
        if (quietasdefault.equals("false")) {
            if (!isTagIncludedInScenario(scenario, notImportantTAG)) {
                boolean isFailed = scenario.isFailed();
                if (isFailed) {
                    prevScenarioFailed = importantScenarioHasFail = isFailed;
                }
            }
        } else {
            if (isTagIncludedInScenario(scenario, importantTAG)) {
                boolean isFailed = scenario.isFailed();
                if (isFailed) {
                    prevScenarioFailed = importantScenarioHasFail = isFailed;
                }
            }
        }
        if (prevScenarioFailed) {
            loggerEnabled = false;
        }
    }

    @Before
    public void quit_if_tagged_scenario_failed(Scenario scenario) throws Throwable {
        if (!featureUri.equals(scenario.getUri())) { // the Scenario is in a new Feature
            if (importantScenarioHasFail) {
                prevScenarioFailed = true;
            }
            if (isTagIncludedInScenario(scenario, continueOnPreviousFeatureFailureTAG)) {
                prevScenarioFailed = false;
            }
        }
        featureUri = scenario.getUri();

        if (prevScenarioFailed) {
            if (!isTagIncludedInScenario(scenario, alwaysTAG)) {
                commonspec.getLogger().warn("An important scenario has failed! TESTS EXECUTION ABORTED!");
                throw new SuppressableException("An important scenario has failed! TESTS EXECUTION ABORTED!", true);
            } else {
                loggerEnabled = true;
            }
        }
    }

    /**
     * Connect to selenium.
     *
     * @throws MalformedURLException
     */
    @Before(order = ORDER_10, value = {"@mobile or @web"})
    public void seleniumSetup() throws MalformedURLException {
        String grid = System.getProperty("SELENIUM_GRID");
        String b = ThreadProperty.get("browser");
        if ("".equals(b)) {
            fail("Non available browsers. Is it BrowsersDataProvider added in your test?");
        }

        DesiredCapabilities capabilities = null;
        String browser;
        String version;

        if (grid != null) {
            browser = b.split("_")[0];
            version = b.split("_")[1];
            commonspec.setBrowserName(browser);
            commonspec.getLogger().debug("Setting up selenium for {}", browser);
        } else {
            browser = "chrome";
            version = "";
        }

        String headers = System.getProperty("PROXY_HEADERS");
        switch (browser.toLowerCase()) {
            case "chrome":
                ChromeOptions chromeOptions = new ChromeOptions();

                if (headers != null && !"".equals(headers)) {
                    File extension = new File(System.getProperty("MODHEADER_PLUGIN"));
                    chromeOptions.addExtensions(extension);
                }
                chromeOptions.addArguments("--no-sandbox");
                chromeOptions.addArguments("--ignore-certificate-errors");
                chromeOptions.addArguments("--disable-dev-shm-usage");
                capabilities = DesiredCapabilities.chrome();
                capabilities.setCapability(ChromeOptions.CAPABILITY, chromeOptions);
                break;
            case "firefox":
                capabilities = DesiredCapabilities.firefox();
                break;
            case "phantomjs":
                capabilities = DesiredCapabilities.phantomjs();
                break;
            case "iphone":
            case "safari":
                capabilities = DesiredCapabilities.iphone();
                capabilities.setCapability("platformName", "iOS");
                capabilities.setCapability("platformVersion", "8.1");
                capabilities.setCapability("deviceName", "iPhone Simulator");
                break;
            case "android":
                capabilities = DesiredCapabilities.android();
                capabilities.setCapability("platformName", "Android");
                capabilities.setCapability("platformVersion", "6.0");
                capabilities.setCapability("deviceName", "Android Emulator");
                capabilities.setCapability("app", "Browser");
                break;
            default:
                commonspec.getLogger().error("Unknown browser: " + browser);
                throw new WebDriverException("Unknown browser: " + browser);
        }

        capabilities.setVersion(version);

        grid = "http://" + (grid != null ? grid : b + ":4444") + "/wd/hub";
        HttpClient.Factory factory = new ApacheHttpClient.Factory(new HttpClientFactory(60000, 60000));
        HttpCommandExecutor executor = new HttpCommandExecutor(new HashMap<String, CommandInfo>(), new URL(grid), factory);
        commonspec.setDriver(new RemoteWebDriver(executor, capabilities));
        if (headers != null && !"".equals(headers)) {
            String[] ar = headers.split(",");
            String headersString = "";
            for (String s: ar) {
                headersString += "     {enabled: true, name: '" + s.split(":")[0] + "', value: '" + s.split(":")[1] + "', comment: ''},";
            }
            //Remove last coma
            headersString = (headersString.substring(0, headersString.length() - 1));
            commonspec.getDriver().get("chrome-extension://idgpnmonknjnojddfkpgkljpfnnfcklj/icon.png");
            commonspec.getDriver().executeScript(
                    "localStorage.setItem('profiles', JSON.stringify([{                " +
                            "  title: 'Selenium', hideComment: true, appendMode: '',           " +
                            "  headers: [                                                      " +
                                headersString +
                            "  ],                                                              " +
                            "  respHeaders: [],                                                " +
                            "  filters: []                                                     " +
                            "}]));                                                             ");
        }
        commonspec.getDriver().manage().timeouts().pageLoadTimeout(PAGE_LOAD_TIMEOUT, TimeUnit.SECONDS);
        commonspec.getDriver().manage().timeouts().implicitlyWait(IMPLICITLY_WAIT, TimeUnit.SECONDS);
        commonspec.getDriver().manage().timeouts().setScriptTimeout(SCRIPT_TIMEOUT, TimeUnit.SECONDS);

        commonspec.getDriver().manage().deleteAllCookies();
        if (capabilities.getCapability("deviceName") == null) {
            commonspec.getDriver().manage().window().setSize(new Dimension(1440, 900));
        }
        commonspec.getDriver().manage().window().maximize();


    }


    /**
     * Close selenium web driver.
     */
    @After(order = ORDER_20, value = {"@mobile or @web"})
    public void seleniumTeardown() {
        if (commonspec.getDriver() != null) {
            commonspec.getLogger().debug("Shutdown Selenium client");
            commonspec.getDriver().close();
            commonspec.getDriver().quit();
        }
    }

    /**
     * Close logger.
     */
    @After(order = ORDER_0)
    public void teardown() {
    }

    @After
    public void afterScenario(Scenario scenario) throws Throwable {
        if (scenario.getStatus() == Result.Type.UNDEFINED) {
            if (!commonspec.getExceptions().isEmpty()) {
                scenario.write(commonspec.getExceptions().get(commonspec.getExceptions().size() - 1).toString());
            }
        }
    }

    @Before(order = ORDER_10, value = "@rest or @dcos or @keos")
    public void restClientSetup() throws Exception {
        commonspec.getLogger().debug("Starting a REST client");
        commonspec.setClient(asyncHttpClient(config().setUseInsecureTrustManager(true).setKeepAlive(false).build()));
        commonspec.initClients();
    }

    @Before(order = ORDER_10)
    public void startK8sClient() {
        commonspec.initKubernetesClient();
    }

    @After(order = ORDER_10, value = "@rest or @dcos or @keos")
    public void restClientTeardown() throws IOException {
        commonspec.getLogger().debug("Shutting down REST client");
        commonspec.getClient().close();
    }

    private boolean isTagIncludedInScenario(Scenario scenario, String customTAG) {
        Collection<String> tags = scenario.getSourceTagNames();
        return tags.contains(customTAG);
    }

    @Before(order = ORDER_20, value = "@dcos")
    public void dcosSetup() throws Exception {
        DcosSpec dcosSpec = new DcosSpec(commonspec);
        MiscSpec miscspec = new MiscSpec(commonspec);
        dcosSpec.obtainBasicInfoFromWorkspace();
        dcosSpec.obtainBasicInfoFromDescriptor(null);
        dcosSpec.getServicesInfoFromMarathon(null);
        dcosSpec.obtainBasicInfoFromETCD();
        miscspec.setGosecVariables();
    }

    @Before(order = ORDER_20, value = "@keos")
    public void keosSetup() throws Exception {
        if (ThreadProperty.get("CLUSTER_KUBE_CONFIG_PATH") == null) {
            commonspec.kubernetesClient.getK8sConfigFromWorkspace(commonspec);
        }
        ThreadProperty.set("isKeosEnv", "true");
    }

    @After(order = ORDER_20, value = "@dcos")
    public void dcosCleanup() {
    }
}
