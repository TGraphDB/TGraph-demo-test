# syntax=docker/dockerfile:1.4

FROM songjinghe/tgraph-demo-test:4.4-latest
MAINTAINER Jinghe Song <songjh@buaa.edu.cn>

RUN echo Asia/Shanghai > /etc/timezone

# cache TGraph source code & maven packages & built java classes and jars

WORKDIR /db/bin

RUN --mount=type=ssh \
    rm -rf temporal-storage && rm -rf demo-test && \
    git clone --depth=1 https://gitee.com/tgraphdb/temporal-storage.git -b oss --single-branch && \
    git clone --depth=1 https://gitee.com/tgraphdb/demo-test.git -b oss --single-branch && \
    cd /db/bin/temporal-neo4j && git pull --ff-only

WORKDIR /db/bin/temporal-storage
RUN mvn -B install -Dmaven.test.skip=true

WORKDIR /db/bin/temporal-neo4j
RUN mvn -B install -Dmaven.test.skip=true -Dlicense.skip=true -Dlicensing.skip=true -Dcheckstyle.skip -Doverwrite -pl org.neo4j:neo4j-kernel -am

WORKDIR /db/bin/demo-test
RUN mvn -B compile exec:java -Dexec.mainClass=edu.buaa.common.RuntimeEnv -Dexec.cleanupDaemonThreads=false

RUN chmod 755 /db/bin/demo-test/docker-entrypoint.sh

ENTRYPOINT ["/db/bin/demo-test/docker-entrypoint.sh"]
