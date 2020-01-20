FROM registry.cn-beijing.aliyuncs.com/songjinghe/tgraph-cache:latest
MAINTAINER Jinghe Song <songjh@act.buaa.edu.cn>

WORKDIR /tgraph/temporal-storage
RUN git pull
RUN mvn -B install -Dmaven.test.skip=true

WORKDIR /tgraph/temporal-neo4j
RUN git pull
RUN mvn -B install -Dmaven.test.skip=true -Dlicense.skip=true -Dlicensing.skip=true -pl org.neo4j:neo4j-cypher -am

WORKDIR /tgraph/TGraph-demo-test
RUN git pull
RUN mvn -B install -DskipTests

VOLUME /tgraph/test

ENV DB_TYPE tgraph
ENV DB_HOST localhost
ENV BENCHMARK_FILE_INPUT /tgraph/test/benchmark-with-result.gz
ENV MAX_CONNECTION_CNT 1

WORKDIR /tgraph/TGraph-demo-test
ENTRYPOINT /bin/bash
CMD ["mvn", "-B", "--offline", "exec:java", "-Dexec.mainClass=org.act.tgraph.demo.benchmark.BenchmarkRunner" ]