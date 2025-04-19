FROM songjinghe/neo4j-env:4.4-latest
MAINTAINER Jinghe Song <songjh@buaa.edu.cn>

WORKDIR /db

RUN git clone --depth=1 https://github.com/TGraphDB/temporal-storage.git -b TGraph2.3latest --single-branch && \
    cd /db/temporal-storage && \
    mvn -B install -Dmaven.test.skip=true && \
    rm -rf /db/temporal-storage

RUN git clone --depth=1 https://gitee.com/tgraphdb/temporal-neo4j-4.4.git -b TGraph-4.4 --single-branch && \
    cd /db/temporal-neo4j-4.4/community && \
    mvn -B install -Dmaven.test.skip=true -am -Dcheckstyle.skip -Dlicense.skip=true -Dlicensing.skip=true -Doverwrite  -pl org.neo4j:neo4j-kernel && \
    rm -rf /db/temporal-neo4j-4.4

RUN git clone --depth=1 https://gitee.com/tgraphdb/demo-test.git -b dev-zzy --single-branch && \
    cd /db/demo-test && \
    mvn -B compile exec:java -Dexec.mainClass=edu.buaa.common.RuntimeEnv -Dexec.cleanupDaemonThreads=false && \
    rm -rf /db/demo-test

CMD /bin/bash
