# Changelog

Only listing significant user-visible, not internal code cleanups and minor bug fixes.

## 1.0.0 (upcoming)

* [GS-8186] Add step to get id from domain policy
* [GS-7908] Included Save file from response step
* [GS-7650] Get profile structure Id from given name
* [SAAS-6968] Upgrade to Cypress 10.11.0

## Previous development

### Branched to branch-0.21 (2022-07-01)

* [CYB-57051] Add enriched JSON format to allow applying CR changes directly
* [EOS-5743] Modify step to retrieve admin and sis basepath from keos.yaml
* [GS-3881] Adapt management-baas path according to keos version
* [QATM-3773] Refactor creating cutom users/groups
* [GS-3161] Add step to create custom JTW
* [GS-3051] Fix check certificate creating federated users
* Kubernetes support
* [EOS-4775] Fix step to remove user/group from tenant
* [SPK-1168] Get log from mesos if CCT doesn't return log path
* [DGPB-6794] Add support to Governance service role
* [DGPB-7161] Modify Gosec function deleteResourceIfExistsDcos to delete collections policies
* [PQA-146] New steps to download and upload k8s universe
* [DGPB-8233] Modify function to copy directories in/from pod
* [DGPB-8260] Add support to Governance requests in k8s
* [DGPB-8815] Add step to delete collection policies in k8s

### Branched to branch-0.20 (2020-12-17)

* [CROSSDATA-2740] Fix get service/container port from Marathon API in DCOS 1.8
* [PQA-40] retrieve ssh user from descriptor
* [PQA-42] obtain info from workspace 
* [SPK-988] Fix runInAllNodes spec
* [QATM-3515] Improve getHostIp method to obtain different IP's if service has multiple instances
* [SAAS-2785] New step to get IP (internal and external) from Marathon instead of CCT
* [ROCK-3086] New CucumberOptions with conditions
* [FRAM-6100] Add convert yaml to json
* [GS-2741] get correct timezone adding dates to policies
* [SPK-1024] Changes in generateMesosLogPath method to read executor logs
* [PQA-53] Fix modifyData method when first modification is a HEADER
* [ROCK-3086] Add VaultAnsible decrypt
* [PQA-55] Add secrets from daedalus to @dcos
* [PQA-55] Improve LDAP connection
* [ROCK-3086] Steps to modify, stop and start Marathon apps
* [QATM-3547] Warning trace added if include tag is wrong
* [SPK-1075] Logs improvements. Get log by host / secured host of task
* [SPK-1075] Fix error generating mesos log path
* [DGPB-5171] Add moveToElement selenium function
* [PQA-62] Allow forcing @dcos Marathon services refresh
* [PQA-63]Fixed include tag aspect with params
* [QATM-3568][QATM-3569] fix password generation through deploy-api and polling api requests

### Branched to branch-0.19 (2020-09-16)

* [QATM-3474] Generate path log manually if CCT request fails

### Branched to branch-0.19 (2020-09-08)

* Bump log4j-core, log4j-slf4j-impl and log4j-1.2-api from 2.0 to 2.13.2
* [GS-2354] Add feature to convert provided date into timestamp
* [SPK-914] Complete new getHostIP method
* [QATM-3470] JDBC improvements. JDBC connection can be used in the following scenarios
* [DGPB-4527] Catch exception in JDBC queries
* [ROCK-2116] Handle Discovery Token
* [QATM-3473] New step to obtain service/container port through Marathon API
* [QATM-3473] Fix cookies in BaseClient
* [CON-328] Apply URL encode in URL params when we create secrets through CCT
* [QATM-3474] Improvements in steps to obtain logs through CCT

### Branched to branch-0.18 (2020-08-04)

* [ROCK-1999] Add clients, models and Marathon spec
* [ROCK-1999] Add some cct and marathon steps
* [ROCK-1999] Fix deployapitest and refactor some client utils
* [ROCK-1999] Not fail when app does not exists in Marathon
* [ROCK-1998] Store rocket cookie for cypress tests
* [ROCK-1998] Generate Steps for Cypress
* [PIT-532] Fix retrieving CA bundle from Vault and add assertions
* [ROCK-1935] Update @dcos & execute cypress with evidences
* [PIT-568] add missing / in url for secrets generation/deletion
* [GS-2212] Refactor step to get json policy with BaaS
* [GS-2033] Fix delete policy with baas if it does not exist
* [GS-2033] Added BaaaS endpoint creating policy and updating resources
* [QATM-3284] Fix loop replacement in datatables
* Add base service clients and models
* [QATM-3353] Fix createResourceIfNotExist method
* [QATM-3375] New step to obtain registered version of a service in gosec management
* [QATM-3389] Set always default path in Vault specs
* Bump jackson-databind from 2.9.10.4 to 2.10.4
* [SPK-861] New step to check if command output is equal to some value with timeout
* [ROCK-1998] Parametrize variables with cypress
* [CON-246] Fix loop break when some scenario was failed 
* [PIT-611] Retrieve secrets from Vault using deploy-api

### Branched to branch-0.17 (2020-05-25)

* [QATM-3333] Fix user management to connect to LDAP
* [PIT-499] revamp gosec and cct steps to avoid set gosec sso token and rest host
* [QATM-3307] Get internal/external IP for tasks of the services
* [QATM-3312] Add two 'default' values to BDT
* [PIT-523] Fix install step for EOS 0.23.X and Universe 0.4.22
* [FRAM-5265] Fix contraints respuesta mesos slaves fallo en echo
* [QATM-3318] Retrieve more params from descriptor/ETCD
* [PIT-524] export EOS_VAULT_PORT with etcd value
* [QATM-3240] Filter default log in debug mode
* [GS-2073] New steps to create users and groups with baas or management
* [GS-2046] Delete gosec resources using baas if exists
* [GS-2111] Refactor get PolicyId step for management-baas
* [QATM-3284] Support to local variables in loop, multiloop and progloop tags

### Branched to branch-0.16 (2020-05-07)

* [PIT-523] Fix install step for EOS 0.23.X and Universe 0.4.22

### Branched to branch-0.16 (2020-04-29)

* [PIT-475]Log management(in less than)
* [QATM-3283] Fix step to scale services
* [EOS-3592] Add glue for Grafana
* [PIT-482] Uninstall services installed in folder
* [PIT-483] Fix uninstall services check expression
* [QATM-3214] Fix runOnEnv / skipOnEnv with local variables
* [PIT-485] fix teardown for services installed in folder
* [PIT-486] add step to update deployed service
* [PIT-487] add steps to handle single descriptor: create, update, delete
* [GS-2007] Get profile from provided tenant
* [PIT-498] revamp RestSpec class
* [QATM-3296] Check status of last task
* [FRAM-5214] Cambio constraints scheduler
* [QATM-1850] Bump jackson-databind from 2.9.10.3 to 2.9.10.4
* [PIT-493] fix for eos version 1.3.X
* [PIT-497] add step to aread value from centralized configuration
* [PIT-493] fix obtain vault ip and not internal name
* [PIT-493] fix cleaning EOS_ACCESS_POINT variable
* [PIT-493][PIT-494] obtain basic info from etcd and fix in obtaining cct services
* [QATM-3288] Poder actualizar servicio de CCT también con json o sin modificaciones
* [PIT-419] Encoding

### Branched to branch-0.15 (2020-04-14)

* [GS-1965] Set warning message instead of exception when gosec variables are not defined
* [PIT-479] Check services installed in folder
* [GS-1957] Create step to remove user/group in tenant
* [GS-1956] Create step to update Gosec resources
* [GS-1954] Delete tenant if it exists
* [PIT-474] Download log file from service
* [PIT-464] Check content of stdout/stderr
* [GS-1872] TestAT refactor for Gosec steps
* [QATM-3200] update zookeeper client
* [PIT-460] Teardown service from CCT(deploy-api)
* [PIT-461] Scale up service from CCT
* [PIT-449] Improve obtain task status from the CCT version
* [QATM-3183] Fix @dcos annotation if some container is null
* [PIT-457] Step to make CCT installations independent from CCT version
* [PIT-458] Step to make CCT uninstallations independent from CCT version
* [QATM-3183] Try/catch added in @dcos to avoid errors if container is wrong
* [QATM-3222] Add local variable for governance cookie in setGoSecSSOCookie
* [FRAM-4364] Test elastic
* [QATM-3218] Add more bootstrap info
* [PIT-450] Retrieve secrets from /people too
* [PIT-447] add steps to work with Stratio HDFS Framework
* [PIT-449]Task status from CCT
* [PIT-448] Secured postgres steps
* [QATM-3259] Get serviceId for service cct-universe
* [PIT-448] new step not possible to consume
* [FRAM-5147] Añadir propiedad para conectarte al bouncer de modo transaccional
* [PIT-463] Steps to upload rules and descriptors to CCT
* [QATM-3264] Set name in SSH tunnels

### Branched to branch-0.14 (2020-03-09)

* [GS-1830] Added scim type to HTTP requests
* [QATM-3165] Add gosecmanagement path to CucumberRunner
* [QATM-3177] Mejorar la obtención de secretos de Vault (kms_utils) en la BDT
* [QATM-3178] Mejora gestión /etc/hosts
* [QATM-3187] Nueva annotation para obtener info de bootstrap automáticamente
* [QATM-3183] Use Marathon API to obtain service versions
* [QATM-3182] Check status for services tasks 
* [QATM-3192] Add steps for configuration-api and calico network management
* [QATM-3194] Update Kafka client version and steps

### Branched to branch-0.13 (2020-02-12)

* [QATM-3140] Let step convert json schema save content to file and/or variable
* [QATM-3125] Fix error in Rocket SSO login
* Set dcosAuthCookie as local variable in step "I set sso token
* [QATM-3107] Keep SSH connections
* [GS-1730] Added step to include a group to a profile including the previous configuration
* [FRAM-4753] Change constraint private function
* [QATM-2770] Fix 'in less than xxx' steps to avoid timeout / unknownhost exceptions
* [QATM-3084] Empty default value for variables
* [FRAM-4753] Change for constraint private function 
* [FRAM-4720] Inclusion constraints scheduler
* [FRAM-4275] Add roles hdfs service
* [QATM-3073] Fix password creation without user
* [QATM-3073] New steps to generate keytabs, certificates and passwords 
* [CROSSDATA-2345] Modify Gosec policies methods to obtain ID when Gosec < 0.17
* [QATM-3071] Fix how to get Vault IP
* [GS-1714] Step to get json policy and save on env variable or policy
* [GS-1714] Changed the endpoint to get all policy details
* [GS-1707] Get REALM from bootstrap and save in variable EOS_REALM
* [LDG-1225] Added not-empty check cell in csv matcher
* [QATM-3028] Allow to choose user in governance header
* [QATM-3054] Performance improvements in obtain info from bootstrap step
* [CROSSDATA-2289] Allow remote execution in parallel script
* [CROSSDATA-2289] New steps to execute command / copy files in all nodes
* [QATM-3036] Use CA trusted for LDAP connection
* [QATM-3030] Add two more variables to basic information
* Set governance cookie
* [QATM-3006] Add X-RolesID header for governanceAPI 1.2
* [GS-1538] Allow response 204 deleting gosec resources
* [QATM-2975] Null support added in query result and new step to check if table is contained in query result
* [CROSSDATA-2232] Steps added to connect to Crossdata server through JDBC
* [QATM-2945] Include progloop tag
* [QATM-2932] Cambio version jdbc e introducción valor encriptacion
* [ROCK-612] Feature/no host verifier
* [QATM-2925] Fix NPE when we try to obtain a browser snapshot
* [QATM-1993] Upgrade Cucumber to 4.4.0
* [SECTY-1482] Added step to get profileId from profileName

### Branched to branch-0.12 (2019-10-03)

* [QATM-2490] Save service response in environment variable without File parameters
* [QATM-2494] Fix Permission denied error when we try to copy a file and destination file name is not defined 
* [QATM-1896] Allow standalone-chrome usage without grid
* [QATM-2529] Fix code error in gtm_proxy step
* [QATM-2489] Added PATCH type to rest requests
* [QATM-2531] add new comparison options
* [QATM-2533] Allow local variables in runOnEnv and skipOnEnv tags
* [QATM-2541] new step for local command execution polling
* [QATM-2534] Fix Jira integration using @ignore @tillfixed(XXX)
* [QATM-2299] Add screenshot in cucumber report when selenium step fails
* [QATM-2299] Fix chromeFullScreenCapture method
* [QATM-2556] return multiline result in local execution as such
* [QATM-2555] Add ignite, k8s and etcd in check constraints step
* [QATM-2558] Add arangodb in check constraints step
* [QATM-2628] small fixes in local command and dcos step
* [QATM-2636] Fix constraints
* [QATM-2658] New step to obtain policyID
* [QATM-2701] Modify create/delete policy steps to obtain policyID
* [SPK-548] Enhanced SSH connection. Avoiding other methods than pem/password and setting Strict Host to false
* [QATM-2734] include X-UserID for gov request
* [SECTY-1314] Added step to create tenant if it does not exist
* [SECTY-1440] BDT refactor for gosec 1.1.1 new paths and ids
* [QATM-2786] New steps to set default values in tenant and gosec variables
* [QATM-2786] New step to include an user or group in existing tenant
* [QATM-2789] Support doesn't contain in SSH Command output
* [QATM-2792] Add steps for LDAP
* [QATM-2793] Use other SSH port rather than default 22
* [QATM-2367] New clear function with keys Control and Delete
* [QATM-2817] Add MarathonLB folder to classpath
* [QATM-2833] Parametrize TenantID Governance
* [SECTY-1482] Create step to get id from tag policies
* [QATM-2895] Modify step to retrieve info from bootstrap

### Branched to branch-0.11 (2019-06-11)

* [QATM-2484] Fix copyTo and copyFrom methods for dirs/files handling
* [QATM-1986] Upgrade Cucumber to 3.0.2
* [QATM-2349] Add exception info when one step fails and hide next steps of scenario
* [QATM-2344] new steps to gather info from bootstrap
* [QATM-2303] refactor of steps due to new cucumber version
* [QATM-2353] Fix infinite loop in some undefined steps
* [QATM-2355] Do not flat template file when it's not a JSON
* [QATM-2352] New catch added in Selenium click step
* [QATM-2359] Fix error in sendRequestTimeout method
* [QATM-2364] Add EOS_ACCESS_POINT in returned info from bootstrap
* [QATM-2363] Add step typing textx if the element exists using selenium
* [QATM-2365] Add retrieve value js
* [QATM-2488] Add tag to avoid execution when environment variables not defined
* [QATM-2541] New step local command execution polling

### Branched to branch-0.10 (2019-05-23)

* [QATM-2137] Fix array json governance
* [QATM-2139] add new tests for modifyData with arrays
* [QATM-2181] new steps for sftp module
* [QATM-2187] Add ignite specs path to cucumber glue
* [LDG-123] Refactor resultMustBeCSV and added test
* [QATM-1901] Add charset=UTF-8 in content-type headers
* [QATM-2178] New step to check if a role of a service complies the established constraints
* [LDG-240] Improve CSV file matcher for random values
* [QATM-2118] New proxy steps added
* [SPARTA-2328] New selenium features (for Intell and Sparta)
* [QATM-2273] Add screenshot to cucumber-report
* [QATM-2302] Fix NPE when module has custom specs
* [QATM-2303] Create user,policy and groups if they don exist previously
* [QATM-2333] Ignore pre-release suffix in versions sent as parameter

### Branched to branch-0.9 (2019-02-25)

* [QATM-1899] Upgrade Cucumber to 2.4.0
* [QATM-1981] Refactor specs and tests
* [QATM-2049] Fix for incorrect step after specs refactor
* [QATM-1992] Fix include aspect error when we have tags in the next scenario after scenario included
* [QATM-2054] add new headers in case of governance request
* [QATM-2058] new step to check REST request does not contain text and fixes for gov requests
* [QATM-2071] fix step to check services distribution in datacenters
* [QATM-1974] RunOnEnv / SkipOnEnv improvements
* [QATM-2067] @always tag allows execute a test even if an important scenario failed previously
* [QATM-2099] Try/catch added in RunOnTagAspect

### Branched to branch-0.8 (2019-02-07)

* [LDG-96] New kafka steps
* [QASAI-33] New step to check if a cookie has been saved correctly
* [QATM-967] Updated Selenium version to 3.9.1
* [PIT-68] Updated commons-lang3 version to 3.7
* [QATM-938] Add capability for getting raw HTML code
* [QATM-1201] Added method to convert json to yaml
* [QATM-1362] allow boolean expressions in runOnEnv and skipOnEnv tags
* [QATM-1362] modify regular expression for boolean conditions in runOnEnv and skipOnEnv
* [QATM-1181] Add glue for PGBouncer
* [QATM-1414] Add changes for search by text in selenium
* [QATM-1411] fix dcos-cli problems with ssh connection
* [QATM-324] Step to retrieve secrets from Vault
* [QATM-1267] Capability to operate with headers on selenium
* [QATM-1267] Changes to include keyboard generated events and delete non editable fields
* [QATM-1421] Changes to include parameter type Long in json files
* [QATM-1412] Step created to connect postgres with tls
* [QATM-1431] Add sql steps to bdt
* [QATM-1431] Refactor close database method name
* [QATM-1264] Add method to obtain dcos-cookie to bdt
* [QATM-1437] fix method to add a new label to framework
* [QATM-1464] set tenant to retrieve auth_token 
* [QATM-1464] fix error in condition to add tenant
* [QATM-1497] Changes to adapt step for different cases
* [QATM-1481] add kibana framework to glue file
* [QATM-1638] Include BODY in DELETE method
* [QATM-1644] fix to add label in DC/OS 1.11
* [QATM-1643] Change regex to match for loop and multiloop
* [QATM-1646] Add Postgres Database resource
* [QATM-1593] new steps for command center integration
* [QATM-1689] Replace securely by security type in DB connection
* [QATM-1593] new steps for command center integration
* [QASAI-33] New step to check if a cookie has benn saved correctly
* [QASAI-4] Step for Double click for selenium
* [QATM-1827] Add < and > in RunOnEnv or SkipOnEnv aspects
* [LDG-96] Added new kafka steps
* [QATM-1678] Change on replacement aspect
* [QATM-1842] Fix error changing result --> result in checkParams having logical operators
* [QASAI-34] Fix JUnitReport
* [QATM-1873] Fix Selenium tests
* [QATM-1871] New command center steps 
* [QATM-1870] add CucumberRunner for CommandCenter
* [QATM-1559] Connect to Cassandra with ssl
* [QATM-1878] Fix error in report when step is passed and it has text like a variable
* [QATM-1933] fix in function to convert jsonschema to json for CCT
* [QATM-1934] fix when converting jsonschema to json handling strings
* [QATM-1935] Allow empty string as default value in variables
* [QATM-1948] retrieve DCOS_AUTH_COOKIE and save in thread variable
* [QATM-1968] Fix to avoid false errors when we check a query response
* [QATM-1950] Create step to check resources after uninstall
* [QATM-1976] Print response if status code returned is different that expected
* [QATM-1977] New step to create metabase cookie
* [QATM-1979] New step to obtain metabase session id and generate cookie

### Branched to branch-0.7 (2018-04-05)

* [CROSSDATA-1550] Add @multiloop tag
* [QATM-1061] Add step to send requests to endpoint with timeout and datatable

### Branched to branch-0.6 (2018-02-22)

* [QATM-236] Quit after tagged scenario fails
* [QATM-70] New background tag
* [QA-189] Removed extra dot in 'service response status' step. Old step removed.
* [QA-152] New aspect merging 'include' and 'loop'. Old aspects removed.
* [QATM-74] New step to store text in a webElement in environment variable.
* [QATM-73] New step to read file, modify according to parameters and store in environment variable.
* [AR-732] Add Command Center - Configuration API Glue

### Branched to branch-0.5 (2017-07-05)

* [QATM-78] Fix public releasing in maven central

### Branched to branch-0.5 (2017-06-12)

* [QA-342] New cucumber tag @loop to multiple scenario executions

### Branched to branch-0.4 (2017-03-06)

* [QA-272] better classes packaging
* [QA-298] Apache2 license. Step definitions redefined

### Branched to branch-0.3 (2017-01-26)

* CukesGHooks will invoke the logger with each step

### 0.2.0 (June 2016)

* Ignored scenarios will fail if ignore cause was an already done jira ticket
* No more a submodule project
* Added new aspect to force the browser name and version in tests development ,using FORCE_BROWSER (even availableUniqueBrowsers factory is defined).

  Eg: mvn -U verify -DSELENIUM_GRID=builder.int.stratio.com:4444 **-DFORCE_BROWSER=chrome_33**
