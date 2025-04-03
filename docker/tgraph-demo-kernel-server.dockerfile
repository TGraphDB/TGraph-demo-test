FROM registry.cn-beijing.aliyuncs.com/songjinghe/tgraph-cache:latest
MAINTAINER Jinghe Song <songjh@act.buaa.edu.cn>

# ENV MAVEN_OPTS "-Xmx512m"

# cache TGraph source code & maven packages & built java classes and jars

RUN mkdir -p /db
WORKDIR /db

RUN git clone --depth=1 https://gitee.com/tgraphdb/temporal-neo4j.git -b TGraph2.3latest --single-branch
RUN git clone --depth=1 https://gitee.com/tgraphdb/temporal-storage.git -b TGraph2.3latest --single-branch
RUN git clone --depth=1 https://gitee.com/tgraphdb/demo-test.git -b dev-sjh --single-branch

WORKDIR /db/temporal-storage
# RUN mvn -B dependency:resolve
RUN mvn -B install -Dmaven.test.skip=true

WORKDIR /db/temporal-neo4j
RUN mvn -B install -DskipTests -Dlicense.skip=true -Dlicensing.skip=true -pl org.neo4j:neo4j-cypher -am

WORKDIR /db/demo-test
RUN mvn -B install -Dmaven.test.skip=true

VOLUME /db/data
ENV DB_PATH /db/data

EXPOSE 9438
ENV DB_PORT 9438

WORKDIR /db/demo-test
ENTRYPOINT /bin/bash
CMD ["mvn", "-B", "--offline", "exec:java", "-Dexec.mainClass=edu.buaa.server.TGraphKernelSnappyServer" ]
