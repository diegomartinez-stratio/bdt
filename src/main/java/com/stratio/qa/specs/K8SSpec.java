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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import com.stratio.qa.utils.ThreadProperty;
import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;
import io.cucumber.datatable.DataTable;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import org.testng.Assert;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

/**
 * Generic Kubernetes Specs.
 *
 * @see <a href="K8SSpec-annotations.html">Kubernetes Steps</a>
 */
public class K8SSpec extends BaseGSpec {

    /**
     * Generic constructor.
     *
     * @param spec object
     */
    public K8SSpec(CommonG spec) {
        this.commonspec = spec;
    }

    @When("^I load Kubernetes configuration from workspace( forcefully)?$")
    public void loadK8sConfigFromWorkspace(String force) throws Exception {
        if (force != null || ThreadProperty.get("CLUSTER_KUBE_CONFIG_PATH") == null) {
            commonspec.kubernetesClient.getK8sConfigFromWorkspace(commonspec);
        }
    }

    @When("^I connect to Kubernetes cluster using config file located at '(.+?)'$")
    public void connectToK8s(String kubeConfigPath) throws Exception {
        commonspec.kubernetesClient.connect(kubeConfigPath);
    }

    @When("^I get (pods|configmaps|serviceaccounts|replicasets|secrets|clusterroles|clusterrolebindings|statefulsets|roles|rolebindings|customresourcedefinitions|deployments|services|ingress|persistentVolumeClaims)( in namespace '(.+?)')? and save it in environment variable '(.+?)'$")
    public void getList(String type, String namespace, String envVar) {
        String response = null;
        switch (type) {
            case "pods":
                response = commonspec.kubernetesClient.getNamespacePods(namespace);
                break;
            case "configmaps":
                response = commonspec.kubernetesClient.getConfigMapList(namespace);
                break;
            case "serviceaccounts":
                response = commonspec.kubernetesClient.getServiceAccountList(namespace);
                break;
            case "replicasets":
                response = commonspec.kubernetesClient.getReplicaSetList(namespace);
                break;
            case "secrets":
                response = commonspec.kubernetesClient.getSecretsList(namespace);
                break;
            case "clusterroles":
                response = commonspec.kubernetesClient.getClusterRoleList(namespace);
                break;
            case "clusterrolebindings":
                response = commonspec.kubernetesClient.getClusterRoleBindingList(namespace);
                break;
            case "statefulsets":
                response = commonspec.kubernetesClient.getStateFulSetList(namespace);
                break;
            case "roles":
                response = commonspec.kubernetesClient.getRoleList(namespace);
                break;
            case "rolebindings":
                response = commonspec.kubernetesClient.getRoleBindingList(namespace);
                break;
            case "customresourcedefinitions":
                response = commonspec.kubernetesClient.getCustomResourceDefinitionList();
                break;
            case "deployments":
                response = commonspec.kubernetesClient.getDeploymentList(namespace);
                break;
            case "services":
                response = commonspec.kubernetesClient.getServiceList(namespace);
                break;
            case "ingress":
                response = commonspec.kubernetesClient.getIngressList(namespace);
                break;
            case "persistentVolumeClaims":
                response = commonspec.kubernetesClient.getPersistentVolumeClaimsList(namespace);
                break;
            default:
        }
        ThreadProperty.set(envVar, response);
    }

    @When("^I get all namespaces and save it in environment variable '(.+?)'$")
    public void getAllNamespaces(String envVar) {
        ThreadProperty.set(envVar, commonspec.kubernetesClient.getAllNamespaces());
    }

    @When("^I check that there is( not)? an event that contains the message '(.+?)' in namespace '(.+?)' (with resource type '(.+?)')? (with resource name '(.+?)')? (with reason '(.+?)')?$")
    public void checkEventNamespace(String not, String message, String namespace, String type, String name, String reason) {
        assertThat(commonspec.kubernetesClient.checkEventNamespace(not, namespace, type, name, reason, message)).as("There aren't event that contains the message " + message + " in namespace " + namespace).isTrue();
    }

    @When("^I describe (pod|service|deployment|configmap|replicaset|serviceaccount|secret|clusterrole|clusterrolebinding|statefulset|role|rolebinding|ingress|persistentVolumeClaims) with name '(.+?)'( in namespace '(.+?)')?( in '(yaml|json)' format)?( and save it in environment variable '(.*?)')?( and save it in file '(.*?)')?$")
    public void describeResource(String type, String name, String namespace, String format, String envVar, String fileName) throws Exception {
        String describeResponse;
        format = (format != null) ? format : "yaml";
        switch (type) {
            case "pod":
                describeResponse = commonspec.kubernetesClient.describePodYaml(name, namespace);
                break;
            case "service":
                describeResponse = commonspec.kubernetesClient.describeServiceYaml(name, namespace);
                break;
            case "deployment":
                describeResponse = commonspec.kubernetesClient.describeDeploymentYaml(name, namespace);
                break;
            case "configmap":
                describeResponse = commonspec.kubernetesClient.describeConfigMap(name, namespace);
                break;
            case "replicaset":
                describeResponse = commonspec.kubernetesClient.describeReplicaSet(name, namespace);
                break;
            case "serviceaccount":
                describeResponse = commonspec.kubernetesClient.describeServiceAccount(name, namespace);
                break;
            case "secret":
                describeResponse = commonspec.kubernetesClient.describeSecret(name, namespace);
                break;
            case "clusterrole":
                describeResponse = commonspec.kubernetesClient.describeClusterRole(name, namespace);
                break;
            case "clusterrolebinding":
                describeResponse = commonspec.kubernetesClient.describeClusterRoleBinding(name, namespace);
                break;
            case "statefulset":
                describeResponse = commonspec.kubernetesClient.describeStateFulSet(name, namespace);
                break;
            case "role":
                describeResponse = commonspec.kubernetesClient.describeRole(name, namespace);
                break;
            case "rolebinding":
                describeResponse = commonspec.kubernetesClient.describeRoleBinding(name, namespace);
                break;
            case "ingress":
                describeResponse = commonspec.kubernetesClient.describeIngress(name, namespace);
                break;
            case "persistentVolumeClaims":
                describeResponse = commonspec.kubernetesClient.describePersistentVolumeClaims(name, namespace);
                break;
            default:
                describeResponse = null;
        }
        if (describeResponse == null) {
            fail("Error obtaining " + type + " information");
        }

        if (format.equals("json")) {
            describeResponse = commonspec.convertYamlStringToJson(describeResponse);
        }

        getCommonSpec().getLogger().debug(type + " Response: " + describeResponse);

        if (envVar != null) {
            ThreadProperty.set(envVar, describeResponse);
        }
        if (fileName != null) {
            writeInFile(describeResponse, fileName);
        }
    }

    @When("^I describe custom resource '(.+?)' with name '(.+?)' in namespace '(.+?)'( in '(yaml|json)' format)?( and save it in file '(.*?)')?$")
    public void describeCustomResource(String name, String nameItem, String namespace, String format, String fileName) throws Exception {
        String describeResponse;
        format = (format != null) ? format : "yaml";
        describeResponse = commonspec.kubernetesClient.describeCustomResourceJson(name, nameItem, namespace);
        if (describeResponse == null) {
            fail("Error obtaining" + name + nameItem + " information");
        }

        if (format.equals("yaml")) {
            String std = describeResponse.replace("\r", "").replace("\n", ""); // make sure we have unix style text regardless of the input
            // parse JSON
            JsonNode jsonNodeTree = new ObjectMapper().readTree(std);
            // save it as YAML
            describeResponse = new YAMLMapper().writeValueAsString(jsonNodeTree);
        }

        getCommonSpec().getLogger().debug(name + " " + nameItem + " Response: " + describeResponse);

        if (fileName != null) {
            writeInFile(describeResponse, fileName);
        }
    }

    @When("^I run pod with name '(.+?)', in namespace '(.+?)', with image '(.+?)'(, with image pull policy '(.+?)')?, restart policy '(.+?)', service account '(.+?)',( environment variables '(.+?)',)? command '(.+?)' and the following arguments:$")
    public void runPod(String name, String namespace, String image, String imagePullPolicy, String restartPolicy, String serviceAccount, String envVars, String command, DataTable arguments) {
        List<String> argumentsList = new ArrayList<>();
        for (int i = 0; i < arguments.column(0).size(); i++) {
            argumentsList.add(arguments.cell(i, 0));
        }
        if (envVars == null) {
            commonspec.kubernetesClient.runPod(name, namespace, image, imagePullPolicy, restartPolicy, serviceAccount, command, argumentsList);
        } else {
            Map<String, String> env = new HashMap<>();
            String[] envVarsList = envVars.split(",");
            for (String envVar : envVarsList) {
                String key = envVar.split("=")[0];
                String value = envVar.split("=")[1];
                env.put(key, value);
            }
            commonspec.kubernetesClient.runPod(name, namespace, image, imagePullPolicy, restartPolicy, serviceAccount, env, command, argumentsList);
        }
    }

    @When("^in less than '(\\d+)' seconds, checking each '(\\d+)' seconds, pod with name '(.+?)' in namespace '(.+?)' has '(running|failed|succeeded)' status( and '(ready|not ready)' state)?$")
    public void assertPodStatus(Integer timeout, Integer wait, String podName, String namespace, String expectedStatus, String expectedState) throws InterruptedException {
        boolean found = false;
        Boolean readyStatusExpected = expectedState != null ? expectedState.equals("ready") : null;
        int i = 0;
        while (!found && i <= timeout) {
            Pod pod = commonspec.kubernetesClient.getPod(podName, namespace);
            try {
                Assert.assertEquals(pod.getStatus().getPhase().toLowerCase(), expectedStatus, "Expected status");
                if (readyStatusExpected != null) {
                    Assert.assertEquals(pod.getStatus().getContainerStatuses().get(pod.getStatus().getContainerStatuses().size() - 1).getReady().booleanValue(), readyStatusExpected.booleanValue(), "Pod ready?");
                }
                found = true;
            } catch (AssertionError | Exception e) {
                getCommonSpec().getLogger().info("Expected state/status don't found after " + i + " seconds");
                if (i >= timeout) {
                    throw e;
                }
                Thread.sleep(wait * 1000);
            }
            i += wait;
        }
    }

    @When("^in less than '(\\d+)' seconds, checking each '(\\d+)' seconds, we have '(\\d+)' pod/s with label filter '(.+?)' in namespace '(.+?)' having '(running|failed|succeeded)' status( and '(ready|not ready)' state)?$")
    public void assertPodStatusWithLabelFilter(Integer timeout, Integer wait, Integer expectedPods, String podSelector, String namespace, String expectedStatus, String expectedState) throws InterruptedException {
        boolean found = false;
        Boolean readyStatusExpected = expectedState != null ? expectedState.equals("ready") : null;
        int i = 0;
        while (!found && i <= timeout) {
            try {
                String[] podsList = {};
                String pods = commonspec.kubernetesClient.getPodsFilteredByLabel(podSelector, namespace);
                if (!pods.equals("")) {
                    podsList = pods.split("\n");
                }
                Assert.assertEquals(podsList.length, expectedPods.intValue(), "Expected pods");
                for (String podName : podsList) {
                    Pod pod = commonspec.kubernetesClient.getPod(podName, namespace);
                    Assert.assertEquals(pod.getStatus().getPhase().toLowerCase(), expectedStatus, "Expected status");
                    if (readyStatusExpected != null) {
                        Assert.assertEquals(pod.getStatus().getContainerStatuses().get(pod.getStatus().getContainerStatuses().size() - 1).getReady().booleanValue(), readyStatusExpected.booleanValue(), "Pod ready?");
                    }
                }
                found = true;
            } catch (AssertionError | Exception e) {
                getCommonSpec().getLogger().info("Expected state/status don't found or pods number are not expected number after " + i + " seconds");
                if (i >= timeout) {
                    throw e;
                }
                Thread.sleep(wait * 1000);
            }
            i += wait;
        }
    }

    @When("^in less than '(\\d+)' seconds, checking each '(\\d+)' seconds, deployment with name '(.+?)' in namespace '(.+?)' has '(\\d+)' replicas ready$")
    public void assertDeploymentStatus(Integer timeout, Integer wait, String deploymentName, String namespace, Integer readyReplicas) throws InterruptedException {
        boolean found = false;
        int i = 0;
        while (!found && i <= timeout) {
            Deployment deployment = commonspec.kubernetesClient.getDeployment(deploymentName, namespace);
            try {
                Assert.assertEquals(deployment.getStatus().getReadyReplicas() != null ? deployment.getStatus().getReadyReplicas().intValue() : 0, readyReplicas.intValue(), "# Ready Replicas");
                found = true;
            } catch (AssertionError | Exception e) {
                getCommonSpec().getLogger().info("Expected replicas ready don't found after " + i + " seconds");
                if (i >= timeout) {
                    throw e;
                }
                Thread.sleep(wait * 1000);
            }
            i += wait;
        }
    }

    @When("^in less than '(\\d+)' seconds, checking each '(\\d+)' seconds, statefulset with name '(.+?)' in namespace '(.+?)' has '(\\d+)' replicas ready$")
    public void assertStatefulsetStatus(Integer timeout, Integer wait, String statefulsetName, String namespace, Integer readyReplicas) throws InterruptedException {
        boolean found = false;
        int i = 0;
        while (!found && i <= timeout) {

            StatefulSet statefulSet = commonspec.kubernetesClient.getStateFulSet(statefulsetName, namespace);
            try {
                Assert.assertEquals(statefulSet.getStatus().getReadyReplicas().intValue(), readyReplicas.intValue(), "# Ready Replicas");
                found = true;
            } catch (AssertionError | Exception e) {
                getCommonSpec().getLogger().info("Expected replicas ready don't found after " + i + " seconds");
                if (i >= timeout) {
                    throw e;
                }
                Thread.sleep(wait * 1000);
            }
            i += wait;
        }
    }

    @When("^in less than '(\\d+)' seconds, checking each '(\\d+)' seconds, custom resource '(.+?)' with name '(.+?)' in namespace '(.+?)' has '(\\d+)' replicas ready$")
    public void assertCustomResourceStatus(Integer timeout, Integer wait, String name, String nameItem, String namespace, Integer readyReplicas) throws InterruptedException, IOException {
        boolean found = false;
        int i = 0;
        while (!found && i <= timeout) {
            try {
                Assert.assertEquals((commonspec.kubernetesClient.getReadyReplicasCustomResource(name, nameItem, namespace)).intValue(), readyReplicas.intValue(), "# Ready Replicas");
                found = true;
            } catch (AssertionError | Exception e) {
                getCommonSpec().getLogger().info("Expected replicas ready don't found after " + i + " seconds");
                if (i >= timeout) {
                    throw e;
                }
                Thread.sleep(wait * 1000);
            }
            i += wait;
        }
    }

    @When("^in less than '(\\d+)' seconds, checking each '(\\d+)' seconds, custom resource '(.+?)' with name '(.+?)' in namespace '(.+?)' has '(.+?)' global status( and description '(.+?)')?$")
    public void assertCustomResourceStatus(Integer timeout, Integer wait, String name, String nameItem, String namespace, String status, String description) throws InterruptedException, IOException {
        boolean found = false;
        int i = 0;
        while (!found && i <= timeout) {
            try {
                Assert.assertEquals((commonspec.kubernetesClient.getGlobalStatusCustomResource(name, nameItem, namespace)), status, "# Global Status");
                if (description != null) {
                    Assert.assertEquals((commonspec.kubernetesClient.getGlobalStatusDescriptionCustomResource(name, nameItem, namespace)), description, "# Global Status Description");
                }
                found = true;
            } catch (AssertionError | Exception e) {
                getCommonSpec().getLogger().info("Expected global status " + status + " don't found after " + i + " seconds");
                if (i >= timeout) {
                    throw e;
                }
                Thread.sleep(wait * 1000);
            }
            i += wait;
        }
    }

    @When("^I create deployment with name '(.+?)', in namespace '(.+?)', with image '(.+?)'( and image pull policy '(.+?)')?$")
    public void createDeployment(String name, String namespace, String image, String imagePullPolicy) {
        commonspec.kubernetesClient.createDeployment(name, namespace, image, imagePullPolicy);
    }

    @When("^I expose deployment with name '(.+?)', in namespace '(.+?)' in port '(\\d+)'$")
    public void createDeployment(String name, String namespace, Integer port) {
        commonspec.kubernetesClient.exposeDeployment(name, name, "ClusterIP", namespace, port, false);
    }

    @When("^I expose deployment with name '(.+?)', in namespace '(.+?)' in port '(\\d+)' with type '(ClusterIP|LoadBalancer)'( and service name '(.+?)')?( using CCT app selector)?$")
    public void createDeployment(String name, String namespace, Integer port, String type, String serviceName, String cctAppSelector) {
        commonspec.kubernetesClient.exposeDeployment(name, serviceName, type, namespace, port, cctAppSelector != null);
    }

    @When("^I get log from pod with name '(.+?)' in namespace '(.+?)'( and save it in environment variable '(.*?)')?( and save it in file '(.*?)')?$")
    public void getLogPod(String name, String namespace, String envVar, String fileName) throws Exception {
        if (envVar != null) {
            ThreadProperty.set(envVar, commonspec.kubernetesClient.getPodLog(name, namespace));
        }

        if (fileName != null) {
            writeInFile(commonspec.kubernetesClient.getPodLog(name, namespace), fileName);
        }
    }

    @When("^I execute '(.+?)' command in pod with name '(.+?)' in namespace '(.+?)'( in container '(.*?)')?( and save it in environment variable '(.+?)')?( and save it in file '(.*?)')?$")
    public void runCommandInPod(String command, String name, String namespace, String container, String envVar, String fileName) throws Exception {
        String result = commonspec.kubernetesClient.execCommand(name, namespace, container, command.split("\n| "));
        if (envVar != null) {
            ThreadProperty.set(envVar, result);
        }
        if (fileName != null) {
            writeInFile(result, fileName);
        }
    }

    @When("^I execute the command defined in datatable in pod with name '(.+?)' in namespace '(.+?)'( in container '(.*?)')?( and save it in environment variable '(.+?)')?( and save it in file '(.*?)')?:$")
    public void runCommandInPodDatatable(String name, String namespace, String container, String envVar, String fileName, DataTable dataTable) throws Exception {
        String result = commonspec.kubernetesClient.execCommand(name, namespace, container, dataTable.asList().toArray(new String[0]));
        if (envVar != null) {
            ThreadProperty.set(envVar, result);
        }
        if (fileName != null) {
            writeInFile(result, fileName);
        }
    }

    @When("^I apply configuration file located at '(.+?)' in namespace '(.+?)'$")
    public void applyConfiguration(String yamlFile, String namespace) throws FileNotFoundException {
        commonspec.kubernetesClient.createOrReplaceResource(yamlFile, namespace);
    }

    @When("^I apply configuration file located at '(.+?)', in namespace '(.+?)', using the following CustomResourceDefinition: version '(.+?)', plural '(.+?)', kind '(.+?)', name '(.+?)', scope '(.+?)', group '(.+?)'(, and return an exception that contains '(.+?)')?$")
    public void applyConfigurationCustomResourceDefinition(String yamlFile, String namespace, String version, String plural, String kind, String name, String scope, String group, String message) throws IOException {
        try {
            commonspec.kubernetesClient.createOrReplaceCustomResource(yamlFile, namespace, version, plural, kind, name, scope, group);
        } catch (AssertionError | Exception e) {
            if (message != null) {
                assertThat(e.getMessage()).contains(message);
            } else {
                throw e;
            }
        }
    }

    @When("^I get custom resource '(.+?)' in namespace '(.+?)' and save it in environment variable '(.+?)'$")
    public void getCustomResources(String name, String namespace, String envVar) throws IOException {
        ThreadProperty.set(envVar, commonspec.kubernetesClient.getCustomResource(name, namespace));
    }

    @Given("^in less than '(\\d+)' seconds, checking each '(\\d+)' seconds, log of pod '(.+?)' in namespace '(.+?)' contains '(.+?)'$")
    public void readLogsInLessEachFromPod(Integer timeout, Integer wait, String podName, String namespace, String expectedLog) throws InterruptedException {
        boolean found = false;
        int i = 0;
        String log = "";
        while (!found && i <= timeout) {
            try {
                log = commonspec.kubernetesClient.getPodLog(podName, namespace);
                if (log.contains(expectedLog)) {
                    found = true;
                } else {
                    getCommonSpec().getLogger().info("'" + expectedLog + "' don't found in log after " + i + " seconds");
                    Thread.sleep(wait * 1000);
                }
            } catch (Exception e) {
                getCommonSpec().getLogger().info("'" + expectedLog + "' don't found in log after " + i + " seconds");
                if (i >= timeout) {
                    throw e;
                }
                Thread.sleep(wait * 1000);
            }
            i += wait;
        }
        assertThat(log).contains(expectedLog);
    }

    @When("^I delete (pod|deployment|service|statefulset) with name '(.+?)' in namespace '(.+?)'$")
    public void deleteResource(String type, String name, String namespace) {
        switch (type) {
            case "pod":
                commonspec.kubernetesClient.deletePod(name, namespace);
                break;
            case "deployment":
                commonspec.kubernetesClient.deleteDeployment(name, namespace);
                break;
            case "service":
                commonspec.kubernetesClient.deleteService(name, namespace);
                break;
            case "statefulset":
                commonspec.kubernetesClient.deleteStatefulset(name, namespace);
                break;
            default:
        }
    }

    @When("^I delete persistentVolumeClaims with label filter '(.+?)' in namespace '(.+?)'$")
    public void deletePersistentVolumeClaims(String label, String namespace) throws Exception {
        commonspec.kubernetesClient.deletePersistentVolumeClaimsWithLabel(label, namespace);
    }

    @When("^I delete custom resource '(.+?)' with name '(.+?)' in namespace '(.+?)'$")
    public void deleteCustomResource(String name, String nameItem, String namespace) throws Exception {
        commonspec.kubernetesClient.deleteCustomResourceItem(name, nameItem, namespace);
    }


    @Given("^I scale deployment '(.+?)' in namespace '(.+?)' to '(\\d+)' instances")
    public void scaleK8s(String deployment, String namespace, Integer instances) {
        commonspec.kubernetesClient.scaleDeployment(deployment, namespace, instances);
    }

    @When("^I get (pods|deployments|replicasets|services|statefulsets|configmaps|serviceacounts|roles|rolebindings) using the following label filter '(.+?)'( in namespace '(.+?)')?( and save it in environment variable '(.*?)')?( and save it in file '(.*?)')?$")
    public void getPodsLabelSelector(String type, String selector, String namespace, String envVar, String fileName) throws Exception {
        String result = "";
        switch (type) {
            case "pods":
                result = commonspec.kubernetesClient.getPodsFilteredByLabel(selector, namespace);
                break;
            case "deployments":
                result = commonspec.kubernetesClient.getDeploymentsFilteredByLabel(selector, namespace);
                break;
            case "replicasets":
                result = commonspec.kubernetesClient.getReplicaSetsFilteredByLabel(selector, namespace);
                break;
            case "services":
                result = commonspec.kubernetesClient.getServicesFilteredByLabel(selector, namespace);
                break;
            case "statefulsets":
                result = commonspec.kubernetesClient.getStateFulSetsFilteredByLabel(selector, namespace);
                break;
            case "configmaps":
                result = commonspec.kubernetesClient.getConfigMapsFilteredByLabel(selector, namespace);
                break;
            case "serviceaccounts":
                result = commonspec.kubernetesClient.getServiceAccountsFilteredByLabel(selector, namespace);
                break;
            case "roles":
                result = commonspec.kubernetesClient.getRolesFilteredByLabel(selector, namespace);
                break;
            case "rolebindings":
                result = commonspec.kubernetesClient.getRolesBindingsFilteredByLabel(selector, namespace);
                break;
            default:
        }
        if (envVar != null) {
            ThreadProperty.set(envVar, result);
        }
        if (fileName != null) {
            writeInFile(result, fileName);
        }
    }

    @When("^I get pods using the following field filter '(.+?)'( in namespace '(.+?)')? and save it in environment variable '(.+?)'$")
    public void getPodsFieldSelector(String selector, String namespace, String envVar) {
        ThreadProperty.set(envVar, commonspec.kubernetesClient.getPodsFilteredByField(selector, namespace));
    }

    // TODO Remove in future (added to avoid compile errors in qa-unified-tests)
    public void setLocalPortForward(Integer containerPort, Integer localhostPort, String type, String name, String namespace) {
        setLocalPortForward(containerPort, localhostPort, type, name, namespace, null);
    }

    @Given("^I forward containerPort '(\\d+)' in localhostPort '(\\d+)' for (pod|service) '(.+?)'( in namespace '(.+?)')?( with id '(.+?)')?$")
    public void setLocalPortForward(Integer containerPort, Integer localhostPort, String type, String name, String namespace, String id) {
        id = id != null ? id : name + "_" + namespace + "_" + containerPort + "_" + localhostPort;
        switch (type) {
            case "pod":
                commonspec.kubernetesClient.setLocalPortForwardPod(namespace, name, containerPort, localhostPort, id);
                break;
            case "service":
                commonspec.kubernetesClient.setLocalPortForwardService(namespace, name, containerPort, localhostPort, id);
                break;
            default:
        }
    }

    // TODO Remove in future (added to avoid compile errors in qa-unified-tests)
    public void closePortForward() throws IOException {
        closePortForward(null);
    }

    @Then("^I close port forward( with id '(.+?)')?$")
    public void closePortForward(String id) throws IOException {
        commonspec.kubernetesClient.closePortForward(id);
    }

    @When("^I set maxReplicas='(\\d+)' in deployment with name '(.+?)' in namespace '(.+?)'$")
    public void setMaxReplicas(Integer maxReplicas, String name, String namespace) {
        commonspec.kubernetesClient.updateHorizontalAutoscaler(namespace, name, maxReplicas);
    }

    @When("^I set minReplicas='(\\d+)' and maxReplicas='(\\d+)' in deployment with name '(.+?)' in namespace '(.+?)'$")
    public void setMinMaxReplicas(Integer minReplicas, Integer maxReplicas, String name, String namespace) {
        commonspec.kubernetesClient.updateHorizontalAutoscaler(namespace, name, minReplicas, maxReplicas);
    }

    /**
     * kubectl cp /tmp/foo <some-namespace>/<some-pod>:/tmp/bar
     */
    @Given("^I outbound copy '(.+?)' to '(.+?)' in pod '(.+?)' in namespace '(.+?)'$")
    public void copyToRemoteFile(String localPath, String remotePath, String podName, String namespace)  {
        commonspec.kubernetesClient.copyFileToPod(podName, namespace, localPath, remotePath);
    }

    /**
     * kubectl cp <some-namespace>/<some-pod>:/tmp/foo /tmp/bar
     */
    @Given("^I inbound copy '(.+?)' to '(.+?)' in pod '(.+?)' in namespace '(.+?)'$")
    public void copyFromRemoteFile(String remotePath, String localPath, String podName, String namespace) {
        commonspec.kubernetesClient.copyFileFromPod(podName, namespace, remotePath, localPath);
    }

    @When("^I create namespace with name '(.+?)'( and labels '(.+?)')?$")
    public void createNamespace(String nsName, String labels) {
        Map<String, String> labelsMap = null;
        if (labels != null) {
            String[] labelsArray = labels.split(",");
            if (labelsArray.length > 0) {
                labelsMap = new HashMap<>();
                for (String l : labelsArray) {
                    labelsMap.put(l.split("=")[0], l.split("=")[1]);
                }
            }
        }
        commonspec.kubernetesClient.createNamespace(nsName, labelsMap);
    }

    @When("^I create service account with name '(.+?)' in namespace '(.+?)'$")
    public void createServiceAccount(String name, String namespace) {
        commonspec.kubernetesClient.createServiceAccount(name, namespace);
    }

    @When("^I create cluster role with name '(.+?)' in namespace '(.+?)' with resources '(.+?)' and verbs '(.+?)'( and api groups '(.+?)')?$")
    public void createClusterRole(String name, String namespace, String resources, String verbs, String apiGroups) {
        commonspec.kubernetesClient.createClusterRole(name, namespace, resources, verbs, apiGroups);
    }

    @When("^I create cluster role binding with name '(.+?)' in namespace '(.+?)' with ClusterRole '(.+?)' and ServiceAccount '(.+?)'$")
    public void createClusterRole(String name, String namespace, String clusterRole, String serviceAccount) {
        commonspec.kubernetesClient.createClusterRoleBinding(name, namespace, clusterRole, serviceAccount);
    }

    @When("^I add/modify variable '(.+?)' with value '(.+?)' in configmap with name '(.+?)' in namespace '(.+?)'$")
    public void addOrModifyConfigmap(String cmKey, String cmValue, String cmName, String namespace) {
        commonspec.kubernetesClient.addValueInConfigMap(cmName, namespace, cmKey, cmValue);
    }
}
