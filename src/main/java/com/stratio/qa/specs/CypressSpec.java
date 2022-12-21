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

import io.cucumber.datatable.DataTable;
import cucumber.api.java.en.Given;
import cucumber.api.java.en.When;
import org.assertj.core.api.Assertions;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Cypress Steps.
 *
 * @see <a href="CommandExecutionSpec-annotations.html">Command Execution Steps &amp; Matching Regex</a>
 */
public class CypressSpec extends BaseGSpec {

    /**
     * Generic constructor.
     *
     * @param spec object
     */
    public CypressSpec(CommonG spec) {
        this.commonspec = spec;
    }

    @When("^I run Cypress testcase '(.+?)'( with config file '(.+?)')?( and store video evidences with path '(.+?)')?( and with exit status '(\\d+)')?( and save the value in environment variable '(.+?)')?( with a timeout of '(.+?)' seconds)?$")
    public void executeCypresswithURLwithVideo(String testcase, String configFile, String videopath, Integer sExitStatus, String envVar, Integer _timeout, DataTable table) throws Exception {
        Integer exitStatus = sExitStatus == null ? 0 : sExitStatus;
        int timeout = _timeout == null ? -1 : _timeout;
        Map<String, String> variables;
        String cypressVariables;
        try {
            variables = table.asMap(String.class, String.class);
            cypressVariables = variables.keySet().stream().map(k -> k + "=" + variables.get(k))
                    .collect(Collectors.joining(" ", "", ""));
        } catch (Exception e) {
            this.commonspec.getLogger().warn("Error parsing Datatable to map. Setting empty...");
            cypressVariables = "";
        }

        String cypressBrowser = System.getProperty("CYPRESS_BROWSER") == null ? "" : " --browser " + System.getProperty("CYPRESS_BROWSER");

        String configVariable = configFile == null ? "" : " --config-file cypress/e2e/" + configFile;
        String videoVariable = videopath == null ? "" : " --config trashAssetsBeforeRuns=false,videoUploadOnPasses=true,videosFolder=" + videopath;
        String command = cypressVariables + " npx cypress run" + cypressBrowser + " --spec cypress/e2e/" + testcase + configVariable + videoVariable;

        this.commonspec.getLogger().info("Executing cypress: " + command);

        commonspec.runLocalCommand(command, timeout);
        commonspec.runCommandLoggerAndEnvVar(exitStatus, envVar, Boolean.TRUE);
        Assertions.assertThat(commonspec.getCommandExitStatus()).isEqualTo(exitStatus);
    }

    @Given("^I run all Cypress tests with host '(.+?)' and token '(.+?)'( and with exit status '(\\d+)')?( and save the value in environment variable '(.+?)')?$")
    public void executeAllCypressTests(String url, String token, Integer sExitStatus, String envVar) throws Exception {
        Integer exitStatus = sExitStatus == null ? 0 : sExitStatus;

        String cypressBrowser = System.getProperty("CYPRESS_BROWSER") == null ? "" : " --browser " + System.getProperty("CYPRESS_BROWSER");

        String Command = "CYPRESS_BASE_URL=https://" + url + " CYPRESS_TOKEN=" + token + " npx cypress run" + cypressBrowser + "  ";
        commonspec.runLocalCommand(Command);
        commonspec.runCommandLoggerAndEnvVar(exitStatus, envVar, Boolean.TRUE);
        Assertions.assertThat(commonspec.getCommandExitStatus()).isEqualTo(exitStatus);

    }

}

