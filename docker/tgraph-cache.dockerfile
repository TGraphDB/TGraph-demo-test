FROM registry.cn-beijing.aliyuncs.com/songjinghe/ubuntu-oraclejdk-maven:latest
MAINTAINER Jinghe Song <songjh@buaa.edu.cn>

# cache TGraph source code & maven packages & built java classes and jars

RUN mkdir /db
WORKDIR /db

RUN git clone --depth=1 https://gitee.com/tgraphdb/temporal-storage.git -b TGraph2.3latest --single-branch
RUN git clone --depth=1 https://gitee.com/tgraphdb/temporal-neo4j.git -b TGraph2.3latest --single-branch
RUN git clone --depth=1 https://gitee.com/tgraphdb/demo-test.git -b dev-sjh --single-branch

WORKDIR /db/temporal-storage
# RUN mvn -B dependency:resolve
RUN mvn -B install -Dmaven.test.skip=true

WORKDIR /db/temporal-neo4j
RUN mvn -B install -Dmaven.test.skip=true -Dlicense.skip=true -Dlicensing.skip=true -pl org.neo4j:neo4j-cypher -am

WORKDIR /db/demo-test
RUN mvn -B install -Dmaven.test.skip=true -DskipTests

ENTRYPOINT /bin/bash
