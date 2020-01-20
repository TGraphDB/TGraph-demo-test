FROM registry.cn-beijing.aliyuncs.com/songjinghe/tgraph-cache:latest
MAINTAINER Jinghe Song <songjh@act.buaa.edu.cn>

WORKDIR /tgraph-source/temporal-storage
RUN git pull
RUN mvn -B install -Dmaven.test.skip=true

WORKDIR /tgraph-source/temporal-neo4j
RUN git pull
RUN mvn -B install -Dmaven.test.skip=true -Dlicense.skip=true -Dlicensing.skip=true -pl org.neo4j:neo4j-cypher -am

WORKDIR /tgraph-source/TGraph-demo-test
RUN git pull
RUN mvn -B install -DskipTests

ENTRYPOINT /bin/bash
WORKDIR /tgraph-source/TGraph-demo-test
VOLUME /tgraph
EXPOSE 8438
CMD ["mvn", "-B", "--offline", "exec:java", "-Dexec.mainClass=org.act.tgraph.demo.server.KernelTcpServer", "-Dexec.args=/tgraph" ]