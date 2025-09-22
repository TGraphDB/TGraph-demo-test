FROM songjinghe/tgraph-maven-cache:2.3-latest
MAINTAINER Jinghe Song <songjh@buaa.edu.cn>

RUN echo Asia/Shanghai > /etc/timezone

# cache TGraph source code & maven packages & built java classes and jars

WORKDIR /db/bin

RUN git clone --depth=1 https://gitee.com/tgraphdb/temporal-storage.git -b TGraph2.3latest --single-branch && \
    git clone --depth=1 https://gitee.com/tgraphdb/temporal-neo4j.git -b TGraph2.3latest --single-branch   && \
    git clone --depth=1 https://gitee.com/tgraphdb/demo-test.git -b dev-sjh --single-branch

WORKDIR /db/bin/temporal-storage
RUN mvn -B clean install -Dmaven.test.skip=true

WORKDIR /db/bin/temporal-neo4j
RUN mvn -B install -Dmaven.test.skip=true -Dlicense.skip=true -Dlicensing.skip=true -pl org.neo4j:neo4j-cypher -am

WORKDIR /db/bin/demo-test
RUN chmod 755 docker-entrypoint.sh && mvn -B compile exec:java -Dexec.mainClass=edu.buaa.common.RuntimeEnv -Dexec.cleanupDaemonThreads=false

ENTRYPOINT ["/db/bin/demo-test/docker-entrypoint.sh"]
