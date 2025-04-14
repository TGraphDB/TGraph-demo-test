FROM registry.cn-beijing.aliyuncs.com/songjinghe/tgraph-cache:latest
MAINTAINER Jinghe Song <songjh@buaa.edu.cn>

VOLUME /benchmark

ENV DB_TYPE TGS
ENV DB_HOST t630
ENV DB_PORT 8441
ENV DB_NAME only.need.in.jdbc
ENV BENCHMARK_FULL_PATH /benchmark/benchmark.json
ENV BENCHMARK_RESULT_PATH /benchmark/TGS.result.json
ENV VERIFY_RESULT false
ENV CLASS_CLIENT edu.buaa.client.NeoTGraphExecutorClient
ENV MAX_CONNECTION_CNT 1
ENV REQ_RATE -1
ENV DEVICE unknown

WORKDIR /db/demo-test
ENTRYPOINT /bin/bash
CMD mvn -B --offline exec:java -Dexec.mainClass=edu.buaa.benchmark.BenchmarkRunner
