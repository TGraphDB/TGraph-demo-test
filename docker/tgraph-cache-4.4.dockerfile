# syntax=docker/dockerfile:1.4

FROM songjinghe/neo4j-env:4.4-latest
MAINTAINER Jinghe Song <songjh@buaa.edu.cn>

WORKDIR /db/bin

RUN wget -q "https://github.com/async-profiler/async-profiler/releases/download/v4.1/async-profiler-4.1-linux-x64.tar.gz" && \
    tar xzf "async-profiler-4.1-linux-x64.tar.gz"

RUN --mount=type=ssh \
    mkdir -p /root/.ssh && chmod 700 /root/.ssh && \
    ssh-keyscan gitee.com >> /root/.ssh/known_hosts && \
    chmod 644 /root/.ssh/known_hosts && \
    git clone --depth=1 git@gitee.com:tgraphdb/temporal-storage.git -b TGraph4.4 --single-branch && \
    git clone --depth=1 git@gitee.com:tgraphdb/temporal-neo4j-4.4.git -b TGraph-4.4 --single-branch && \
    git clone --depth=1 git@gitee.com:tgraphdb/demo-test.git -b jdk11 --single-branch

# RUN --mount=type=cache,target=/root/.m2/repository \

RUN cd /db/bin/temporal-storage && \
    mvn -B install -Dmaven.test.skip=true

RUN cd /db/bin/temporal-neo4j-4.4 && \
    mvn -B install -DskipTests -Dcheckstyle.skip -Dlicense.skip=true -Dlicensing.skip=true -Doverwrite && \
    mv /db/bin/temporal-neo4j-4.4 /db/bin/temporal-neo4j

RUN cd /db/bin/demo-test && \
    mvn -q dependency:go-offline && \
    mvn -B compile exec:java -Dexec.mainClass=edu.buaa.common.RuntimeEnv -Dexec.cleanupDaemonThreads=false
