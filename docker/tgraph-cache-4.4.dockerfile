FROM songjinghe/neo4j-env:4.4-latest
MAINTAINER Jinghe Song <songjh@buaa.edu.cn>

WORKDIR /db/bin

RUN wget -q "https://github.com/async-profiler/async-profiler/releases/download/v4.1/async-profiler-4.1-linux-x64.tar.gz" && \
    tar xzf "async-profiler-4.1-linux-x64.tar.gz"

RUN git clone --depth=1 https://gitee.com/TGraphDB/temporal-storage.git -b TGraph4.4 --single-branch && \
    cd /db/bin/temporal-storage && \
    mvn -B install -Dmaven.test.skip=true

RUN git clone --depth=1 https://gitee.com/tgraphdb/temporal-neo4j-4.4.git -b TGraph-4.4 --single-branch && \
    cd /db/bin/temporal-neo4j-4.4 && \
    mvn -B install -DskipTests -Dcheckstyle.skip -Dlicense.skip=true -Dlicensing.skip=true -Doverwrite && \
    mv /db/bin/temporal-neo4j-4.4 /db/bin/temporal-neo4j

RUN git clone --depth=1 https://gitee.com/tgraphdb/demo-test.git -b jdk11 --single-branch && \
    cd /db/bin/demo-test && \
    mvn -B compile exec:java -Dexec.mainClass=edu.buaa.common.RuntimeEnv -Dexec.cleanupDaemonThreads=false
