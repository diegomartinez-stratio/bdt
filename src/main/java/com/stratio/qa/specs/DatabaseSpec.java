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
import com.datastax.driver.core.DataType;
import com.datastax.driver.core.Row;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.util.JSON;
import org.asynchttpclient.Response;
import com.stratio.qa.assertions.DBObjectsAssert;
import com.stratio.qa.exceptions.DBException;
import com.stratio.qa.utils.JDBCConnection;
import com.stratio.qa.utils.ThreadProperty;
import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;
import io.cucumber.datatable.DataTable;
import org.assertj.core.api.Assertions;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.testng.Assert;
import org.testng.asserts.Assertion;

import javax.net.ssl.SSLException;
import java.lang.reflect.InvocationTargetException;
import java.net.UnknownHostException;
import java.sql.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.Pattern;

import static com.stratio.qa.assertions.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;

/**
 * Generic Database Specs.
 *
 * @see <a href="DatabaseSpec-annotations.html">Database Steps &amp; Matching Regex</a>
 */
public class DatabaseSpec extends BaseGSpec {

    public static final Integer ES_DEFAULT_NATIVE_PORT = 9300;

    public static final String ES_DEFAULT_CLUSTER_NAME = "elasticsearch";

    public static final int VALUE_SUBSTRING = 3;

    /**
     * Generic constructor.
     *
     * @param spec object
     */
    public DatabaseSpec(CommonG spec) {
        this.commonspec = spec;

    }

    /**
     * Create a basic Index.
     *
     * @param index_name index name
     * @param table      the table where index will be created.
     * @param column     the column where index will be saved
     * @param keyspace   keyspace used
     * @throws Exception exception
     */
    @Given("^I create a Cassandra index named '(.+?)' in table '(.+?)' using magic_column '(.+?)' using keyspace '(.+?)'$")
    public void createBasicMapping(String index_name, String table, String column, String keyspace) throws Exception {
        String query = "CREATE INDEX " + index_name + " ON " + table + " (" + column + ");";
        commonspec.getCassandraClient().executeQuery(query);
    }

    /**
     * Create a Cassandra Keyspace.
     *
     * @param keyspace cassandra keyspace
     */
    @Given("^I create a Cassandra keyspace named '(.+)'$")
    public void createCassandraKeyspace(String keyspace) {
        commonspec.getCassandraClient().createKeyspace(keyspace);
    }

    /**
     * Connect to cluster.
     *
     * @param clusterType DB type (Cassandra|Mongo|Elasticsearch|OpenSearch)
     * @param url         url where is started Cassandra cluster
     */
    @Given("^I( securely)? connect to '(Cassandra|Mongo|Elasticsearch|OpenSearch)' cluster at '(.+)'$")
    public void connect(String secured, String clusterType, String url) throws DBException, UnknownHostException {
        switch (clusterType) {
            case "Cassandra":
                commonspec.getCassandraClient().setHost(url);
                commonspec.getCassandraClient().connect(secured);
                break;
            case "Mongo":
                commonspec.getMongoDBClient().connect();
                break;
            case "Elasticsearch":
                LinkedHashMap<String, Object> settings_map = new LinkedHashMap<String, Object>();
                settings_map.put("cluster.name", System.getProperty("ES_CLUSTER", ES_DEFAULT_CLUSTER_NAME));
                commonspec.getElasticSearchClient().setSettings(settings_map);
                commonspec.getElasticSearchClient().connect();
                break;
            case "OpenSearch":
                LinkedHashMap<String, Object> settings_map_open_search = new LinkedHashMap<String, Object>();
                settings_map_open_search.put("cluster.name", System.getProperty("ES_CLUSTER", ES_DEFAULT_CLUSTER_NAME));
                commonspec.getOpenSearchClient().setSettings(settings_map_open_search);
                commonspec.getOpenSearchClient().connect();
                break;
            default:
                throw new DBException("Unknown cluster type");
        }
    }

    /**
     * Connect to ElasticSearch using custom parameters
     *
     * @param host        ES host
     * @param nativePort  ES port
     * @param clusterName ES clustername
     * @param keyStorePath File keystore in format .jks
     * @param keyStorePassword KeyStore Password
     * @throws NumberFormatException exception
     */
    @Given("^I connect to Elasticsearch cluster at host '(.+?)'( using native port '(.+?)')?( with trustStorePath '(.+?)')?( and trustStorePassword '(.+?)')?( with keyStorePath '(.+?)')?( and keyStorePassword '(.+?)')?( using cluster name '(.+?)')?$")
    public void connectToElasticSearch(String host, String nativePort, String trustStorePath, String trustStorePassword, String keyStorePath, String keyStorePassword, String clusterName) throws NumberFormatException, SSLException {
        LinkedHashMap<String, Object> settings_map = new LinkedHashMap<String, Object>();
        if (clusterName != null) {
            settings_map.put("cluster.name", clusterName);
        } else {
            settings_map.put("cluster.name", ES_DEFAULT_CLUSTER_NAME);
        }
        commonspec.getElasticSearchClient().setSettings(settings_map);
        if (nativePort != null) {
            commonspec.getElasticSearchClient().setNativePort(Integer.valueOf(nativePort));
        } else {
            commonspec.getElasticSearchClient().setNativePort(ES_DEFAULT_NATIVE_PORT);
        }
        commonspec.getElasticSearchClient().setHost(host);

        if (trustStorePath != null && trustStorePassword != null && keyStorePath != null  && keyStorePassword != null) {
            commonspec.getElasticSearchClient().connect(keyStorePath, keyStorePassword, trustStorePath, trustStorePassword);
        } else {
            commonspec.getElasticSearchClient().connect();
        }
    }

    /**
     * Connect to OpenSearch using custom parameters
     *
     * @param host        ES host
     * @param nativePort  ES port
     * @param clusterName ES clustername
     * @param keyStorePath File keystore in format .jks
     * @param keyStorePassword KeyStore Password
     * @throws NumberFormatException exception
     */
    @Given("^I connect to OpenSearch cluster at host '(.+?)'( using native port '(.+?)')?( with trustStorePath '(.+?)')?( and trustStorePassword '(.+?)')?( with keyStorePath '(.+?)')?( and keyStorePassword '(.+?)')?( using cluster name '(.+?)')?$")
    public void connectToOpenSearch(String host, String nativePort, String trustStorePath, String trustStorePassword, String keyStorePath, String keyStorePassword, String clusterName) throws NumberFormatException, SSLException {
        LinkedHashMap<String, Object> settings_map = new LinkedHashMap<String, Object>();
        if (clusterName != null) {
            settings_map.put("cluster.name", clusterName);
        } else {
            settings_map.put("cluster.name", ES_DEFAULT_CLUSTER_NAME);
        }
        commonspec.getOpenSearchClient().setSettings(settings_map);
        if (nativePort != null) {
            commonspec.getOpenSearchClient().setNativePort(Integer.valueOf(nativePort));
        } else {
            commonspec.getOpenSearchClient().setNativePort(ES_DEFAULT_NATIVE_PORT);
        }
        commonspec.getOpenSearchClient().setHost(host);

        if (trustStorePath != null && trustStorePassword != null && keyStorePath != null  && keyStorePassword != null) {
            commonspec.getOpenSearchClient().connect(keyStorePath, keyStorePassword, trustStorePath, trustStorePassword);
        } else {
            commonspec.getOpenSearchClient().connect();
        }
    }

    /**
     * Create table
     *
     * @param table     Cassandra table
     * @param datatable datatable used for parsing elements
     * @param keyspace  Cassandra keyspace
     */
    @Given("^I create a Cassandra table named '(.+?)' using keyspace '(.+?)' with:$")
    public void createTableWithData(String table, String keyspace, DataTable datatable) {
        try {
            commonspec.getCassandraClient().useKeyspace(keyspace);
            int attrLength = datatable.cells().get(0).size();
            Map<String, String> columns = new HashMap<String, String>();
            ArrayList<String> pk = new ArrayList<String>();

            for (int i = 0; i < attrLength; i++) {
                columns.put(datatable.cells().get(0).get(i),
                        datatable.cells().get(1).get(i));
                if ((datatable.cells().size() == 3) && datatable.cells().get(2).get(i).equalsIgnoreCase("PK")) {
                    pk.add(datatable.cells().get(0).get(i));
                }
            }
            if (pk.isEmpty()) {
                throw new Exception("A PK is needed");
            }
            commonspec.getCassandraClient().createTableWithData(table, columns, pk);
        } catch (Exception e) {
            commonspec.getLogger().debug("Exception captured");
            commonspec.getLogger().debug(e.toString());
            commonspec.getExceptions().add(e);
        }
    }

    /**
     * Insert Data
     *
     * @param table     Cassandra table
     * @param datatable datatable used for parsing elements
     * @param keyspace  Cassandra keyspace
     */
    @Given("^I insert in keyspace '(.+?)' and table '(.+?)' with:$")
    public void insertData(String keyspace, String table, DataTable datatable) {
        try {
            commonspec.getCassandraClient().useKeyspace(keyspace);
            int attrLength = datatable.cells().get(0).size();
            Map<String, Object> fields = new HashMap<String, Object>();
            for (int e = 1; e < datatable.cells().size(); e++) {
                for (int i = 0; i < attrLength; i++) {
                    fields.put(datatable.cells().get(0).get(i), datatable.cells().get(e).get(i));

                }
                commonspec.getCassandraClient().insertData(keyspace + "." + table, fields);

            }
        } catch (Exception e) {
            commonspec.getLogger().debug("Exception captured");
            commonspec.getLogger().debug(e.toString());
            commonspec.getExceptions().add(e);
        }
    }

    /**
     * Save clustername of elasticsearch in an environment varible for future use.
     *
     * @param host   elasticsearch connection
     * @param port   elasticsearch port
     * @param envVar thread variable where to store the value
     * @throws IllegalAccessException    exception
     * @throws IllegalArgumentException  exception
     * @throws SecurityException         exception
     * @throws NoSuchFieldException      exception
     * @throws ClassNotFoundException    exception
     * @throws InstantiationException    exception
     * @throws InvocationTargetException exception
     * @throws NoSuchMethodException     exception
     */
    @Given("^I obtain elasticsearch cluster name in '([^:]+?)(:.+?)?' and save it in variable '(.+?)'?$")
    public void saveElasticCluster(String host, String port, String envVar) throws Exception {

        commonspec.setRestProtocol("http://");
        commonspec.setRestHost(host);
        commonspec.setRestPort(port);

        Future<Response> response;

        response = commonspec.generateRequest("GET", false, null, null, "/", "", "json", "");
        commonspec.setResponse("GET", response.get());

        String json;
        String parsedElement;
        json = commonspec.getResponse().getResponse();
        parsedElement = "$..cluster_name";

        String json2 = "[" + json + "]";
        String value = commonspec.getJSONPathString(json2, parsedElement, "0");

        if (value == null) {
            throw new Exception("No cluster name is found");
        } else {
            ThreadProperty.set(envVar, value);
        }
    }


    /**
     * Save clustername of opensearch in an environment varible for future use.
     *
     * @param host   opensearch connection
     * @param port   opensearch port
     * @param envVar thread variable where to store the value
     * @throws IllegalAccessException    exception
     * @throws IllegalArgumentException  exception
     * @throws SecurityException         exception
     * @throws NoSuchFieldException      exception
     * @throws ClassNotFoundException    exception
     * @throws InstantiationException    exception
     * @throws InvocationTargetException exception
     * @throws NoSuchMethodException     exception
     */
    @Given("^I obtain opensearch cluster name in '([^:]+?)(:.+?)?' and save it in variable '(.+?)'?$")
    public void saveOpenSearchCluster(String host, String port, String envVar) throws Exception {

        commonspec.setRestProtocol("http://");
        commonspec.setRestHost(host);
        commonspec.setRestPort(port);

        Future<Response> response;

        response = commonspec.generateRequest("GET", false, null, null, "/", "", "json", "");
        commonspec.setResponse("GET", response.get());

        String json;
        String parsedElement;
        json = commonspec.getResponse().getResponse();
        parsedElement = "$..cluster_name";

        String json2 = "[" + json + "]";
        String value = commonspec.getJSONPathString(json2, parsedElement, "0");

        if (value == null) {
            throw new Exception("No cluster name is found");
        } else {
            ThreadProperty.set(envVar, value);
        }
    }


    /**
     * Drop all the ElasticSearch indexes.
     */
    @Given("^I drop every existing elasticsearch index$")
    public void dropElasticsearchIndexes() {
        commonspec.getElasticSearchClient().dropAllIndexes();
    }

    /**
     * Drop all the OpenSearch indexes.
     */
    @Given("^I drop every existing opensearch index$")
    public void dropOpenSearchIndexes() {
        commonspec.getOpenSearchClient().dropAllIndexes();
    }

    /**
     * Drop an specific index of ElasticSearch.
     *
     * @param index ES index
     */
    @Given("^I drop an elasticsearch index named '(.+?)'$")
    public void dropElasticsearchIndex(String index) {
        commonspec.getElasticSearchClient().dropSingleIndex(index);
    }

    /**
     * Drop an specific index of OpenSearch.
     *
     * @param index ES index
     */
    @Given("^I drop an opensearch index named '(.+?)'$")
    public void dropOpenSearchIndex(String index) {
        commonspec.getOpenSearchClient().dropSingleIndex(index);
    }

    /**
     * Drop a Cassandra Keyspace.
     *
     * @param keyspace Cassandra keyspace
     */
    @Given("^I drop a Cassandra keyspace '(.+)'$")
    public void dropCassandraKeyspace(String keyspace) {
        commonspec.getCassandraClient().dropKeyspace(keyspace);
    }

    /**
     * Create a MongoDB dataBase.
     *
     * @param databaseName Mongo database
     */
    @Given("^I create a MongoDB dataBase '(.+?)'$")
    public void createMongoDBDataBase(String databaseName) {
        commonspec.getMongoDBClient().connectToMongoDBDataBase(databaseName);

    }

    /**
     * Drop MongoDB Database.
     *
     * @param databaseName mongo database
     */
    @Given("^I drop a MongoDB database '(.+?)'$")
    public void dropMongoDBDataBase(String databaseName) {
        commonspec.getMongoDBClient().dropMongoDBDataBase(databaseName);
    }

    /**
     * Insert data in a MongoDB table.
     *
     * @param dataBase Mongo database
     * @param tabName  Mongo table
     * @param table    Datatable used for insert elements
     */
    @Given("^I insert into a MongoDB database '(.+?)' and table '(.+?)' this values:$")
    public void insertOnMongoTable(String dataBase, String tabName, DataTable table) {
        commonspec.getMongoDBClient().connectToMongoDBDataBase(dataBase);
        commonspec.getMongoDBClient().insertIntoMongoDBCollection(tabName, table);
    }

    /**
     * Truncate table in MongoDB.
     *
     * @param database Mongo database
     * @param table    Mongo table
     */
    @Given("^I drop every document at a MongoDB database '(.+?)' and table '(.+?)'")
    public void truncateTableInMongo(String database, String table) {
        commonspec.getMongoDBClient().connectToMongoDBDataBase(database);
        commonspec.getMongoDBClient().dropAllDataMongoDBCollection(table);
    }

    /**
     * Insert document in a MongoDB table.
     *
     * @param dataBase   Mongo database
     * @param collection Mongo collection
     * @param document   document used for schema
     */
    @Given("^I insert into MongoDB database '(.+?)' and collection '(.+?)' the document from schema '(.+?)'$")
    public void insertOnMongoTable(String dataBase, String collection, String document) throws Exception {
        String retrievedDoc = commonspec.retrieveData(document, "json");
        commonspec.getMongoDBClient().connectToMongoDBDataBase(dataBase);
        commonspec.getMongoDBClient().insertDocIntoMongoDBCollection(collection, retrievedDoc);
    }

    /**
     * Connect to JDBC secured/not secured database
     *
     * @param securityType  database security type string
     * @param encryption  database encription string
     * @param trans  database transactional string
     * @param database  database connection string
     * @param host      database host
     * @param port      database port
     * @param user      database user
     * @param password  database password
     * @param ca        database self signed certs
     * @param crt:      database certificate
     * @param key:      database private key
     * @throws Exception exception     *
     */
    @Given("^I connect with JDBC and security type '(TLS|MD5|TRUST|CERT|LDAP|tls|md5|trust|cert|ldap)'( and encryption)? to database( transactional)? '(.+?)' on host '(.+?)' and port '(.+?)' with user '(.+?)'( and password '(.+?)')?( and root ca '(.+?)')?(, crt '(.+?)')?( and key '(.+?)' certificates)?$")
    public void connectDatabasePostgres(String securityType, String encryption, String trans, String database, String host, String port, String user, String password, String ca, String crt, String key) throws Exception {
        this.commonspec.getExceptions().clear();
        if ("TLS".equals(securityType) || "tls".equals(securityType) || "CERT".equals(securityType) || "cert".equals(securityType)) {
            commonspec.getLogger().debug("opening secure database");
            try {
                this.commonspec.connectToPostgreSQLDatabase(encryption, trans, database, host, port, user, password, true, ca, crt, key);
            } catch (Exception e) {
                this.commonspec.getExceptions().add(e);
            }
        } else {
            commonspec.getLogger().debug("opening database");
            try {
                this.commonspec.connectToPostgreSQLDatabase(encryption, trans, database, host, port, user, password, false, ca, crt, key);
            } catch (Exception e) {
                this.commonspec.getExceptions().add(e);
            }
        }
    }

    /**
     * Execute a query with schema over a cluster
     *
     * @param fields        columns on which the query is executed. Example: "latitude,longitude" or "*" or "count(*)"
     * @param schema        the file of configuration (.conf) with the options of mappin. If schema is the word "empty", method will not add a where clause.
     * @param type          type of the changes in schema (string or json)
     * @param table         table for create the index
     * @param magic_column  magic column where index will be saved. If you don't need index, you can add the word "empty"
     * @param keyspace      keyspace used
     * @param modifications all data in "where" clause. Where schema is "empty", query has not a where clause. So it is necessary to provide an empty table. Example:  ||.
     */
    @When("^I execute a query over fields '(.+?)' with schema '(.+?)' of type '(json|string)' with magic_column '(.+?)' from table: '(.+?)' using keyspace: '(.+?)' with:$")
    public void sendQueryOfType(String fields, String schema, String type, String magic_column, String table, String keyspace, DataTable modifications) {
        try {
            commonspec.setResultsType("cassandra");
            commonspec.getCassandraClient().useKeyspace(keyspace);
            commonspec.getLogger().debug("Starting a query of type " + commonspec.getResultsType());

            String query = "";

            if (schema.equals("empty") && magic_column.equals("empty")) {

                query = "SELECT " + fields + " FROM " + table + ";";

            } else if (!schema.equals("empty") && magic_column.equals("empty")) {
                String retrievedData = commonspec.retrieveData(schema, type);
                String modifiedData = commonspec.modifyData(retrievedData, type, modifications).toString();
                query = "SELECT " + fields + " FROM " + table + " WHERE " + modifiedData + ";";


            } else {
                String retrievedData = commonspec.retrieveData(schema, type);
                String modifiedData = commonspec.modifyData(retrievedData, type, modifications).toString();
                query = "SELECT " + fields + " FROM " + table + " WHERE " + magic_column + " = '" + modifiedData + "';";

            }
            commonspec.getLogger().debug("query: {}", query);
            com.datastax.driver.core.ResultSet results = commonspec.getCassandraClient().executeQuery(query);
            commonspec.setCassandraResults(results);
        } catch (Exception e) {
            commonspec.getLogger().debug("Exception captured");
            commonspec.getLogger().debug(e.toString());
            commonspec.getExceptions().add(e);
        }


    }

    /**
     * Execute a query on (mongo) database
     *
     * @param query         path to query
     * @param type          type of data in query (string or json)
     * @param collection    collection in database
     * @param modifications modifications to perform in query
     */
    @When("^I execute a query '(.+?)' of type '(json|string)' in mongo '(.+?)' database using collection '(.+?)' with:$")
    public void sendQueryOfType(String query, String type, String database, String collection, DataTable modifications) throws Exception {
        try {
            commonspec.setResultsType("mongo");
            String retrievedData = commonspec.retrieveData(query, type);
            String modifiedData = commonspec.modifyData(retrievedData, type, modifications);
            commonspec.getMongoDBClient().connectToMongoDBDataBase(database);
            DBCollection dbCollection = commonspec.getMongoDBClient().getMongoDBCollection(collection);
            DBObject dbObject = (DBObject) JSON.parse(modifiedData);
            DBCursor cursor = dbCollection.find(dbObject);
            commonspec.setMongoResults(cursor);
        } catch (Exception e) {
            commonspec.getExceptions().add(e);
        }
    }

    /**
     * Execute query with filter over elasticsearch
     *
     * @param indexName
     * @param mappingName
     * @param columnName
     * @param filterType  it could be equals, gt, gte, lt and lte.
     * @param value       value of the column to be filtered.
     */
    @When("^I execute an elasticsearch query over index '(.*?)' and mapping '(.*?)' and column '(.*?)' with value '(.*?)' to '(.*?)'$")
    public void elasticSearchQueryWithFilter(String indexName, String mappingName, String
            columnName, String filterType, String value) {
        try {
            commonspec.setResultsType("elasticsearch");
            commonspec.setElasticsearchResults(
                    commonspec.getElasticSearchClient()
                            .searchSimpleFilterElasticsearchQuery(indexName, columnName, value, filterType)

            );
        } catch (Exception e) {
            commonspec.getLogger().debug("Exception captured");
            commonspec.getLogger().debug(e.toString());
            commonspec.getExceptions().add(e);
        }
    }


    /**
     * Create a Cassandra index.
     *
     * @param index_name    index name
     * @param schema        the file of configuration (.conf) with the options of mappin
     * @param type          type of the changes in schema (string or json)
     * @param table         table for create the index
     * @param magic_column  magic column where index will be saved
     * @param keyspace      keyspace used
     * @param modifications data introduced for query fields defined on schema
     */
    @When("^I create a Cassandra index named '(.+?)' with schema '(.+?)' of type '(json|string)' in table '(.+?)' using magic_column '(.+?)' using keyspace '(.+?)' with:$")
    public void createCustomMapping(String index_name, String schema, String type, String table, String magic_column, String keyspace, DataTable modifications) throws Exception {
        String retrievedData = commonspec.retrieveData(schema, type);
        String modifiedData = commonspec.modifyData(retrievedData, type, modifications).toString();
        String query = "CREATE CUSTOM INDEX " + index_name + " ON " + keyspace + "." + table + "(" + magic_column + ") "
                + "USING 'com.stratio.cassandra.lucene.Index' WITH OPTIONS = " + modifiedData;
        commonspec.getLogger().debug("Will execute a cassandra query: {}", query);
        commonspec.getCassandraClient().executeQuery(query);
    }

    /**
     * Drop table
     *
     * @param table
     * @param keyspace
     */
    @When("^I drop a Cassandra table named '(.+?)' using keyspace '(.+?)'$")
    public void dropTableWithData(String table, String keyspace) {
        try {
            commonspec.getCassandraClient().useKeyspace(keyspace);
            commonspec.getCassandraClient().dropTable(table);
        } catch (Exception e) {
            commonspec.getLogger().debug("Exception captured");
            commonspec.getLogger().debug(e.toString());
            commonspec.getExceptions().add(e);
        }
    }

    /**
     * Truncate table
     *
     * @param table
     * @param keyspace
     */
    @When("^I truncate a Cassandra table named '(.+?)' using keyspace '(.+?)'$")
    public void truncateTable(String table, String keyspace) {
        try {
            commonspec.getCassandraClient().useKeyspace(keyspace);
            commonspec.getCassandraClient().truncateTable(table);
        } catch (Exception e) {
            commonspec.getLogger().debug("Exception captured");
            commonspec.getLogger().debug(e.toString());
            commonspec.getExceptions().add(e);
        }
    }

    /**
     * Create an elasticsearch index.
     *
     * @param index
     */
    @When("^I create an elasticsearch index named '(.+?)'( with '(.*?)' shards)?( with '(.*?)' replicas)?( removing existing index if exist)?$")
    public void createElasticsearchIndex(String index, String shards, String replicas, String removeIndex) {
        Settings.Builder settings = Settings.builder();

        if (shards != null) {
            settings.put("index.number_of_shards", shards);
        }
        if (replicas != null) {
            settings.put("index.number_of_replicas", replicas);
        }
        if (removeIndex != null && commonspec.getElasticSearchClient().indexExists(index)) {
            commonspec.getElasticSearchClient().dropSingleIndex(index);
        }
        commonspec.getElasticSearchClient().createSingleIndex(index, settings);
    }


    /**
     * Create an opensearch index.
     *
     * @param index
     */
    @When("^I create an opensearch index named '(.+?)'( with '(.*?)' shards)?( with '(.*?)' replicas)?( removing existing index if exist)?$")
    public void createOpenSearchIndex(String index, String shards, String replicas, String removeIndex) {
        org.opensearch.common.settings.Settings.Builder settings = org.opensearch.common.settings.Settings.builder();

        if (shards != null) {
            settings.put("index.number_of_shards", shards);
        }
        if (replicas != null) {
            settings.put("index.number_of_replicas", replicas);
        }
        if (removeIndex != null && commonspec.getOpenSearchClient().indexExists(index)) {
            commonspec.getOpenSearchClient().dropSingleIndex(index);
        }
        commonspec.getOpenSearchClient().createSingleIndex(index, settings);
    }


    /**
     * Check number of shards from elasticsearch index.
     *
     * @param index
     * @param numberOfShards
     */
    @Then("^The number of shards from index '(.+?)' is '(.+?)'$")
    public void checkNumberOfShardsFromIndex(String index, String numberOfShards) {
        assertThat(commonspec.getElasticSearchClient().getNumberOfShardsFromIndex(index)).isEqualTo(numberOfShards);
    }

    /**
     * Check number of replicas from elasticsearch index.
     *
     * @param index
     * @param numberOfReplicas
     */
    @Then("^The number of replicas from index '(.+?)' is '(.+?)'$")
    public void checkNumberOfReplicasFromIndex(String index, String numberOfReplicas) {
        assertThat(commonspec.getElasticSearchClient().getNumberOfReplicasFromIndex(index)).isEqualTo(numberOfReplicas);
    }


    /**
     * Obtain number of shards from elasticsearch index.
     *
     * @param index
     */
    @Then("^I obtain the number of shards from index '(.+?)'$")
    public void obtainNumberOfShardsFromIndex(String index) {
        commonspec.getElasticSearchClient().getNumberOfShardsFromIndex(index);
    }

    /**
     * Check number of shards from elasticsearch index.
     *
     * @param index
     */
    @Then("^I obtain the number of replicas from index '(.+?)'$")
    public void obtainNumberOfReplicasFromIndex(String index) {
        commonspec.getElasticSearchClient().getNumberOfReplicasFromIndex(index);
    }

    /**
     * Index a document.
     *
     * @param baseData
     * @param indexName
     * @throws Exception
     */
    @When("^I index a document '(.+?)' with id '(.+?)' in the index named '(.+?)'$")
    public void indexElasticsearchDocument(String baseData, String id, String indexName) throws Exception {
        // Retrieve data
        String retrieveData = commonspec.retrieveData(baseData, "json");
        commonspec.getElasticSearchClient().indexDocument(indexName, id, retrieveData);
    }

    /**
     * Index a document in opensearch.
     *
     * @param baseData
     * @param indexName
     * @throws Exception
     */
    @When("^I index in opensearch a document '(.+?)' with id '(.+?)' in the index named '(.+?)'$")
    public void indexOpenSearchDocument(String baseData, String id, String indexName) throws Exception {
        // Retrieve data
        String retrieveData = commonspec.retrieveData(baseData, "json");
        commonspec.getOpenSearchClient().indexDocument(indexName, id, retrieveData);
    }

    /**
     * Check that the ElasticSearch index exists.
     *
     * @param documentId
     * @param indexName
     */
    @Then("^An elasticsearch document id '(.+?)' exists in an index named '(.+?)'")
    public void elasticSearchDocumentExist(String documentId, String indexName) {
        assert (commonspec.getElasticSearchClient().existsDocument(indexName, documentId)) : "There is no document in these index";
    }

    /**
     * Check that the OpenSearch index exists.
     *
     * @param documentId
     * @param indexName
     */
    @Then("^An opensearch document id '(.+?)' exists in an index named '(.+?)'")
    public void openSearchDocumentExist(String documentId, String indexName) {
        assert (commonspec.getOpenSearchClient().existsDocument(indexName, documentId)) : "There is no document in these index";
    }

    /**
     * Check that the ElasticSearch document not exists.
     *
     * @param documentId
     * @param indexName
     */
    @Then("^An elasticsearch document id '(.+?)' does not exist in an index named '(.+?)'")
    public void elasticSearchDocumentNotExist(String documentId, String indexName) {
        assert (!commonspec.getElasticSearchClient().existsDocument(indexName, documentId)) : "There is a document in these index";
    }

    /**
     * Check that the OpenSearch document not exists.
     *
     * @param documentId
     * @param indexName
     */
    @Then("^An opensearch document id '(.+?)' does not exist in an index named '(.+?)'")
    public void openSearchDocumentNotExist(String documentId, String indexName) {
        assert (!commonspec.getOpenSearchClient().existsDocument(indexName, documentId)) : "There is a document in these index";
    }

    /**
     * Delete a document.
     *
     * @param documentId
     * @param indexName
     * @throws Exception
     */
    @Then("^I delete an elasticsearch document with id '(.+?)' in the index named '(.+?)'$")
    public void elasticSearchDocumentDelete(String documentId, String indexName) {
        commonspec.getElasticSearchClient().deleteDocument(indexName, documentId);
    }

    /**
     * Delete a document.
     *
     * @param documentId
     * @param indexName
     * @throws Exception
     */
    @Then("^I delete an opensearch document with id '(.+?)' in the index named '(.+?)'$")
    public void openSearchDocumentDelete(String documentId, String indexName) {
        commonspec.getOpenSearchClient().deleteDocument(indexName, documentId);
    }

    /*
     * @param query
     * executes query in database
     *
     *
     */
    @When("^I execute query '(.+?)'$")
    public void executeQuery(String query) throws Exception {
        ThreadProperty.remove("querysize");
        getCommonSpec().setPreviousSqlResult(null);
        Statement myStatement = null;
        int result = 0;
        Connection myConnection = this.commonspec.getConnection();
        if (myConnection == null) {
            throw new Exception("JDBC connection is not opened");
        }

        try {
            myStatement = myConnection.createStatement();
            result = myStatement.executeUpdate(query);
            myStatement.close();
        } catch (Exception e) {
            e.printStackTrace();
            assertThat(result).as(e.getClass().getName() + ": " + e.getMessage()).isNotEqualTo(0);
        }
    }

    /*
     * @param query
     * executes query in database
     */
    @When("^I execute query '(.+?)' through JDBC connection(, returning an exception)?(, returning a warning)?( with message \"(.+?)\")?$")
    public void executeJdbcQuery(String query, String exceptionReturned, String warningReturned, String exceptionMessageExpected) throws Exception {
        Connection myConnection = this.commonspec.getConnection();
        if (myConnection == null) {
            throw new Exception("JDBC connection is not opened");
        }
        try {
            String warningMessage = null;
            ThreadProperty.remove("querysize");
            getCommonSpec().setPreviousSqlResult(null);
            Statement myStatement = myConnection.createStatement();
            myStatement.execute(query);

            if (warningReturned != null) {
                if (myStatement.getWarnings() != null) {
                    warningMessage = myStatement.getWarnings().toString();
                } else {
                    fail("Query was executed successfully but we expected a warning");
                }
            }

            myStatement.close();
            if (exceptionReturned != null) {
                fail("Query was executed successfully but we expected an exception");
            }
            if (warningReturned != null && exceptionMessageExpected != null) {
                Assertions.assertThat(warningMessage).contains(exceptionMessageExpected);
            }
        } catch (Exception e) {
            if (exceptionReturned == null) {
                fail("Error executing query -> " + e.getMessage());
            } else {
                getCommonSpec().getLogger().warn("Error executing query -> " + e.getMessage());
                if (exceptionMessageExpected != null) {
                    Assertions.assertThat(e.getMessage()).as("Query error message").contains(exceptionMessageExpected);
                }
            }
        }
    }

    /*
     * @param query Query to execute
     * @param cancelTime query will be cancelled when cancelTime was reached
     * Executes query in database and cancel it after calcelTime seconds
     */
    @When("^I execute query '(.+?)' in background and I cancel it after '(\\d+)' seconds, returning a Query cancelled SQLSyntaxErrorException$")
    public void executeJdbcQuery(String query, Integer cancelTime) throws Exception {
        Connection myConnection = this.commonspec.getConnection();
        if (myConnection == null) {
            throw new Exception("JDBC connection is not opened");
        }
        try {
            ThreadProperty.remove("querysize");
            getCommonSpec().setPreviousSqlResult(null);
            Statement myStatement = myConnection.createStatement();
            Future<Void> future = CompletableFuture.supplyAsync(() -> {
                try {
                    Thread.sleep(cancelTime * 1000);
                    myStatement.cancel();
                } catch (SQLException | InterruptedException e) {
                    throw new RuntimeException(e);
                }
                return null;
            });
            myStatement.execute(query);
            future.cancel(true);
            fail("Query was executed succesfully (it should be cancelled)");
        } catch (SQLSyntaxErrorException e) {
            assertThat(e.getMessage()).as("SQLSyntaxErrorException message").contains("Query cancelled");
        }
    }

    /*
     * @param query
     * selects data from database
     * and sends it to environment variable
     *
     */
    @When("^I query the database with '(.+?)'$")
    public void selectData(String query) throws Exception {
        Statement myStatement = null;
        //postgres table
        List<String> sqlTable = new ArrayList<String>();
        List<String> sqlTableAux = new ArrayList<String>();
        Map<String, List<String>> sqlResultMap = new HashMap<>();
        Connection myConnection = this.commonspec.getConnection();
        if (myConnection == null) {
            throw new Exception("JDBC connection is not opened");
        }
        java.sql.ResultSet rs = null;
        try {
            myStatement = myConnection.createStatement();
            rs = myStatement.executeQuery(query);
            //column names
            ResultSetMetaData resultSetMetaData = rs.getMetaData();
            int count = resultSetMetaData.getColumnCount();
            for (int i = 1; i <= count; i++) {
                sqlTable.add(resultSetMetaData.getColumnName(i).toString());
                sqlResultMap.put(resultSetMetaData.getColumnName(i), new ArrayList<>());
            }
            //takes column names and column count
            int resultSize = 0;
            while (rs.next()) {
                resultSize++;
                for (int i = 1; i <= count; i++) {
                    //aux list without column names
                    sqlTableAux.add(rs.getObject(i) != null ? rs.getObject(i).toString() : "<EMPTY>");
                    List<String> previousList = sqlResultMap.get(resultSetMetaData.getColumnName(i));
                    if (previousList == null) {
                        previousList = new ArrayList<>();
                    }
                    previousList.add(rs.getObject(i) != null ? rs.getObject(i).toString() : "<EMPTY>");
                    sqlResultMap.put(resultSetMetaData.getColumnName(i), previousList);
                }
            }
            ThreadProperty.set("querysize", String.valueOf(resultSize));
            sqlTable.addAll(sqlTableAux);

            //sends raws to environment variable
            for (int i = 0; i < sqlTable.size(); i++) {
                ThreadProperty.set("queryresponse" + i, sqlTable.get(i));
            }
            ThreadProperty.remove("queryresponse" + sqlTable.size());
            getCommonSpec().setPreviousSqlResult(sqlResultMap);
            rs.close();
            myStatement.close();
        } catch (Exception e) {
            e.printStackTrace();
            Assertions.assertThat(rs).as("There are no response from SELECT").isNotNull();
        }
    }

    /**
     * Checks if a keyspaces exists in Cassandra.
     *
     * @param keyspace
     */
    @Then("^a Cassandra keyspace '(.+?)' exists$")
    public void assertKeyspaceOnCassandraExists(String keyspace) {
        assertThat(commonspec.getCassandraClient().getKeyspaces()).as("The keyspace " + keyspace + " exists on cassandra").contains(keyspace);
    }

    /**
     * Checks a keyspace does not exist in Cassandra.
     *
     * @param keyspace
     */
    @Then("^a Cassandra keyspace '(.+?)' does not exist$")
    public void assertKeyspaceOnCassandraDoesNotExist(String keyspace) {
        assertThat(commonspec.getCassandraClient().getKeyspaces()).as("The keyspace " + keyspace + " does not exist on cassandra").doesNotContain(keyspace);
    }

    /**
     * Checks if a cassandra keyspace contains a table.
     *
     * @param keyspace
     * @param tableName
     */
    @Then("^a Cassandra keyspace '(.+?)' contains a table '(.+?)'$")
    public void assertTableExistsOnCassandraKeyspace(String keyspace, String tableName) {
        assertThat(commonspec.getCassandraClient().getTables(keyspace)).as("The table " + tableName + "exists on cassandra").contains(tableName);
    }

    /**
     * Checks a cassandra keyspace does not contain a table.
     *
     * @param keyspace
     * @param tableName
     */
    @Then("^a Cassandra keyspace '(.+?)' does not contain a table '(.+?)'$")
    public void assertTableDoesNotExistOnCassandraKeyspace(String keyspace, String tableName) {
        assertThat(commonspec.getCassandraClient().getTables(keyspace)).as("The table " + tableName + "exists on cassandra").doesNotContain(tableName);
    }

    /**
     * Checks the number of rows in a cassandra table.
     *
     * @param keyspace
     * @param tableName
     * @param numberRows
     */
    @Then("^a Cassandra keyspace '(.+?)' contains a table '(.+?)' with '(.+?)' rows$")
    public void assertRowNumberOfTableOnCassandraKeyspace(String keyspace, String tableName, String numberRows) {
        Long numberRowsLong = Long.parseLong(numberRows);
        commonspec.getCassandraClient().useKeyspace(keyspace);
        assertThat(commonspec.getCassandraClient().executeQuery("SELECT COUNT(*) FROM " + tableName + ";").all().get(0).getLong(0)).as("The table " + tableName + "exists on cassandra").
                isEqualTo(numberRowsLong);
    }

    /**
     * Checks if a cassandra table contains the values of a DataTable.
     *
     * @param keyspace
     * @param tableName
     * @param data
     * @throws InterruptedException
     */
    @Then("^a Cassandra keyspace '(.+?)' contains a table '(.+?)' with values:$")
    public void assertValuesOfTable(String keyspace, String tableName, DataTable data) throws InterruptedException {
        //  USE of Keyspace
        commonspec.getCassandraClient().useKeyspace(keyspace);
        // Obtain the types and column names of the datatable
        // to return in a hashmap,
        Map<String, String> dataTableColumns = extractColumnNamesAndTypes(data.cells().get(0));
        // check if the table has columns
        String query = "SELECT * FROM " + tableName + " LIMIT 1;";
        com.datastax.driver.core.ResultSet res = commonspec.getCassandraClient().executeQuery(query);
        equalsColumns(res.getColumnDefinitions(), dataTableColumns);
        //receiving the string from the select with the columns
        // that belong to the dataTable
        List<String> selectQueries = giveQueriesList(data, tableName, columnNames(data.cells().get(0)));
        //Check the data  of cassandra with different queries
        int index = 1;
        for (String execQuery : selectQueries) {
            res = commonspec.getCassandraClient().executeQuery(execQuery);
            List<Row> resAsList = res.all();
            assertThat(resAsList.size()).as("The query " + execQuery + " not return any result on Cassandra").isGreaterThan(0);
            assertThat(resAsList.get(0).toString()
                    .substring(VALUE_SUBSTRING)).as("The resultSet is not as expected").isEqualTo(data.cells().get(index).toString().replace("'", ""));
            index++;
        }
    }

    @SuppressWarnings("rawtypes")
    private void equalsColumns(ColumnDefinitions resCols, Map<String, String> dataTableColumns) {
        Iterator it = dataTableColumns.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry e = (Map.Entry) it.next();
            assertThat(resCols.toString()).as("The table not contains the column.").contains(e.getKey().toString());
            DataType type = resCols.getType(e.getKey().toString());
            assertThat(type.getName().toString()).as("The column type is not equals.").isEqualTo(e.getValue().toString());
        }
    }

    private List<String> giveQueriesList(DataTable data, String tableName, String colNames) {
        List<String> queryList = new ArrayList<String>();
        for (int i = 1; i < data.cells().size(); i++) {
            String query = "SELECT " + colNames + " FROM " + tableName;
            List<String> row = data.cells().get(i);
            query += conditionWhere(row, colNames.split(",")) + ";";
            queryList.add(query);
        }
        return queryList;
    }

    private String conditionWhere(List<String> values, String[] columnNames) {
        StringBuilder condition = new StringBuilder();
        condition.append(" WHERE ");
        Pattern numberPat = Pattern.compile("^\\d+(\\.*\\d*)?");
        Pattern booleanPat = Pattern.compile("true|false");
        for (int i = 0; i < values.size() - 1; i++) {
            condition.append(columnNames[i]).append(" =");
            condition.append(" ").append(values.get(i)).append(" AND ");
        }
        condition.append(columnNames[columnNames.length - 1]).append(" =");
        condition.append(" ").append(values.get(values.size() - 1));

        condition.append(" ALLOW FILTERING");
        return condition.toString();
    }

    private String columnNames(List<String> firstRow) {
        StringBuilder columnNamesForQuery = new StringBuilder();
        for (String s : firstRow) {
            String[] aux = s.split("-");
            columnNamesForQuery.append(aux[0]).append(",");
        }
        return columnNamesForQuery.toString().substring(0, columnNamesForQuery.length() - 1);
    }

    private Map<String, String> extractColumnNamesAndTypes(List<String> firstRow) {
        HashMap<String, String> columns = new HashMap<String, String>();
        for (String s : firstRow) {
            String[] aux = s.split("-");
            columns.put(aux[0], aux[1]);
        }
        return columns;
    }


    /**
     * Checks the values of a MongoDB table.
     *
     * @param dataBase
     * @param tableName
     * @param data
     */
    @Then("^a Mongo dataBase '(.+?)' contains a table '(.+?)' with values:")
    public void assertValuesOfTableMongo(String dataBase, String tableName, DataTable data) {
        commonspec.getMongoDBClient().connectToMongoDBDataBase(dataBase);
        ArrayList<DBObject> result = (ArrayList<DBObject>) commonspec.getMongoDBClient().readFromMongoDBCollection(
                tableName, data);
        DBObjectsAssert.assertThat(result).containedInMongoDBResult(data);

    }

    /**
     * Checks if a MongoDB database contains a table.
     *
     * @param database
     * @param tableName
     */
    @Then("^a Mongo dataBase '(.+?)' doesnt contains a table '(.+?)'$")
    public void aMongoDataBaseContainsaTable(String database, String tableName) {
        commonspec.getMongoDBClient().connectToMongoDBDataBase(database);
        Set<String> collectionsNames = commonspec.getMongoDBClient().getMongoDBCollections();
        assertThat(collectionsNames).as("The Mongo dataBase contains the table").doesNotContain(tableName);
    }

    /**
     * Checks the different results of a previous query
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
     *                        IMPORTANT: There no should be no existing columns
     * @throws Exception
     */
    @Then("^There are results found with:$")
    public void resultsMustBe(DataTable expectedResults) throws Exception {

        String type = commonspec.getResultsType();
        assertThat(type).isNotEqualTo("").overridingErrorMessage("It's necessary to define the result type");
        switch (type) {
            case "cassandra":
                commonspec.resultsMustBeCassandra(expectedResults);
                break;
            case "mongo":
                commonspec.resultsMustBeMongo(expectedResults);
                break;
            case "elasticsearch":
                commonspec.resultsMustBeElasticsearch(expectedResults);
                break;
            case "csv":
                commonspec.resultsMustBeCSV(expectedResults);
                break;
            default:
                commonspec.getLogger().warn("default switch branch on results check");
        }
    }

    /**
     * Check that the ElasticSearch index exists.
     *
     * @param indexName
     */
    @Then("^An elasticsearch index named '(.+?)' exists")
    public void elasticSearchIndexExist(String indexName) {
        assert (commonspec.getElasticSearchClient().indexExists(indexName)) : "There is no index with that name";
    }

    /**
     * Check that the OpenSearch index exists.
     *
     * @param indexName
     */
    @Then("^An opensearch index named '(.+?)' exists")
    public void openSearchIndexExist(String indexName) {
        assert (commonspec.getOpenSearchClient().indexExists(indexName)) : "There is no index with that name";
    }

    /**
     * Check that the ElasticSearch index does not exist.
     *
     * @param indexName
     */
    @Then("^An elasticsearch index named '(.+?)' does not exist")
    public void elasticSearchIndexDoesNotExist(String indexName) {
        assert !commonspec.getElasticSearchClient().indexExists(indexName) : "There is an index with that name";
    }

    /**
     * Check that the OpenSearch index does not exist.
     *
     * @param indexName
     */
    @Then("^An opensearch index named '(.+?)' does not exist")
    public void openSearchIndexDoesNotExist(String indexName) {
        assert !commonspec.getOpenSearchClient().indexExists(indexName) : "There is an index with that name";
    }

    /**
     * Check that an elasticsearch index contains a specific document
     *
     * @param indexName
     * @param columnName
     * @param columnValue
     */
    @Then("^The Elasticsearch index named '(.+?)' contains a column named '(.+?)' with the value '(.+?)'$")
    public void elasticSearchIndexContainsDocument(String indexName, String columnName, String columnValue) throws Exception {
        Assertions.assertThat((commonspec.getElasticSearchClient().searchSimpleFilterElasticsearchQuery(
                indexName,
                columnName,
                columnValue,
                "equals"
        ).size()) > 0).isTrue().withFailMessage("The index does not contain that document");
    }

    /**
     * Check that an opensearch index contains a specific document
     *
     * @param indexName
     * @param columnName
     * @param columnValue
     */
    @Then("^The OpenSearch index named '(.+?)' contains a column named '(.+?)' with the value '(.+?)'$")
    public void openSearchIndexContainsDocument(String indexName, String columnName, String columnValue) throws Exception {
        Assertions.assertThat((commonspec.getOpenSearchClient().searchSimpleFilterOpensearchQuery(
                indexName,
                columnName,
                columnValue,
                "equals"
        ).size()) > 0).isTrue().withFailMessage("The index does not contain that document");
    }

    /*
     * @param table
     * checks table existence
     *
     */
    @Then("^table '(.+?)' exists$")
    public void checkTable(String tableName) throws Exception {
        Statement myStatement = null;
        Connection myConnection = this.commonspec.getConnection();
        if (myConnection == null) {
            throw new Exception("JDBC connection is not opened");
        }

        //query checks table existence, existence table name in system table  pg_tables
        String query = "SELECT * FROM pg_tables WHERE tablename = " + "\'" + tableName + "\'" + ";";
        try {
            myStatement = myConnection.createStatement();
            java.sql.ResultSet rs = myStatement.executeQuery(query);
            //if there are no data row
            if (rs.next() == false) {
                Assertions.assertThat(rs.next()).as("there are no table " + tableName).isTrue();
            } else {
                //data exist
                String resultTableName = rs.getString(2);
                assertThat(resultTableName).as("there are incorrect table name " + tableName).contains(tableName);
            }
            rs.close();
            myStatement.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }


    /*
     * @param table
     * checks table existence negative case
     *
     */
    @Then("^table '(.+?)' doesn't exists$")
    public void checkTableFalse(String tableName) throws Exception {
        Statement myStatement = null;
        Connection myConnection = this.commonspec.getConnection();
        if (myConnection == null) {
            throw new Exception("JDBC connection is not opened");
        }

        String query = "SELECT * FROM pg_tables WHERE tablename = " + "\'" + tableName + "\'" + ";";
        try {
            myStatement = myConnection.createStatement();
            java.sql.ResultSet rs = myStatement.executeQuery(query);
            //if there are no data row, table doesn't exists
            if (rs.next() == false) {
                Assertions.assertThat(rs.next()).as("table exists " + tableName).isFalse();
            } else {
                String resultTableName = rs.getString(2);
                assertThat(resultTableName).as("table exists " + tableName).isEqualToIgnoringCase(tableName);
            }
            rs.close();
            myStatement.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    /*
     * @param tableName
     * @param dataTable
     * compares two tables: pattern table and the result from remote database
     * by default: order by id
     *
     */
    @Then("^I check that table '(.+?)' is iqual to$")
    public void comparetable(String tableName, DataTable dataTable) throws Exception {
        Statement myStatement = null;
        java.sql.ResultSet rs = null;

        //from postgres table
        List<String> sqlTable = new ArrayList<String>();
        List<String> sqlTableAux = new ArrayList<String>();
        //from Cucumber Datatable
        List<String> tablePattern = new ArrayList<String>();
        //comparison is by lists of string
        tablePattern = dataTable.asList(String.class);


        Connection myConnection = this.commonspec.getConnection();
        if (myConnection == null) {
            throw new Exception("JDBC connection is not opened");
        }
        String query = "SELECT * FROM " + tableName + " order by " + "id" + ";";
        try {
            myStatement = myConnection.createStatement();
            rs = myStatement.executeQuery(query);

            //takes column names and culumn count
            ResultSetMetaData resultSetMetaData = rs.getMetaData();
            int count = resultSetMetaData.getColumnCount();
            for (int i = 1; i <= count; i++) {
                sqlTable.add(resultSetMetaData.getColumnName(i).toString());
            }

            //takes column names and culumn count
            while (rs.next()) {
                for (int i = 1; i <= count; i++) {
                    //aux list without column names
                    sqlTableAux.add(rs.getObject(i).toString());
                }
            }
            sqlTable.addAll(sqlTableAux);

            assertThat(sqlTable).as("Not equal elements!").isEqualTo(tablePattern);
            rs.close();
            myStatement.close();
        } catch (Exception e) {
            e.printStackTrace();
            assertThat(rs).as("There are no table " + tableName).isNotNull();
        }
    }

    /*
     * closes opened database
     *
     */
    @Then("^I close database connection$")
    public void closeDatabase() throws Exception {
        JDBCConnection.closeConnection();
    }

    /*
     * @param tableName
     * checks the result from select
     *
     */
    @Then("^I check that result is:$")
    public void comparetable(DataTable dataTable) throws Exception {

        //from Cucumber Datatable, the pattern to verify
        List<String> tablePattern = new ArrayList<String>();
        tablePattern = dataTable.asList(String.class);

        //the result from select
        List<String> sqlTable = new ArrayList<String>();

        //the result is taken from previous step
        for (int i = 0; ThreadProperty.get("queryresponse" + i) != null; i++) {
            String ip_value = ThreadProperty.get("queryresponse" + i);
            sqlTable.add(i, ip_value);
        }

        for (int i = 0; ThreadProperty.get("queryresponse" + i) != null; i++) {
            ThreadProperty.remove("queryresponse" + i);
        }

        assertThat(tablePattern).as("response is not equal to the expected").isEqualTo(sqlTable);
    }

    /**
     * @param objetType
     * @param objectName
     * @throws Exception
     */
    @Then("^'(.+?)' '(.+?)' exists$")
    public void checkObjectExists(String objetType, String objectName) throws Exception {
        Statement myStatement = null;
        Connection myConnection = this.commonspec.getConnection();
        if (myConnection == null) {
            throw new Exception("JDBC connection is not opened");
        }

        String query;

        switch (objetType) {
            case "Database":
                query = "SELECT datname FROM pg_database WHERE datname = " + "\'" + objectName + "\'" + ";";
                break;

            case "Table":
                query = "SELECT tablename FROM pg_tables WHERE tablename = " + "\'" + objectName + "\'" + ";";
                break;

            case "View":
                query = "SELECT viewname FROM pg_views WHERE viewname = " + "\'" + objectName + "\'" + ";";
                break;

            case "MaterializedView":
                query = "SELECT matviewname FROM pg_matviews WHERE matviewname = " + "\'" + objectName + "\'" + ";";
                break;

            case "Sequence":
                query = "SELECT sequence_name FROM information_schema.sequences WHERE sequence_name = " + "\'" + objectName + "\'" + ";";
                break;

            case "Foreign Data Wrapper":
                query = "SELECT foreign_data_wrapper_name FROM information_schema.foreign_data_wrappers WHERE foreign_data_wrapper_name = " + "\'" + objectName + "\'" + ";";
                break;

            case "Foreign Server":
                query = "SELECT foreign_server_name FROM information_schema.foreign_servers WHERE foreign_server_name = " + "\'" + objectName + "\'" + ";";
                break;

            case "Function":
                query = "SELECT p.proname FROM pg_catalog.pg_proc p JOIN pg_catalog.pg_namespace n ON n.oid = p.pronamespace WHERE p.proname = " + "\'" + objectName + "\'" + ";";
                break;

            case "Schema":
                query = "SELECT schema_name from information_schema.schemata join pg_namespace on schema_name = nspname where schema_name = " + "\'" + objectName + "\'" + ";";
                break;

            case "Domain":
                query = "SELECT domain_name from information_schema.domains WHERE domain_name = " + "\'" + objectName + "\'" + ";";
                break;

            case "Type":
                query = "SELECT user_defined_type_name FROM information_schema.user_defined_types WHERE user_defined_type_name = " + "\'" + objectName + "\'" + ";";
                break;

            case "Column":
                query = "select column_name from information_schema.columns WHERE column_name = " + "\'" + objectName + "\'" + ";";
                break;

            default:
                query = "SELECT 1;";
                break;
        }

        try {
            myStatement = myConnection.createStatement();
            ResultSet rs = myStatement.executeQuery(query);
            //if there are no data row
            if (rs.next() == false) {
                assertThat(rs.next()).as("there are no " + objetType + ": " + objectName).isTrue();
            } else {
                //data exist
                String resultName = rs.getString(1);
                assertThat(resultName).as("there are incorrect " + objetType + " name: " + objectName).contains(objectName);
            }
            rs.close();
            myStatement.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Then("^'(.+?)' '(.+?)' doesn't exists$")
    public void checkObjectNoExists(String objetType, String objectName) throws Exception {
        Statement myStatement = null;
        Connection myConnection = this.commonspec.getConnection();
        if (myConnection == null) {
            throw new Exception("JDBC connection is not opened");
        }

        String query;

        switch (objetType) {
            case "Database":
                query = "SELECT datname FROM pg_database WHERE datname = " + "\'" + objectName + "\'" + ";";
                break;

            case "Table":
                query = "SELECT tablename FROM pg_tables WHERE tablename = " + "\'" + objectName + "\'" + ";";
                break;

            case "View":
                query = "SELECT viewname FROM pg_views WHERE viewname = " + "\'" + objectName + "\'" + ";";
                break;

            case "MaterializedView":
                query = "SELECT matviewname FROM pg_matviews WHERE matviewname = " + "\'" + objectName + "\'" + ";";
                break;

            case "Sequence":
                query = "SELECT sequence_name FROM information_schema.sequences WHERE sequence_name = " + "\'" + objectName + "\'" + ";";
                break;

            case "Foreign Data Wrapper":
                query = "SELECT foreign_data_wrapper_name FROM information_schema.foreign_data_wrappers WHERE foreign_data_wrapper_name = " + "\'" + objectName + "\'" + ";";
                break;

            case "Foreign Server":
                query = "SELECT foreign_server_name FROM information_schema.foreign_servers WHERE foreign_server_name = " + "\'" + objectName + "\'" + ";";
                break;

            case "Function":
                query = "SELECT p.proname FROM pg_catalog.pg_proc p JOIN pg_catalog.pg_namespace n ON n.oid = p.pronamespace WHERE p.proname = " + "\'" + objectName + "\'" + ";";
                break;

            case "Schema":
                query = "SELECT schema_name from information_schema.schemata join pg_namespace on schema_name = nspname where schema_name = " + "\'" + objectName + "\'" + ";";
                break;

            case "Domain":
                query = "SELECT domain_name from information_schema.domains WHERE domain_name = " + "\'" + objectName + "\'" + ";";
                break;

            case "Type":
                query = "SELECT user_defined_type_name FROM information_schema.user_defined_types WHERE user_defined_type_name = " + "\'" + objectName + "\'" + ";";
                break;

            case "Column":
                query = "select column_name from information_schema.columns WHERE column_name = " + "\'" + objectName + "\'" + ";";
                break;

            default:
                query = "SELECT 1;";
                break;
        }

        try {
            myStatement = myConnection.createStatement();
            ResultSet rs = myStatement.executeQuery(query);
            //if there are no data row, table doesn't exists
            if (rs.next() == false) {
                assertThat(rs.next()).as(objectName + " exists " + objectName).isFalse();
            } else {
                String resultName = rs.getString(1);
                assertThat(resultName).as(objectName + " exists " + objectName).isEqualToIgnoringCase(objectName);
            }
            rs.close();
            myStatement.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Then(value = "The result has to have '(.*?)' rows$")
    public void assertLengthResultXDDriver(String resultSize) {
        Assert.assertEquals(ThreadProperty.get("querysize"), resultSize);
        ThreadProperty.remove("querysize");
    }

    @Given(value = "^I connect with JDBC to Crossdata server( using TLS)? with parameters: host '(.*?)', port '(.*?)', user '(.*?)'(, password '(.*?)')?(, keystore '(.*?)')?(, keystore password '(.*?)')?(, truststore '(.*?)')?(, truststore password '(.*?)')?( and pagination)?$")
    public void createXDDriver(String security, String host, String port, String user, String password, String keyStorePath, String keyStorePass, String trustStorePath, String trustStorePass, String pagination) {
        try {
            commonspec.connectToCrossdataDatabase(security != null, host, port, keyStorePath, keyStorePass, trustStorePath, trustStorePass, user, password, pagination != null);
        } catch (Exception e) {
            fail("Error when we trying to connect to Crossdata Database -> " + e.getMessage());
        }
    }

    @Given(value = "^I connect with JDBC to database '(.*?)' with url '(.*?)', user '(.*?)'( and password '(.*?)')$")
    public void createCustomDriver(String database, String url, String user, String password) {
        try {
            commonspec.connectToCustomDatabase(database, url, user, password);
        } catch (Exception e) {
            fail("Error when we trying to connect to JDBC Database -> " + e.getMessage());
        }
    }

    /*
     * Test if dataTable is contained in query result
     *
     * @param tableName
     */
    @Then("^I check that result contains:$")
    public void compareTableContains(DataTable dataTable) {
        if (getCommonSpec().getPreviousSqlResult() == null) {
            fail("Result not stored. Query must be executed with step 'I query the database with ...'");
        }
        List<List<String>> datatableCells = dataTable.cells();
        Map<String, List<String>> previousSqlResult = getCommonSpec().getPreviousSqlResult();
        List<String> headersList = datatableCells.get(0);
        // Check that columns exists
        for (int col = 0; col < headersList.size(); col++) {
            if (previousSqlResult.get(headersList.get(col)) == null) {
                fail("Column '" + headersList.get(col) + "' doesn't appear in SQL result");
            }
        }
        for (int row = 1; row < datatableCells.size(); row++) {
            int col = 0;
            List<String> columnResult = previousSqlResult.get(headersList.get(col));
            Assertions.assertThat(columnResult).as("Value: " + datatableCells.get(row).get(col) + " is not returned in column: " + headersList.get(col)).contains(datatableCells.get(row).get(col));
            int auxRow = 0;
            while (col < datatableCells.get(row).size() && auxRow < columnResult.size()) {
                columnResult = previousSqlResult.get(headersList.get(col));
                if (columnResult.get(auxRow).equals(datatableCells.get(row).get(col))) {
                    col++;
                } else {
                    auxRow++;
                    col = 0;
                }
            }
            if (auxRow >= columnResult.size()) {
                fail("Error in table comparison: comparing table. Row " + row + " not found in query response.");
            }
        }
    }

    @Then("^I wait for '(\\d+)' seconds, checking every '(\\d+)' seconds for users existance:$")
    public void waitForUsersExistance(int wait, int period, DataTable users) throws Exception {
        String prefixQuery = "SELECT usename FROM pg_catalog.pg_user WHERE usename IN (";
        String suffixQuery = ") ORDER BY usename;";
        String usersQuery = "";
        boolean finished = false;

        List<String> expectedResult = new ArrayList<>();
        expectedResult.add("usename");
        for (int i = 0; i < users.cells().size(); i++) {
            String user = users.cells().get(i).get(0);
            if (i == 0) {
                usersQuery = usersQuery + "'" + user + "'";
            } else {
                usersQuery = usersQuery + ",'" + user + "'";
            }
            expectedResult.add(user);
        }

        String query = prefixQuery + usersQuery + suffixQuery;

        for (int i = 0; (i <= wait); i += period) {

            this.selectData(query);

            try {
                compareList(expectedResult);
                finished = true;
                commonspec.getLogger().info("Users found in database.");
                break;
            } catch (Throwable e) {
                commonspec.getLogger().info("Users not in database yet. Sleeping for " + period + " seconds.");
                Thread.sleep(period * 1000);
            }
        }

        if (!finished) {
            Assert.fail("Users are not available in database after specified timeout.");
        }
    }

    @Then("^I wait for '(\\d+)' seconds, checking every '(\\d+)' seconds for groups existance:$")
    public void waitForGroupsExistance(int wait, int period, DataTable groups) throws Exception {
        String prefixQuery = "SELECT groname FROM pg_catalog.pg_group WHERE groname IN (";
        String suffixQuery = ") ORDER BY groname;";
        String groupsQuery = "";
        boolean finished = false;

        List<String> expectedResult = new ArrayList<>();
        expectedResult.add("groname");
        for (int i = 0; i < groups.cells().size(); i++) {
            String group = groups.cells().get(i).get(0);
            if (i == 0) {
                groupsQuery = groupsQuery + "'g_" + group + "'";
            } else {
                groupsQuery = groupsQuery + ",'g_" + group + "'";
            }
            expectedResult.add("g_" + group);
        }

        String query = prefixQuery + groupsQuery + suffixQuery;

        for (int i = 0; (i <= wait); i += period) {

            this.selectData(query);

            try {
                compareList(expectedResult);
                finished = true;
                commonspec.getLogger().info("Groups found in database.");
                break;
            } catch (AssertionError | Exception e) {
                commonspec.getLogger().info("Groups not in database yet. Sleeping for " + period + " seconds.");
                Thread.sleep(period * 1000);
            }
        }

        if (!finished) {
            Assert.fail("Groups are not available in database after specified timeout.");
        }
    }

    @Then("^I wait for '(\\d+)' seconds, checking every '(\\d+)' seconds in group '(.*?)' for users existance:$")
    public void waitUsersInGroupExistance(int wait, int period, String group, DataTable users) throws Exception {
        String prefixQuery = "SELECT group_.groname group_name, array_to_string(array_agg(member_.rolname::text ORDER BY member_.rolname), ',') AS members FROM pg_group group_ LEFT JOIN pg_auth_members am ON am.roleid=group_.grosysid LEFT JOIN pg_roles member_ ON member_.oid=am.member WHERE group_.groname='g_";
        String suffixQuery = "' GROUP BY group_.grosysid , group_.groname ORDER BY group_.groname;";
        String query = prefixQuery + group + suffixQuery;

        String usersList = "";
        for (int i = 0; i < users.cells().size(); i++) {
            String user = users.cells().get(i).get(0);
            if (i == 0) {
                usersList = usersList + user;
            } else {
                usersList = usersList + "," + user;
            }
        }

        boolean finished = false;

        List<List<String>> rawData = Arrays.asList(Arrays.asList("group_name", "members"), Arrays.asList("g_" + group, usersList));
        List<String> expectedResult = new ArrayList<>();
        rawData.forEach(e -> expectedResult.addAll(e));

        for (int i = 0; (i <= wait); i += period) {

            this.selectData(query);

            try {
                compareList(expectedResult);
                finished = true;
                commonspec.getLogger().info("Users belonging to group in database found.");
                break;
            } catch (AssertionError | Exception e) {
                commonspec.getLogger().info("Users do not belong to group in database yet. Sleeping for " + period + " seconds.");
                Thread.sleep(period * 1000);
            }
        }

        if (!finished) {
            Assert.fail("Users do not belong to group in database after specified timeout.");
        }
    }

    public void compareList(List<String> tablePattern) throws Exception {

        List<String> sqlTable = new ArrayList<String>();

        // the result is taken from previous step
        for (int i = 0; ThreadProperty.get("queryresponse" + i) != null; i++) {
            String ip_value = ThreadProperty.get("queryresponse" + i);
            sqlTable.add(i, ip_value);
        }

        for (int i = 0; ThreadProperty.get("queryresponse" + i) != null; i++) {
            ThreadProperty.remove("queryresponse" + i);
        }

        assertThat(tablePattern).as("response is not equal to the expected").isEqualTo(sqlTable);
    }
}
