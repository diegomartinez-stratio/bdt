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

package com.stratio.qa.aspects;

import com.stratio.qa.cucumber.runner.Glue;
import com.stratio.qa.cucumber.runtime.RuntimeOptions;
import com.stratio.qa.cucumber.testng.CucumberReporter;
import com.stratio.qa.cucumber.testng.TestSourcesModelUtil;
import com.stratio.qa.exceptions.NonReplaceableException;
import com.stratio.qa.specs.CommonG;
import com.stratio.qa.specs.HookGSpec;
import com.stratio.qa.utils.ExceptionList;
import com.stratio.qa.utils.StepException;
import com.stratio.qa.utils.ThreadProperty;
import cucumber.api.PickleStepTestStep;
import cucumber.api.Result.Type;
import cucumber.api.Scenario;
import cucumber.runner.AmbiguousStepDefinitionsException;
import cucumber.runner.EventBus;
import cucumber.runtime.Backend;
import cucumber.runtime.CucumberException;
import cucumber.runtime.StepDefinitionMatch;
import gherkin.events.PickleEvent;
import gherkin.pickles.*;
import gherkin.pickles.Argument;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.List;

import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;

@Aspect
public class ReplacementAspect {

    private final Logger logger = LoggerFactory.getLogger(this.getClass().getCanonicalName());

    private Glue glue;

    private List<String> undefinedSteps = new ArrayList<>();

    private String lastEchoedStep = "";

    @Pointcut("execution (cucumber.runner.Runner.new(..)) && "
            + "args (bus, backends, runtimeOptions)")
    protected void runnerInit(EventBus bus, Collection<? extends Backend> backends, RuntimeOptions runtimeOptions) {
    }

    @After(value = "runnerInit(bus, backends, runtimeOptions)")
    public void runnerInitGlue(JoinPoint jp, EventBus bus, Collection<? extends Backend> backends, RuntimeOptions runtimeOptions) throws Throwable {
        glue = new Glue(bus);
        for (Backend backend : backends) {
            backend.loadGlue(glue, runtimeOptions.getGlue());
        }
    }

    @Pointcut("execution (* cucumber.runtime.formatter.DefaultSummaryPrinter.printSnippets(..))")
    protected void print() {
    }

    @Around(value = "print()")
    public void printSnippets(ProceedingJoinPoint pjp) throws Throwable {
        if (!undefinedSteps.isEmpty()) {
            logger.error("The following steps are undefined:");
            for (String undefinedStep:undefinedSteps) {
                logger.error("    {}", undefinedStep);
            }
        }
    }

    @Pointcut("execution (* cucumber.runner.Runner.runPickle(..)) && "
            + "args (pickle)")
    protected void replacementScenarios(PickleEvent pickle) {
    }

    @Before(value = "replacementScenarios(pickle)")
    public void aroundScenarios(JoinPoint jp, PickleEvent pickle) throws Throwable {
        String scenarioName = pickle.pickle.getName();
        String newScenarioName;
        try {
            newScenarioName = replacedElement(scenarioName, jp);
        } catch (Exception e) {
            ExceptionList.INSTANCE.getExceptions().add(e);
            newScenarioName = scenarioName + " | Placeholder not replaced -- " + e.toString();
        }
        if (!newScenarioName.equals(pickle.pickle.getName())) {
            Pickle pickle1 = pickle.pickle;
            Field field = null;
            Class current = pickle1.getClass();
            do {
                try {
                    field = current.getDeclaredField("name");
                } catch (Exception e) { }
            } while ((current = current.getSuperclass()) != null);

            field.setAccessible(true);
            field.set(pickle1, newScenarioName);
        }
    }

    @Pointcut("execution (* cucumber.runner.TestStep.executeStep(..)) && "
            + "args (scenario, skipSteps)")
    protected void replacementStar(Scenario scenario, boolean skipSteps) {
    }

    @Around(value = "replacementStar(scenario, skipSteps)")
    public Type aroundReplacementStar(ProceedingJoinPoint pjp, Scenario scenario, boolean skipSteps) throws Throwable {
        if (pjp.getTarget() instanceof PickleStepTestStep) {
            if (StepException.INSTANCE.getException() != null) {
                return Type.SKIPPED;
            } else {
                if (scenario.getSourceTagNames().contains("@error")) {
                    for (String tag: scenario.getSourceTagNames()) {
                        if (tag.contains("errorMessage")) {
                            String msg = tag.substring((tag.lastIndexOf("(") + 1), (tag.length()) - 1).replaceAll("__", " ");
                            logger.error("-> " + msg);
                            throw new CucumberException(msg);
                        }
                    }
                }
                try {
                    PickleStepTestStep pickleTestStep = (PickleStepTestStep) pjp.getTarget();
                    PickleStep step = pickleTestStep.getPickleStep();

                    // Replace elements in datatable and doc string
                    StringBuilder sbDataTable = new StringBuilder();
                    List<Argument> argumentList = new ArrayList<>(step.getArgument());
                    for (int a = 0; a < argumentList.size(); a++) {
                        if (argumentList.get(a) instanceof PickleTable) {
                            PickleTable pickleTable = (PickleTable) argumentList.get(a);
                            List<PickleRow> pickleRowList = new ArrayList<>(pickleTable.getRows());
                            for (int r = 0; r < pickleRowList.size(); r++) {
                                PickleRow pickleRow = pickleRowList.get(r);
                                List<PickleCell> pickleCellList = new ArrayList<>(pickleRow.getCells());
                                sbDataTable.append("| ");
                                for (int c = 0; c < pickleCellList.size(); c++) {
                                    PickleCell pickleCell = pickleCellList.get(c);
                                    pickleCellList.set(c, new PickleCell(pickleCell.getLocation(), replacedElement(pickleCell.getValue(), pjp)));
                                    sbDataTable.append(pickleCellList.get(c).getValue()).append(" | ");
                                }
                                pickleRowList.set(r, new PickleRow(pickleCellList));
                                sbDataTable.append("\n");
                            }
                            pickleTable = new PickleTable(pickleRowList);
                            argumentList.set(a, pickleTable);
                        }
                        if (argumentList.get(a) instanceof PickleString) {
                            PickleString pickleString = (PickleString) argumentList.get(a);
                            pickleString = new PickleString(pickleString.getLocation(), replacedElement(pickleString.getContent(), pjp));
                            argumentList.set(a, pickleString);
                        }
                    }

                    // Set new datatable in PickleStep object
                    step = new PickleStep(step.getText(), argumentList, step.getLocations());

                    // Replace elements in step
                    String uri = pickleTestStep.getStepLocation().split(":")[0];
                    String stepName = step.getText();
                    String newName = replacedElement(stepName, pjp);
                    String keyword = TestSourcesModelUtil.INSTANCE.getTestSourcesModel().getKeywordFromSource(scenario.getUri(), pickleTestStep.getStepLine());
                    if (!stepName.equals(newName)) {
                        //field up to BasicStatement, from Step and ExampleStep
                        Field field = null;
                        Class current = step.getClass();
                        do {
                            try {
                                field = current.getDeclaredField("text");
                            } catch (Exception e) {
                            }
                        } while ((current = current.getSuperclass()) != null);

                        field.setAccessible(true);
                        field.set(step, newName.replaceAll("(\\r|\\n|\\r\\n)+", "\\\\n"));
                        scenario.write(newName);
                    }
                    step = new PickleStep(step.getText(), argumentList, step.getLocations());

                    TestSourcesModelUtil.INSTANCE.getTestSourcesModel().addReplacedStep(scenario.getUri(), pickleTestStep.getStepLine(), step);
                    lastEchoedStep = pickleTestStep.getStepText();
                    if (HookGSpec.loggerEnabled) {
                        logger.info("   {}{}", keyword, newName);
                        if (!sbDataTable.toString().isEmpty()) {
                            logger.info("  {}", sbDataTable.toString());
                        }
                    }

                    // Run step
                    try {
                        StepDefinitionMatch definitionMatch = glue.stepDefinitionMatch(uri, step);
                        if (definitionMatch != null) {
                            if (!skipSteps) {
                                definitionMatch.runStep(scenario);
                                return Type.PASSED;
                            } else {
                                definitionMatch.dryRunStep(scenario);
                                return Type.SKIPPED;
                            }
                        } else {
                            logger.error("Undefined step!! {}", newName);
                            String undefinedStep = scenario.getUri() + " # " + newName;
                            if (!undefinedSteps.contains(undefinedStep)) {
                                undefinedSteps.add(undefinedStep);
                            }
                            return Type.UNDEFINED;
                        }
                    } catch (AmbiguousStepDefinitionsException asde) {
                        return (Type) pjp.proceed();
                    }
                } catch (Exception e) {
                    StepException.INSTANCE.setException(e);
                    throw e;
                }
            }
        }
        return (Type) pjp.proceed();
    }

    protected String replacedElement(String el, JoinPoint jp) throws NonReplaceableException {
        try {
            if (el.contains("${")) {
                el = replaceEnvironmentPlaceholders(el, jp);
            }
            if (el.contains("!{")) {
                el = replaceReflectionPlaceholders(el, jp);
            }
            if (el.contains("@{")) {
                el = replaceCodePlaceholders(el, jp);
            }
            return el;
        } catch (Exception e) {
            if (e instanceof NonReplaceableException) {
                throw e;
            } else {
                throw new NonReplaceableException("Unreplaceable element: " + el + " - " + e);
            }
        }
    }

    /**
     * Replaces every placeholded element, enclosed in @{} with the
     * corresponding attribute value in local Common class
     * <p>
     * If the element starts with:
     * - IP: We expect it to be followed by '.' + interface name (i.e. IP.eth0). It can contain other replacements.
     * <p>
     * If the element starts with:
     * - JSON: We expect it to be followed by '.' + path_to_json_file (relative to src/test/resources or
     * target/test-classes). The json is read and its content is returned as a string
     * <p>
     * If the element starts with:
     * - FILE: We expect it to be followed by '.' + path_to_file (relative to src/test/resources or
     * target/test-classes). The file is read and its content is returned as a string
     *
     * @param element element to be replaced
     * @param pjp JoinPoint
     * @return String
     * @throws NonReplaceableException exception
     */
    protected String replaceCodePlaceholders(String element, JoinPoint pjp) throws NonReplaceableException {
        String newVal = element;
        while (newVal.contains("@{")) {
            String placeholder = newVal.substring(newVal.indexOf("@{"), newVal.indexOf("}", newVal.indexOf("@{")) + 1);
            String property = placeholder.substring(2, placeholder.length() - 1).toLowerCase();
            String subproperty = "";
            CommonG commonJson;
            if (placeholder.contains(".")) {
                property = placeholder.substring(2, placeholder.indexOf(".")).toLowerCase();
                subproperty = placeholder.substring(placeholder.indexOf(".") + 1, placeholder.length() - 1);
            } else {
                if (pjp.getThis() instanceof CucumberReporter.TestMethod) {
                    return newVal;
                } else {
                    logger.error("{} -> {} placeholded element has not been replaced previously.", element, property);
                    throw new NonReplaceableException("Unreplaceable placeholder: " + placeholder);
                }
            }

            switch (property) {
                case "ip":
                    boolean found = false;
                    if (!subproperty.isEmpty()) {
                        Enumeration<InetAddress> ifs = null;
                        try {
                            ifs = NetworkInterface.getByName(subproperty).getInetAddresses();
                        } catch (SocketException e) {
                            this.logger.error(e.getMessage());
                        }
                        while (ifs.hasMoreElements() && !found) {
                            InetAddress itf = ifs.nextElement();
                            if (itf instanceof Inet4Address) {
                                String ip = itf.getHostAddress();
                                newVal = newVal.replace(placeholder, ip);
                                found = true;
                            }
                        }
                    }
                    if (!found) {
                        throw new NonReplaceableException("Interface " + subproperty + " not available");
                    }
                    break;
                case "json":
                case "file":
                    commonJson = new CommonG();
                    newVal = newVal.replace(placeholder, commonJson.retrieveData(subproperty, property));
                    break;
                default:
                    commonJson = new CommonG();
                    commonJson.getLogger().error("Replacement with an undefined option ({})", property);
                    newVal = newVal.replace(placeholder, "");
            }
        }
        return newVal;
    }


    /**
     * Replaces every placeholded element, enclosed in !{} with the
     * corresponding attribute value in local Common class
     *
     * @param element element to be replaced
     * @param pjp JoinPoint
     * @return String
     * @throws NonReplaceableException exception
     */
    protected String replaceReflectionPlaceholders(String element, JoinPoint pjp) throws NonReplaceableException {
        String newVal = element;
        while (newVal.contains("!{")) {
            String placeholder = newVal.substring(newVal.indexOf("!{"),
                    newVal.indexOf("}", newVal.indexOf("!{")) + 1);
            String attribute = placeholder.substring(2, placeholder.length() - 1);
            // we want to use value previously saved
            String prop = ThreadProperty.get(attribute);

            if (prop == null && (pjp.getThis() instanceof CucumberReporter.TestMethod)) {
                return element;
            } else if (prop == null) {
                logger.error("{} -> {} local var has not been saved correctly previously.", element, attribute);
                throw new NonReplaceableException("Unreplaceable placeholder: " + placeholder);
            } else {
                newVal = newVal.replace(placeholder, prop);
            }
        }
        return newVal;
    }


    /**
     * Replaces every placeholded element, enclosed in ${} with the
     * corresponding java property
     *
     * @param element element to be replaced
     * @param jp JoinPoint
     * @return String
     * @throws NonReplaceableException exception
     */
    protected String replaceEnvironmentPlaceholders(String element, JoinPoint jp) throws NonReplaceableException {
        String newVal = element;
        while (newVal.contains("${")) {
            String placeholder = newVal.substring(newVal.indexOf("${"),
                    newVal.indexOf("}", newVal.indexOf("${")) + 1);
            String modifier = "";
            String sysProp;
            String defaultValue = "";
            String prop;
            String placeholderAux = "";
            Boolean emptyDefault = false;

            if (placeholder.contains(":-")) {
                defaultValue = placeholder.substring(placeholder.indexOf(":-") + 2, placeholder.length() - 1);
                if ("''".equals(defaultValue)) {
                    emptyDefault = true;
                    defaultValue = "";
                }
                placeholderAux = placeholder.substring(0, placeholder.indexOf(":-")) + "}";
            }

            if (placeholderAux.contains(".")) {
                if (placeholder.contains(":-")) {
                    sysProp = placeholderAux.substring(2, placeholderAux.indexOf("."));
                    modifier = placeholderAux.substring(placeholderAux.indexOf(".") + 1, placeholderAux.length() - 1);
                } else {
                    sysProp = placeholder.substring(2, placeholder.indexOf("."));
                    modifier = placeholder.substring(placeholder.indexOf(".") + 1, placeholder.length() - 1);
                }
            } else {
                if (defaultValue.isEmpty() && !emptyDefault) {
                    if (placeholder.contains(".")) {
                        modifier = placeholder.substring(placeholder.indexOf(".") + 1, placeholder.length() - 1);
                        sysProp = placeholder.substring(2, placeholder.indexOf("."));
                    } else {
                        sysProp = placeholder.substring(2, placeholder.length() - 1);
                    }
                } else {
                    sysProp = placeholder.substring(2, placeholder.indexOf(":-"));
                }
            }

            if (defaultValue.isEmpty()) {
                if (emptyDefault) {
                    prop = System.getProperty(sysProp, defaultValue);
                } else {
                    prop = System.getProperty(sysProp);
                }

            } else {
                prop = System.getProperty(sysProp, defaultValue);
            }

            if (prop == null && (jp.getThis() instanceof CucumberReporter.TestMethod)) {
                return element;
            } else if (prop == null) {
                logger.error("{} -> {} env var has not been defined.", element, sysProp);
                throw new NonReplaceableException("Unreplaceable placeholder: " + placeholder);
            }

            if ("toLower".equals(modifier)) {
                prop = prop.toLowerCase();
            } else if ("toUpper".equals(modifier)) {
                prop = prop.toUpperCase();
            }
            newVal = newVal.replace(placeholder, prop);
        }

        // Allow setting empty string as default value
        if (newVal.equalsIgnoreCase("\"\"")) {
            newVal = "";
        }
        return newVal;
    }
}

