#!/bin/bash

# This script pack the main functions of the project
# How to use this script:
# > source runTask.sh
# > truck_download 2017 6
# > ...

unset PREPARE
unset PREPARE_NEW
unset PREPARE_TEST
unset PREPARE_OLD
unset PREPARE_TSC

export MAVEN_OPTS='-Xmx50g -Xms4g'
# export MAVEN_OPTS='-Xmx18g -Xms12g'
# Debug options
# export MAVEN_OPTS='-Xmx18g -Xms10g -agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=5005'

# Function: print system info of current machine (both hardware and software)
# Example: unicity_new path-to-data.txt 200 3600 2 10 20
# Explain:
# 1. data set file: path-to-data.txt
# 2. radius=200, timeInterval=3600, select 2 points, repeat 10 times, using 20 CPU core.
function systemInfo() {
    if [ -z ${PREPARE_NEW+x} ]
    then
        PREPARE_NEW=' clean compile '
    else
        PREPARE_NEW=''
    fi
    mvn -B ${PREPARE_NEW} exec:java \
        -Dexec.mainClass="org.act.tgraph.demo.vo.PhysicalEnv"
}


# Function: Start TGraph TCP Server which accept TCypher queries.
# Example: tcypherServerStart path-to-db-dir 2016 11
# Explain: download 201611 data to path-to-file.txt
# Note:
# 1. 201501-201507,201603-201610 data not exist
# 2. download speed is 100MB/10minutes
function tcypherServerStart() {
    mvn -B clean compile exec:java \
        -Dexec.mainClass="org.act.tgraph.demo.utils.TCypherServer" \
        -Dexec.args="$1 $2 $3 $4 $5 $6 $7"
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


for i in 1 2 3 4
do
	for j in 9 10 5
	do
		printf '';#echo $i $j
	done
done

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
