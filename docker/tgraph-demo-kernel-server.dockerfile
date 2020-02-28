FROM registry.cn-beijing.aliyuncs.com/songjinghe/tgraph-cache:latest
MAINTAINER Jinghe Song <songjh@act.buaa.edu.cn>

WORKDIR /tgraph/temporal-storage
RUN git pull && mvn -B install -Dmaven.test.skip=true

WORKDIR /tgraph/temporal-neo4j
RUN git pull && mvn -B install -Dmaven.test.skip=true -Dlicense.skip=true -Dlicensing.skip=true -pl org.neo4j:neo4j-cypher -am

WORKDIR /tgraph/TGraph-demo-test
RUN git pull && mvn -B install -DskipTests

VOLUME /tgraph/db
EXPOSE 8438
ENV DB_PATH /tgraph/db

WORKDIR /tgraph/TGraph-demo-test
ENTRYPOINT /bin/bash
CMD ["mvn", "-B", "--offline", "exec:java", "-Dexec.mainClass=edu.buaa.server.TGraphKernelTcpServer" ]