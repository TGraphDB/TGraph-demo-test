FROM registry.cn-beijing.aliyuncs.com/songjinghe/ubuntu-oraclejdk-maven:latest
MAINTAINER Jinghe Song <songjh@act.buaa.edu.cn>

# cache TGraph source code & maven packages & built java classes and jars

RUN mkdir -p /tgraph
WORKDIR /tgraph

RUN git clone --depth=1 https://github.com/TGraphDB/temporal-storage.git -b TGraph2.3latest --single-branch
RUN git clone --depth=1 https://github.com/TGraphDB/temporal-neo4j.git -b TGraph2.3latest --single-branch
RUN git clone --depth=1 https://github.com/TGraphDB/TGraph-demo-test.git -b master --single-branch

WORKDIR /tgraph/temporal-storage
# RUN mvn -B dependency:resolve
RUN mvn -B install -Dmaven.test.skip=true && \
    cd /tgraph/temporal-neo4j && \
    mvn -B install -DskipTests -Dlicense.skip=true -Dlicensing.skip=true -pl org.neo4j:neo4j-cypher -am && \
    cd /tgraph/TGraph-demo-test && \
    mvn -B install -DskipTests

VOLUME /tgraph

ENTRYPOINT /bin/bash