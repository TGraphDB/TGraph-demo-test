FROM songjinghe/ubuntu-oraclejdk8-maven:latest
MAINTAINER Jinghe Song <songjh@buaa.edu.cn>

# cache TGraph source code & maven packages & built java classes and jars

RUN mkdir /db
WORKDIR /db

RUN git clone --depth=1 https://gitee.com/tgraphdb/temporal-storage.git -b TGraph2.3latest --single-branch && \
    cd /db/temporal-storage && \
    mvn -B install -Dmaven.test.skip=true && \
    rm -rf /db/temporal-storage

RUN git clone --depth=1 https://gitee.com/tgraphdb/temporal-neo4j.git -b TGraph2.3latest --single-branch && \
    cd /db/temporal-neo4j && \
    mvn -B install -Dmaven.test.skip=true -Dlicense.skip=true -Dlicensing.skip=true -pl org.neo4j:neo4j-cypher -am && \
    rm -rf /db/temporal-neo4j

RUN git clone --depth=1 https://gitee.com/tgraphdb/demo-test.git -b dev-sjh --single-branch && \
    cd /db/demo-test && \
    mvn -B dependency:resolve && \
    rm -rf /db/demo-test

ENTRYPOINT /bin/bash
