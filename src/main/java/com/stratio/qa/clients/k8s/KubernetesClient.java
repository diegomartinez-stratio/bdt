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

package com.stratio.qa.clients.k8s;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.stratio.qa.specs.CommandExecutionSpec;
import com.stratio.qa.specs.CommonG;
import com.stratio.qa.specs.FileSpec;
import com.stratio.qa.utils.ThreadProperty;
import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.api.model.apiextensions.v1beta1.CustomResourceDefinition;
import io.fabric8.kubernetes.api.model.apps.*;
import io.fabric8.kubernetes.api.model.extensions.Ingress;
import io.fabric8.kubernetes.api.model.rbac.*;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.LocalPortForward;
import io.fabric8.kubernetes.client.dsl.ExecListener;
import io.fabric8.kubernetes.client.dsl.ExecWatch;
import io.fabric8.kubernetes.client.dsl.base.CustomResourceDefinitionContext;
import io.fabric8.kubernetes.client.extended.run.RunConfigBuilder;
import io.fabric8.kubernetes.client.internal.SerializationUtils;
import okhttp3.Response;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

public class KubernetesClient {

    private static KubernetesClient CLIENT;

    private static io.fabric8.kubernetes.client.KubernetesClient k8sClient;

    private static LocalPortForward localPortForward;

    private static Map<String, LocalPortForward> localPortForwardMap = new HashMap<>();

    private static String localPortForwardId;

    private static CountDownLatch execLatch = new CountDownLatch(1);

    private static final Logger logger = LoggerFactory.getLogger(KubernetesClient.class);

    private KubernetesClient() {

    }

    public static KubernetesClient getInstance() {
        if (CLIENT == null) {
            CLIENT = new KubernetesClient();
        }
        return CLIENT;
    }

    public static Logger getLogger() {
        return logger;
    }

    public void connect(String kubeConfigPath) throws IOException {
        StringBuilder contentBuilder = new StringBuilder();
        try (Stream<String> stream = Files.lines(Paths.get(kubeConfigPath), StandardCharsets.UTF_8)) {
            stream.forEach(s -> contentBuilder.append(s).append("\n"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        k8sClient = new DefaultKubernetesClient(Config.fromKubeconfig(contentBuilder.toString()));
    }

    public void getK8sConfigFromWorkspace(CommonG commonspec) throws Exception {
        String clusterName = System.getProperty("KEOS_CLUSTER_ID");
        if (clusterName == null) {
            commonspec.getLogger().info("Info cannot be retrieved from workspace without KEOS_CLUSTER_ID variable");
            return;
        }

        boolean setKubernetesHost = false;

        if (System.getProperty("CLUSTER_KUBE_CONFIG_PATH") != null) {
            ThreadProperty.set("CLUSTER_KUBE_CONFIG_PATH", System.getProperty("CLUSTER_KUBE_CONFIG_PATH"));
            ThreadProperty.set("CLUSTER_SSH_USER", System.getProperty("CLUSTER_SSH_USER") != null ? System.getProperty("CLUSTER_SSH_USER") : "NotSet");
            ThreadProperty.set("CLUSTER_SSH_PEM_PATH", System.getProperty("CLUSTER_SSH_PEM_PATH") != null ? System.getProperty("CLUSTER_SSH_PEM_PATH") : "NotSet");
        } else {
            String daedalusSystem = "keos-workspaces.int.stratio.com";
            String workspaceName = "keos-workspace-" + clusterName;
            String workspaceURL = "http://" + daedalusSystem + "/" + workspaceName + ".tgz";

            // Download workspace
            String commandWget = "wget " + workspaceURL;
            commonspec.runLocalCommand(commandWget);

            // Untar workspace
            CommandExecutionSpec commandExecutionSpec = new CommandExecutionSpec(commonspec);
            String commandUntar = "tar -C target/test-classes/ -xvf " + workspaceName + ".tgz";
            commandExecutionSpec.executeLocalCommand(commandUntar, null, null);

            // Clean
            String commandRmTgz = "rm " + workspaceName + ".tgz";
            commandExecutionSpec.executeLocalCommand(commandRmTgz, null, null);

            // Obtain and export values
            if (!new File("target/test-classes/" + workspaceName + "/keos.json").exists()) {
                FileSpec fileSpec = new FileSpec(commonspec);
                fileSpec.convertYamlToJson(workspaceName + "/keos.yaml", workspaceName + "/keos.json");
            }
            String keosJson = commonspec.retrieveData(workspaceName + "/keos.json", "json");
            ThreadProperty.set("CLUSTER_SSH_USER", commonspec.getJSONPathString(keosJson, "$.infra.ssh_user", null).replaceAll("\\[", "").replaceAll("\\]", "").replaceAll("\"", ""));

            if (commonspec.getJSONPathString(keosJson, "$.keos.~", null).contains("auth")) {
                if (commonspec.getJSONPathString(keosJson, "$.keos.auth.~", null).contains("admin") && commonspec.getJSONPathString(keosJson, "$.keos.auth.admin.~", null).contains("vHost")) {
                    ThreadProperty.set("ADMIN_VHOST", commonspec.getJSONPathString(keosJson, "$.keos.auth.admin.vHost", null).replaceAll("\"", ""));
                }
                if (commonspec.getJSONPathString(keosJson, "$.keos.auth.~", null).contains("admin") && commonspec.getJSONPathString(keosJson, "$.keos.auth.admin.~", null).contains("basepath")) {
                    ThreadProperty.set("ADMIN_BASEPATH", commonspec.getJSONPathString(keosJson, "$.keos.auth.admin.basepath", null).replaceAll("\"", ""));
                }
                if (commonspec.getJSONPathString(keosJson, "$.keos.auth.~", null).contains("sis") && commonspec.getJSONPathString(keosJson, "$.keos.auth.sis.~", null).contains("vHost")) {
                    ThreadProperty.set("SIS_VHOST", commonspec.getJSONPathString(keosJson, "$.keos.auth.sis.vHost", null).replaceAll("\"", ""));
                }
                if (commonspec.getJSONPathString(keosJson, "$.keos.auth.~", null).contains("sis") && commonspec.getJSONPathString(keosJson, "$.keos.auth.sis.~", null).contains("basepath")) {
                    ThreadProperty.set("SIS_BASEPATH", commonspec.getJSONPathString(keosJson, "$.keos.auth.sis.basepath", null).replaceAll("\"", ""));
                }
            }

            ThreadProperty.set("CLUSTER_SSH_PEM_PATH", "./target/test-classes/" + workspaceName + "/key");
            ThreadProperty.set("CLUSTER_KUBE_CONFIG_PATH", "./target/test-classes/" + workspaceName + "/.kube/config");
            try {
                commonspec.getJSONPathString(keosJson, "$.keos.calico.service_loadbalancer_pools", null);
            } catch (Exception e) {
                setKubernetesHost = true;
            }
        }

        // Connect to Kubernetes
        getInstance().connect(ThreadProperty.get("CLUSTER_KUBE_CONFIG_PATH"));

        // Vault values
        getK8sVaultConfig(commonspec);

        // Get worker and set ingress hosts variables
        getK8sWorkerAndIngressHosts();

        // Save IP in /etc/hosts
        if (setKubernetesHost) {
            commonspec.getETCHOSTSManagementUtils().addK8sHost(ThreadProperty.get("WORKER_IP"), ThreadProperty.get("KEOS_SIS_HOST") + " " + ThreadProperty.get("KEOS_OAUTH2_PROXY_HOST"));
        }

        // Set variables from command-center-config configmap
        getK8sCCTConfig(commonspec);

        // Set gosec and cct paths
        getIngressPath();

        // Default values for some variables
        ThreadProperty.set("KEOS_PASSWORD", System.getProperty("KEOS_PASSWORD") != null ? System.getProperty("KEOS_PASSWORD") : "1234");
    }

    private void getK8sVaultConfig(CommonG commonspec) throws Exception {
        String vaultRoot = new JSONObject(commonspec.convertYamlStringToJson(getInstance().describeSecret("vault-unseal-keys", "keos-core"))).getJSONObject("data").getString("vault-root");
        commonspec.runLocalCommand("echo " + vaultRoot + " | base64 -d");
        commonspec.runCommandLoggerAndEnvVar(0, "VAULT_TOKEN", Boolean.TRUE);
        ThreadProperty.set("VAULT_HOST", "127.0.0.1");
        String serviceJson = commonspec.convertYamlStringToJson(getInstance().describeServiceYaml("vault", "keos-core"));
        ThreadProperty.set("VAULT_PORT", commonspec.getJSONPathString(serviceJson, "$.spec.ports[0].port", null).replaceAll("\\[", "").replaceAll("\\]", "").replaceAll("\"", ""));
    }

    private void getK8sWorkerAndIngressHosts() {
        // Get worker
        for (Node node : k8sClient.nodes().withLabelSelector(getLabelSelector("node-role.kubernetes.io/worker=")).list().getItems()) {
            // Check conditions ready
            boolean conditionsReady = false;
            for (NodeCondition nodeCondition : node.getStatus().getConditions()) {
                if (nodeCondition.getType().equals("Ready") && nodeCondition.getStatus().equals("True")) {
                    conditionsReady = true;
                    break;
                }
            }
            if (conditionsReady) {
                for (NodeAddress nodeAddress : node.getStatus().getAddresses()) {
                    if (nodeAddress.getType().equals("InternalIP")) {
                        ThreadProperty.set("WORKER_IP", nodeAddress.getAddress());
                    }
                }
            }
            if (ThreadProperty.get("WORKER_IP") != null) {
                break;
            }
        }

        // Get ingress hosts
        for (Ingress ingress : k8sClient.extensions().ingresses().inNamespace("keos-auth").list().getItems()) {
            String varName = null;
            switch (ingress.getMetadata().getName()) {
                case "sis":
                    varName = "KEOS_SIS_HOST";
                    break;
                case "oauth2-proxy":
                    varName = "KEOS_OAUTH2_PROXY_HOST";
                    break;
                default:
            }
            if (varName != null) {
                ThreadProperty.set(varName, ingress.getSpec().getRules().get(0).getHost());
            }
        }
    }

    private void getIngressPath() {
        ThreadProperty.set("KEOS_GOSEC_INGRESS_PATH", "/ui");
        Ingress gosecIngress = k8sClient.extensions().ingresses().inNamespace("keos-core").withName("gosec-management-ui").get();
        if (gosecIngress.getSpec().getRules().get(0).getHttp().toString().contains("path=/gosec")) {
            ThreadProperty.set("KEOS_GOSEC_INGRESS_PATH", "/gosec");
        }
        if (gosecIngress.getSpec().getRules().get(0).getHttp().toString().contains("path=/gosec/ui")) {
            ThreadProperty.set("KEOS_GOSEC_INGRESS_PATH", "/gosec/ui");
        }
        ThreadProperty.set("KEOS_CCT_INGRESS_PATH", "/cct");
        Ingress cctIngress = k8sClient.extensions().ingresses().inNamespace("keos-cct").withName("cct-ui").get();
        if (cctIngress.getSpec().getRules().get(0).getHttp().toString().contains("path=/cct-ui")) {
            ThreadProperty.set("KEOS_CCT_INGRESS_PATH", "/cct-ui");
        } else if (cctIngress.getSpec().getRules().get(0).getHttp().toString().contains("path=/cct/ui")) {
            ThreadProperty.set("KEOS_CCT_INGRESS_PATH", "/cct/ui");
        }
        ThreadProperty.set("KEOS_CCT_ORCHESTRATOR_INGRESS_PATH", "/cct-orchestrator-service");
        Ingress cctOrchestratorIngress = k8sClient.extensions().ingresses().inNamespace("keos-cct").withName("cct-orchestrator-service").get();
        if (cctOrchestratorIngress.getSpec().getRules().get(0).getHttp().toString().contains("path=/cct/cct-orchestrator-service")) {
            ThreadProperty.set("KEOS_CCT_ORCHESTRATOR_INGRESS_PATH", "/cct/cct-orchestrator-service");
        }
        ThreadProperty.set("KEOS_CCT_UNIVERSE_SERVICE_INGRESS_PATH", "/cct-universe-service");
        Ingress cctUniverseServiceIngress = k8sClient.extensions().ingresses().inNamespace("keos-cct").withName("cct-universe-service").get();
        if (cctUniverseServiceIngress.getSpec().getRules().get(0).getHttp().toString().contains("path=/cct/cct-universe-service")) {
            ThreadProperty.set("KEOS_CCT_UNIVERSE_SERVICE_INGRESS_PATH", "/cct/cct-universe-service");
        }
        ThreadProperty.set("KEOS_GOSEC_BAAS_INGRESS_PATH", "/baas");
        Ingress gosecBaasIngress = k8sClient.extensions().ingresses().inNamespace("keos-core").withName("gosec-management-baas").get();
        if (gosecBaasIngress != null && gosecBaasIngress.getSpec().getRules().get(0).getHttp().toString().contains("path=/gosec/baas")) {
            ThreadProperty.set("KEOS_GOSEC_BAAS_INGRESS_PATH", "/gosec/baas");
        }
    }

    private void getK8sCCTConfig(CommonG commonspec) {
        try {
            String centralConfigJson = getConfigMap("command-center-config", "keos-cct").getData().get("central-config.json");

            obtainJSONInfoAndExpose(commonspec, centralConfigJson, "$.admin_fqdn", "KEOS_FQDN", null);

            obtainJSONInfoAndExpose(commonspec, centralConfigJson, "$.eos.dockerRegistry", "DOCKER_REGISTRY", null);
            obtainJSONInfoAndExpose(commonspec, centralConfigJson, "$.eos.proxyAccessPointURL", "KEOS_ACCESS_POINT", null);

            obtainJSONInfoAndExpose(commonspec, centralConfigJson, "$.globals.sso.ssoTenantDefault", "KEOS_TENANT", null);

            obtainJSONInfoAndExpose(commonspec, centralConfigJson, "$.globals.kerberos.realm", "KEOS_REALM", null);
            obtainJSONInfoAndExpose(commonspec, centralConfigJson, "$.globals.kerberos.kdcHost", "KDC_HOST", null);
            obtainJSONInfoAndExpose(commonspec, centralConfigJson, "$.globals.kerberos.kdcPort", "KDC_PORT", null);
            obtainJSONInfoAndExpose(commonspec, centralConfigJson, "$.globals.kerberos.kadminHost", "KADMIN_HOST", null);
            obtainJSONInfoAndExpose(commonspec, centralConfigJson, "$.globals.kerberos.kadminPort", "KADMIN_PORT", null);

            obtainJSONInfoAndExpose(commonspec, centralConfigJson, "$.globals.ldap.adminUserUuid", "KEOS_USER", null);
            obtainJSONInfoAndExpose(commonspec, centralConfigJson, "$.globals.ldap.url", "LDAP_URL", null);
            obtainJSONInfoAndExpose(commonspec, centralConfigJson, "$.globals.ldap.port", "LDAP_PORT", null);
            obtainJSONInfoAndExpose(commonspec, centralConfigJson, "$.globals.ldap.userDn", "LDAP_USER_DN", null);
            obtainJSONInfoAndExpose(commonspec, centralConfigJson, "$.globals.ldap.groupDN", "LDAP_GROUP_DN", null);
            obtainJSONInfoAndExpose(commonspec, centralConfigJson, "$.globals.ldap.ldapBase", "LDAP_BASE", null);
            obtainJSONInfoAndExpose(commonspec, centralConfigJson, "$.globals.ldap.adminrouterAuthorizedGroup", "LDAP_ADMIN_GROUP", null);

            obtainJSONInfoAndExpose(commonspec, centralConfigJson, "$.globals.vault.vaultHost", "KEOS_VAULT_HOST_INTERNAL", null);

            // TODO ARTIFACT_REPOSITORY doesn't appear in configmap, set default value
            ThreadProperty.set("ARTIFACT_REPOSITORY", System.getProperty("ARTIFACT_REPOSITORY") != null ? System.getProperty("ARTIFACT_REPOSITORY") : "http://qa.int.stratio.com/repository");
        } catch (Exception e) {
            commonspec.getLogger().error("Error reading command center config", e);
        }
    }

    /**
     * Obtain info from json and expose in thread variable
     *
     * @param json         : json to look for info in
     * @param jqExpression : jq expression to obtain specific info from json
     * @param envVar       : thread variable where to expose value
     * @param position     : position in value obtained with jq
     */
    public void obtainJSONInfoAndExpose(CommonG commonspec, String json, String jqExpression, String envVar, String position) {
        String value = commonspec.getJSONPathString(json, jqExpression, position).replaceAll("\\[", "").replaceAll("\\]", "").replaceAll("\"", "");
        ThreadProperty.set(envVar, value);
    }

    /**
     * kubectl get pods -n namespace
     *
     * @param namespace
     */
    public String getNamespacePods(String namespace) {
        StringBuilder result = new StringBuilder();
        PodList podList = namespace != null ? k8sClient.pods().inNamespace(namespace).list() : k8sClient.pods().inAnyNamespace().list();
        for (Pod pod : podList.getItems()) {
            result.append(pod.getMetadata().getName()).append("\n");
        }
        return result.length() > 0 ? result.substring(0, result.length() - 1) : result.toString();
    }

    /**
     * kubectl get namespaces
     */
    public String getAllNamespaces() {
        StringBuilder result = new StringBuilder();
        for (Namespace namespace : k8sClient.namespaces().list().getItems()) {
            result.append(namespace.getMetadata().getName()).append("\n");
        }
        return result.length() > 0 ? result.substring(0, result.length() - 1) : result.toString();
    }

    /**
     * kubectl get events -n namespace
     *
     * @param namespace
     */
    public Boolean checkEventNamespace(String not, String namespace, String type, String name, String reason, String message) {
        EventList eventList = k8sClient.v1().events().inNamespace(namespace).list();
        for (Event event : eventList.getItems()) {
            if ((not == null && event.getMessage().contains(message)) || (not != null && !event.getMessage().contains(message))) {
                if (!((reason != null && !event.getReason().equals(reason)) || (type != null && !event.getInvolvedObject().getKind().equals(type)) || (name != null && !event.getInvolvedObject().getName().equals(name)))) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Return pod object
     *
     * @param podName   Pod name
     * @param namespace Namespace
     * @return Pod object
     */
    public Pod getPod(String podName, String namespace) {
        return namespace != null ? k8sClient.pods().inNamespace(namespace).withName(podName).get() : k8sClient.pods().withName(podName).get();
    }

    /**
     * Return deployment object
     *
     * @param deploymentName Deployment name
     * @param namespace      Namespace
     * @return Deployment object
     */
    public Deployment getDeployment(String deploymentName, String namespace) {
        return k8sClient.apps().deployments().inNamespace(namespace).withName(deploymentName).get();
    }

    /**
     * Return pvc object
     *
     * @param pvcName   Pvc name
     * @param namespace Namespace
     * @return Pvc object
     */
    public PersistentVolumeClaim getPersistentVolumeClaims(String pvcName, String namespace) {
        return namespace != null ? k8sClient.persistentVolumeClaims().inNamespace(namespace).withName(pvcName).get() : k8sClient.persistentVolumeClaims().withName(pvcName).get();
    }

    /**
     * kubectl describe pod
     *
     * @param podName   Pod name
     * @param namespace Namespace (optional)
     * @return String with pod yaml
     * @throws JsonProcessingException
     */
    public String describePodYaml(String podName, String namespace) throws JsonProcessingException {
        return SerializationUtils.dumpAsYaml(getPod(podName, namespace));
    }

    /**
     * kubectl describe service myservice
     *
     * @param serviceName Service
     * @param namespace   Namespace
     * @return String with service yaml
     */
    public String describeServiceYaml(String serviceName, String namespace) throws Exception {
        if (namespace == null) {
            throw new Exception("Namespace is mandatory");
        }
        return SerializationUtils.dumpAsYaml(k8sClient.services().inNamespace(namespace).withName(serviceName).get());
    }

    /**
     * kubectl describe deployment myDeployment
     *
     * @param deploymentName Service
     * @param namespace      Namespace
     * @return String with service yaml
     */
    public String describeDeploymentYaml(String deploymentName, String namespace) throws Exception {
        return SerializationUtils.dumpAsYaml(getDeployment(deploymentName, namespace));
    }

    /**
     * kubectl describe pgcluster xxx -n xxxx
     *
     * @param name      customresourcedefinition name (ex:pgclusters.postgres.stratio.com)
     * @param nameItem  pgcluster name
     * @param namespace Namespace
     * @return String with custom resource in json format
     */
    public String describeCustomResourceJson(String name, String nameItem, String namespace) throws JsonProcessingException {

        CustomResourceDefinition crd = k8sClient.customResourceDefinitions().withName(name).get();
        CustomResourceDefinitionContext crdContext = CustomResourceDefinitionContext.fromCrd(crd);

        Map<String, Object> list = k8sClient.customResource(crdContext).list(namespace);
        List<Map<String, Object>> items = (List<Map<String, Object>>) list.get("items");
        for (Map<String, Object> customResource : items) {
            Map<String, Object> metadata = (Map<String, Object>) customResource.get("metadata");
            if (metadata.get("name").equals(nameItem)) {
                return new JSONObject(customResource).toString();
            }
        }
        return null;
    }

    /**
     * kubectl describe pvc
     *
     * @param pvcName   PersistentVolumeClaims name
     * @param namespace Namespace (optional)
     * @return String with pvc yaml
     * @throws JsonProcessingException
     */
    public String describePersistentVolumeClaims(String pvcName, String namespace) throws JsonProcessingException {
        return SerializationUtils.dumpAsYaml(getPersistentVolumeClaims(pvcName, namespace));
    }

    /**
     * kubectl apply -f yamlOrJsonFile.yml
     *
     * @param file
     * @param namespace
     * @throws FileNotFoundException
     */
    public void createOrReplaceResource(String file, String namespace) throws FileNotFoundException {
        k8sClient.load(new FileInputStream(file))
                .inNamespace(namespace)
                .createOrReplace();
    }

    /**
     * kubectl apply -f yamlOrJsonFile.yml
     * Using a custom resource
     *
     * @param file
     * @param namespace
     * @throws FileNotFoundException
     */
    public void createOrReplaceCustomResource(String file, String namespace, String version, String plural, String kind, String name, String scope, String group) throws IOException {
        CustomResourceDefinitionContext customResourceDefinitionContext = new CustomResourceDefinitionContext.Builder()
                .withVersion(version)
                .withPlural(plural)
                .withKind(kind)
                .withName(name)
                .withScope(scope)
                .withGroup(group)
                .build();
        Map<String, Object> myObject = k8sClient.customResource(customResourceDefinitionContext).load(new FileInputStream(file));
        k8sClient.customResource(customResourceDefinitionContext).createOrReplace(namespace, myObject);
    }

    /**
     * kubectl get pgcluster -n xxxxx
     * Using a custom resource
     *
     * @param name      customresourcedefinition name (ex:pgclusters.postgres.stratio.com)
     * @param namespace
     */
    public String getCustomResource(String name, String namespace) throws IOException {
        CustomResourceDefinition crd = k8sClient.customResourceDefinitions().withName(name).get();
        CustomResourceDefinitionContext crdContext = CustomResourceDefinitionContext.fromCrd(crd);

        Map<String, Object> list = k8sClient.customResource(crdContext).list(namespace);
        List<Map<String, Object>> items = (List<Map<String, Object>>) list.get("items");
        StringBuilder result = new StringBuilder();
        for (Map<String, Object> customResource : items) {
            Map<String, Object> metadata = (Map<String, Object>) customResource.get("metadata");
            result.append(metadata.get("name")).append("\n");
        }
        return result.length() > 0 ? result.substring(0, result.length() - 1) : result.toString();
    }

    /**
     * Using a custom resource
     *
     * @param name      customresourcedefinition name (ex:pgclusters.postgres.stratio.com)
     * @param nameItem  pgcluster name
     * @param namespace Namespace
     * @return replicas number is ready
     */
    public Integer getReadyReplicasCustomResource(String name, String nameItem, String namespace) throws IOException {
        CustomResourceDefinition crd = k8sClient.customResourceDefinitions().withName(name).get();
        CustomResourceDefinitionContext crdContext = CustomResourceDefinitionContext.fromCrd(crd);

        Map<String, Object> list = k8sClient.customResource(crdContext).list(namespace);
        List<Map<String, Object>> items = (List<Map<String, Object>>) list.get("items");
        StringBuilder result = new StringBuilder();
        Integer replicas = 0;
        for (Map<String, Object> customResource : items) {
            Map<String, Object> metadata = (Map<String, Object>) customResource.get("metadata");
            Map<String, Object> status = (Map<String, Object>) customResource.get("status");
            if (metadata.get("name").equals(nameItem)) {
                return Integer.valueOf(result.append(status.get("readyInstances")).toString().split("/")[0]);
            }
        }
        return replicas;
    }

    /**
     * Using a custom resource
     *
     * @param name      customresourcedefinition name (ex:pgclusters.postgres.stratio.com)
     * @param nameItem  pgcluster name
     * @param namespace Namespace
     * @return global status
     */
    public String getGlobalStatusCustomResource(String name, String nameItem, String namespace) throws IOException {
        CustomResourceDefinition crd = k8sClient.customResourceDefinitions().withName(name).get();
        CustomResourceDefinitionContext crdContext = CustomResourceDefinitionContext.fromCrd(crd);

        Map<String, Object> list = k8sClient.customResource(crdContext).list(namespace);
        List<Map<String, Object>> items = (List<Map<String, Object>>) list.get("items");
        StringBuilder result = new StringBuilder();
        for (Map<String, Object> customResource : items) {
            Map<String, Object> metadata = (Map<String, Object>) customResource.get("metadata");
            Map<String, Object> status = (Map<String, Object>) customResource.get("status");
            Map<String, Object> globalStatus = (Map<String, Object>) status.get("globalStatus");
            if (metadata.get("name").equals(nameItem)) {
                return result.append(globalStatus.get("status")).toString();
            }
        }
        return result.toString();
    }

    /**
     * Using a custom resource
     *
     * @param name      customresourcedefinition name (ex:pgclusters.postgres.stratio.com)
     * @param nameItem  pgcluster name
     * @param namespace Namespace
     * @return global status description
     */
    public String getGlobalStatusDescriptionCustomResource(String name, String nameItem, String namespace) throws IOException {
        CustomResourceDefinition crd = k8sClient.customResourceDefinitions().withName(name).get();
        CustomResourceDefinitionContext crdContext = CustomResourceDefinitionContext.fromCrd(crd);

        Map<String, Object> list = k8sClient.customResource(crdContext).list(namespace);
        List<Map<String, Object>> items = (List<Map<String, Object>>) list.get("items");
        StringBuilder result = new StringBuilder();
        for (Map<String, Object> customResource : items) {
            Map<String, Object> metadata = (Map<String, Object>) customResource.get("metadata");
            Map<String, Object> status = (Map<String, Object>) customResource.get("status");
            Map<String, Object> globalStatus = (Map<String, Object>) status.get("globalStatus");
            if (metadata.get("name").equals(nameItem)) {
                return result.append(globalStatus.get("description")).toString();
            }
        }
        return result.toString();
    }

    /**
     * kubectl create deployment xxx -n namespace --image myimage
     *
     * @param deploymentName  Deployment name
     * @param namespace       Namespace
     * @param image           Image
     * @param imagePullPolicy Image pull policy (IfNotPresent as default value)
     */
    public void createDeployment(String deploymentName, String namespace, String image, String imagePullPolicy) {
        imagePullPolicy = imagePullPolicy != null ? imagePullPolicy : "IfNotPresent";
        Deployment deployment = new DeploymentBuilder()
                .withNewMetadata()
                .withName(deploymentName)
                .endMetadata()
                .withNewSpec()
                .withReplicas(1)
                .withNewTemplate()
                .withNewMetadata()
                .addToLabels("app", deploymentName)
                .endMetadata()
                .withNewSpec()
                .addNewContainer()
                .withName(deploymentName)
                .withImage(image)
                .withImagePullPolicy(imagePullPolicy)
                .endContainer()
                .endSpec()
                .endTemplate()
                .withNewSelector()
                .addToMatchLabels("app", deploymentName)
                .endSelector()
                .endSpec()
                .build();
        k8sClient.apps().deployments().inNamespace(namespace).create(deployment);
    }

    /**
     * kubectl expose deployment xxx -n namespace --port=xxxx
     *
     * @param deploymentName Deployment to expose
     * @param namespace      Namespace
     * @param port           Port to expose
     */
    public void exposeDeployment(String deploymentName, String serviceName, String type, String namespace, Integer port, Boolean cctApp) {
        Service service = new ServiceBuilder()
                .withNewMetadata()
                .withName(serviceName != null ? serviceName : deploymentName)
                .endMetadata()
                .withNewSpec()
                .withSelector(Collections.singletonMap(cctApp ? "cct.stratio.com/application_id" : "app", deploymentName))
                .addNewPort()
                .withProtocol("TCP")
                .withPort(port)
                .withTargetPort(new IntOrString(port))
                .endPort()
                .withType(type)
                .endSpec()
                .build();
        k8sClient.services().inNamespace(namespace).create(service);
    }

    /**
     * kubectl logs pod
     *
     * @param pod       Pod name
     * @param namespace Namespace
     * @return pod log
     */
    public String getPodLog(String pod, String namespace) {
        return k8sClient.pods().inNamespace(namespace).withName(pod).getLog();
    }

    /**
     * kubectl exec mypod -- command
     *
     * @param pod       Pod
     * @param namespace Namespace
     * @param command   Command to execute
     * @param container Container of Pod
     * @throws InterruptedException
     */
    public String execCommand(String pod, String namespace, String container, String[] command) throws InterruptedException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayOutputStream error = new ByteArrayOutputStream();
        execLatch = new CountDownLatch(1);
        ExecWatch execWatch;
        if (container == null) {
            execWatch = k8sClient.pods().inNamespace(namespace).withName(pod)
                    .writingOutput(out)
                    .writingError(error)
                    .usingListener(new MyPodExecListener())
                    .exec(command);

        } else {
            execWatch = k8sClient.pods().inNamespace(namespace).withName(pod)
                    .inContainer(container)
                    .writingOutput(out)
                    .writingError(error)
                    .usingListener(new MyPodExecListener())
                    .exec(command);

        }
        boolean latchTerminationStatus = execLatch.await(30, TimeUnit.SECONDS);
        if (!latchTerminationStatus) {
            logger.warn("Latch could not terminate within specified time");
        }
        logger.debug("Exec Output: {} ", out);
        execWatch.close();
        return out.toString();
    }

    /**
     * kubectl run xxx --image=xxx --restart=xxx --serviceaccount=xxx --namespace=xxx --command -- mycommand
     *
     * @param podName         Pod name
     * @param namespace       Namespace
     * @param image           Image
     * @param imagePullPolicy Image pull policy (IfNotPresent as default value)
     * @param restartPolicy   Restart policy
     * @param serviceAccount  Service Account
     * @param command         Command to execute
     * @param args            Command arguments
     */
    public void runPod(String podName, String namespace, String image, String imagePullPolicy, String restartPolicy, String serviceAccount, String command, List<String> args) {
        imagePullPolicy = imagePullPolicy != null ? imagePullPolicy : "IfNotPresent";
        RunConfigBuilder runConfig = new RunConfigBuilder()
                .withName(podName)
                .withImage(image)
                .withImagePullPolicy(imagePullPolicy)
                .withRestartPolicy(restartPolicy)
                .withServiceAccount(serviceAccount)
                .withCommand(command)
                .withArgs(args);
        k8sClient.run().inNamespace(namespace).withName(podName).withImage(image).withRunConfig(runConfig.build()).done();
    }

    /**
     * kubectl run xxx --image=xxx --restart=xxx --serviceaccount=xxx --namespace=xxx --command -- mycommand
     *
     * @param podName         Pod name
     * @param namespace       Namespace
     * @param image           Image
     * @param imagePullPolicy Image pull policy (IfNotPresent as default value)
     * @param restartPolicy   Restart policy
     * @param serviceAccount  Service Account
     * @param env             Environment variables
     * @param command         Command to execute
     * @param args            Command arguments
     */
    public void runPod(String podName, String namespace, String image, String imagePullPolicy, String restartPolicy, String serviceAccount, Map<String, String> env, String command, List<String> args) {
        imagePullPolicy = imagePullPolicy != null ? imagePullPolicy : "IfNotPresent";
        RunConfigBuilder runConfig = new RunConfigBuilder()
                .withName(podName)
                .withImage(image)
                .withImagePullPolicy(imagePullPolicy)
                .withRestartPolicy(restartPolicy)
                .withServiceAccount(serviceAccount)
                .withCommand(command)
                .withEnv(env)
                .withArgs(args);
        k8sClient.run().inNamespace(namespace).withName(podName).withImage(image).withRunConfig(runConfig.build()).done();
    }

    /**
     * kubectl delete pod mypod
     *
     * @param pod       Pod to delete
     * @param namespace Namespace
     */
    public void deletePod(String pod, String namespace) {
        k8sClient.pods().inNamespace(namespace).withName(pod).delete();
    }

    /**
     * kubectl delete deployment mydeployment
     *
     * @param deployment Deployment to delete
     * @param namespace  Namespace
     */
    public void deleteDeployment(String deployment, String namespace) {
        k8sClient.apps().deployments().inNamespace(namespace).withName(deployment).delete();
    }

    /**
     * kubectl delete service myservice
     *
     * @param service   Service to delete
     * @param namespace Namespace
     */
    public void deleteService(String service, String namespace) {
        k8sClient.services().inNamespace(namespace).withName(service).delete();
    }

    /**
     * kubectl delete statefulset mystatefulset
     *
     * @param statefulset   Statefulset to delete
     * @param namespace Namespace
     */
    public void deleteStatefulset(String statefulset, String namespace) {
        k8sClient.apps().statefulSets().inNamespace(namespace).withName(statefulset).delete();
    }

    /**
     * kubectl delete service myservice
     *
     * @param label     label filter witch has persistent volume claims to delete
     * @param namespace Namespace
     */
    public void deletePersistentVolumeClaimsWithLabel(String label, String namespace) {
        k8sClient.persistentVolumeClaims().inNamespace(namespace).withLabel(label).delete();
    }

    /**
     * kubectl delete pgcluster mypgcluster
     * Using a custom resource
     *
     * @param name      customresourcedefinition name (ex:pgclusters.postgres.stratio.com)
     * @param nameItem  pgcluster name
     * @param namespace Namespace
     */
    public void deleteCustomResourceItem(String name, String nameItem, String namespace) throws IOException {
        CustomResourceDefinition crd = k8sClient.customResourceDefinitions().withName(name).get();
        CustomResourceDefinitionContext crdContext = CustomResourceDefinitionContext.fromCrd(crd);
        k8sClient.customResource(crdContext).delete(namespace, nameItem);
    }

    /**
     * kubectl scale --replicas=4 -n namespace deploy/xxx
     *
     * @param deployment deployment to scale
     * @param namespace  Namespace
     */
    public void scaleDeployment(String deployment, String namespace, Integer instances) {
        k8sClient.apps().deployments().inNamespace(namespace).withName(deployment).scale(instances);
    }

    /**
     * private function which return label selector
     *
     * @param selector Label filter (separated by comma)
     * @return LabelSelector
     */
    private LabelSelector getLabelSelector(String selector) {
        String[] arraySelector = selector.split(",");
        LabelSelector labelSelector = new LabelSelector();
        Map<String, String> expressions = new HashMap<>();
        for (String sel : arraySelector) {
            String labelKey = sel.split("=")[0];
            String labelValue = "";
            if (sel.split("=").length > 1) {
                labelValue = sel.split("=")[1];
            }
            expressions.put(labelKey, labelValue);
        }
        labelSelector.setMatchLabels(expressions);
        return labelSelector;
    }

    /**
     * kubectl get pods --selector=version=v1 -o jsonpath='{.items[*].metadata.name}'
     *
     * @param selector  Label filter (separated by comma)
     * @param namespace Namespace
     * @return Pods list filtered
     */
    public String getPodsFilteredByLabel(String selector, String namespace) {
        LabelSelector labelSelector;
        labelSelector = getLabelSelector(selector);
        StringBuilder result = new StringBuilder();
        PodList podList = namespace != null ?
                k8sClient.pods().inNamespace(namespace).withLabelSelector(labelSelector).list() :
                k8sClient.pods().inAnyNamespace().withLabelSelector(labelSelector).list();
        for (Pod pod : podList.getItems()) {
            result.append(pod.getMetadata().getName()).append("\n");
        }
        return result.length() > 0 ? result.substring(0, result.length() - 1) : result.toString();
    }

    /**
     * kubectl get deployments --selector=version=v1 -o jsonpath='{.items[*].metadata.name}'
     *
     * @param selector  Label filter (separated by comma)
     * @param namespace Namespace
     * @return Deployments list filtered
     */
    public String getDeploymentsFilteredByLabel(String selector, String namespace) {
        LabelSelector labelSelector;
        labelSelector = getLabelSelector(selector);
        StringBuilder result = new StringBuilder();
        DeploymentList deploymentList = namespace != null ?
                k8sClient.apps().deployments().inNamespace(namespace).withLabelSelector(labelSelector).list() :
                k8sClient.apps().deployments().inAnyNamespace().withLabelSelector(labelSelector).list();
        for (Deployment deployment : deploymentList.getItems()) {
            result.append(deployment.getMetadata().getName()).append("\n");
        }
        return result.length() > 0 ? result.substring(0, result.length() - 1) : result.toString();
    }

    /**
     * kubectl get replicasets --selector=version=v1 -o jsonpath='{.items[*].metadata.name}'
     *
     * @param selector  Label filter (separated by comma)
     * @param namespace Namespace
     * @return Replicasets list filtered
     */
    public String getReplicaSetsFilteredByLabel(String selector, String namespace) {
        LabelSelector labelSelector;
        labelSelector = getLabelSelector(selector);
        StringBuilder result = new StringBuilder();
        ReplicaSetList replicasetList = namespace != null ?
                k8sClient.apps().replicaSets().inNamespace(namespace).withLabelSelector(labelSelector).list() :
                k8sClient.apps().replicaSets().inAnyNamespace().withLabelSelector(labelSelector).list();
        for (ReplicaSet replicaset : replicasetList.getItems()) {
            result.append(replicaset.getMetadata().getName()).append("\n");
        }
        return result.length() > 0 ? result.substring(0, result.length() - 1) : result.toString();
    }

    /**
     * kubectl get services --selector=version=v1 -o jsonpath='{.items[*].metadata.name}'
     *
     * @param selector  Label filter (separated by comma)
     * @param namespace Namespace
     * @return services list filtered
     */
    public String getServicesFilteredByLabel(String selector, String namespace) {
        LabelSelector labelSelector;
        labelSelector = getLabelSelector(selector);
        StringBuilder result = new StringBuilder();
        ServiceList serviceList = namespace != null ?
                k8sClient.services().inNamespace(namespace).withLabelSelector(labelSelector).list() :
                k8sClient.services().inAnyNamespace().withLabelSelector(labelSelector).list();
        for (Service service : serviceList.getItems()) {
            result.append(service.getMetadata().getName()).append("\n");
        }
        return result.length() > 0 ? result.substring(0, result.length() - 1) : result.toString();
    }

    /**
     * kubectl get statefulsets --selector=version=v1 -o jsonpath='{.items[*].metadata.name}'
     *
     * @param selector  Label filter (separated by comma)
     * @param namespace Namespace
     * @return StateFulSets list filtered
     */
    public String getStateFulSetsFilteredByLabel(String selector, String namespace) {
        LabelSelector labelSelector;
        labelSelector = getLabelSelector(selector);
        StringBuilder result = new StringBuilder();
        StatefulSetList statefulSetList = namespace != null ?
                k8sClient.apps().statefulSets().inNamespace(namespace).withLabelSelector(labelSelector).list() :
                k8sClient.apps().statefulSets().inAnyNamespace().withLabelSelector(labelSelector).list();
        for (StatefulSet statefulSet : statefulSetList.getItems()) {
            result.append(statefulSet.getMetadata().getName()).append("\n");
        }
        return result.length() > 0 ? result.substring(0, result.length() - 1) : result.toString();
    }

    /**
     * kubectl get configmaps --selector=version=v1 -o jsonpath='{.items[*].metadata.name}'
     *
     * @param selector  Label filter (separated by comma)
     * @param namespace Namespace
     * @return ConfigMaps list filtered
     */
    public String getConfigMapsFilteredByLabel(String selector, String namespace) {
        LabelSelector labelSelector;
        labelSelector = getLabelSelector(selector);
        StringBuilder result = new StringBuilder();
        ConfigMapList configMapList = namespace != null ?
                k8sClient.configMaps().inNamespace(namespace).withLabelSelector(labelSelector).list() :
                k8sClient.configMaps().inAnyNamespace().withLabelSelector(labelSelector).list();
        for (ConfigMap configMap : configMapList.getItems()) {
            result.append(configMap.getMetadata().getName()).append("\n");
        }
        return result.length() > 0 ? result.substring(0, result.length() - 1) : result.toString();
    }

    /**
     * kubectl get serviceaccounts --selector=version=v1 -o jsonpath='{.items[*].metadata.name}'
     *
     * @param selector  Label filter (separated by comma)
     * @param namespace Namespace
     * @return ServiceAccounts list filtered
     */
    public String getServiceAccountsFilteredByLabel(String selector, String namespace) {
        LabelSelector labelSelector;
        labelSelector = getLabelSelector(selector);
        StringBuilder result = new StringBuilder();
        ServiceAccountList serviceAccountList = namespace != null ?
                k8sClient.serviceAccounts().inNamespace(namespace).withLabelSelector(labelSelector).list() :
                k8sClient.serviceAccounts().inAnyNamespace().withLabelSelector(labelSelector).list();
        for (ServiceAccount serviceAccount : serviceAccountList.getItems()) {
            result.append(serviceAccount.getMetadata().getName()).append("\n");
        }
        return result.length() > 0 ? result.substring(0, result.length() - 1) : result.toString();
    }

    /**
     * kubectl get roles --selector=version=v1 -o jsonpath='{.items[*].metadata.name}'
     *
     * @param selector  Label filter (separated by comma)
     * @param namespace Namespace
     * @return Roles list filtered
     */
    public String getRolesFilteredByLabel(String selector, String namespace) {
        LabelSelector labelSelector;
        labelSelector = getLabelSelector(selector);
        StringBuilder result = new StringBuilder();
        RoleList roleList = namespace != null ?
                k8sClient.rbac().roles().inNamespace(namespace).withLabelSelector(labelSelector).list() :
                k8sClient.rbac().roles().inAnyNamespace().withLabelSelector(labelSelector).list();
        for (Role role : roleList.getItems()) {
            result.append(role.getMetadata().getName()).append("\n");
        }
        return result.length() > 0 ? result.substring(0, result.length() - 1) : result.toString();
    }

    /**
     * kubectl get rolebindings --selector=version=v1 -o jsonpath='{.items[*].metadata.name}'
     *
     * @param selector  Label filter (separated by comma)
     * @param namespace Namespace
     * @return Role list filtered
     */
    public String getRolesBindingsFilteredByLabel(String selector, String namespace) {
        LabelSelector labelSelector;
        labelSelector = getLabelSelector(selector);
        StringBuilder result = new StringBuilder();
        RoleBindingList roleBindingList = namespace != null ?
                k8sClient.rbac().roleBindings().inNamespace(namespace).withLabelSelector(labelSelector).list() :
                k8sClient.rbac().roleBindings().inAnyNamespace().withLabelSelector(labelSelector).list();
        for (RoleBinding roleBinding : roleBindingList.getItems()) {
            result.append(roleBinding.getMetadata().getName()).append("\n");
        }
        return result.length() > 0 ? result.substring(0, result.length() - 1) : result.toString();
    }

    /**
     * kubectl get pods --field-selector=status.phase=Running
     *
     * @param selector  Label filter (separated by comma)
     * @param namespace Namespace
     * @return Pods list filtered
     */
    public String getPodsFilteredByField(String selector, String namespace) {
        String[] arraySelector = selector.split(",");
        Map<String, String> fields = new HashMap<>();
        for (String sel : arraySelector) {
            String fieldKey = sel.split("=")[0];
            String fieldValue = sel.split("=")[1];
            fields.put(fieldKey, fieldValue);
        }
        StringBuilder result = new StringBuilder();
        PodList podList = namespace != null ?
                k8sClient.pods().inNamespace(namespace).withFields(fields).list() :
                k8sClient.pods().inAnyNamespace().withFields(fields).list();
        for (Pod pod : podList.getItems()) {
            result.append(pod.getMetadata().getName()).append("\n");
        }
        return result.length() > 0 ? result.substring(0, result.length() - 1) : result.toString();
    }

    /**
     * Get a configmap
     *
     * @param name      Config map name
     * @param namespace Namespace
     */
    public ConfigMap getConfigMap(String name, String namespace) {
        return k8sClient.configMaps().inNamespace(namespace).withName(name).get();
    }

    /**
     * Get configmap list in selected namespace
     *
     * @param namespace Namespace
     * @return Configmap list
     */
    public String getConfigMapList(String namespace) {
        StringBuilder result = new StringBuilder();
        for (ConfigMap configMap : k8sClient.configMaps().inNamespace(namespace).list().getItems()) {
            result.append(configMap.getMetadata().getName()).append("\n");
        }
        return result.length() > 0 ? result.substring(0, result.length() - 1) : result.toString();
    }

    /**
     * kubectl describe configmap xxx -n namespace
     *
     * @param configMapName Config map name
     * @param namespace     Namespace
     * @return String with config map
     */
    public String describeConfigMap(String configMapName, String namespace) {
        return getConfigMap(configMapName, namespace).getData().toString();
    }

    /**
     * Add or modify value in configmap
     *
     * @param configMapName Configmap name
     * @param namespace Namespace
     * @param key Key
     * @param value Value
     */
    public void addValueInConfigMap(String configMapName, String namespace, String key, String value) {
        Map<String, String> configMapData = getConfigMap(configMapName, namespace).getData();
        configMapData.put(key, value);
        ConfigMap configMapAux = getConfigMap(configMapName, namespace);
        configMapAux.setData(configMapData);
        k8sClient.configMaps().inNamespace(namespace).withName(configMapName).replace(configMapAux);
    }

    /**
     * Get a replicaset
     *
     * @param name      Replicaset name
     * @param namespace Namespace
     */
    public ReplicaSet getReplicaSet(String name, String namespace) {
        return k8sClient.apps().replicaSets().inNamespace(namespace).withName(name).get();
    }

    /**
     * Get replicaset list in selected namespace
     *
     * @param namespace Namespace
     * @return Replicaset list
     */
    public String getReplicaSetList(String namespace) {
        StringBuilder result = new StringBuilder();
        for (ReplicaSet replicaSet : k8sClient.apps().replicaSets().inNamespace(namespace).list().getItems()) {
            result.append(replicaSet.getMetadata().getName()).append("\n");
        }
        return result.length() > 0 ? result.substring(0, result.length() - 1) : result.toString();
    }

    /**
     * kubectl describe replicaset xxx -n namespace
     *
     * @param replicaSetName Config map name
     * @param namespace      Namespace
     * @return String with replicaset
     */
    public String describeReplicaSet(String replicaSetName, String namespace) throws JsonProcessingException {
        return SerializationUtils.dumpAsYaml(getReplicaSet(replicaSetName, namespace));
    }

    /**
     * Get a serviceAccount
     *
     * @param name      serviceAccount name
     * @param namespace Namespace
     */
    public ServiceAccount getServiceAccount(String name, String namespace) {
        return k8sClient.serviceAccounts().inNamespace(namespace).withName(name).get();
    }

    /**
     * Get ServiceAccount list in selected namespace
     *
     * @param namespace Namespace
     * @return ServiceAccount list
     */
    public String getServiceAccountList(String namespace) {
        StringBuilder result = new StringBuilder();
        for (ServiceAccount serviceAccount : k8sClient.serviceAccounts().inNamespace(namespace).list().getItems()) {
            result.append(serviceAccount.getMetadata().getName()).append("\n");
        }
        return result.length() > 0 ? result.substring(0, result.length() - 1) : result.toString();
    }

    /**
     * kubectl describe serviceaccount xxx -n namespace
     *
     * @param serviceAccountName serviceAccount name
     * @param namespace          Namespace
     * @return String with serviceAccount
     */
    public String describeServiceAccount(String serviceAccountName, String namespace) throws JsonProcessingException {
        return SerializationUtils.dumpAsYaml(getServiceAccount(serviceAccountName, namespace));
    }

    /**
     * Get secret
     *
     * @param name      secret name
     * @param namespace Namespace
     */
    public Secret getSecret(String name, String namespace) {
        return k8sClient.secrets().inNamespace(namespace).withName(name).get();
    }

    /**
     * Get Secrets list in selected namespace
     *
     * @param namespace Namespace
     * @return Secrets list
     */
    public String getSecretsList(String namespace) {
        StringBuilder result = new StringBuilder();
        for (Secret secret : k8sClient.secrets().inNamespace(namespace).list().getItems()) {
            result.append(secret.getMetadata().getName()).append("\n");
        }
        return result.length() > 0 ? result.substring(0, result.length() - 1) : result.toString();
    }

    /**
     * kubectl describe secret xxx -n namespace
     *
     * @param secretName secret name
     * @param namespace  Namespace
     * @return String with secret
     */
    public String describeSecret(String secretName, String namespace) throws JsonProcessingException {
        return SerializationUtils.dumpAsYaml(getSecret(secretName, namespace));
    }

    /**
     * Get clusterrole
     *
     * @param name      clusterrole name
     * @param namespace Namespace
     */
    public ClusterRole getClusterRole(String name, String namespace) {
        return k8sClient.rbac().clusterRoles().inNamespace(namespace).withName(name).get();
    }

    /**
     * Get clusterrole list in selected namespace
     *
     * @param namespace Namespace
     * @return clusterrole list
     */
    public String getClusterRoleList(String namespace) {
        StringBuilder result = new StringBuilder();
        for (ClusterRole clusterRole : k8sClient.rbac().clusterRoles().inNamespace(namespace).list().getItems()) {
            result.append(clusterRole.getMetadata().getName()).append("\n");
        }
        return result.length() > 0 ? result.substring(0, result.length() - 1) : result.toString();
    }

    /**
     * kubectl describe clusterrole xxx -n namespace
     *
     * @param crName    clusterrole name
     * @param namespace Namespace
     * @return String with clusterrole
     */
    public String describeClusterRole(String crName, String namespace) throws JsonProcessingException {
        return SerializationUtils.dumpAsYaml(getClusterRole(crName, namespace));
    }

    /**
     * Get clusterrolebinding
     *
     * @param name      clusterrolebinding name
     * @param namespace Namespace
     */
    public ClusterRoleBinding getClusterRoleBinding(String name, String namespace) {
        return k8sClient.rbac().clusterRoleBindings().inNamespace(namespace).withName(name).get();
    }

    /**
     * Get clusterrolebinding list in selected namespace
     *
     * @param namespace Namespace
     * @return clusterrolebinding list
     */
    public String getClusterRoleBindingList(String namespace) {
        StringBuilder result = new StringBuilder();
        for (ClusterRoleBinding clusterRoleBinding : k8sClient.rbac().clusterRoleBindings().inNamespace(namespace).list().getItems()) {
            result.append(clusterRoleBinding.getMetadata().getName()).append("\n");
        }
        return result.length() > 0 ? result.substring(0, result.length() - 1) : result.toString();
    }

    /**
     * kubectl describe clusterrolebinding xxx -n namespace
     *
     * @param crName    clusterrolebinding name
     * @param namespace Namespace
     * @return String with clusterrolebinding
     */
    public String describeClusterRoleBinding(String crName, String namespace) throws JsonProcessingException {
        return SerializationUtils.dumpAsYaml(getClusterRoleBinding(crName, namespace));
    }

    /**
     * Get statefulset
     *
     * @param name      statefulset name
     * @param namespace Namespace
     */
    public StatefulSet getStateFulSet(String name, String namespace) {
        return k8sClient.apps().statefulSets().inNamespace(namespace).withName(name).get();
    }

    /**
     * Get statefulset list in selected namespace
     *
     * @param namespace Namespace
     * @return statefulset list
     */
    public String getStateFulSetList(String namespace) {
        StringBuilder result = new StringBuilder();
        for (StatefulSet statefulSet : k8sClient.apps().statefulSets().inNamespace(namespace).list().getItems()) {
            result.append(statefulSet.getMetadata().getName()).append("\n");
        }
        return result.length() > 0 ? result.substring(0, result.length() - 1) : result.toString();
    }

    /**
     * kubectl describe statefulset xxx -n namespace
     *
     * @param stateFulSetName statefulset name
     * @param namespace       Namespace
     * @return String with statefulset
     */
    public String describeStateFulSet(String stateFulSetName, String namespace) throws JsonProcessingException {
        return SerializationUtils.dumpAsYaml(getStateFulSet(stateFulSetName, namespace));
    }

    /**
     * Get role
     *
     * @param name      role name
     * @param namespace Namespace
     */
    public Role getRole(String name, String namespace) {
        return k8sClient.rbac().roles().inNamespace(namespace).withName(name).get();
    }

    /**
     * Get role list in selected namespace
     *
     * @param namespace Namespace
     * @return role list
     */
    public String getRoleList(String namespace) {
        StringBuilder result = new StringBuilder();
        for (Role role : k8sClient.rbac().roles().inNamespace(namespace).list().getItems()) {
            result.append(role.getMetadata().getName()).append("\n");
        }
        return result.length() > 0 ? result.substring(0, result.length() - 1) : result.toString();
    }

    /**
     * kubectl describe role xxx -n namespace
     *
     * @param roleName  role name
     * @param namespace Namespace
     * @return String with role
     */
    public String describeRole(String roleName, String namespace) throws JsonProcessingException {
        return SerializationUtils.dumpAsYaml(getRole(roleName, namespace));
    }

    /**
     * Get rolebinding
     *
     * @param name      rolebinding name
     * @param namespace Namespace
     */
    public RoleBinding getRoleBinding(String name, String namespace) {
        return k8sClient.rbac().roleBindings().inNamespace(namespace).withName(name).get();
    }

    /**
     * Get rolebinding list in selected namespace
     *
     * @param namespace Namespace
     * @return rolebinding list
     */
    public String getRoleBindingList(String namespace) {
        StringBuilder result = new StringBuilder();
        for (RoleBinding roleBinding : k8sClient.rbac().roleBindings().inNamespace(namespace).list().getItems()) {
            result.append(roleBinding.getMetadata().getName()).append("\n");
        }
        return result.length() > 0 ? result.substring(0, result.length() - 1) : result.toString();
    }

    /**
     * kubectl describe rolebinding xxx -n namespace
     *
     * @param roleName  rolebinding name
     * @param namespace Namespace
     * @return String with rolebinding
     */
    public String describeRoleBinding(String roleName, String namespace) throws JsonProcessingException {
        return SerializationUtils.dumpAsYaml(getRoleBinding(roleName, namespace));
    }

    /**
     * Get customresourcedefinition list
     *
     * @return customresourcedefinition list
     */
    public String getCustomResourceDefinitionList() {
        StringBuilder result = new StringBuilder();
        for (CustomResourceDefinition customResourceDefinition : k8sClient.customResourceDefinitions().list().getItems()) {
            result.append(customResourceDefinition.getMetadata().getName()).append("\n");
        }
        return result.length() > 0 ? result.substring(0, result.length() - 1) : result.toString();
    }

    /**
     * Get deployment list in selected namespace
     *
     * @param namespace Namespace
     * @return deployment list
     */
    public String getDeploymentList(String namespace) {
        StringBuilder result = new StringBuilder();
        for (Deployment deployment : k8sClient.apps().deployments().inNamespace(namespace).list().getItems()) {
            result.append(deployment.getMetadata().getName()).append("\n");
        }
        return result.length() > 0 ? result.substring(0, result.length() - 1) : result.toString();
    }

    /**
     * Get service list in selected namespace
     *
     * @param namespace Namespace
     * @return service list
     */
    public String getServiceList(String namespace) {
        StringBuilder result = new StringBuilder();
        for (Service service : k8sClient.services().inNamespace(namespace).list().getItems()) {
            result.append(service.getMetadata().getName()).append("\n");
        }
        return result.length() > 0 ? result.substring(0, result.length() - 1) : result.toString();
    }

    /**
     * Get ingress
     *
     * @param name      name
     * @param namespace Namespace
     * @return Ingress
     */
    public Ingress getIngress(String name, String namespace) {
        return k8sClient.extensions().ingresses().inNamespace(namespace).withName(name).get();
    }

    /**
     * kubectl get ingress -n namespace
     *
     * @param namespace Namespace
     * @return ingress list
     */
    public String getIngressList(String namespace) {
        StringBuilder result = new StringBuilder();
        for (Ingress ingress : k8sClient.extensions().ingresses().inNamespace(namespace).list().getItems()) {
            result.append(ingress.getMetadata().getName()).append("\n");
        }
        return result.length() > 0 ? result.substring(0, result.length() - 1) : result.toString();
    }

    /**
     * kubectl get pvc -n namespace
     *
     * @param namespace Namespace
     * @return Persistant Volume Claim list
     */
    public String getPersistentVolumeClaimsList(String namespace) {
        StringBuilder result = new StringBuilder();
        for (PersistentVolumeClaim pvc : k8sClient.persistentVolumeClaims().inNamespace(namespace).list().getItems()) {
            result.append(pvc.getMetadata().getName()).append("\n");
        }
        return result.length() > 0 ? result.substring(0, result.length() - 1) : result.toString();
    }

    /**
     * kubectl describe ingress -n namespace
     *
     * @param ingressName Ingress name
     * @param namespace   Namespace
     * @return String with ingress information
     */
    public String describeIngress(String ingressName, String namespace) throws JsonProcessingException {
        return SerializationUtils.dumpAsYaml(getIngress(ingressName, namespace));
    }

    /**
     * Set local port forward for a service
     *
     * @param namespace     Namespace
     * @param name          Name
     * @param containerPort container port
     * @param localHostPort local host port
     */

    public void setLocalPortForwardService(String namespace, String name, int containerPort, int localHostPort, String localPortForwardId) {
        localPortForward = k8sClient.services().inNamespace(namespace).withName(name).portForward(containerPort, localHostPort);
        localPortForwardMap.put(localPortForwardId, localPortForward);
        this.localPortForwardId = localPortForwardId;
    }

    /**
     * Set local port forward for a pod
     *
     * @param namespace     Namespace
     * @param name          Name
     * @param containerPort container port
     * @param localHostPort local host port
     */
    public void setLocalPortForwardPod(String namespace, String name, int containerPort, int localHostPort, String localPortForwardId) {
        localPortForward = k8sClient.pods().inNamespace(namespace).withName(name).portForward(containerPort, localHostPort);
        localPortForwardMap.put(localPortForwardId, localPortForward);
        this.localPortForwardId = localPortForwardId;
    }

    public void closePortForward(String id) throws IOException {
        if (id != null) {
            localPortForward = localPortForwardMap.get(id);
            localPortForwardId = id;
        }
        if (localPortForward != null && localPortForward.isAlive()) {
            localPortForward.close();
        }
        localPortForward = null;
        if (localPortForwardId != null) {
            localPortForwardMap.remove(localPortForwardId);
        }
        localPortForwardId = null;
    }

    /**
     * kubectl patch hpa <deployment> -n <namespace> -p '{"spec":{"maxReplicas": <number>}}'
     *
     * @param namespace   Namespace
     * @param name        Deployment name
     * @param maxReplicas Max replicas
     */
    public void updateHorizontalAutoscaler(String namespace, String name, int maxReplicas) {
        k8sClient.autoscaling().v1().horizontalPodAutoscalers().inNamespace(namespace).withName(name).edit().editSpec().withMaxReplicas(maxReplicas).endSpec().done();
    }

    /**
     * kubectl patch hpa <deployment> -n <namespace> -p '{"spec":{"minReplicas": <number>, "maxReplicas": <number>}}'
     *
     * @param namespace   Namespace
     * @param name        Deployment name
     * @param maxReplicas Max replicas
     */
    public void updateHorizontalAutoscaler(String namespace, String name, int minReplicas, int maxReplicas) {
        k8sClient.autoscaling().v1().horizontalPodAutoscalers().inNamespace(namespace).withName(name).edit().editSpec().withMinReplicas(minReplicas).withMaxReplicas(maxReplicas).endSpec().done();
    }

    public void copyFileToPod(String podName, String namespace, String filePath, String destinationPath) {
        File local = new File(filePath);
        if (local.isDirectory()) {
            k8sClient.pods().inNamespace(namespace).withName(podName).dir(destinationPath).upload(Paths.get(filePath));
        } else {
            k8sClient.pods().inNamespace(namespace).withName(podName).file(destinationPath).upload(Paths.get(filePath));
        }
    }

    public void copyFileFromPod(String podName, String namespace, String filePath, String destinationPath) {
        File local = new File(destinationPath);
        if (local.isDirectory()) {
            k8sClient.pods().inNamespace(namespace).withName(podName).dir(filePath).copy(Paths.get(destinationPath));
        } else {
            k8sClient.pods().inNamespace(namespace).withName(podName).file(filePath).copy(Paths.get(destinationPath));
        }
    }

    /**
     * Creates a new namespace with labels (if labelsMap != null)
     * kubectl create namespace xxx
     *
     * @param namespaceName
     * @param labelsMap
     */
    public void createNamespace(String namespaceName, Map<String, String> labelsMap) {
        if (labelsMap != null) {
            k8sClient.namespaces().createOrReplace(new NamespaceBuilder().withNewMetadata().withName(namespaceName).addToLabels(labelsMap).endMetadata().build());
        } else {
            k8sClient.namespaces().createOrReplace(new NamespaceBuilder().withNewMetadata().withName(namespaceName).endMetadata().build());
        }
    }

    /**
     * Creates a new serviceAccount
     * kubectl create serviceaccount spark-test -n <namespace>
     *
     * @param serviceAccountName
     * @param namespace
     */
    public void createServiceAccount(String serviceAccountName, String namespace) {
        k8sClient.serviceAccounts().inNamespace(namespace).createOrReplace(new ServiceAccountBuilder().withNewMetadata().withName(serviceAccountName).endMetadata().build());
    }

    /**
     * Creates a new clusterRole
     * kubectl create clusterrole xxx --verb=get --verb=list --verb=watch --verb=create --verb=delete --resource=pods --resource=services --resource=configmaps --resource=secrets --namespace=<namespace>
     *
     * @param clusterRoleName
     * @param namespace
     * @param resources
     * @param verbs
     * @param apiGroups
     */
    public void createClusterRole(String clusterRoleName, String namespace, String resources, String verbs, String apiGroups) {
        String[] resourcesList = resources.split(",");
        String[] verbsList = verbs.split(",");
        String[] apiGroupsList = apiGroups != null ? apiGroups.split(",") : new String[]{""};
        PolicyRule policyRule = new PolicyRuleBuilder().addToApiGroups(apiGroupsList).addToResources(resourcesList).addToVerbs(verbsList).build();
        k8sClient.rbac().clusterRoles().inNamespace(namespace).createOrReplace(new ClusterRoleBuilder().withRules(policyRule).withNewMetadata().withName(clusterRoleName).endMetadata().build());
    }

    /**
     * Creates a new clusterRoleBinding
     * kubectl create clusterrolebinding xxx --clusterrole=xxx --serviceaccount=<namespace>:<serviceaccount> --namespace=<namespace>
     *
     * @param clusterRoleBindingName
     * @param namespace
     * @param clusterRole
     * @param serviceAccount
     */
    public void createClusterRoleBinding(String clusterRoleBindingName, String namespace, String clusterRole, String serviceAccount) {
        Subject subject = new SubjectBuilder().withNewKind("ServiceAccount").withNewName(serviceAccount.split(":")[1]).withNewNamespace(serviceAccount.split(":")[0]).build();
        k8sClient.rbac().clusterRoleBindings().inNamespace(namespace).createOrReplace(new ClusterRoleBindingBuilder().withNewRoleRef("", "ClusterRole", clusterRole).withSubjects(subject).withNewMetadata().withName(clusterRoleBindingName).endMetadata().build());
    }

    private static class MyPodExecListener implements ExecListener {
        @Override
        public void onOpen(Response response) {
            getLogger().debug("K8S Shell was opened");
        }

        @Override
        public void onFailure(Throwable throwable, Response response) {
            getLogger().warn("Some error encountered in K8S Shell");
            execLatch.countDown();
        }

        @Override
        public void onClose(int i, String s) {
            getLogger().debug("K8S Shell Closing");
            execLatch.countDown();
        }
    }
}
