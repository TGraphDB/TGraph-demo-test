FROM songjinghe/tgraph-maven-cache:4.4-latest
MAINTAINER Jinghe Song <songjh@buaa.edu.cn>

# cache TGraph source code & maven packages & built java classes and jars

WORKDIR /db/bin

RUN git clone --depth=1 https://gitee.com/tgraphdb/temporal-storage.git -b TGraph2.3latest --single-branch && \
    git clone --depth=1 https://gitee.com/tgraphdb/temporal-neo4j-4.4.git -b TGraph-4.4 --single-branch   && \
    git clone --depth=1 https://gitee.com/tgraphdb/demo-test.git -b dev-zzy --single-branch

WORKDIR /db/bin/temporal-storage
RUN mvn -B install -Dmaven.test.skip=true

WORKDIR /db/bin/temporal-neo4j-4.4
RUN mvn -B install -Dmaven.test.skip=true -Dlicense.skip=true -Dlicensing.skip=true -Dcheckstyle.skip -Doverwrite -pl org.neo4j:neo4j-kernel -am

WORKDIR /db/bin/demo-test
RUN mvn -B compile exec:java -Dexec.mainClass=edu.buaa.common.RuntimeEnv -Dexec.cleanupDaemonThreads=false

CMD /bin/bash
