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

import org.asynchttpclient.Response;
import com.stratio.qa.assertions.Assertions;
import com.stratio.qa.utils.ThreadProperty;
import cucumber.api.java.en.When;
import io.cucumber.datatable.DataTable;
import org.hjson.JsonArray;
import org.hjson.JsonObject;
import org.hjson.JsonValue;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

/**
 * Generic Gosec Specs.
 *
 * @see <a href="GosecSpec-annotations.html">Gosec Steps &amp; Matching Regex</a>
 */
public class GosecSpec extends BaseGSpec {

    private final Logger logger = LoggerFactory.getLogger(GosecSpec.class);

    RestSpec restSpec;

    MiscSpec miscSpec;

    /**
     * Generic constructor.
     *
     * @param spec object
     */
    public GosecSpec(CommonG spec) {
        this.commonspec = spec;
        restSpec = new RestSpec(spec);
        miscSpec = new MiscSpec(spec);
    }

    /**
     * Create resource in Gosec
     *
     * @param resource        : type of resource (enum value)
     * @param resourceId      : name of the resource to be created
     * @param tenantOrig      : tenant where resource is gonna be created (OPTIONAL)
     * @param tenantLoginInfo : user and password to log into tenant (OPTIONAL)
     * @param endPoint        : endpoint to send request to (OPTIONAL)
     * @param loginInfo       : user and password to log in service (OPTIONAL)
     * @param baseData        : base information to use for request
     * @param type            : type of data (enum value) (OPTIONAL)
     * @param modifications   : modifications to perform oven base data
     * @throws Exception
     */
    @When("^I create '(policy|user|group)' '(.+?)'( in tenant '(.+?)')?( with tenant user and tenant password '(.+:.+?)')?( using API service path '(.+?)')?( with user and password '(.+:.+?)')? based on '([^:]+?)'( as '(json|string|gov)')? with:$")
    public void createResource(String resource, String resourceId, String tenantOrig, String tenantLoginInfo, String endPoint, String loginInfo, String baseData, String type, DataTable modifications) throws Exception {
        createResourceIfNotExist(resource, resourceId, tenantOrig, tenantLoginInfo, endPoint, loginInfo, false, baseData, type, modifications);
    }

    /**
     * Create resource in Gosec if it doesn exist already
     *
     * @param resource        : type of resource (enum value)
     * @param resourceId      : name of the resource to be created
     * @param tenantOrig      : tenant where resource is gonna be created (OPTIONAL)
     * @param tenantLoginInfo : user and password to log into tenant (OPTIONAL)
     * @param endPoint        : endpoint to send request to (OPTIONAL)
     * @param loginInfo       : user and password to log in service (OPTIONAL)
     * @param baseData        : base information to use for request
     * @param type            : type of data (enum value) (OPTIONAL)
     * @param modifications   : modifications to perform oven base data
     * @throws Exception
     */
    @When("^I create '(policy|user|group)' '(.+?)'( in tenant '(.+?)')?( with tenant user and tenant password '(.+:.+?)')?( using API service path '(.+?)')?( with user and password '(.+:.+?)')? if it does not exist based on '([^:]+?)'( as '(json|string|gov)')? with:$")
    public void createResourceIfNotExist(String resource, String resourceId, String tenantOrig, String tenantLoginInfo, String endPoint, String loginInfo, String baseData, String type, DataTable modifications) throws Exception {
        createResourceIfNotExist(resource, resourceId, tenantOrig, tenantLoginInfo, endPoint, loginInfo, true, baseData, type, modifications);
    }

    /**
     * Creates a custom resource in gosec management if the resource doesn't exist
     *
     * @param resource        : type of resource (enum value)
     * @param resourceId      : name of the resource to be created
     * @param tenantOrig      : tenant where resource is gonna be created
     * @param tenantLoginInfo : user and password to log into tenant
     * @param endPoint        : endpoint to send request to
     * @param loginInfo       : user and password to log in service
     * @param doesNotExist    : (if 'empty', creation is forced deleting the previous policy if exists)
     * @param baseData        : base information to use for request
     * @param type            : type of data (enum value)
     * @param modifications   : modifications to perform oven base data
     * @throws Exception
     */
    private void createResourceIfNotExist(String resource, String resourceId, String tenantOrig, String tenantLoginInfo, String endPoint, String loginInfo, boolean doesNotExist, String baseData, String type, DataTable modifications) throws Exception {
        if (ThreadProperty.get("isKeosEnv") != null && ThreadProperty.get("isKeosEnv").equals("true")) {
            createResourceIfNotExistKeos(resource, resourceId, tenantOrig, tenantLoginInfo, endPoint, loginInfo, doesNotExist, baseData, type, modifications);
        } else {
            createResourceIfNotExistDcos(resource, resourceId, tenantOrig, tenantLoginInfo, endPoint, loginInfo, doesNotExist, baseData, type, modifications);
        }
    }

    private void createResourceIfNotExistDcos(String resource, String resourceId, String tenantOrig, String tenantLoginInfo, String endPoint, String loginInfo, boolean doesNotExist, String baseData, String type, DataTable modifications) throws Exception {
        Integer expectedStatusCreate = 201;
        Integer[] expectedStatusDelete = {200, 204};
        String endPointResource = "";
        String endPointPolicies = "/service/gosecmanagement" + ThreadProperty.get("API_POLICIES");
        String endPointPolicy = "/service/gosecmanagement" + ThreadProperty.get("API_POLICY");
        String newEndPoint = "";
        String gosecVersion = ThreadProperty.get("gosec-management_version");
        List<List<String>> newModifications;
        newModifications = convertDataTableToModifiableList(modifications);
        Boolean addSourceType = false;
        String managementBaasVersion = ThreadProperty.get("gosec-management-baas_version");

        if (tenantOrig != null) {
            // Set REST connection
            commonspec.setCCTConnection(tenantOrig, tenantLoginInfo);
        }

        if (managementBaasVersion != null) {
            endPointPolicies = "/service/gosec-management-baas/management/policies";
            endPointPolicy = "/service/gosec-management-baas/management/policy?pid=";
        }

        if (endPoint != null) {
            endPointResource = endPoint + resourceId;

            if (endPoint.contains("id")) {
                newEndPoint = endPoint.replace("?id=", "");
            } else {
                newEndPoint = endPoint.substring(0, endPoint.length() - 1);
            }
        } else {
            if (resource.equals("policy")) {
                endPoint = "/service/gosecmanagement" + ThreadProperty.get("API_POLICY");
                if (managementBaasVersion != null) {
                    endPoint = "/service/gosec-management-baas/management/policy?pid=";
                }
            } else {
                if (resource.equals("user")) {
                    endPoint = "/service/gosecmanagement" + ThreadProperty.get("API_USER");
                } else {
                    endPoint = "/service/gosecmanagement" + ThreadProperty.get("API_GROUP");
                }
            }
            if (endPoint.contains("id")) {
                newEndPoint = endPoint.replace("?id=", "");
            } else {
                newEndPoint = endPoint.substring(0, endPoint.length() - 1);
            }
            endPointResource = endPoint + resourceId;
        }

        if (gosecVersion != null) {
            String[] gosecVersionArray = gosecVersion.split("\\.");
            // Add inputSourceType if Gosec >= 1.4.x
            if (Integer.parseInt(gosecVersionArray[0]) >= 1 && Integer.parseInt(gosecVersionArray[1]) >= 4) {
                addSourceType = true;
            }
        }

        try {
            assertThat(commonspec.getRestHost().isEmpty() || commonspec.getRestPort().isEmpty());

            if (resource.equals("policy")) {
                restSpec.sendRequestNoDataTable("GET", endPointPolicies, loginInfo, null, null);
                if (commonspec.getResponse().getStatusCode() == 200) {
                    String policyId = getPolicyIdFromResponse(commonspec.getResponse().getResponse(), resourceId, managementBaasVersion != null);
                    if (!policyId.equals("")) {
                        commonspec.getLogger().debug("PolicyId obtained: {}", policyId);
                        endPointResource = endPointPolicy + policyId;
                    } else {
                        endPointResource = endPointPolicy + "thisIsANewPolicyId";
                    }
                }
            }

            restSpec.sendRequestNoDataTable("GET", endPointResource, loginInfo, null, null);

            if (commonspec.getResponse().getStatusCode() != 200) {
                if (resource.equals("user") && (addSourceType)) {
                    commonspec.getLogger().warn("Gosec Version:{} -> Adding inputsourceType = CUSTOM", gosecVersion);
                    List<String> newField = Arrays.asList("$.inputSourceType", "ADD", "CUSTOM", "string");
                    newModifications.add(newField);
                }
                // Create datatable with modified data
                DataTable gosecModifications = DataTable.create(newModifications);
                // Send POST request
                restSpec.sendRequest("POST", newEndPoint, loginInfo, baseData, type, gosecModifications);
                try {
                    if (commonspec.getResponse().getStatusCode() == 409) {
                        commonspec.getLogger().warn("The resource {} already exists", resourceId);
                    } else {
                        try {
                            assertThat(commonspec.getResponse().getStatusCode()).isEqualTo(expectedStatusCreate);
                        } catch (AssertionError e) {
                            commonspec.getLogger().warn("Error creating Resource {}: {}", resourceId, commonspec.getResponse().getResponse());
                            throw e;
                        }
                        commonspec.getLogger().warn("Resource {} created", resourceId);
                    }
                } catch (Exception e) {
                    commonspec.getLogger().warn("Error creating user {}: {}", resourceId, commonspec.getResponse().getResponse());
                    throw e;
                }
            } else {
                commonspec.getLogger().warn("{}:{} already exist", resource, resourceId);
                if (resource.equals("policy") && commonspec.getResponse().getStatusCode() == 200) {
                    if (doesNotExist) {
                        // Policy already exists
                        commonspec.getLogger().warn("Policy {} already exist - not created", resourceId);

                    } else {
                        // Delete policy if exists
                        restSpec.sendRequest("DELETE", endPointResource, loginInfo, baseData, type, modifications);
                        commonspec.getLogger().warn("Policy {} deleted", resourceId);

                        try {
                            assertThat(commonspec.getResponse().getStatusCode()).isIn(expectedStatusDelete);
                        } catch (AssertionError e) {
                            commonspec.getLogger().warn("Error deleting Policy {}: {}", resourceId, commonspec.getResponse().getResponse());
                            throw e;
                        }
                        createResourceIfNotExistDcos(resource, resourceId, tenantOrig, tenantLoginInfo, endPoint, loginInfo, doesNotExist, baseData, type, modifications);
                    }
                }
            }
        } catch (Exception e) {
            commonspec.getLogger().error("Rest Host or Rest Port are not initialized {}{}", commonspec.getRestHost(), commonspec.getRestPort());
            throw e;
        }
    }

    private void createResourceIfNotExistKeos(String resource, String resourceId, String tenantOrig, String tenantLoginInfo, String endPoint, String loginInfo, boolean doesNotExist, String baseData, String type, DataTable modifications) throws Exception {
        Integer expectedStatusCreate = 201;
        Integer[] expectedStatusDelete = {200, 204};
        String endPointResource = "";
        String resourcePrefix = getResourcePrefix(resource);
        String baasPath = ThreadProperty.get("KEOS_GOSEC_BAAS_INGRESS_PATH");
        String endPointPolicies = baasPath + "/management/policies";

        if (tenantOrig != null) {
            // Set REST connection
            commonspec.setCCTConnection(tenantOrig, tenantLoginInfo);
        }

        if (endPoint == null) {
            endPoint = getResourceEndpoint(resource);
        }
        endPointResource = endPoint + resourcePrefix + resourceId;

        try {
            assertThat(commonspec.getRestHost().isEmpty() || commonspec.getRestPort().isEmpty());

            if (resource.equals("policy")) {
                restSpec.sendRequestNoDataTable("GET", endPointPolicies, loginInfo, null, null);
                if (commonspec.getResponse().getStatusCode() == 200) {
                    String policyId = getPolicyIdFromResponse(commonspec.getResponse().getResponse(), resourceId, true);
                    if (!policyId.equals("")) {
                        commonspec.getLogger().debug("PolicyId obtained: {}", policyId);
                        endPointResource = endPoint + resourcePrefix + policyId;
                    }
                }
            }

            restSpec.sendRequestNoDataTable("GET", endPointResource, loginInfo, null, null);

            if (commonspec.getResponse().getStatusCode() != 200) {
                // Send POST request
                restSpec.sendRequest("POST", endPoint, loginInfo, baseData, type, modifications);
                try {
                    if (commonspec.getResponse().getStatusCode() == 409) {
                        commonspec.getLogger().warn("The resource {} already exists", resourceId);
                    } else {
                        try {
                            assertThat(commonspec.getResponse().getStatusCode()).isEqualTo(expectedStatusCreate);
                        } catch (AssertionError e) {
                            commonspec.getLogger().warn("Error creating Resource {}: {}", resourceId, commonspec.getResponse().getResponse());
                            throw e;
                        }
                        commonspec.getLogger().warn("Resource {} created", resourceId);
                    }
                } catch (Exception e) {
                    commonspec.getLogger().warn("Error creating user {}: {}", resourceId, commonspec.getResponse().getResponse());
                    throw e;
                }
            } else {
                commonspec.getLogger().warn("{}:{} already exist", resource, resourceId);
                if (resource.equals("policy") && commonspec.getResponse().getStatusCode() == 200) {
                    if (doesNotExist) {
                        // Policy already exists
                        commonspec.getLogger().warn("Policy {} already exist - not created", resourceId);
                    } else {
                        // Delete policy if exists
                        restSpec.sendRequest("DELETE", endPointResource, loginInfo, baseData, type, modifications);
                        commonspec.getLogger().warn("Policy {} deleted", resourceId);

                        try {
                            assertThat(commonspec.getResponse().getStatusCode()).isIn(expectedStatusDelete);
                        } catch (AssertionError e) {
                            commonspec.getLogger().warn("Error deleting Policy {}: {}", resourceId, commonspec.getResponse().getResponse());
                            throw e;
                        }
                        createResourceIfNotExistKeos(resource, resourceId, tenantOrig, tenantLoginInfo, endPoint, loginInfo, doesNotExist, baseData, type, modifications);
                    }
                }
            }
        } catch (Exception e) {
            commonspec.getLogger().error("Rest Host or Rest Port are not initialized {}{}", commonspec.getRestHost(), commonspec.getRestPort());
            throw e;
        }
    }

    private String getResourcePrefix(String resource) {
        switch (resource) {
            case "policy":
            case "collectionPolicy":
                return "?pid=";
            case "user":
                return "?uid=";
            case "group":
                return "?gid=";
            default:
                return "";
        }
    }

    private String getResourceEndpoint(String resource) {
        String baasPath = ThreadProperty.get("KEOS_GOSEC_BAAS_INGRESS_PATH");
        switch (resource) {
            case "policy":
                return baasPath + "/management/policy";
            case "collectionPolicy":
                return baasPath + "/management/policy/domain";
            case "user":
                return baasPath + "/management/user";
            case "group":
                return baasPath + "/management/group";
            default:
                return "";
        }
    }

    private String getPolicyIdFromResponse(String response, String policyName, boolean isManagementBaas) throws Exception {
        if (isManagementBaas) {
            commonspec.runLocalCommand("echo '" + response + "' | jq '.list[] | select (.name == \"" + policyName + "\").pid' | sed s/\\\"//g");
            return commonspec.getCommandResult().trim();
        } else {
            commonspec.runLocalCommand("echo '" + response + "' | jq '.list[] | select (.name == \"" + policyName + "\").id' | sed s/\\\"//g");
            String policyId = commonspec.getCommandResult().trim();
            if (!policyId.equals("")) {
                return policyId;
            } else {
                commonspec.runLocalCommand("echo '" + response + "' | jq '.[] | select (.name == \"" + policyName + "\").id' | sed s/\\\"//g");
                return commonspec.getCommandResult().trim();
            }
        }
    }

    /**
     * Deletes a resource in gosec management if the resourceId exists previously.
     *
     * @param resource        : type of resource (enum value)
     * @param resourceId      : name of the resource to be created
     * @param tenantOrig      : tenant resource is gonna be deleted from (OPTIONAL)
     * @param tenantLoginInfo : user and password to log into tenant (OPTIONAL)
     * @param endPoint        : endpoint to send request to (OPTIONAL)
     * @param loginInfo       : user and password to log in service (OPTIONAL)
     * @throws Exception
     */
    @When("^I delete '(policy|user|group|collectionPolicy)' '(.+?)'( from tenant '(.+?)')?( with tenant user and tenant password '(.+:.+?)')?( using API service path '(.+?)')?( with user and password '(.+:.+?)')? if it exists$")
    public void deleteUserIfExists(String resource, String resourceId, String tenantOrig, String tenantLoginInfo, String endPoint, String loginInfo) throws Exception {
        if (ThreadProperty.get("isKeosEnv") != null && ThreadProperty.get("isKeosEnv").equals("true")) {
            deleteResourceIfExistsKeos(resource, resourceId, tenantOrig, tenantLoginInfo, endPoint, loginInfo);
        } else {
            deleteResourceIfExistsDcos(resource, resourceId, tenantOrig, tenantLoginInfo, endPoint, loginInfo);
        }
    }

    public void deleteResourceIfExistsDcos(String resource, String resourceId, String tenantOrig, String tenantLoginInfo, String endPoint, String loginInfo) throws Exception {
        Integer[] expectedStatusDelete = {200, 204};
        String endPointResource = "";
        String endPointPolicy = "/service/gosecmanagement" + ThreadProperty.get("API_POLICY");
        String endPointPolicies = "/service/gosecmanagement" + ThreadProperty.get("API_POLICIES");
        String endPointCollectionPolicy = "";
        String endPointCollectionsPolicies = "";
        String managementBaasVersion = ThreadProperty.get("gosec-management-baas_version");

        if (tenantOrig != null) {
            // Set REST connection
            commonspec.setCCTConnection(tenantOrig, tenantLoginInfo);
        }

        if (!resource.equals("policy")) {
            resourceId = resourceId.replaceAll("\\s+", ""); //delete white spaces
        }

        if (managementBaasVersion != null) {
            endPointPolicies = "/service/gosec-management-baas/management/policies";
            endPointPolicy = "/service/gosec-management-baas/management/policy?pid=";
            endPointCollectionsPolicies = "/service/gosec-management-baas/management/policies/domains";
            endPointCollectionPolicy = "/service/gosec-management-baas/management/policy/domain?pid=";
        }

        if (endPoint != null) {
            endPointResource = endPoint + resourceId;
        } else {
            if (resource.equals("policy")) {
                endPoint = "/service/gosecmanagement" + ThreadProperty.get("API_POLICY");
                if (managementBaasVersion != null) {
                    endPoint = "/service/gosec-management-baas/management/policy?pid=";
                }
            } else if (resource.equals("collectionPolicy")) {
                if (managementBaasVersion == null) {
                    commonspec.getLogger().error("Collections policies can only be used with gosec-management-baas");
                    throw new Exception("Collections policies can only be used with gosec-management-baas");
                }
                endPointPolicy = endPointCollectionPolicy;
                endPointPolicies = endPointCollectionsPolicies;
            } else {
                if (resource.equals("user")) {
                    endPoint = "/service/gosecmanagement" + ThreadProperty.get("API_USER");
                    if (managementBaasVersion != null) {
                        endPoint = "/service/gosec-management-baas/management/user?uid=";
                    }
                } else {
                    endPoint = "/service/gosecmanagement" + ThreadProperty.get("API_GROUP");
                    if (managementBaasVersion != null) {
                        endPoint = "/service/gosec-management-baas/management/group?gid=";
                    }
                }
            }
        }

        try {
            assertThat(commonspec.getRestHost().isEmpty() || commonspec.getRestPort().isEmpty());

            if ((resource.equals("policy") || resource.equals("collectionPolicy"))) {
                restSpec.sendRequestNoDataTable("GET", endPointPolicies, loginInfo, null, null);
                if (commonspec.getResponse().getStatusCode() == 200) {
                    String policyId = getPolicyIdFromResponse(commonspec.getResponse().getResponse(), resourceId, managementBaasVersion != null);
                    if (!policyId.equals("")) {
                        commonspec.getLogger().debug("PolicyId obtained: {}", policyId);
                        endPointResource = endPointPolicy + policyId;
                    } else {
                        endPointResource = endPointPolicy + "thisPolicyDoesNotExistId";
                    }
                }
            } else {
                endPointResource = endPoint + resourceId;
            }

            restSpec.sendRequestNoDataTable("GET", endPointResource, loginInfo, null, null);

            if (commonspec.getResponse().getStatusCode() == 200) {
                // Delete resource if exists
                restSpec.sendRequestNoDataTable("DELETE", endPointResource, loginInfo, null, null);
                commonspec.getLogger().warn("Resource {} deleted", resourceId);

                try {
                    assertThat(commonspec.getResponse().getStatusCode()).isIn(expectedStatusDelete);
                } catch (AssertionError e) {
                    commonspec.getLogger().warn("Error deleting Resource {}: {}", resourceId, commonspec.getResponse().getResponse());
                    throw e;
                }
            } else {
                commonspec.getLogger().warn("Resource {} with id {} not found so it's not deleted", resource, resourceId);
            }
        } catch (Exception e) {
            commonspec.getLogger().error("Rest Host or Rest Port are not initialized {}: {}", commonspec.getRestHost(), commonspec.getRestPort());
            throw e;
        }
    }

    public void deleteResourceIfExistsKeos(String resource, String resourceId, String tenantOrig, String tenantLoginInfo, String endPoint, String loginInfo) throws Exception {
        Integer[] expectedStatusDelete = {200, 204};
        String endPointResource = "";
        String resourcePrefix = getResourcePrefix(resource);
        String baasPath = ThreadProperty.get("KEOS_GOSEC_BAAS_INGRESS_PATH");
        String endPointPolicies = baasPath + "/management/policies";
        String endPointCollectionsPolicies = baasPath + "/management/policies/domains";

        if (tenantOrig != null) {
            // Set REST connection
            commonspec.setCCTConnection(tenantOrig, tenantLoginInfo);
        }

        if (endPoint == null) {
            endPoint = getResourceEndpoint(resource);
        }
        endPointResource = endPoint + resourcePrefix + resourceId;

        try {
            assertThat(commonspec.getRestHost().isEmpty() || commonspec.getRestPort().isEmpty());

            if ((resource.equals("policy")) || (resource.equals("collectionPolicy"))) {

                if (resource.equals("collectionPolicy")) {
                    endPointPolicies = endPointCollectionsPolicies;
                }

                restSpec.sendRequestNoDataTable("GET", endPointPolicies, loginInfo, null, null);
                if (commonspec.getResponse().getStatusCode() == 200) {
                    String policyId = getPolicyIdFromResponse(commonspec.getResponse().getResponse(), resourceId, true);
                    if (!policyId.equals("")) {
                        commonspec.getLogger().debug("PolicyId obtained: {}", policyId);
                        endPointResource = endPoint + resourcePrefix + policyId;
                    } else {
                        endPointResource = endPoint + resourcePrefix + "thisPolicyDoesNotExistId";
                    }
                }
            }

            restSpec.sendRequestNoDataTable("GET", endPointResource, loginInfo, null, null);

            if (commonspec.getResponse().getStatusCode() == 200) {
                // Delete resource if exists
                restSpec.sendRequestNoDataTable("DELETE", endPointResource, loginInfo, null, null);
                commonspec.getLogger().warn("Resource {} deleted", resourceId);

                try {
                    assertThat(commonspec.getResponse().getStatusCode()).isIn(expectedStatusDelete);
                } catch (AssertionError e) {
                    commonspec.getLogger().warn("Error deleting Resource {}: {}", resourceId, commonspec.getResponse().getResponse());
                    throw e;
                }
            } else {
                commonspec.getLogger().warn("Resource {} with id {} not found so it's not deleted", resource, resourceId);
            }
        } catch (Exception e) {
            commonspec.getLogger().error("Rest Host or Rest Port are not initialized {}: {}", commonspec.getRestHost(), commonspec.getRestPort());
            throw e;
        }
    }

    /**
     * Retrieve id from policy
     *
     * @param tag             : whether it is a tag policy or not (OPTIONAL)
     * @param policyName      : policy name to obtain id from
     * @param tenantOrig      : tenant where policy lives (OPTIONAL)
     * @param tenantLoginInfo : user and password to log into tenant (OPTIONAL)
     * @param envVar          : thread variable where to store result
     * @throws Exception
     */
    @When("^I get id from( tag)? policy with name '(.+?)'( in tenant '(.+?)')?( with tenant user and tenant password '(.+:.+?)')? and save it in environment variable '(.+?)'$")
    public void getPolicyId(String tag, String policyName, String tenantOrig, String tenantLoginInfo, String envVar) throws Exception {
        if (ThreadProperty.get("isKeosEnv") != null && ThreadProperty.get("isKeosEnv").equals("true")) {
            getPolicyIdKeos(tag, policyName, tenantOrig, tenantLoginInfo, envVar);
        } else {
            getPolicyIdDcos(tag, policyName, tenantOrig, tenantLoginInfo, envVar);
        }
    }

    private void getPolicyIdDcos(String tag, String policyName, String tenantOrig, String tenantLoginInfo, String envVar) throws Exception {
        String endPoint = "/service/gosecmanagement" + ThreadProperty.get("API_POLICIES");
        String managementBaasVersion = ThreadProperty.get("gosec-management-baas_version");
        if (managementBaasVersion != null) {
            endPoint = "/service/gosec-management-baas/management/policies";
        }
        if (tag != null) {
            endPoint = "/service/gosecmanagement" + ThreadProperty.get("API_TAGS");
            if (managementBaasVersion != null) {
                endPoint = "/service/gosec-management-baas/management/policies/tags";
            }
        }
        getPolicyIdCommon(endPoint, policyName, tenantOrig, tenantLoginInfo, envVar);
    }

    private void getPolicyIdKeos(String tag, String policyName, String tenantOrig, String tenantLoginInfo, String envVar) throws Exception {
        String baasPath = ThreadProperty.get("KEOS_GOSEC_BAAS_INGRESS_PATH");
        String endPoint = baasPath + "/management/policies";

        if (tag != null) {
            endPoint += "/tags";
        }
        getPolicyIdCommon(endPoint, policyName, tenantOrig, tenantLoginInfo, envVar);
    }

    private void getPolicyIdCommon(String endPoint, String policyName, String tenantOrig, String tenantLoginInfo, String envVar) throws Exception {
        if (tenantOrig != null) {
            // Set REST connection
            commonspec.setCCTConnection(tenantOrig, tenantLoginInfo);
        }

        assertThat(commonspec.getRestHost().isEmpty() || commonspec.getRestPort().isEmpty());
        restSpec.sendRequestNoDataTable("GET", endPoint, null, null, null);
        if (commonspec.getResponse().getStatusCode() == 200) {
            String policyId = getPolicyIdFromResponse(commonspec.getResponse().getResponse(), policyName, true);
            if (policyId.equals("")) {
                fail("Error obtaining ID from policy " + policyName);
            } else {
                ThreadProperty.set(envVar, policyId);
            }
        } else {
            if (commonspec.getResponse().getStatusCode() == 404) {
                fail("Error obtaining policies from gosecmanagement {} (Response code = " + commonspec.getResponse().getStatusCode() + ")", endPoint);
            }
        }
    }

    /**
     * Create tenant in cluster
     *
     * @param tenantId      : name of the tenant to be created
     * @param baseData      : base information to use for request
     * @param type          : type of base info (enum value) (OPTIONAL)
     * @param modifications : modifications to perform over base data
     * @throws Exception
     */
    @When("^I create tenant '(.+?)' if it does not exist based on '([^:]+?)'( as '(json|string|gov)')? with:$")
    public void createTenant(String tenantId, String baseData, String type, DataTable modifications) throws Exception {
        // Set REST connection
        commonspec.setCCTConnection(null, null);

        String endPoint = "/service/gosec-identities-daas/identities/tenants";

        if (ThreadProperty.get("isKeosEnv") != null && ThreadProperty.get("isKeosEnv").equals("true")) {
            endPoint = "/gosec/identities/identities/tenants";
        }

        String endPointResource = endPoint + "/" + tenantId;
        Integer expectedStatus = 201;
        assertThat(commonspec.getRestHost().isEmpty() || commonspec.getRestPort().isEmpty());
        restSpec.sendRequestNoDataTable("GET", endPointResource, null, null, null);
        if (commonspec.getResponse().getStatusCode() == 200) {
            commonspec.getLogger().warn("Tenant {} already exist - not created", tenantId);
        } else {
            restSpec.sendRequest("POST", endPoint, null, baseData, type, modifications);
            try {
                assertThat(commonspec.getResponse().getStatusCode()).isEqualTo(expectedStatus);
            } catch (AssertionError e) {
                commonspec.getLogger().warn("Error creating Tenant {}: {}", tenantId, commonspec.getResponse().getResponse());
                throw e;
            }
        }
    }

    /**
     * Delete tenant
     *
     * @param tenantId : tenant to be deleted
     * @throws Exception
     */

    @When("^I delete tenant '(.+?)' if it exists$")
    public void deleteTenant(String tenantId) throws Exception {
        // Set REST connection
        commonspec.setCCTConnection(null, null);

        String endPoint = "/service/gosec-identities-daas/identities/tenants";

        if (ThreadProperty.get("isKeosEnv") != null && ThreadProperty.get("isKeosEnv").equals("true")) {
            endPoint = "/gosec/identities/identities/tenants";
        }

        String endPointResource = endPoint + "/" + tenantId;
        Integer expectedStatus = 204;
        assertThat(commonspec.getRestHost().isEmpty() || commonspec.getRestPort().isEmpty());
        restSpec.sendRequestNoDataTable("GET", endPointResource, null, null, null);
        if (commonspec.getResponse().getStatusCode() == 200) {
            restSpec.sendRequestNoDataTable("DELETE", endPointResource, null, null, null);
            commonspec.getLogger().warn("Tenant {} deleted", tenantId);
            try {
                assertThat(commonspec.getResponse().getStatusCode()).isEqualTo(expectedStatus);
            } catch (AssertionError e) {
                commonspec.getLogger().warn("Error deleting Tenant {}: {}", tenantId, commonspec.getResponse().getResponse());
                throw e;
            }
        } else {
            commonspec.getLogger().warn("Tenant {} does not exist - not deleted", tenantId);
        }
    }

    /**
     * Include resource in tenant
     *
     * @param resource   : resource type to be included (enum value)
     * @param resourceId : resource name
     * @param tenantId   : tenant where to store resource in
     * @throws Exception
     */
    @When("^I include '(user|group)' '(.+?)' in tenant '(.+?)'$")
    public void includeResourceInTenant(String resource, String resourceId, String tenantId) throws Exception {
        String endPointGetAllUsers = "/service/gosec-identities-daas/identities/users";
        String endPointGetAllGroups = "/service/gosec-identities-daas/identities/groups";
        String endPointTenant = "/service/gosec-identities-daas/identities/tenants/" + tenantId;
        String requestOp = "PATCH";
        Integer expectedStatusCode = 204;

        if (ThreadProperty.get("isKeosEnv") != null && ThreadProperty.get("isKeosEnv").equals("true")) {
            endPointGetAllUsers = "/gosec/baas/management/users";
            endPointGetAllGroups = "/gosec/baas/management/groups";
            endPointTenant = "/gosec/baas/management/tenant?tid=" + tenantId;
            requestOp = "PUT";
            expectedStatusCode = 200;
        }

        assertThat(commonspec.getRestHost().isEmpty() || commonspec.getRestPort().isEmpty());
        String uidOrGid = "uid";
        String uidOrGidTenant = "uids";
        String endPointGosec = endPointGetAllUsers;

        if (resource.equals("group")) {
            uidOrGid = "gid";
            uidOrGidTenant = "gids";
            endPointGosec = endPointGetAllGroups;
        }

        // Set REST connection
        commonspec.setCCTConnection(null, null);

        restSpec.sendRequestNoDataTable("GET", endPointGosec, null, null, null);
        if (commonspec.getResponse().getStatusCode() == 200) {
            if (commonspec.getResponse().getResponse().contains("\"" + uidOrGid + "\":\"" + resourceId + "\"")) {
                restSpec.sendRequestNoDataTable("GET", endPointTenant, null, null, null);
                if (commonspec.getResponse().getStatusCode() == 200) {
                    JsonObject jsonTenantInfo = new JsonObject(JsonValue.readHjson(commonspec.getResponse().getResponse()).asObject());
                    if (((JsonArray) jsonTenantInfo.get(uidOrGidTenant)).values().contains(JsonValue.valueOf(resourceId))) {
                        commonspec.getLogger().debug("{} is already included in tenant", resourceId);
                    } else {
                        ((JsonArray) jsonTenantInfo.get(uidOrGidTenant)).add(resourceId);
                        Future<Response> response = commonspec.generateRequest(requestOp, false, null, null, endPointTenant, JsonValue.readHjson(jsonTenantInfo.toString()).toString(), "json", "");
                        commonspec.setResponse(requestOp, response.get());
                        if (commonspec.getResponse().getStatusCode() != expectedStatusCode) {
                            throw new Exception("Error adding " + resource + " " + resourceId + " in tenant " + tenantId + " - Status code: " + commonspec.getResponse().getStatusCode());
                        }
                    }
                } else {
                    throw new Exception("Error obtaining info from tenant " + tenantId + " - Status code: " + commonspec.getResponse().getStatusCode());
                }
            } else {
                throw new Exception(resource + " " + resourceId + " doesn't exist in Gosec");
            }
        } else {
            throw new Exception("Error obtaining " + resource + "s - Status code: " + commonspec.getResponse().getStatusCode());
        }
    }

    /**
     * Obtain id from profile
     *
     * @param profileName     : profile to obtain id from
     * @param tenantOrig      : tenant where policy lives (OPTIONAL)
     * @param tenantLoginInfo : user and password to log into tenant (OPTIONAL)
     * @param envVar          : thread variable where to store result
     * @throws Exception
     */
    @When("^I get id from profile with name '(.+?)'( in tenant '(.+?)')?( with tenant user and tenant password '(.+:.+?)')? and save it in environment variable '(.+?)'$")
    public void getProfiled(String profileName, String tenantOrig, String tenantLoginInfo, String envVar) throws Exception {
        String endPoint = "/service/gosec-identities-daas/identities/profiles";
        if (tenantOrig != null) {
            endPoint = "/service/gosec-identities-daas/identities/profiles?tid=" + tenantOrig;
        }

        if (tenantOrig != null) {
            // Set REST connection
            commonspec.setCCTConnection(tenantOrig, tenantLoginInfo);
        }

        assertThat(commonspec.getRestHost().isEmpty() || commonspec.getRestPort().isEmpty());
        restSpec.sendRequestNoDataTable("GET", endPoint, null, null, null);
        if (commonspec.getResponse().getStatusCode() == 200) {
            commonspec.runLocalCommand("echo '" + commonspec.getResponse().getResponse() + "' | jq '.list[] | select (.name == \"" + profileName + "\").pid' | sed s/\\\"//g");
            commonspec.runCommandLoggerAndEnvVar(0, envVar, Boolean.TRUE);
            if (ThreadProperty.get(envVar) == null || ThreadProperty.get(envVar).trim().equals("")) {
                fail("Error obtaining ID from profile " + profileName);
            }
        } else {
            commonspec.getLogger().warn("Profile with id: {} does not exist", profileName);
        }
    }

    /**
     * Obtain json from policy
     *
     * @param tag             : whether it is a tag policy or not (OPTIONAL)
     * @param policyName      : policy name to obtain json from
     * @param tenantOrig      : tenant where policy lives (OPTIONAL)
     * @param tenantLoginInfo : user and password to log into tenant (OPTIONAL)
     * @param envVar          : thread variable where to store result (OPTIONAL)
     * @param fileName        : file name where to store result (OPTIONAL)
     * @throws Exception
     */
    @When("^I get json from( tag)? policy with name '(.+?)'( in tenant '(.+?)')?( with tenant user and tenant password '(.+:.+?)')? and save it( in environment variable '(.*?)')?( in file '(.*?)')?$")
    public void getPolicyJson(String tag, String policyName, String tenantOrig, String tenantLoginInfo, String envVar, String fileName) throws Exception {
        if (ThreadProperty.get("isKeosEnv") != null && ThreadProperty.get("isKeosEnv").equals("true")) {
            getPolicyJsonKeos(tag, policyName, tenantOrig, tenantLoginInfo, envVar, fileName);
        } else {
            getPolicyJsonDcos(tag, policyName, tenantOrig, tenantLoginInfo, envVar, fileName);
        }
    }

    private void getPolicyJsonDcos(String tag, String policyName, String tenantOrig, String tenantLoginInfo, String envVar, String fileName) throws Exception {
        String endPoint = "/service/gosecmanagement/api/policy";
        String newEndPoint = "/service/gosecmanagement/api/policies";
        String getEndPoint = "/service/gosecmanagement/api/policy/";
        String errorMessage = "api/policies";
        String errorMessage2 = "api/policy";
        String managementBaasVersion = ThreadProperty.get("gosec-management-baas_version");

        if (managementBaasVersion != null) {
            endPoint = "/service/gosec-management-baas/management/policies";
            getEndPoint = "/service/gosec-management-baas/management/policy?pid=";
        }

        if (tag != null) {
            if (managementBaasVersion != null) {
                endPoint = "/service/gosec-management-baas/management/policies";
                newEndPoint = "/service/gosec-management-baas/management/api/policies/tags";
            } else {
                endPoint = "/service/gosecmanagement/api/policy/tag";
                newEndPoint = "/service/gosecmanagement/api/policies/tags";
            }
            errorMessage = "api/policies/tags";
            errorMessage2 = "api/policy/tag";
        }

        if (tenantOrig != null) {
            // Set REST connection
            commonspec.setCCTConnection(tenantOrig, tenantLoginInfo);
        }

        assertThat(commonspec.getRestHost().isEmpty() || commonspec.getRestPort().isEmpty());
        restSpec.sendRequestNoDataTable("GET", endPoint, null, null, null);
        if (commonspec.getResponse().getStatusCode() == 200) {
            String policyId = getPolicyIdFromResponse(commonspec.getResponse().getResponse(), policyName, managementBaasVersion != null);
            if (policyId.equals("")) {
                fail("Policy does not exist -> " + policyName);
            }
            restSpec.sendRequestNoDataTable("GET", getEndPoint + policyId, null, null, null);
            if (commonspec.getResponse().getStatusCode() == 200) {
                savePolicyJsonInEnvVarAndFile(commonspec.getResponse().getResponse(), policyName, envVar, fileName);
            } else {
                fail("Error obtaining policy with ID {} from gosecmanagement (Response code = " + commonspec.getResponse().getStatusCode() + ")", policyId);
            }
        } else {
            if (commonspec.getResponse().getStatusCode() == 404) {
                commonspec.getLogger().warn("Error 404 accessing endpoint {}: checking the new endpoint for Gosec 1.1.1", endPoint);
                restSpec.sendRequestNoDataTable("GET", newEndPoint, null, null, null);
                if (commonspec.getResponse().getStatusCode() == 200) {
                    String policyId = getPolicyIdFromResponse(commonspec.getResponse().getResponse(), policyName, false);
                    restSpec.sendRequestNoDataTable("GET", "/service/gosecmanagement/api/policy?id=" + policyId, null, null, null);
                    if (commonspec.getResponse().getStatusCode() == 200) {
                        savePolicyJsonInEnvVarAndFile(commonspec.getResponse().getResponse(), policyName, envVar, fileName);
                    } else {
                        fail("Error obtaining policy with ID {} from gosecmanagement (Response code = " + commonspec.getResponse().getStatusCode() + ")", policyId);
                    }
                } else {
                    fail("Error obtaining policies from gosecmanagement {} (Response code = " + commonspec.getResponse().getStatusCode() + ")", errorMessage);
                }
            } else {
                fail("Error obtaining policies from gosecmanagement {} (Response code = " + commonspec.getResponse().getStatusCode() + ")", errorMessage2);
            }
        }
    }

    private void getPolicyJsonKeos(String tag, String policyName, String tenantOrig, String tenantLoginInfo, String envVar, String fileName) throws Exception {
        String baasPath = ThreadProperty.get("KEOS_GOSEC_BAAS_INGRESS_PATH");
        String endPoint = baasPath + "/management/policy";
        String endPointPolicies = baasPath + "/management/policies";

        if (tag != null) {
            endPoint = baasPath + "management/policy/tags";
            endPointPolicies = baasPath + "/management/policies/tags";
        }

        if (tenantOrig != null) {
            // Set REST connection
            commonspec.setCCTConnection(tenantOrig, tenantLoginInfo);
        }

        assertThat(commonspec.getRestHost().isEmpty() || commonspec.getRestPort().isEmpty());
        restSpec.sendRequestNoDataTable("GET", endPointPolicies, null, null, null);
        if (commonspec.getResponse().getStatusCode() == 200) {
            String policyId = getPolicyIdFromResponse(commonspec.getResponse().getResponse(), policyName, true);
            if (policyId.equals("")) {
                fail("Policy does not exist -> " + policyName);
            }
            restSpec.sendRequestNoDataTable("GET", endPoint + "?pid=" + policyId, null, null, null);

            if (commonspec.getResponse().getStatusCode() == 200) {
                savePolicyJsonInEnvVarAndFile(commonspec.getResponse().getResponse(), policyName, envVar, fileName);
            } else {
                fail("Error obtaining policy with ID {} from gosecmanagement (Response code = " + commonspec.getResponse().getStatusCode() + ")", policyId);
            }
        } else {
            fail("Error obtaining policies from gosecmanagement (Response code = " + commonspec.getResponse().getStatusCode() + ")");
        }
    }

    private void savePolicyJsonInEnvVarAndFile(String response, String policyName, String envVar, String fileName) throws IOException {
        if (envVar != null) {
            ThreadProperty.set(envVar, response);

            if (ThreadProperty.get(envVar) == null || ThreadProperty.get(envVar).trim().equals("")) {
                fail("Error obtaining JSON from policy " + policyName);
            }
        }
        if (fileName != null) {
            // Create file (temporary) and set path to be accessible within test
            File tempDirectory = new File(System.getProperty("user.dir") + "/target/test-classes/");
            String absolutePathFile = tempDirectory.getAbsolutePath() + "/" + fileName;
            commonspec.getLogger().debug("Creating file {} in 'target/test-classes'", absolutePathFile);
            // Note that this Writer will delete the file if it exists
            Writer out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(absolutePathFile), StandardCharsets.UTF_8));
            try {
                out.write(response);
            } catch (Exception e) {
                commonspec.getLogger().error("Custom file {} hasn't been created:\n{}", absolutePathFile, e.toString());
            } finally {
                out.close();
            }

            Assertions.assertThat(new File(absolutePathFile).isFile());
        }
    }

    /**
     * Include group inside profile
     *
     * @param groupId         : id of the group to be included in profile
     * @param profileId       : id of the profile where to include group
     * @param tenantOrig      : tenant where profile lives (OPTIONAL)
     * @param tenantLoginInfo : user and password to log into tenant (OPTIONAL)
     * @throws Exception
     */
    @When("^I include group '(.+?)' in profile '(.+?)'( in tenant '(.+?)')?( with tenant user and tenant password '(.+:.+?)')?( with user and password '(.+:.+?)')?$")
    public void includeGroupInProfile(String groupId, String profileId, String tenantOrig, String tenantLoginInfo, String loginInfo) throws Exception {
        String endPointGetGroup = "/service/gosecmanagement/api/group?id=" + groupId;
        String endPointGetProfile = "/service/gosecmanagement/api/profile?id=" + profileId;
        String groups = "groups";
        String pid = "pid";
        String id = "id";
        String roles = "roles";
        Boolean content = false;

        if (tenantOrig != null) {
            // Set REST connection
            commonspec.setCCTConnection(tenantOrig, tenantLoginInfo);
        }

        assertThat(commonspec.getRestHost().isEmpty() || commonspec.getRestPort().isEmpty());

        restSpec.sendRequestNoDataTable("GET", endPointGetGroup, null, null, null);
        if (commonspec.getResponse().getStatusCode() == 200) {
            JsonObject jsonGroupInfo = new JsonObject(JsonValue.readHjson(commonspec.getResponse().getResponse()).asObject());
            restSpec.sendRequestNoDataTable("GET", endPointGetProfile, null, null, null);
            if (commonspec.getResponse().getStatusCode() == 200) {
                JsonObject jsonProfileInfo = new JsonObject(JsonValue.readHjson(commonspec.getResponse().getResponse()).asObject());
                // Get groups from profile
                JsonArray jsonGroups = (JsonArray) jsonProfileInfo.get(groups);
                // Get size of groups
                String[] stringGroups = new String[jsonGroups.size() + 1];
                // Create json for put
                JSONObject putObject = new JSONObject(commonspec.getResponse().getResponse());
                // Remove groups and roles in json
                putObject.remove(groups);
                putObject.remove(roles);

                for (int i = 0; i < jsonGroups.size(); i++) {
                    String jsonIds = ((JsonObject) jsonGroups.get(i)).getString("id", "");

                    if (jsonIds.equals(groupId)) {
                        commonspec.getLogger().warn("{} is already included in the profile {}", groupId, profileId);
                        content = true;
                        break;
                    } else {
                        stringGroups[i] = jsonIds;
                    }
                }

                if (!content) {
                    // Add new group in array of gids
                    stringGroups[jsonGroups.size()] = groupId;
                    // Add gids array to new json for PUT request
                    putObject.put("gids", stringGroups);

                    commonspec.getLogger().warn("Json for PUT request---> {}", putObject);
                    Future<Response> response = commonspec.generateRequest("PUT", false, null, null, endPointGetProfile, JsonValue.readHjson(putObject.toString()).toString(), "json", "");
                    commonspec.setResponse("PUT", response.get());
                    if (commonspec.getResponse().getStatusCode() != 204) {
                        throw new Exception("Error adding Group: " + groupId + " in Profile " + profileId + " - Status code: " + commonspec.getResponse().getStatusCode());
                    }
                }

            } else {
                throw new Exception("Error obtaining Profile: " + profileId + "- Status code: " + commonspec.getResponse().getStatusCode());
            }

        } else {
            throw new Exception("Error obtaining Group: " + groupId + "- Status code: " + commonspec.getResponse().getStatusCode());
        }
    }

    /**
     * Convert DataTable to modifiable list
     *
     * @param dataTable : DataTable data
     * @return
     */
    private List<List<String>> convertDataTableToModifiableList(DataTable dataTable) {
        List<List<String>> lists = dataTable.asLists(String.class);
        List<List<String>> updateableLists = new ArrayList<>();
        for (int i = 0; i < lists.size(); i++) {
            List<String> list = lists.get(i);
            List<String> updateableList = new ArrayList<>();
            for (int j = 0; j < list.size(); j++) {
                updateableList.add(j, list.get(j));
            }
            updateableLists.add(i, updateableList);
        }
        return updateableLists;
    }

    /**
     * Updates a resource in gosec management if the resourceId exists previously.
     *
     * @param resource        : type of resource (policy, user, group or tenant)
     * @param resourceId      : policy name, userId, groupId or tenantId
     * @param tenantOrig      : tenant where resource lives (OPTIONAL)
     * @param tenantLoginInfo : user and password to log into tenant (OPTIONAL)
     * @param loginInfo       : user and password to log in service (OPTIONAL)
     * @param type            : type of data (json,string,gov) (OPTIONAL)
     * @param modifications   : data to modify the resource
     * @throws Exception if the resource does not exists or the request fails
     */
    @When("^I update '(policy|user|group|tenant)' '(.+?)'( in tenant '(.+?)')?( with tenant user and tenant password '(.+:.+?)')?( with user and password '(.+:.+?)')? based on '([^:]+?)'( as '(json|string|gov)')? with:$")
    public void updateResource(String resource, String resourceId, String tenantOrig, String tenantLoginInfo, String loginInfo, String baseData, String type, DataTable modifications) throws Exception {
        if (ThreadProperty.get("isKeosEnv") != null && ThreadProperty.get("isKeosEnv").equals("true")) {
            updateResourceKeos(resource, resourceId, tenantOrig, tenantLoginInfo, loginInfo, baseData, type, modifications);
        } else {
            updateResourceDcos(resource, resourceId, tenantOrig, tenantLoginInfo, loginInfo, baseData, type, modifications);
        }
    }

    private void updateResourceDcos(String resource, String resourceId, String tenantOrig, String tenantLoginInfo, String loginInfo, String baseData, String type, DataTable modifications) throws Exception {
        Integer[] expectedStatusUpdate = {200, 201, 204};
        String endPointPolicy = "/service/gosecmanagement" + ThreadProperty.get("API_POLICY");
        String endPointPolicies = "/service/gosecmanagement" + ThreadProperty.get("API_POLICIES");
        String endPoint = "";
        String endPointResource = "";
        String managementBaasVersion = ThreadProperty.get("gosec-management-baas_version");

        if (tenantOrig != null) {
            // Set REST connection
            commonspec.setCCTConnection(tenantOrig, tenantLoginInfo);
        }

        if (managementBaasVersion != null) {
            endPointPolicies = "/service/gosec-management-baas/management/policies";
            endPointPolicy = "/service/gosec-management-baas/management/policy?pid=";
        }

        if (resource.equals("policy")) {
            endPoint = "/service/gosecmanagement" + ThreadProperty.get("API_POLICY");
            if (managementBaasVersion != null) {
                endPoint = "/service/gosec-management-baas/management/policy?pid=";
            }
        } else {
            if (resource.equals("user")) {
                endPoint = "/service/gosecmanagement" + ThreadProperty.get("API_USER");
                if (managementBaasVersion != null) {
                    endPoint = "/service/gosec-management-baas/management/user?uid=";
                }
            } else {
                endPoint = "/service/gosecmanagement" + ThreadProperty.get("API_GROUP");
                if (managementBaasVersion != null) {
                    endPoint = "/service/gosec-management-baas/management/group?gid=";
                }
            }
            if (resource.equals("tenant")) {
                endPoint = "/service/gosec-identities-daas/identities/tenants/";
            }
        }

        try {
            assertThat(commonspec.getRestHost().isEmpty() || commonspec.getRestPort().isEmpty());

            if (resource.equals("policy")) {
                restSpec.sendRequestNoDataTable("GET", endPointPolicies, loginInfo, null, null);
                if (commonspec.getResponse().getStatusCode() == 200) {
                    String policyId = getPolicyIdFromResponse(commonspec.getResponse().getResponse(), resourceId, managementBaasVersion != null);
                    if (!policyId.equals("")) {
                        commonspec.getLogger().debug("PolicyId obtained: {}", policyId);
                        endPointResource = endPointPolicy + policyId;
                    } else {
                        endPointResource = endPointPolicy + "thisPolicyDoesNotExistId";
                    }
                }
            } else {
                endPointResource = endPoint + resourceId;
            }

            restSpec.sendRequestNoDataTable("GET", endPointResource, loginInfo, null, null);

            if (commonspec.getResponse().getStatusCode() == 200) {
                if (resource.equals("tenant")) {
                    restSpec.sendRequest("PATCH", endPointResource, loginInfo, baseData, type, modifications);
                } else {
                    restSpec.sendRequest("PUT", endPointResource, loginInfo, baseData, type, modifications);
                }
                commonspec.getLogger().warn("Resource {}:{} updated", resource, resourceId);

                try {
                    assertThat(commonspec.getResponse().getStatusCode()).isIn(expectedStatusUpdate);
                } catch (AssertionError e) {
                    commonspec.getLogger().error("Error updating Resource {} {}: {}", resource, resourceId, commonspec.getResponse().getResponse());
                    throw e;
                }
            } else {
                commonspec.getLogger().error("Resource {}:{} not found so it's not updated", resource, resourceId);
            }
        } catch (Exception e) {
            commonspec.getLogger().error("Rest Host or Rest Port are not initialized {}: {}", commonspec.getRestHost(), commonspec.getRestPort());
            throw e;
        }
    }

    private void updateResourceKeos(String resource, String resourceId, String tenantOrig, String tenantLoginInfo, String loginInfo, String baseData, String type, DataTable modifications) throws Exception {
        Integer[] expectedStatusUpdate = {200, 201, 204};
        String resourcePrefix = getResourcePrefix(resource);
        String baasPath = ThreadProperty.get("KEOS_GOSEC_BAAS_INGRESS_PATH");
        String endPointPolicies = baasPath + "/management/policies";
        String endPoint = getResourceEndpoint(resource);
        String endPointResource = endPoint + resourcePrefix + resourceId;


        if (tenantOrig != null) {
            // Set REST connection
            commonspec.setCCTConnection(tenantOrig, tenantLoginInfo);
        }

        if (resource.equals("tenant")) {
            //TODO K8s
            fail("Tenant resource not supported yet in K8s spec");
        }

        try {
            assertThat(commonspec.getRestHost().isEmpty() || commonspec.getRestPort().isEmpty());

            if (resource.equals("policy")) {
                restSpec.sendRequestNoDataTable("GET", endPointPolicies, loginInfo, null, null);
                if (commonspec.getResponse().getStatusCode() == 200) {
                    String policyId = getPolicyIdFromResponse(commonspec.getResponse().getResponse(), resourceId, true);
                    if (!policyId.equals("")) {
                        commonspec.getLogger().debug("PolicyId obtained: {}", policyId);
                        endPointResource = endPoint + resourcePrefix + policyId;
                    } else {
                        endPointResource = endPoint + resourcePrefix + "thisPolicyDoesNotExistId";
                    }
                }
            }

            restSpec.sendRequestNoDataTable("GET", endPointResource, loginInfo, null, null);

            if (commonspec.getResponse().getStatusCode() == 200) {
                if (resource.equals("tenant")) {
                    //TODO K8s
                    fail("Tenant resource not supported yet in K8s spec");
                    //restSpec.sendRequest("PATCH", endPointResource, loginInfo, baseData, type, modifications);
                } else {
                    restSpec.sendRequest("PUT", endPointResource, loginInfo, baseData, type, modifications);
                }
                commonspec.getLogger().warn("Resource {}:{} updated", resource, resourceId);

                try {
                    assertThat(commonspec.getResponse().getStatusCode()).isIn(expectedStatusUpdate);
                } catch (AssertionError e) {
                    commonspec.getLogger().error("Error updating Resource {} {}: {}", resource, resourceId, commonspec.getResponse().getResponse());
                    throw e;
                }
            } else {
                commonspec.getLogger().error("Resource {}:{} not found so it's not updated", resource, resourceId);
            }
        } catch (Exception e) {
            commonspec.getLogger().error("Rest Host or Rest Port are not initialized {}: {}", commonspec.getRestHost(), commonspec.getRestPort());
            throw e;
        }
    }

    /**
     * Removes user or group from tenant if the resource exists and has been assigned previously
     *
     * @param resource   : type of resource (user or group)
     * @param resourceId : userId or groupId
     * @param tenantId   : tenant to remove resource from
     * @throws Exception if the resource does not exists or the request fails
     */
    @When("^I remove '(user|group)' '(.+?)' from tenant '(.+?)'$")
    public void removeResourceInTenant(String resource, String resourceId, String tenantId) throws Exception {
        String endPointGetAllUsers = "/service/gosec-identities-daas/identities/users";
        String endPointGetAllGroups = "/service/gosec-identities-daas/identities/groups";
        String endPointTenant = "/service/gosec-identities-daas/identities/tenants/" + tenantId;
        assertThat(commonspec.getRestHost().isEmpty() || commonspec.getRestPort().isEmpty());
        String uidOrGid = "uid";
        String uidOrGidTenant = "uids";
        String endPointGosec = endPointGetAllUsers;
        if (resource.equals("group")) {
            uidOrGid = "gid";
            uidOrGidTenant = "gids";
            endPointGosec = endPointGetAllGroups;
        }

        // Set REST connection
        commonspec.setCCTConnection(null, null);

        restSpec.sendRequestNoDataTable("GET", endPointGosec, null, null, null);
        if (commonspec.getResponse().getStatusCode() == 200) {
            if (commonspec.getResponse().getResponse().contains("\"" + uidOrGid + "\":\"" + resourceId + "\"")) {
                restSpec.sendRequestNoDataTable("GET", endPointTenant, null, null, null);
                if (commonspec.getResponse().getStatusCode() == 200) {
                    JsonObject jsonTenantInfo = new JsonObject(JsonValue.readHjson(commonspec.getResponse().getResponse()).asObject());
                    if (((JsonArray) jsonTenantInfo.get(uidOrGidTenant)).values().contains(JsonValue.valueOf(resourceId))) {
                        // remove resource from tenant
                        // Get groups/users from tenant
                        JsonArray jsonGroups = (JsonArray) jsonTenantInfo.get(uidOrGidTenant);
                        // Create new string for new data without resource
                        ArrayList<String> stringGroups = new ArrayList<String>();
                        // Create json for put
                        JSONObject putObject = new JSONObject(commonspec.getResponse().getResponse());
                        // Remove ids in json
                        putObject.remove(uidOrGidTenant);
                        // create new array with values without resourceId
                        for (int i = 0; i < jsonGroups.size(); i++) {
                            String jsonIds = jsonGroups.get(i).toString().substring(1, jsonGroups.get(i).toString().length() - 1);
                            if (jsonIds.equals(resourceId)) {
                                commonspec.getLogger().warn("{} {} removed from tenant {}", resource, resourceId, tenantId);
                            } else {
                                stringGroups.add(jsonIds);
                            }
                        }
                        putObject.put(uidOrGidTenant, new JSONArray(stringGroups));
                        commonspec.getLogger().debug("Json for PATCH request---> {}", putObject);
                        Future<Response> response = commonspec.generateRequest("PATCH", false, null, null, endPointTenant, JsonValue.readHjson(putObject.toString()).toString(), "json", "");
                        commonspec.setResponse("PATCH", response.get());
                        if (commonspec.getResponse().getStatusCode() != 204) {
                            throw new Exception("Error removing " + resource + " " + resourceId + " in tenant " + tenantId + " - Status code: " + commonspec.getResponse().getStatusCode());
                        }

                    } else {
                        commonspec.getLogger().error("{} is not included in tenant -> not removed", resourceId);
                    }
                } else {
                    throw new Exception("Error obtaining info from tenant " + tenantId + " - Status code: " + commonspec.getResponse().getStatusCode());
                }
            } else {
                throw new Exception(resource + " " + resourceId + " doesn't exist in Gosec");
            }
        } else {
            throw new Exception("Error obtaining " + resource + "s - Status code: " + commonspec.getResponse().getStatusCode());
        }
    }


    /**
     * Create custom user in Gosec Management or Gosec Management BaaS
     *
     * @param userName        : name of the user to be created
     * @param tenantOrig      : tenant where resource is gonna be created (OPTIONAL)
     * @param tenantLoginInfo : user and password to log into tenant (OPTIONAL)
     * @param keytab          : creation of keytab in vault (OPTIONAL)
     * @param certificate     : creation of certificate in vault (OPTIONAL)
     * @param groups          : groups for custom user (OPTIONAL)
     * @param doesNotExist    : (IF false) resource will be overwritten if exists previously  (OPTIONAL)
     * @throws Exception
     */
    @When("^I create (custom|system) user '(.+?)'( in tenant '(.+?)')?( with tenant user and tenant password '(.+:.+?)')?( generating keytab)?( generating certificate)?( assigned to groups '(.+?)')?( if it does not exist)?$")
    public void createUserResource(String type, String userName, String tenantOrig, String tenantLoginInfo, String keytab, String certificate, String groups, String doesNotExist) throws Exception {
        Boolean booleanExist = false;
        if (doesNotExist != null) {
            booleanExist = true;
        }
        if (ThreadProperty.get("isKeosEnv") != null && ThreadProperty.get("isKeosEnv").equals("true")) {
            createUserResourceKeos(type, userName, tenantOrig, tenantLoginInfo, keytab, certificate, groups, booleanExist);
        } else {
            createUserResourceDcos(type, userName, tenantOrig, tenantLoginInfo, keytab, certificate, groups, booleanExist);
        }
    }

    private void createUserResourceDcos(String type, String userName, String tenantOrig, String tenantLoginInfo, String keytab, String certificate, String groups, boolean doesNotExist) throws Exception {
        String managementVersion = ThreadProperty.get("gosec-management_version");
        String managementBaasVersion = ThreadProperty.get("gosec-management-baas_version");
        String endPoint = "/service/gosecmanagement" + ThreadProperty.get("API_USER");
        Integer[] expectedStatusDelete = {200, 204};
        Boolean addSourceType = false;
        Boolean managementBaas = false;
        Boolean addEnable = false;
        String endPointResource = "";
        String uid = userName.replaceAll("\\s+", ""); //delete white spaces
        String newEndPoint = "";
        String data = "";

        if (tenantOrig != null) {
            // Set REST connection
            commonspec.setCCTConnection(tenantOrig, tenantLoginInfo);
        }

        if (endPoint.contains("id")) {
            newEndPoint = endPoint.replace("?id=", "");
        } else {
            newEndPoint = endPoint.substring(0, endPoint.length() - 1);
        }

        if (managementBaasVersion != null) {
            endPoint = "/service/gosec-management-baas/management/user?uid=";
            newEndPoint = "/service/gosec-management-baas/management/user";
        }

        try {
            assertThat(commonspec.getRestHost().isEmpty() || commonspec.getRestPort().isEmpty());

            if (managementBaasVersion != null) {  //vamos por management-baas
                managementBaas = true;
                String[] gosecBaasVersionArray = managementBaasVersion.split("\\.");
                if (Integer.parseInt(gosecBaasVersionArray[0]) >= 1 && Integer.parseInt(gosecBaasVersionArray[1]) >= 4) {
                    addEnable = true;
                }
            } else {
                if (managementVersion != null) {  //vamos por management
                    String[] gosecVersionArray = managementVersion.split("\\.");
                    // Add inputSourceType if Gosec >= 1.4.x
                    if (Integer.parseInt(gosecVersionArray[0]) >= 1 && Integer.parseInt(gosecVersionArray[1]) >= 4) {
                        addSourceType = true;
                    }
                } else {
                    fail("Check gosec management or management-baas is available");
                }
            }

            //check if user exists and build json
            endPointResource = endPoint + uid;
            restSpec.sendRequestNoDataTable("GET", endPointResource, null, null, null);

            if (commonspec.getResponse().getStatusCode() != 200) {

                if (!managementBaas) {
                    //json for gosec-management endpoint
                    data = generateManagementUserJson(uid, userName, groups, keytab, certificate, addSourceType);

                } else {
                    //json for gosec-management-baas endpoint
                    data = generateBaasUserJson(uid, userName, groups, keytab, certificate, addEnable, type);
                }

                // Send POST request
                sendIdentitiesPostRequest("user", userName, data, newEndPoint);

            } else {
                if (doesNotExist) {
                    commonspec.getLogger().warn("Custom user:{} already exist", userName);
                } else {
                    // Delete user and send the request again
                    deleteResourceIfExistsDcos("user", userName, tenantOrig, tenantLoginInfo, endPoint, null);

                    try {
                        assertThat(commonspec.getResponse().getStatusCode()).isIn(expectedStatusDelete);
                    } catch (AssertionError e) {
                        commonspec.getLogger().warn("Error deleting User {}: {}", userName, commonspec.getResponse().getResponse());
                        throw e;
                    }
                    //send request again
                    createUserResourceDcos(type, userName, tenantOrig, tenantLoginInfo, keytab, certificate, groups, doesNotExist);
                }
            }

        } catch (Exception e) {
            commonspec.getLogger().error("Rest Host or Rest Port are not initialized {}{}", commonspec.getRestHost(), commonspec.getRestPort());
            throw e;
        }

    }

    private void createUserResourceKeos(String type, String userName, String tenantOrig, String tenantLoginInfo, String keytab, String certificate, String groups, boolean doesNotExist) throws Exception {
        String baasPath = ThreadProperty.get("KEOS_GOSEC_BAAS_INGRESS_PATH");
        String endPoint = baasPath + "/management/user";
        String endPointResource = "";
        Integer[] expectedStatusDelete = {200, 204};
        Boolean addEnable = false;
        String managementBaasVersion = commonspec.kubernetesClient.getDeploymentVersion("gosec-management-baas", "keos-core");

        commonspec.getLogger().debug("gosec-management-baas version: {}", managementBaasVersion);

        String[] gosecBaasVersionArray = managementBaasVersion.split("\\.");
        if (Integer.parseInt(gosecBaasVersionArray[0]) >= 1 && Integer.parseInt(gosecBaasVersionArray[1]) >= 4) {
            addEnable = true;
        }
        String uid = userName.replaceAll("\\s+", ""); //delete white spaces
        String data = generateBaasUserJson(uid, userName, groups, keytab, certificate, addEnable, type);
        if (tenantOrig != null) {
            // Set REST connection
            commonspec.setCCTConnection(tenantOrig, tenantLoginInfo);
        }
        try {
            assertThat(commonspec.getRestHost().isEmpty() || commonspec.getRestPort().isEmpty());
            //check if user exists and build json
            endPointResource = endPoint + "?uid=" + uid;
            restSpec.sendRequestNoDataTable("GET", endPointResource, null, null, null);
            if (commonspec.getResponse().getStatusCode() != 200) {
                // Send POST request
                sendIdentitiesPostRequest("user", userName, data, endPoint);
            } else {
                if (doesNotExist) {
                    commonspec.getLogger().warn("Custom user:{} already exist", userName);
                } else {
                    // Delete user and send the request again
                    deleteResourceIfExistsKeos("user", userName, tenantOrig, tenantLoginInfo, endPoint, null);

                    try {
                        assertThat(commonspec.getResponse().getStatusCode()).isIn(expectedStatusDelete);
                    } catch (AssertionError e) {
                        commonspec.getLogger().warn("Error deleting User {}: {}", userName, commonspec.getResponse().getResponse());
                        throw e;
                    }
                    //send request again
                    createUserResourceKeos(type, userName, tenantOrig, tenantLoginInfo, keytab, certificate, groups, doesNotExist);
                }
            }
        } catch (Exception e) {
            commonspec.getLogger().error("Rest Host or Rest Port are not initialized {}{}", commonspec.getRestHost(), commonspec.getRestPort());
            throw e;
        }
    }


    /**
     * Create custom group in Gosec Management or Gosec Management BaaS
     *
     * @param groupName       : name of the user to be created
     * @param tenantOrig      : tenant where resource is gonna be created (OPTIONAL)
     * @param tenantLoginInfo : user and password to log into tenant (OPTIONAL)
     * @param users           : users for custom group (OPTIONAL)
     * @param groups          : groups for custom group (nested) (OPTIONAL)
     * @param doesNotExist    : (IF false) resource will be overwritten if exists previously  (OPTIONAL)
     * @throws Exception
     */
    @When("^I create (custom|system) group '(.+?)'( in tenant '(.+?)')?( with tenant user and tenant password '(.+:.+?)')?( assigned to users '(.+?)')?( assigned to groups '(.+?)')?( if it does not exist)?$")
    public void createGroupResource(String type, String groupName, String tenantOrig, String tenantLoginInfo, String users, String groups, String doesNotExist) throws Exception {
        Boolean booleanExist = false;
        if (doesNotExist != null) {
            booleanExist = true;
        }
        if (ThreadProperty.get("isKeosEnv") != null && ThreadProperty.get("isKeosEnv").equals("true")) {
            createGroupResourceKeos(type, groupName, tenantOrig, tenantLoginInfo, users, groups, booleanExist);
        } else {
            createGroupResourceDcos(type, groupName, tenantOrig, tenantLoginInfo, users, groups, booleanExist);
        }
    }

    private void createGroupResourceDcos(String type, String groupName, String tenantOrig, String tenantLoginInfo, String users, String groups, boolean doesNotExist) throws Exception {
        String managementVersion = ThreadProperty.get("gosec-management_version");
        String managementBaasVersion = ThreadProperty.get("gosec-management-baas_version");
        String endPoint = "/service/gosecmanagement" + ThreadProperty.get("API_GROUP");
        Boolean addSourceType = false;
        Boolean managementBaas = false;
        Integer[] expectedStatusDelete = {200, 204};
        String endPointResource = "";
        String gid = groupName.replaceAll("\\s+", ""); //delete white spaces
        Integer expectedStatusCreate = 201;
        String newEndPoint = "";
        String data = "";

        if (tenantOrig != null) {
            // Set REST connection
            commonspec.setCCTConnection(tenantOrig, tenantLoginInfo);
        }

        if (endPoint.contains("id")) {
            newEndPoint = endPoint.replace("?id=", "");
        } else {
            newEndPoint = endPoint.substring(0, endPoint.length() - 1);
        }

        if (managementBaasVersion != null) {
            endPoint = "/service/gosec-management-baas/management/group?gid=";
            newEndPoint = "/service/gosec-management-baas/management/group";
        }

        try {
            assertThat(commonspec.getRestHost().isEmpty() || commonspec.getRestPort().isEmpty());

            if (managementBaasVersion != null) {  //vamos por management-baas
                managementBaas = true;
            } else {
                if (managementVersion != null) {  //vamos por management
                    String[] gosecVersionArray = managementVersion.split("\\.");
                    // Add inputSourceType if Gosec >= 1.4.x
                    if (Integer.parseInt(gosecVersionArray[0]) >= 1 && Integer.parseInt(gosecVersionArray[1]) >= 4) {
                        addSourceType = true;
                    }
                } else {
                    fail("Check gosec management or management-baas is available");
                }
            }

            //check if user exists and build json
            endPointResource = endPoint + gid;
            restSpec.sendRequestNoDataTable("GET", endPointResource, null, null, null);

            if (commonspec.getResponse().getStatusCode() != 200) {
                if (!managementBaas) {
                    //json for gosec-management endpoint
                    data = generateManagementGroupJson(gid, groupName, users, groups, addSourceType);

                } else {
                    //json for gosec-management-baas endpoint
                    data = generateBaasGroupJson(gid, groupName, users, groups, type);
                }

                // Send POST request
                sendIdentitiesPostRequest("group", groupName, data, newEndPoint);

            } else {
                if (doesNotExist) {
                    commonspec.getLogger().warn("Custom group:{} already exist", groupName);
                } else {
                    // Delete group and send the request again
                    deleteResourceIfExistsDcos("group", groupName, tenantOrig, tenantLoginInfo, endPoint, null);

                    try {
                        assertThat(commonspec.getResponse().getStatusCode()).isIn(expectedStatusDelete);
                    } catch (AssertionError e) {
                        commonspec.getLogger().warn("Error deleting Group {}: {}", groupName, commonspec.getResponse().getResponse());
                        throw e;
                    }
                    //send request again
                    createGroupResourceDcos(type, groupName, tenantOrig, tenantLoginInfo, users, groups, doesNotExist);
                }
            }

        } catch (Exception e) {
            commonspec.getLogger().error("Rest Host or Rest Port are not initialized {}{}", commonspec.getRestHost(), commonspec.getRestPort());
            throw e;
        }

    }

    private void createGroupResourceKeos(String type, String groupName, String tenantOrig, String tenantLoginInfo, String users, String groups, boolean doesNotExist) throws Exception {
        String baasPath = ThreadProperty.get("KEOS_GOSEC_BAAS_INGRESS_PATH");
        String endPoint = baasPath + "/management/group";
        String endPointResource = "";
        String gid = groupName.replaceAll("\\s+", ""); //delete white spaces
        String data = generateBaasGroupJson(gid, groupName, users, groups, type);
        Integer[] expectedStatusDelete = {200, 204};

        if (tenantOrig != null) {
            // Set REST connection
            commonspec.setCCTConnection(tenantOrig, tenantLoginInfo);
        }

        try {
            assertThat(commonspec.getRestHost().isEmpty() || commonspec.getRestPort().isEmpty());
            //check if user exists and build json
            endPointResource = endPoint + "?gid=" + gid;
            restSpec.sendRequestNoDataTable("GET", endPointResource, null, null, null);
            if (commonspec.getResponse().getStatusCode() != 200) {
                // Send POST request
                sendIdentitiesPostRequest("group", groupName, data, endPoint);
            } else {
                if (doesNotExist) {
                    commonspec.getLogger().warn("Custom group:{} already exist", groupName);
                } else {
                    // Delete group and send the request again
                    deleteResourceIfExistsKeos("group", groupName, tenantOrig, tenantLoginInfo, endPoint, null);

                    try {
                        assertThat(commonspec.getResponse().getStatusCode()).isIn(expectedStatusDelete);
                    } catch (AssertionError e) {
                        commonspec.getLogger().warn("Error deleting Group {}: {}", groupName, commonspec.getResponse().getResponse());
                        throw e;
                    }
                    //send request again
                    createGroupResourceKeos(type, groupName, tenantOrig, tenantLoginInfo, users, groups, doesNotExist);
                }
            }
        } catch (Exception e) {
            commonspec.getLogger().error("Rest Host or Rest Port are not initialized {}{}", commonspec.getRestHost(), commonspec.getRestPort());
            throw e;
        }
    }

    private String generateManagementGroupJson(String gid, String groupName, String users, String groups, Boolean addSourceType) {
        JSONObject postJson = new JSONObject();
        String data = "";
        postJson.put("id", gid);
        postJson.put("name", groupName);
        if (users != null) {
            String[] uids = users.split(",");
            postJson.put("users", uids);
        } else {
            postJson.put("users", new JSONArray());
        }
        if (groups != null) {
            String[] gids = groups.split(",");
            postJson.put("groups", gids);
        } else {
            postJson.put("groups", new JSONArray());
        }
        if (addSourceType) {
            postJson.put("inputSourceType", "CUSTOM");
        } else {
            postJson.put("custom", "true");
        }
        data = postJson.toString();
        return data;
    }

    private String generateBaasGroupJson(String gid, String groupName, String users, String groups, String type) {
        JSONObject postJson = new JSONObject();
        String data = "";
        postJson.put("gid", gid);
        postJson.put("name", groupName);

        if (type.equals("system")) {
            postJson.put("inputSourceType", "System");
        } else {
            if (type.equals("custom")) {
                postJson.put("inputSourceType", "Custom");
            }
        }

        if (users != null) {
            String[] uids = users.split(",");
            JSONArray jArray = new JSONArray();

            for (int i = 0; i < uids.length; i++) {
                JSONObject uidsObject = new JSONObject();
                uidsObject.put("uid", uids[i]);
                jArray.put(uidsObject);
            }
            postJson.put("users", jArray);
        } else {
            postJson.put("users", new JSONArray());
        }
        if (groups != null) {
            String[] gids = groups.split(",");
            JSONArray jArray = new JSONArray();

            for (int i = 0; i < gids.length; i++) {
                JSONObject gidsObject = new JSONObject();
                gidsObject.put("gid", gids[i]);
                jArray.put(gidsObject);
            }
            postJson.put("groups", jArray);
        } else {
            postJson.put("groups", new JSONArray());
        }
        data = postJson.toString();
        return data;
    }

    private String generateManagementUserJson(String uid, String userName, String groups, String keytab, String certificate, Boolean addSourceType) {
        JSONObject postJson = new JSONObject();
        String data = "";
        postJson.put("id", uid);
        postJson.put("name", userName);
        postJson.put("email", uid + "@stratio.com");
        if (groups != null) {
            String[] gids = groups.split(",");
            postJson.put("groups", gids);
        } else {
            postJson.put("groups", new JSONArray());
        }
        if (addSourceType) {
            postJson.put("inputSourceType", "CUSTOM");
        } else {
            postJson.put("custom", "true");
        }
        if (keytab != null) {
            postJson.put("keytab", true);
        }
        if (certificate != null) {
            postJson.put("certificate", true);
        }
        data = postJson.toString();
        return data;
    }

    private String generateBaasUserJson(String uid, String userName, String groups, String keytab, String certificate, Boolean addEnable, String type) {
        JSONObject postJson = new JSONObject();
        String data = "";
        postJson.put("uid", uid);
        postJson.put("name", userName);
        postJson.put("email", uid + "@stratio.com");

        if (type.equals("system")) {
            postJson.put("inputSourceType", "System");
        } else {
            if (type.equals("custom")) {
                postJson.put("inputSourceType", "Custom");
            }
        }

        if (groups != null) {
            String[] gids = groups.split(",");
            JSONArray jArray = new JSONArray();

            for (int i = 0; i < gids.length; i++) {
                JSONObject gidsObject = new JSONObject();
                gidsObject.put("gid", gids[i]);
                jArray.put(gidsObject);
            }
            postJson.put("groups", jArray);
        } else {
            postJson.put("groups", new JSONArray());
        }
        if (keytab != null) {
            postJson.put("keytab", true);
        }
        if (certificate != null) {
            postJson.put("certificate", true);
        }
        if (addEnable != null) {
            postJson.put("enable", true);
        }
        data = postJson.toString();
        return data;
    }

    private void sendIdentitiesPostRequest(String resource, String resourceName, String data, String newEndPoint) throws Exception {
        Integer expectedStatusCreate = 201;
        commonspec.getLogger().warn("Json for POST request---> {}", data);
        Future<Response> response = commonspec.generateRequest("POST", true, null, null, newEndPoint, data, "json");
        commonspec.setResponse("POST", response.get());

        try {
            if (commonspec.getResponse().getStatusCode() == 409) {
                commonspec.getLogger().warn("The custom {} {} already exists", resource, resourceName);
            } else {
                try {
                    assertThat(commonspec.getResponse().getStatusCode()).isEqualTo(expectedStatusCreate);
                } catch (AssertionError e) {
                    commonspec.getLogger().warn("Error creating custom {} {}: {}", resource, resourceName, commonspec.getResponse().getResponse());
                    throw e;
                }
                commonspec.getLogger().warn("Custom {} {} created", resource, resourceName);
            }
        } catch (Exception e) {
            commonspec.getLogger().warn("Error creating custom {} {}: {}", resource, resourceName, commonspec.getResponse().getResponse());
            throw e;
        }
    }

    @When("^I get version of service '(.+?)' with id '(.+?)'( in tenant '(.+?)')?( with tenant user and tenant password '(.+:.+?)')? and save it in environment variable '(.+?)'$")
    public void getServiceVersion(String serviceType, String serviceId, String tenant, String tenantLoginInfo, String envVar) throws Exception {
        if (ThreadProperty.get("isKeosEnv") != null && ThreadProperty.get("isKeosEnv").equals("true")) {
            getServiceVersionKeos(serviceType, serviceId, tenant, tenantLoginInfo, envVar);
        } else {
            getServiceVersionDcos(serviceType, serviceId, tenant, tenantLoginInfo, envVar);
        }
    }

    public void getServiceVersionDcos(String serviceType, String serviceId, String tenant, String tenantLoginInfo, String envVar) throws Exception {
        String endpoint = "/service/gosecmanagement/api/service";
        String gosecVersion = ThreadProperty.get("gosec-management_version");
        String managementBaasVersion = ThreadProperty.get("gosec-management-baas_version");
        if (managementBaasVersion != null) {
            endpoint = "/service/gosec-management-baas/management/services";
        }
        getServiceVersionCommon(endpoint, serviceType, serviceId, tenant, tenantLoginInfo, envVar, gosecVersion, managementBaasVersion != null);
    }

    public void getServiceVersionKeos(String serviceType, String serviceId, String tenant, String tenantLoginInfo, String envVar) throws Exception {
        String baasPath = ThreadProperty.get("KEOS_GOSEC_BAAS_INGRESS_PATH");
        String endpoint = baasPath + "/management/services";
        getServiceVersionCommon(endpoint, serviceType, serviceId, tenant, tenantLoginInfo, envVar, "N/A", true);
    }

    private void getServiceVersionCommon(String endpoint, String serviceType, String serviceId, String tenant, String tenantLoginInfo, String envVar, String gosecVersion, boolean isManagementBaas) throws Exception {
        if (tenant != null) {
            commonspec.setCCTConnection(tenant, tenantLoginInfo);
        }
        restSpec.sendRequestNoDataTable("GET", endpoint, null, null, null);
        if (commonspec.getResponse().getStatusCode() == 200) {
            if (isManagementBaas) {
                miscSpec.saveElementEnvironment(null, "$.[?(@.serviceType == \"" + serviceType + "\")].versionList[*]", "BDT_PLUGINS");
                String bdtPlugins = ThreadProperty.get("BDT_PLUGINS");
                commonspec.runLocalCommand("echo '" + bdtPlugins + "' | jq '.[] | select (.serviceList[].name == \"" + serviceId + "\").version' | tr -d '\"'");
                commonspec.runCommandLoggerAndEnvVar(0, envVar, Boolean.TRUE);
            } else {
                if (gosecVersion.startsWith("0")) {
                    ThreadProperty.set(envVar, "N/A");
                } else {
                    miscSpec.saveElementEnvironment(null, "$.[?(@.type == \"" + serviceType + "\")].pluginList[*]", "BDT_PLUGINS");
                    String bdtPlugins = ThreadProperty.get("BDT_PLUGINS");
                    commonspec.runLocalCommand("echo '" + bdtPlugins + "' | jq -Mrc '.[] as $parent | $parent.instanceList[] | select (.name == \"" + serviceId + "\" and .status == \"READY\") | $parent.version'");
                    commonspec.runCommandLoggerAndEnvVar(0, envVar, Boolean.TRUE);
                }
            }
        } else {
            fail("GET request to endpoint " + endpoint + " returns " + commonspec.getResponse().getStatusCode());
        }
    }

    @When("^I create 'key' '(.+?)'( with own key '(.+?)')? in tenant '(.+?)' with tenant user and tenant password '(.+:.+?)'( if it does not exist)?$")
    public void createKey(String keyName, String ownKey, String tenantOrig, String tenantLoginInfo, String doesNotExist) throws Exception {
        String endPoint = "/service/gosec-management-baas/management/encryption/key";
        int expectedResponse = 200;
        if (ThreadProperty.get("isKeosEnv") != null && ThreadProperty.get("isKeosEnv").equals("true")) {
            endPoint = "/gosec/baas/management/encryption/key";
        }
        commonspec.setCCTConnection(tenantOrig, tenantLoginInfo);
        JSONObject jsonKey = new JSONObject();
        jsonKey.put("name", keyName);
        jsonKey.put("value", ownKey);
        jsonKey.put("assets", new JSONArray());
        writeInFile(jsonKey.toString(), "keyBody.json");
        restSpec.sendRequestNoDataTable("POST", endPoint, null, "keyBody.json", "json");
        if (doesNotExist != null && (commonspec.getResponse().getStatusCode() == 409 || commonspec.getResponse().getStatusCode() == 412)) {
            commonspec.getLogger().warn("Key existed previously. It was not created.");
        } else {
            assertThat(commonspec.getResponse().getStatusCode()).isEqualTo(expectedResponse);
        }
        new File(System.getProperty("user.dir") + "/target/test-classes/keyBody.json").delete();
    }

    @When("^I create 'asset' '(.+?)' using key '(.+?)' and algorithm '(aes256|chacha256)' in tenant '(.+?)' with tenant user and tenant password '(.+:.+?)'( if it does not exist)?$")
    public void createAsset(String assetName, String keyName, String algorithm, String tenantOrig, String tenantLoginInfo, String doesNotExist) throws Exception {
        String endPointGetKey = "/service/gosec-management-baas/management/encryption/keys";
        String endPointPostAsset = "/service/gosec-management-baas/management/encryption/asset";
        String endPointGetAsset = "/service/gosec-management-baas/management/encryption/assets?from=0&count=10000&orderBy=name&order=asc";
        if (ThreadProperty.get("isKeosEnv") != null && ThreadProperty.get("isKeosEnv").equals("true")) {
            endPointGetKey = "/gosec/baas/management/encryption/keys";
            endPointPostAsset = "/gosec/baas/management/encryption/asset";
            endPointGetAsset = "/gosec/baas/management/encryption/assets?from=0&count=10000&orderBy=name&order=asc";
        }
        commonspec.setCCTConnection(tenantOrig, tenantLoginInfo);
        // Get Key ID
        restSpec.sendRequestNoDataTable("GET", endPointGetKey, null, null, null);
        writeInFile(commonspec.getResponse().getResponse(), "keyresponse.json");
        commonspec.runLocalCommand("cat target/test-classes/keyresponse.json | jq -cMr '.list[] | select(.name==\"" + keyName + "\")'");
        new File(System.getProperty("user.dir") + "/target/test-classes/keyresponse.json").delete();
        // Post Asset
        JSONObject jsonAsset = new JSONObject();
        jsonAsset.put("name", assetName);
        jsonAsset.put("algorithm", algorithm);
        jsonAsset.put("key", new JSONObject(commonspec.getCommandResult()));
        writeInFile(jsonAsset.toString(), "assetBody.json");
        restSpec.sendRequestNoDataTable("POST", endPointPostAsset, null, "assetBody.json", "json");
        if (doesNotExist != null && commonspec.getResponse().getStatusCode() == 409) {
            restSpec.sendRequestNoDataTable("GET", endPointGetAsset, null, null, null);
            writeInFile(commonspec.getResponse().getResponse(), "assetList.json");
            commonspec.runLocalCommand("cat target/test-classes/assetList.json | jq -cMr '.list[] | select(.name==\"" + assetName + "\")'");
            JSONObject jsonAssetGet = new JSONObject(commonspec.getCommandResult());
            commonspec.runLocalCommand("rm target/test-classes/assetList.json");
            if (jsonAssetGet.getString("name").equals(assetName) && jsonAssetGet.getString("algorithm").equals(algorithm)) {
                commonspec.getLogger().warn("Asset existed previously. It was not created.");
            } else {
                throw new Exception("Asset exists but it has different algorithm. Expected: " + algorithm + ". Current: " + jsonAssetGet.getString("algorithm"));
            }
        } else {
            assertThat(commonspec.getResponse().getStatusCode()).isEqualTo(201);
        }
        new File(System.getProperty("user.dir") + "/target/test-classes/assetBody.json").delete();
    }


    @When("^I include '(user|group)' '(.+?)' in role '(.+?)' for tenant '(.+?)'$")
    public void includeResourceInRole(String resource, String resourceId, String roleName, String tenantId) throws Exception {
        String endPointGetAllUsers = "/service/gosec-identities-daas/identities/users";
        String endPointGetAllGroups = "/service/gosec-identities-daas/identities/groups";
        String endPointGetAllRoles = "/service/gosec-identities-daas/profiling/role?count=1&name=" + roleName + "&tid=" + tenantId;
        String endPointRolePatch = "/service/gosec-identities-daas/profiling/role/bulk/identities?tid=" + tenantId;

        if (ThreadProperty.get("isKeosEnv") != null && ThreadProperty.get("isKeosEnv").equals("true")) {
            endPointGetAllUsers = "/gosec/identities/identities/users";
            endPointGetAllGroups = "/gosec/identities/identities/groups";
            endPointGetAllRoles = "/gosec/identities/profiling/role?count=1&name=" + roleName + "&tid=" + tenantId;
            endPointRolePatch = "/gosec/identities/profiling/role/bulk/identities?tid=" + tenantId;
        }

        String rid = "roleId";
        Boolean content = false;
        String select = "USERS";

        assertThat(commonspec.getRestHost().isEmpty() || commonspec.getRestPort().isEmpty());
        String uidOrGid = "uid";
        String usersOrGroups = "users";
        String endPointGosec = endPointGetAllUsers;
        String usersOrGroupsIds = "uids";

        if (resource.equals("group")) {
            uidOrGid = "gid";
            usersOrGroups = "groups";
            usersOrGroupsIds = "gids";
            endPointGosec = endPointGetAllGroups;
            select = "GROUPS";
        }

        // Set REST connection
        commonspec.setCCTConnection(null, null);

        //GET role with provided name and tid
        restSpec.sendRequestNoDataTable("GET", endPointGetAllRoles, null, null, null);
        if (commonspec.getResponse().getStatusCode() == 200) {
            //GET roleId - rid
            commonspec.runLocalCommand("echo '" + commonspec.getResponse().getResponse() + "' | jq .profiles[0].rid | sed s/\\\"//g");
            rid = commonspec.getCommandResult().trim();

            if ((rid == null) || (rid.equals(""))) {
                throw new Exception("Role" + " " + roleName + " doesn't exist in Gosec for tenant " + tenantId);
            }
            commonspec.getLogger().debug("RID obtenido--> {}", rid);

            String endPointRole = "/service/gosec-identities-daas/profiling/role/" + rid + "?select=" + select;

            if (ThreadProperty.get("isKeosEnv") != null && ThreadProperty.get("isKeosEnv").equals("true")) {
                endPointRole = "/gosec/identities/profiling/role/" + rid + "?select=" + select;
            }

            //GET user/group
            restSpec.sendRequestNoDataTable("GET", endPointGosec, null, null, null);
            if ((commonspec.getResponse().getStatusCode() == 200) && ((commonspec.getResponse().getResponse().contains("\"" + uidOrGid + "\":\"" + resourceId + "\"")))) {
                //GET role
                restSpec.sendRequestNoDataTable("GET", endPointRole, null, null, null);
                if (commonspec.getResponse().getStatusCode() == 200) {

                    JsonObject jsonRoleInfo = new JsonObject(JsonValue.readHjson(commonspec.getResponse().getResponse()).asObject());
                    // Get users/groups from role
                    JsonArray jsonGroups = (JsonArray) jsonRoleInfo.get(usersOrGroups);
                    // Get size of users/groups
                    String[] stringGroups = new String[jsonGroups.size() + 1];
                    // Create json for put
                    JSONObject putObject = new JSONObject(commonspec.getResponse().getResponse());

                    for (int i = 0; i < jsonGroups.size(); i++) {
                        String jsonIds = ((JsonObject) jsonGroups.get(i)).getString(uidOrGid, "");

                        if (jsonIds.equals(resourceId)) {
                            commonspec.getLogger().warn("{} is already included in the role {}", resourceId, roleName);
                            content = true;
                            break;
                        } else {
                            stringGroups[i] = jsonIds;
                        }
                    }

                    if (!content) {
                        // Add new user/group in array of uids/gids
                        stringGroups[jsonGroups.size()] = resourceId;
                        String[] stringRids = new String[1];
                        stringRids[0] = rid;

                        //PATCH object
                        JSONObject patchObject = new JSONObject();
                        patchObject.put(usersOrGroupsIds, stringGroups);
                        patchObject.put("op", "add");
                        patchObject.put("rids", stringRids);

                        commonspec.getLogger().warn("Json for PATCH request---> {}", patchObject);

                        //Create json data with header cluster-owner included
                        List<List<String>> rawData = Arrays.asList(
                                Arrays.asList("cluster-owner", "HEADER", "true", "n/a")
                        );
                        DataTable gosecDataTable = DataTable.create(rawData);
                        //Create file with json data
                        writeInFile(patchObject.toString(), "filePatchProfiling.json");

                        restSpec.sendRequest("PATCH", endPointRolePatch, null, "filePatchProfiling.json", "json", gosecDataTable);

                        if (commonspec.getResponse().getStatusCode() != 200) {
                            throw new Exception("Error adding User/Group: " + resourceId + " in Role " + roleName + " - Status code: " + commonspec.getResponse().getStatusCode());
                        }

                    }
                } else {
                    throw new Exception("Role" + " " + roleName + " doesn't exist in Gosec for tenant " + tenantId);
                }
            } else {
                throw new Exception(resource + " " + resourceId + " doesn't exist in Gosec");
            }
        } else {
            throw new Exception("Role" + " " + roleName + " doesn't exist in Gosec for tenant " + tenantId);
        }
    }

    @When("^I delete '(user|group)' '(.+?)' from role '(.+?)' in tenant '(.+?)'$")
    public void deleteResourceInRole(String resource, String resourceId, String roleName, String tenantId) throws Exception {
        String endPointGetAllUsers = "/service/gosec-identities-daas/identities/users";
        String endPointGetAllGroups = "/service/gosec-identities-daas/identities/groups";
        String endPointGetAllRoles = "/service/gosec-identities-daas/profiling/role?count=1&name=" + roleName + "&tid=" + tenantId;
        String endPointRolePatch = "/service/gosec-identities-daas/profiling/role/bulk/identities?tid=" + tenantId;

        if (ThreadProperty.get("isKeosEnv") != null && ThreadProperty.get("isKeosEnv").equals("true")) {
            endPointGetAllUsers = "/gosec/identities/identities/users";
            endPointGetAllGroups = "/gosec/identities/identities/groups";
            endPointGetAllRoles = "/gosec/identities/profiling/role?count=1&name=" + roleName + "&tid=" + tenantId;
            endPointRolePatch = "/gosec/identities/profiling/role/bulk/identities?tid=" + tenantId;
        }

        String rid = "roleId";
        Boolean content = false;
        String select = "USERS";

        assertThat(commonspec.getRestHost().isEmpty() || commonspec.getRestPort().isEmpty());
        String uidOrGid = "uid";
        String usersOrGroups = "users";
        String endPointGosec = endPointGetAllUsers;
        String usersOrGroupsIds = "uids";

        if (resource.equals("group")) {
            uidOrGid = "gid";
            usersOrGroups = "groups";
            usersOrGroupsIds = "gids";
            endPointGosec = endPointGetAllGroups;
            select = "GROUPS";
        }

        // Set REST connection
        commonspec.setCCTConnection(null, null);

        //GET role with provided name and tid
        restSpec.sendRequestNoDataTable("GET", endPointGetAllRoles, null, null, null);
        if (commonspec.getResponse().getStatusCode() == 200) {
            //GET roleId - rid
            commonspec.runLocalCommand("echo '" + commonspec.getResponse().getResponse() + "' | jq .profiles[0].rid | sed s/\\\"//g");
            rid = commonspec.getCommandResult().trim();

            if ((rid == null) || (rid.equals(""))) {
                throw new Exception("Role" + " " + roleName + " doesn't exist in Gosec for tenant " + tenantId);
            }
            commonspec.getLogger().debug("RID obtenido--> {}", rid);

            String endPointRole = "/service/gosec-identities-daas/profiling/role/" + rid + "?select=" + select;

            if (ThreadProperty.get("isKeosEnv") != null && ThreadProperty.get("isKeosEnv").equals("true")) {
                endPointRole = "/gosec/identities/profiling/role/" + rid + "?select=" + select;
            }

            //GET user/group
            restSpec.sendRequestNoDataTable("GET", endPointGosec, null, null, null);
            if ((commonspec.getResponse().getStatusCode() == 200) && ((commonspec.getResponse().getResponse().contains("\"" + uidOrGid + "\":\"" + resourceId + "\"")))) {
                //GET role
                restSpec.sendRequestNoDataTable("GET", endPointRole, null, null, null);
                if (commonspec.getResponse().getStatusCode() == 200) {

                    JsonObject jsonRoleInfo = new JsonObject(JsonValue.readHjson(commonspec.getResponse().getResponse()).asObject());
                    // Get users/groups from role
                    JsonArray jsonGroups = (JsonArray) jsonRoleInfo.get(usersOrGroups);
                    // Get size of users/groups
                    String[] stringGroups = new String[jsonGroups.size() + 1];
                    // Create json for put
                    JSONObject putObject = new JSONObject(commonspec.getResponse().getResponse());

                    for (int i = 0; i < jsonGroups.size(); i++) {
                        String jsonIds = ((JsonObject) jsonGroups.get(i)).getString(uidOrGid, "");

                        if (jsonIds.equals(resourceId)) {
                            commonspec.getLogger().debug("Deleting resource: {} from the role: {}", resourceId, roleName);
                            content = true;
                            break;
                        } else {
                            stringGroups[i] = jsonIds;
                        }
                    }

                    if (!content) {
                        commonspec.getLogger().warn("Resource: {} is not included in role: {} for tenant: {}", resourceId, roleName, tenantId);

                    } else {
                        // Include resourceId to stringDelete[]
                        String[] stringDelete = new String[1];
                        stringDelete[0] = resourceId;
                        // Include rid to stringRids[]
                        String[] stringRids = new String[1];
                        stringRids[0] = rid;

                        //PATCH object
                        JSONObject patchObject = new JSONObject();
                        patchObject.put(usersOrGroupsIds, stringDelete);
                        patchObject.put("op", "delete");
                        patchObject.put("rids", stringRids);

                        commonspec.getLogger().warn("Json for PATCH request---> {}", patchObject);

                        //Create json data with header cluster-owner included
                        List<List<String>> rawData = Arrays.asList(
                                Arrays.asList("cluster-owner", "HEADER", "true", "n/a")
                        );
                        DataTable gosecDataTable = DataTable.create(rawData);
                        //Create file with json data
                        writeInFile(patchObject.toString(), "filePatchProfiling.json");

                        restSpec.sendRequest("PATCH", endPointRolePatch, null, "filePatchProfiling.json", "json", gosecDataTable);

                        if (commonspec.getResponse().getStatusCode() != 200) {
                            throw new Exception("Error deleting User/Group: " + resourceId + " in Role " + roleName + " - Status code: " + commonspec.getResponse().getStatusCode());
                        }

                    }
                } else {
                    throw new Exception("Role" + " " + roleName + " doesn't exist in Gosec for tenant " + tenantId);
                }
            } else {
                throw new Exception(resource + " " + resourceId + " doesn't exist in Gosec");
            }
        } else {
            throw new Exception("Role" + " " + roleName + " doesn't exist in Gosec for tenant " + tenantId);
        }
    }

    @When("^I run '(partial|total)' ldap synchronizer$")
    public void runLdapSynchronizer(String type) throws Exception {
        String basePath = "/service/gosec-management-baas";
        String endPoint = "/management/ldap/synchronize";
        if (ThreadProperty.get("isKeosEnv") != null && ThreadProperty.get("isKeosEnv").equals("true")) {
            basePath = ThreadProperty.get("KEOS_GOSEC_BAAS_INGRESS_PATH");
        }
        commonspec.setCCTConnection(null, null);
        JSONObject jsonAsset = new JSONObject();
        jsonAsset.put("type", type);
        writeInFile(jsonAsset.toString(), "ldapSync.conf");
        restSpec.sendRequestNoDataTable("POST", basePath + endPoint, null, "ldapSync.conf", "json");
        assertThat(commonspec.getResponse().getStatusCode()).isEqualTo(201);
        new File(System.getProperty("user.dir") + "/target/test-classes/ldapSync.conf").delete();
    }

    @When("^I get id from profile structure with name '(.+?)' and save it in environment variable '(.+?)'$")
    public void getProfilingStructureId(String name, String envVar) throws Exception {
        Boolean content = false;
        String endPointGetAllStructures = "/gosec/baas/management/profiling/structure/application";

        restSpec.sendRequestNoDataTable("GET", endPointGetAllStructures, null, null, null);
        if (commonspec.getResponse().getStatusCode() == 200) {
            JsonArray jsonAllStructures = (JsonArray) (JsonValue.readHjson(commonspec.getResponse().getResponse()));

            for (int i = 0; i < jsonAllStructures.size(); i++) {
                String jsonName = ((JsonObject) jsonAllStructures.get(i)).getString("name", "");
                String jsonId = ((JsonObject) jsonAllStructures.get(i)).getString("id", "");

                if (jsonName.equals(name)) {
                    ThreadProperty.set(envVar, jsonId);
                    commonspec.getLogger().warn("Profile Structure Id={}", ThreadProperty.get(envVar));
                    content = true;
                    break;
                }
            }
            if (!content) {
                commonspec.getLogger().warn("Profile structure:{} does not exist", name);
            }

        } else {
            commonspec.getLogger().error("Error getting profile structures");
        }
    }
}

