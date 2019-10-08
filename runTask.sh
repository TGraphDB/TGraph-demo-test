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
        -Dexec.mainClass="org.act.tgraph.demo.vo.RuntimeEnv"
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



# Function: output utility of data set evaluated by Trajectory Simple Concatenation (TSC) Model
# Example: tsc  path-to-data  path-to-nwa.exe  path-to-map.pbf  39.6797 40.2523 116.0074 116.7380  path-to-params  20
# Explain:
#   path-to-data is the raw data file
#   path-to-nwa.exe is the executable file compiled from the "Never Walk Along" project
#   path-to-map.pbf is the OSM map file used for map-matching
#   follow are geography bound filter, south north west east
#   follow are param file.
#   finally is the max number of threads to run this program.
# Notes:
#   pass no param to generate a example param.csv file in current working dir.
function tsc() {
    if [ -z "$1" ]
    then
        mvn -B clean compile exec:java \
            -Dexec.mainClass="task.EvalUtilityByTSC" \
            -Dexec.args=""
    else
        if [ -z ${PREPARE_TSC+x} ]
        then
            PREPARE_TSC=' clean compile '
        else
            PREPARE_TSC=''
        fi
        printf "$1 $2 $3 $4 $5 $6 $7 $8 $9\n"
        mvn -B ${PREPARE_TSC} exec:java \
            -Dexec.mainClass="task.EvalUtilityByTSC" \
            -Dexec.args="$1 $2 $3 $4 $5 $6 $7 $8 $9"
    fi
}


# Function: output all (start,end) time pair of all car's trajectories
# Example: extract_time  path-to-data  39.6797 40.2523 116.0074 116.7380
# Explain:
#   path-to-data is the raw data file
#   follow are geography bound filter, south north west east
function extract_time() {
    if [ -z ${PREPARE_TIME+x} ]
    then
        PREPARE_TIME=' clean compile '
    else
        PREPARE_TIME=''
    fi
    printf "$1 $2 $3 $4 $5\n"
    mvn -B ${PREPARE_TIME} exec:java \
        -Dexec.mainClass="task.MaxOverlapTime" \
        -Dexec.args="$1 $2 $3 $4 $5"
}

function test_code(){
    if [ -z ${PREPARE_TEST+x} ]
    then
        PREPARE_TEST=' clean compile '
    else
        PREPARE_TEST=''
    fi
    mvn -B clean test -Dtest=$1 -Djvm.args="-Xmx50g -Xms5g"
    #echo ${PREPARE_TEST}
}
