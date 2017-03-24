Feature: Elasticsearch steps test

  Scenario: Connect to Elasticsearch
    Given I connect to Elasticsearch cluster at host '${ES_NODE}'
    Given I connect to 'Elasticsearch' cluster at '${ES_CLUSTER}'

  Scenario: Obtain clustername in Elasticsearch
    Given I obtain elasticsearch cluster name in '${ES_NODE}' and save it in variable 'clusternameES'

  Scenario: Connect to Elasticsearch with clustername obtained
    Given I connect to Elasticsearch cluster at host '${ES_NODE}' using native port '9200' using cluster name '!{clusternameES}'

  Scenario: Create new index in Elasticsearch
    Given I create an elasticsearch index named 'indexES' removing existing index if exist
    Then An elasticsearch index named 'indexES' exists

#  Scenario: Execute a query in Elasticsearch
#    Given I execute an elasticsearch query over index 'indexES' and mapping '.*' and column '.*' with value '.*' to '.*'
#    Given The Elasticsearch index named '.+' and mapping '.+' contains a column named '.+' with the value '.+'

  Scenario: Connect to Elasticsearch with params
    Given I drop an elasticsearch index named 'indexES'
    Given An elasticsearch index named 'indexES' does not exist

  Scenario: Connect to Elasticsearch with params
    Given I drop every existing elasticsearch index

