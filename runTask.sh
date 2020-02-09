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
# export MAVEN_OPTS='-Xmx18g -Xms10g -agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=5005'


# Function: print system info of current machine (both hardware and software), no argument needed.
function systemInfo() {
    if [ -z ${IS_COMPILED+x} ]
    then
        IS_COMPILED=' clean compile '
    else
        IS_COMPILED=''
    fi
    mvn -B ${IS_COMPILED} exec:java \
        -Dexec.mainClass="edu.buaa.vo.RuntimeEnv"
}


# Function: Start TGraph TCP Server which accept TCypher queries.
# Example: tcypherServerStart path-to-db-dir
# Explain: path-to-db-dir is a TGraph DB folder which contains traffic demo road network topology
function tcypherServerStart() {
    mvn -B clean compile exec:java \
        -Dexec.mainClass="run.TCypherSocketServer" \
        -Dexec.classpathScope="test" \
        -Dexec.args="$1"
}

# Function: Test TGraph TCypher Server write performance.
# Example: tcypherClientWriteTest /media/song/test/db-network-only-ro 192.168.1.141 8 10 200000 /media/song/test/data-set/beijing-traffic/TGraph/byday/100501
# Explain:
#  /media/song/test/db-network-only-ro is a TGraph DB folder which contains traffic demo road network topology
#  192.168.1.141  is the TCypher Server hostname
#  8 is the number of connections to the server(both server and client use one thread to process one connection
#  10 is the number of Cypher queries per transaction
#  200000 is the total number of data lines to send.(from the data file)
#  /media/song/test/data-set/beijing-traffic/TGraph/byday/100501 is the path of the data file
function tcypherClientWriteSpropTest() {
    if [ -z ${IS_COMPILED+x} ]
    then
        IS_COMPILED=' clean test-compile '
    else
        IS_COMPILED=''
    fi
    mvn -B ${IS_COMPILED} exec:java \
        -Dexec.mainClass="org.act.temporal.test.tcypher.WriteStaticPropertyTest" \
        -Dexec.classpathScope="test" \
        -Dexec.args="$1 $2 $3 $4 $5 $6"
}

# Function: Test TGraph TCypher Server write performance. almost same as above but set temporal property.
function tcypherClientWriteTpropTest() {
    if [ -z ${IS_COMPILED+x} ]
    then
        IS_COMPILED=' clean test-compile '
    else
        IS_COMPILED=''
    fi
    mvn -B ${IS_COMPILED} exec:java \
        -Dexec.mainClass="run.TCypherWriteTemporalPropertyTest" \
        -Dexec.classpathScope="test" \
        -Dexec.args="$1 $2 $3 $4 $5 $6"
}


function genBenchmark() {
  export WORK_DIR="E:\tgraph\test-data"
  export BENCHMARK_FILE_OUTPUT=benchmark
  export TEMPORAL_DATA_PER_TX=100
  export TEMPORAL_DATA_START=0503
  export TEMPORAL_DATA_END=0504
  export REACHABLE_AREA_TX_CNT=20
  mvn -B --offline compile exec:java -Dexec.mainClass="edu.buaa.benchmark.BenchmarkTxArgsGenerator"
}

function genResult() {
  export BENCHMARK_FILE_INPUT="E:\tgraph\test-data\benchmark.gz"
  export BENCHMARK_FILE_OUTPUT="E:\tgraph\test-data\benchmark-with-result.gz"
  export REACHABLE_AREA_REPLACE_TX=true
  mvn -B --offline compile exec:java -Dexec.mainClass="edu.buaa.benchmark.BenchmarkTxResultGenerator"
}

function runBenchmark() {
  export DB_TYPE=tgraph_kernel
  #export DB_TYPE=sql_server
  #export DB_HOST=39.96.57.88
  export DB_HOST=localhost
  export BENCHMARK_FILE_INPUT="E:\tgraph\test-data\benchmark-with-result.gz"
  export MAX_CONNECTION_CNT=1
  export VERIFY_RESULT=true
  mvn -B --offline compile exec:java -Dexec.mainClass="edu.buaa.benchmark.BenchmarkRunner"
}

function runTGraphKernelServer(){
  export DB_PATH="E:\tgraph\test-db"
  mvn -B --offline compile exec:java -Dexec.mainClass="edu.buaa.server.KernelTcpServer"
}

function runSQLServer(){
  export DB_PATH=/tmp/testdb
  mvn -B --offline compile exec:java -Dexec.mainClass="edu.buaa.server.KernelTcpServer"
}
