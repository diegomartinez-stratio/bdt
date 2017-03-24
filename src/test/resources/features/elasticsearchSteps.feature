Feature: Elasticsearch steps test

  Scenario: Connect to Elasticsearch
    Given I connect to 'Cassandra|Mongo|Elasticsearch' cluster at '<string>'
    Given I connect to Elasticsearch cluster at host '.+' using native port '(.+') using cluster name '(.+')
    Given I drop an elasticsearch index named '.+'
    Given I drop every existing elasticsearch index
    Given I obtain elasticsearch cluster name in '[^:]+:.+' and save it in variable '.+'
    Given An elasticsearch index named '.+' does not exist
    Given An elasticsearch index named '.+' exists
    Given I create an elasticsearch index named '.+' removing existing index if exist
    Given I execute an elasticsearch query over index '.*' and mapping '.*' and column '.*' with value '.*' to '.*'
    Given The Elasticsearch index named '.+' and mapping '.+' contains a column named '.+' with the value '.+'