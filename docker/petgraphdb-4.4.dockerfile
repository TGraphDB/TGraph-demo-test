# syntax=docker/dockerfile:1.4

FROM songjinghe/tgraph-demo-test:4.4-latest
MAINTAINER Jinghe Song <songjh@buaa.edu.cn>

RUN echo Asia/Shanghai > /etc/timezone

# cache TGraph source code & maven packages & built java classes and jars

WORKDIR /db/bin

RUN --mount=type=ssh \
    cd /db/bin/demo-test && \
    git config remote.origin.fetch "+refs/heads/bolt-server:refs/remotes/origin/bolt-server" && \
    git fetch origin && git checkout -b bolt-server origin/bolt-server && \
    cd /db/bin/temporal-neo4j   && git pull --ff-only && \
    cd /db/bin/temporal-storage && git pull --ff-only

WORKDIR /db/bin/temporal-storage
RUN mvn -B install -Dmaven.test.skip=true
  
WORKDIR /db/bin/temporal-neo4j
RUN mvn -B install -Dmaven.test.skip=true -Dlicense.skip=true -Dlicensing.skip=true -Dcheckstyle.skip -Doverwrite -pl org.neo4j:neo4j-kernel -am

WORKDIR /db/bin/demo-test
RUN mvn -B compile exec:java -Dexec.mainClass=edu.buaa.common.RuntimeEnv -Dexec.cleanupDaemonThreads=false

EXPOSE 17687
VOLUME /database
ENV DB_PATH=/database
ENV DB_PORT=17687
ENV PETG_PASSWD=beihang@2013
ENV CONFIG_MEMTABLE_SIZE=4
ENV CONFIG_FBUFFER_SIZE=6
ENV CONFIG_MAX_CACHE_SIZE=1024
ENV MAVEN_OPTS="-Xmx3g -Xms1g -XX:MaxDirectMemorySize=2g"
ENV CLASS_SERVER=edu.buaa.server.system.TCypherServer

EXPOSE 12339
ENV DEBUG_HTTP_HOST=0.0.0.0

RUN chmod 755 /db/bin/demo-test/docker-entrypoint.sh
ENTRYPOINT ["/db/bin/demo-test/docker-entrypoint.sh"]
