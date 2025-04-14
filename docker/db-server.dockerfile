# for non-jdbc servers only. i.e. Neo4j, TGraphDB

FROM registry.cn-beijing.aliyuncs.com/songjinghe/tgraph-cache:latest
MAINTAINER Jinghe Song <songjh@buaa.edu.cn>
# ENV MAVEN_OPTS "-Xmx512m"

# db milestone folder
VOLUME /database

ENV CLASS_SERVER edu.buaa.server.TGraphKernelSnappyServer
ENV DB_PATH /database
ENV DB_PORT 8441

EXPOSE $DB_PORT
WORKDIR /db/demo-test
ENTRYPOINT /bin/bash
CMD mvn -B --offline exec:java -Dexec.mainClass=$CLASS_SERVER -Dexec.cleanupDaemonThreads=false
