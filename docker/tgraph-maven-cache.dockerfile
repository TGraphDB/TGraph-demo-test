FROM songjinghe/ubuntu-oraclejdk8-maven:latest
MAINTAINER Jinghe Song <songjh@buaa.edu.cn>

# cache TGraph source code & maven packages & built java classes and jars

RUN mkdir /db
WORKDIR /db

RUN git clone --depth=1 https://github.com/TGraphDB/temporal-storage.git -b TGraph2.3latest --single-branch && \
    cd /db/temporal-storage && \
    mvn -B install -Dmaven.test.skip=true && \
    rm -rf /db/temporal-storage

RUN git clone --depth=1 https://github.com/TGraphDB/temporal-neo4j.git -b TGraph2.3latest --single-branch && \
    cd /db/temporal-neo4j/community && \
    rm /db/temporal-neo4j/community/unsafe/src/test/java/sun/nio/ch/DelegateFileDispatcher.java && \
    rm -rf /db/temporal-neo4j/community/io/src/test/java/org/neo4j/adversaries && \
    rm -rf /db/temporal-neo4j/community/io/src/test/java/org/neo4j/io && \
    rm -rf /db/temporal-neo4j/community/io/src/test/java/org/neo4j/test/LinearHistoryPageCacheTracerTest.java && \
    rm -rf /db/temporal-neo4j/community/logging/src/test/java/org/neo4j/logging/RotatingFileOutputStreamSupplierTest.java && \
    rm -rf /db/temporal-neo4j/community/kernel/src/test/java/org/ && \
    rm -rf /db/temporal-neo4j/community/kernel/src/test/java/examples/ && \
    rm -rf /db/temporal-neo4j/community/graphviz/src/test/java/org/neo4j/visualization/graphviz/TestGraphvizSubgraphOutput.java && \
    rm -rf /db/temporal-neo4j/community/graphviz/src/test/java/org/neo4j/visualization/graphviz/TestNewGraphvizWriter.java && \
    mvn -B install -Dlicense.skip=true -Dlicensing.skip=true -DskipTests=true       -pl org.neo4j:neo4j-cypher -am && \
    rm -rf /db/temporal-neo4j

# mvn -B install -Dlicense.skip=true -Dlicensing.skip=true -Dmaven.test.skip=true -pl org.neo4j:neo4j-cypher -am && \
# mvn -B install -Dlicense.skip=true -Dlicensing.skip=true -DskipTests=true       -pl org.neo4j:neo4j-unsafe -am && \
# mvn -B install -Dlicense.skip=true -Dlicensing.skip=true -DskipTests=true       -pl org.neo4j:neo4j-io -am && \
# mvn -B install -Dlicense.skip=true -Dlicensing.skip=true -DskipTests=true       -pl org.neo4j:neo4j-logging -am && \

RUN git clone --depth=1 https://gitee.com/tgraphdb/demo-test.git -b dev-sjh --single-branch && \
    cd /db/demo-test && \
    mvn -B dependency:resolve && \
    rm -rf /db/demo-test

ENTRYPOINT /bin/bash
