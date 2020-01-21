FROM registry.cn-beijing.aliyuncs.com/songjinghe/tgraph:latest
MAINTAINER Jinghe Song <songjh@act.buaa.edu.cn>

WORKDIR /tgraph/temporal-storage
RUN git pull && mvn -B install -Dmaven.test.skip=true
WORKDIR /tgraph/temporal-neo4j
RUN git pull && mvn -B install -Dmaven.test.skip=true -Dlicense.skip=true -Dlicensing.skip=true -pl org.neo4j:neo4j-cypher -am
WORKDIR /tgraph/TGraph-demo-test
RUN git pull && mvn -B install -DskipTests

ENTRYPOINT /bin/bash
VOLUME /tgraph/test

ENV WORK_DIR /tgraph/test
ENV BENCHMARK_WITH_RESULT true
ENV BENCHMARK_FILE_OUTPUT benchmark
ENV TEMPORAL_DATA_PER_TX 100
ENV TEMPORAL_DATA_START 0503
ENV TEMPORAL_DATA_END   0507
ENV REACHABLE_AREA_TX_CNT 20

WORKDIR /tgraph/TGraph-demo-test
CMD ["mvn", "-B", "--offline", "exec:java", "-Dexec.mainClass=edu.buaa.benchmark.BenchmarkTxArgsGenerator" ]