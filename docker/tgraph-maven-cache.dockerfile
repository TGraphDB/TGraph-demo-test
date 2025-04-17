FROM songjinghe/ubuntu-oraclejdk8-maven:latest
MAINTAINER Jinghe Song <songjh@buaa.edu.cn>

# cache TGraph source code & maven packages & built java classes and jars

RUN wget -q "http://tgraphdb.water-crystal.org/repo.tar.gz" && \
    mkdir /root/.m2 && \
    tar xzf "repo.tar.gz" -C /root/.m2/

WORKDIR /db

RUN git clone --depth=1 https://github.com/TGraphDB/temporal-storage.git -b TGraph2.3latest --single-branch && \
    cd /db/temporal-storage && \
    mvn -B install -Dmaven.test.skip=true && \
    rm -rf /db/temporal-storage

RUN git clone --depth=1 https://github.com/TGraphDB/temporal-neo4j.git -b TGraph2.3latest --single-branch && \
    cd /db/temporal-neo4j/community && \
    mvn -B install -Dlicense.skip=true -Dlicensing.skip=true -Dmaven.test.skip=true -pl org.neo4j:neo4j-cypher -am && \
    rm -rf /db/temporal-neo4j

# mvn -B install -Dlicense.skip=true -Dlicensing.skip=true -Dmaven.test.skip=true -pl org.neo4j:neo4j-cypher -am && \
# mvn -B install -Dlicense.skip=true -Dlicensing.skip=true -DskipTests=true       -pl org.neo4j:neo4j-unsafe -am && \
# mvn -B install -Dlicense.skip=true -Dlicensing.skip=true -DskipTests=true       -pl org.neo4j:neo4j-io -am && \
# mvn -B install -Dlicense.skip=true -Dlicensing.skip=true -DskipTests=true       -pl org.neo4j:neo4j-logging -am && \

RUN git clone --depth=1 https://gitee.com/tgraphdb/demo-test.git -b dev-sjh --single-branch && \
    cd /db/demo-test && \
    mvn -B compile exec:java -Dexec.mainClass=edu.buaa.common.RuntimeEnv -Dexec.cleanupDaemonThreads=false && \
    rm -rf /db/demo-test

CMD /bin/bash
