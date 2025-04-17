# for non-jdbc servers only. i.e. Neo4j, TGraphDB

FROM songjinghe/tgraph-demo-test:2.3
MAINTAINER Jinghe Song <songjh@buaa.edu.cn>

# ENV MAVEN_OPTS "-Xmx512m"

WORKDIR /db/bin/demo-test
RUN git pull

# db milestone folder
VOLUME /database

ENV CLASS_SERVER edu.buaa.server.TGraphKernelSnappyServer
ENV DB_PATH /database
ENV DB_PORT 8441

EXPOSE $DB_PORT

# ENTRYPOINT /bin/bash
CMD mvn -B --offline exec:java -Dexec.mainClass=$CLASS_SERVER -Dexec.cleanupDaemonThreads=false
