#!/bin/bash

# This script pack the main functions of the project
# How to use this script:
# > source runTask.sh
# > truck_download 2017 6
# > ...

unset PREPARE
unset IS_COMPILED
unset PREPARE_TEST
unset PREPARE_OLD
unset PREPARE_TSC

export MAVEN_OPTS='-Xmx50g -Xms4g'
# export MAVEN_OPTS='-Xmx18g -Xms12g'
# # Debug options
#export MAVEN_OPTS='-Xmx18g -Xms10g -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005'

#======================================== environment variable ========================================

datasetName="bj60c"
datasetSize="60day"

#the local storage of the TGraphDB
export DB_PATH="C:\tgraph\test-db\Tgraph-$datasetName-$datasetSize"
#the directory where the CSV files are stored
export RAW_DATA_PATH="E:\test-data-60c"
#the start date
export DATA_START=0501
#the end date
export DATA_END=0630
#host
export DB_HOST=localhost
#the start time of the query operation
export TEMPORAL_DATA_START=201005011000
#the end time of the query operation
export TEMPORAL_DATA_END=201005011800
#the start time of creating index
export INDEX_TEMPORAL_DATA_START=201005010900
#the end time of creating index
export INDEX_TEMPORAL_DATA_END=201005011900
#the result file is saved in the file directory
fileName="Tgraph-${datasetName}-${datasetSize}-result"
RESULT_DATA_PATH="E:\tgraph\test-result"\\"$fileName"
if [ ! -d "$RESULT_DATA_PATH" ]; then
    mkdir $RESULT_DATA_PATH
fi
export RESULT_DATA_PATH

##index id of aggregation max
#export INDEX_ID_OF_MAX=1
##index id of aggregation duration
#export INDEX_ID_OF_DURATION=1
##index id of entity temporal condition test
#export INDEX_ID_OF_CONDITION=1

export VERIFY_RESULT=false

#====================================================================================================


# Function: print system info of current machine (both hardware and software), no argument needed.
function systemInfo() {
    if [ -z ${IS_COMPILED+x} ]
    then
        IS_COMPILED=' clean compile '
    else
        IS_COMPILED=''
    fi
    mvn -B ${IS_COMPILED} exec:java \
        -Dexec.mainClass="edu.buaa.client.RuntimeEnv"
}

## Function: Start TGraph TCP Server which accept TCypher queries.
## Example: tcypherServerStart path-to-db-dir
## Explain: path-to-db-dir is a TGraph DB folder which contains traffic demo road network topology
#function runTCypherServer() {
#    mvn -B clean compile exec:java \
#        -Dexec.mainClass="simple.TCypherSocketServer" \3
#        -Dexec.classpathScope="test" \
#        -Dexec.args="$1"
#}

#======================================== server install and compile ========================================

function codeInstallAndCompile() {
  #the path of TGraphDB
  NEO4J_PATH="E:\TGraphDB\temporal-neo4j"
  STORAGE_PATH="E:\TGraphDB\temporal-storage"
  SHELL_FILE_PATH="E:\TGraphDB\TGraph-demo-test"
  #install temporal-storage
  echo -e "\033[47;30m [Tgraph Install Info]---------------[ Installation of Temporal-storage is about to start ]--------------- \033[0m"
  cd $STORAGE_PATH
  mvn clean compile
  mvn install -Dmaven.test.skip
  echo -e "\033[47;30m [Tgraph Install Info]---------------[ Installation of Temporal-storage is complete ]--------------- \033[0m"
  #install temporal-neo4j-kernel
  echo -e "\033[47;30m [Tgraph Install Info]---------------[ Installation of Temporal-neo4j-kernel is about to start ]--------------- \033[0m"
  cd $NEO4J_PATH
  mvn install -pl org.neo4j:neo4j-kernel -am -Dmaven.test.skip -Dlicensing.skip -Dlicense.skip
  echo -e "\033[47;30m [Tgraph Install Info]---------------[ Installation of Temporal-neo4j-kernel is complete ]--------------- \033[0m"
  cd $SHELL_FILE_PATH
  echo -e "\033[47;30m [Tgraph Install Info]---------------[ Installation of TGraphDB is complete ]--------------- \033[0m"
}

#======================================== server ON an OFF ========================================

#server without index
function runTGraphKernelServer(){
  mvn -B --offline compile exec:java -Dexec.mainClass="edu.buaa.server.TGraphKernelTcpServer"
}
#server with index
function runTGraphIndexedKernelServer(){
  mvn -B --offline compile exec:java -Dexec.mainClass="edu.buaa.server.TGraphIndexedKernelTcpServer"
}
#close server
function closeServer(){
  echo EXIT | nc localhost 8438
}

#====================================================================================================
#function runSQLServer(){
#  export DB_PATH=/tmp/testdb
#  mvn -B --offline compile exec:java -Dexec.mainClass="edu.buaa.server.TGraphKernelTcpServer"
#}
#
#function runNeo4jKernelServer(){
#  export DB_PATH="E:\tgraph\test-db"
#  export DB_TYPE=array
#  export DB_TYPE=treemap
#  mvn -B --offline compile exec:java -Dexec.mainClass="edu.buaa.server.Neo4jKernelTcpServer"
#}


#======================================== TESTS without INDEX ========================================

function runWriteTest() {
  export TEMPORAL_DATA_PER_TX=100
  export MAX_CONNECTION_CNT=1
  mvn -B --offline test -Dtest=simple.tgraph.kernel.WriteTemporalPropertyTest
}

function runSnapshotTest() {
  export TEST_PROPERTY_NAME=travel_time
  export MAX_CONNECTION_CNT=16
  export SERVER_RESULT_FILE="Tgraph_Result_SnapshotTest.gz"
  mvn -B --offline test -Dtest=simple.tgraph.kernel.SnapshotTest
}

function runSnapshotAggregationMaxTest() {
  export TEST_PROPERTY_NAME=travel_time
  export SERVER_RESULT_FILE="Tgraph_Result_SnapshotAggregationMaxTest.gz"
  export MAX_CONNECTION_CNT=16
  mvn -B --offline test -Dtest=simple.tgraph.kernel.SnapshotAggregationMaxTest
}

function runSnapshotAggregationDurationTest() {
  export TEST_PROPERTY_NAME=full_status
  export SERVER_RESULT_FILE="Tgraph_Result_SnapshotAggregationDurationTest.gz"
  export MAX_CONNECTION_CNT=16
  mvn -B --offline test -Dtest=simple.tgraph.kernel.SnapshotAggregationDurationTest
}

function runEntityTemporalConditionTest() {
  export TEST_PROPERTY_NAME=travel_time
  export TEMPORAL_CONDITION=600
  export SERVER_RESULT_FILE="Tgraph_Result_EntityTemporalConditionTest.gz"
  export MAX_CONNECTION_CNT=16
  mvn -B --offline test -Dtest=simple.tgraph.kernel.EntityTemporalConditionTest
}




#function runReachableAreaQueryTest() {
#  export TEST_START_CROSS_ID=75124
#  export TEMPORAL_DATA_START=201006300830
#  export TRAVEL_TIME=50000
#  export DB_HOST=localhost
#  export RAW_DATA_PATH="E:\tgraph\test-result"
#  export SERVER_RESULT_FILE="Result_EntityTemporalConditionTest.gz"
#  export MAX_CONNECTION_CNT=16
#  export VERIFY_RESULT=false
#  mvn -B --offline test -Dtest=simple.tgraph.kernel.ReachableAreaQueryTest
#}

#========================================== TESTS with INDEX =========================================

function runCreateAggrMaxIndex() {
  export INDEX_PROPERTY_NAME=travel_time
  export SERVER_RESULT_FILE="ID_CreateAggrMaxIndexTest.gz"
  export MAX_CONNECTION_CNT=1
  mvn -B --offline test -Dtest=simple.tgraph.kernel.index.CreateTGraphAggrMaxIndexTest
}

function runSnapshotAggregationMaxIndexTest() {
  export SERVER_RESULT_FILE="Tgraph_Result_SnapshotAggregationMaxIndexTest.gz"
  export MAX_CONNECTION_CNT=1
  mvn -B --offline test -Dtest=simple.tgraph.kernel.index.SnapshotAggregationMaxIndexTest
}

function runCreateAggrDurationIndex {
  export SERVER_RESULT_FILE="ID_CreateAggrDurationIndexTest.gz"
  export MAX_CONNECTION_CNT=1
  mvn -B --offline test -Dtest=simple.tgraph.kernel.index.CreateTGraphAggrDurationIndexTest
}

function runSnapshotAggregationDurationIndexTest() {
  export TEST_PROPERTY_NAME=full_status
  export SERVER_RESULT_FILE="Tgraph_Result_SnapshotAggregationDurationIndexTest.gz"
  export MAX_CONNECTION_CNT=16
  mvn -B --offline test -Dtest=simple.tgraph.kernel.index.SnapshotAggregationDurationIndexTest
}

function runCreateTemporalConditionIndex {
  export SERVER_RESULT_FILE="ID_CreateTGraphEntityTemporalConditionIndexTest.gz"
  export MAX_CONNECTION_CNT=1
  mvn -B --offline test -Dtest=simple.tgraph.kernel.index.CreateTGraphEntityTemporalConditionIndexTest
}
function runEntityTemporalConditionIndexTest() {
  export TEMPORAL_CONDITION=600
  export SERVER_RESULT_FILE="Tgraph_Result_EntityTemporalConditionIndexTest.gz"
  export MAX_CONNECTION_CNT=16
  mvn -B --offline test -Dtest=simple.tgraph.kernel.index.EntityTemporalConditionIndexTest
}


#========================================== auto test =========================================

#tests without index
function autoTest() {

  echo -e "\033[47;30m [Tgraph Test Info]---------------[ Tests are about to start ]--------------- \033[0m"
  sleep 5
  echo -e "\033[47;30m [Tgraph Test Info]---------------[ SnapshotTest is about to start ]---------------$(date "+%Y-%m-%d %H:%M:%S") \033[0m"
  sleep 5
  runSnapshotTest
  echo -e "\033[47;30m [Tgraph Test Info]---------------[ End of SnapshotTest ]--------------- \033[0m"
  sleep 100
  echo -e "\033[47;30m [Tgraph Test Info]---------------[ SnapshotAggregationMaxTest is about to start ]---------------$(date "+%Y-%m-%d %H:%M:%S") \033[0m"
  sleep 5
  runSnapshotAggregationMaxTest
  echo -e "\033[47;30m [Tgraph Test Info]---------------[ End of SnapshotAggregationMaxTest ]--------------- \033[0m"
  sleep 100
  echo -e "\033[47;30m [Tgraph Test Info]---------------[ SnapshotAggregationDurationTest is about to start ]---------------$(date "+%Y-%m-%d %H:%M:%S") \033[0m"
  sleep 5
  runSnapshotAggregationDurationTest
  echo -e "\033[47;30m [Tgraph Test Info]---------------[ End of SnapshotAggregationDurationTest ]--------------- \033[0m"
  sleep 100
  echo -e "\033[47;30m [Tgraph Test Info]---------------[ EntityTemporalConditionTest is about to start ]---------------$(date "+%Y-%m-%d %H:%M:%S") \033[0m"
  sleep 5
  runEntityTemporalConditionTest
  echo -e "\033[47;30m [Tgraph Test Info]---------------[ End of EntityTemporalConditionTest ]--------------- \033[0m"
  sleep 100
  echo -e "\033[47;30m [Tgraph Test Info]---------------[ End of all tests, Server is closing ]--------------- \033[0m"
  sleep 5
  closeServer
}

#tests with index.
#Tips: 1st, start the server automation script. 2nd, start the client automation script

function serverAutoTestWithIndex() {
  runTGraphIndexedKernelServer
  runTGraphIndexedKernelServer
  runTGraphIndexedKernelServer
  runTGraphIndexedKernelServer
 }

function clientAutoTestWithIndex() {

  TXT_FILE_PATH="E:\tgraph\test-result"
  SHELL_FILE_PATH="E:\TGraphDB\TGraph-demo-test"

  #get max index id
  echo -e "\033[47;30m [Tgraph Test Info]---------------[ All tests with index are about to start ]--------------- \033[0m"
  echo -e "\033[47;30m [Tgraph Test Info]---------------[ Aggregation Max Index is creating ]--------------- \033[0m"
  runCreateAggrMaxIndex
  echo -e "\033[47;30m [Tgraph Test Info]---------------[ Aggregation Max Index is created ]--------------- \033[0m"
  echo -e "\033[47;30m [Tgraph Test Info]---------------[ Waiting! server is restarting ]--------------- \033[0m"
  closeServer
  sleep 120
  #SnapshotAggregationMaxIndexTest
  echo -e "\033[47;30m [Tgraph Test Info]---------------[ Server restart successfully ]--------------- \033[0m"
  echo -e "\033[47;30m [Tgraph Test Info]---------------[ Getting aggregation max Index ID ]--------------- \033[0m"
  cd $TXT_FILE_PATH
  firstline=`head -1 INDEX_ID_OF_MAX.txt`
  export INDEX_ID_OF_MAX=$firstline
  cd $SHELL_FILE_PATH
  echo -e "\033[47;30m [Tgraph Test Info]---------------[ Aggregation max Index ID was obtained successfully ]--------------- \033[0m"
  echo -e "\033[47;30m [Tgraph Test Info]---------------[ SnapshotAggregationMaxIndexTest is about to start ]---------------$(date "+%Y-%m-%d %H:%M:%S") \033[0m"
  runSnapshotAggregationMaxIndexTest
  echo -e "\033[47;30m [Tgraph Test Info]---------------[ End of SnapshotAggregationMaxIndexTest ]--------------- \033[0m"
  sleep 60

  #get duration index id
  echo -e "\033[47;30m [Tgraph Test Info]---------------[ Aggregation Duration Index is creating ]--------------- \033[0m"
  runCreateAggrDurationIndex
  echo -e "\033[47;30m [Tgraph Test Info]---------------[ Aggregation Duration Index is created ]--------------- \033[0m"
  echo -e "\033[47;30m [Tgraph Test Info]---------------[ Waiting! server is restarting ]--------------- \033[0m"
  closeServer
  sleep 120
  #SnapshotAggregationDurationIndexTest
  echo -e "\033[47;30m [Tgraph Test Info]---------------[ Server restart successfully ]--------------- \033[0m"
  echo -e "\033[47;30m [Tgraph Test Info]---------------[ Getting aggregation duration Index ID ]--------------- \033[0m"
  cd $TXT_FILE_PATH
  firstline=`head -1 INDEX_ID_OF_DURATION.txt`
  export INDEX_ID_OF_DURATION=$firstline
  cd $SHELL_FILE_PATH
  echo -e "\033[47;30m [Tgraph Test Info]---------------[ Aggregation duration Index ID was obtained successfully ]--------------- \033[0m"
  echo -e "\033[47;30m [Tgraph Test Info]---------------[ SnapshotAggregationDurationIndexTest is about to start ]---------------$(date "+%Y-%m-%d %H:%M:%S") \033[0m"
  runSnapshotAggregationDurationIndexTest
  echo -e "\033[47;30m [Tgraph Test Info]---------------[ End of SnapshotAggregationDurationIndexTest ]--------------- \033[0m"
  sleep 60

  #get Temporal Condition Index
  echo -e "\033[47;30m [Tgraph Test Info]---------------[ Temporal Condition Index is creating ]--------------- \033[0m"
  runCreateTemporalConditionIndex
  echo -e "\033[47;30m [Tgraph Test Info]---------------[ Temporal Condition Index is created ]--------------- \033[0m"
  echo -e "\033[47;30m [Tgraph Test Info]---------------[ Waiting! server is restarting ]--------------- \033[0m"
  closeServer
  sleep 120
  #EntityTemporalConditionIndexTest
  echo -e "\033[47;30m [Tgraph Test Info]---------------[ Server restart successfully ]--------------- \033[0m"
  echo -e "\033[47;30m [Tgraph Test Info]---------------[ EntityTemporalConditionIndexTest is about to start ]---------------$(date "+%Y-%m-%d %H:%M:%S") \033[0m"
  runEntityTemporalConditionIndexTest
  echo -e "\033[47;30m [Tgraph Test Info]---------------[ End of SnapshotAggregationDurationIndexTest ]--------------- \033[0m"
  sleep 60
  echo -e "\033[47;30m [Tgraph Test Info]---------------[ End of all tests, Server is closing ]--------------- \033[0m"
  closeServer
}




#function runReachableAreaQueryTest() {
#  export TEST_START_CROSS_ID=75124
#  export TEMPORAL_DATA_START=201006300830
#  export TRAVEL_TIME=50000
#  export DB_HOST=localhost
#  export RAW_DATA_PATH="E:\tgraph\test-result"
#  export SERVER_RESULT_FILE="Result_EntityTemporalConditionTest.gz"
#  export MAX_CONNECTION_CNT=16
#  export VERIFY_RESULT=false
#  mvn -B --offline test -Dtest=simple.tgraph.kernel.ReachableAreaQueryTest
#}

## Function: Test TGraph TCypher Server write performance.
## Example: tcypherClientWriteTest /media/song/test/db-network-only-ro 192.168.1.141 8 10 200000 /media/song/test/data-set/beijing-traffic/TGraph/byday/100501
## Explain:
##  /media/song/test/db-network-only-ro is a TGraph DB folder which contains traffic demo road network topology
##  192.168.1.141  is the TCypher Server hostname
##  8 is the number of connections to the server(both server and client use one thread to process one connection
##  10 is the number of Cypher queries per transaction
##  200000 is the total number of data lines to send.(from the data file)
##  /media/song/test/data-set/beijing-traffic/TGraph/byday/100501 is the path of the data file
#function tcypherClientWriteSpropTest() {
#    if [ -z ${IS_COMPILED+x} ]
#    then
#        IS_COMPILED=' clean test-compile '
#    else
#        IS_COMPILED=''
#    fi
#    mvn -B ${IS_COMPILED} exec:java \
#        -Dexec.mainClass="org.act.temporal.test.tcypher.WriteStaticPropertyTest" \
#        -Dexec.classpathScope="test" \
#        -Dexec.args="$1 $2 $3 $4 $5 $6"
#}
#
#
#
#function genBenchmark() {
#  export WORK_DIR="E:"
#  export BENCHMARK_FILE_OUTPUT=benchmark
#  export TEMPORAL_DATA_PER_TX=100
#  export TEMPORAL_DATA_START=0503
#  export TEMPORAL_DATA_END=0504
#  export REACHABLE_AREA_TX_CNT=20
#  mvn -B --offline compile exec:java -Dexec.mainClass="edu.buaa.benchmark.BenchmarkTxGenerator"
#}
#
#function genResult() {
#  export BENCHMARK_FILE_INPUT="E:\tgraph\test-data\benchmark.gz"
#  export BENCHMARK_FILE_OUTPUT="E:\tgraph\test-data\benchmark-with-result.gz"
#  export REACHABLE_AREA_REPLACE_TX=true
#  mvn -B --offline compile exec:java -Dexec.mainClass="edu.buaa.benchmark.BenchmarkTxResultGenerator"
#}
#
#function runBenchmark() {
#  export DB_TYPE=tgraph_kernel
#  #export DB_TYPE=sql_server
#  #export DB_HOST=39.96.57.88
#  export DB_HOST=localhost
#  export BENCHMARK_FILE_INPUT="E:\tgraph\test-data\benchmark-with-result.gz"
#  export MAX_CONNECTION_CNT=1
#  export VERIFY_RESULT=true
#  mvn -B --offline compile exec:java -Dexec.mainClass="edu.buaa.benchmark.BenchmarkRunner"
#}