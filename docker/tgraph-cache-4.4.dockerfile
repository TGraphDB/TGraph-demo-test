FROM songjinghe/neo4j-env:4.4-latest
MAINTAINER Jinghe Song <songjh@buaa.edu.cn>

WORKDIR /db/bin

RUN git clone --depth=1 https://github.com/TGraphDB/temporal-storage.git -b TGraph2.3latest --single-branch && \
    cd /db/bin/temporal-storage && \
    mvn -B install -Dmaven.test.skip=true

RUN git clone --depth=1 https://gitee.com/tgraphdb/temporal-neo4j-4.4.git -b TGraph-4.4 --single-branch && \
    cd /db/bin/temporal-neo4j-4.4 && \
    mvn -B install -DskipTests -Dcheckstyle.skip -Dlicense.skip=true -Dlicensing.skip=true -Doverwrite && \
    mv /db/bin/temporal-neo4j-4.4 /db/bin/temporal-neo4j

RUN git clone --depth=1 https://gitee.com/tgraphdb/demo-test.git -b dev-zzy --single-branch && \
    cd /db/bin/demo-test && \
    mvn -B compile exec:java -Dexec.mainClass=edu.buaa.common.RuntimeEnv -Dexec.cleanupDaemonThreads=false
