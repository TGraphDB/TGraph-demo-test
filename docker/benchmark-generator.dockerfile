FROM songjinghe/tgraph-demo-test:2.3
MAINTAINER Jinghe Song <songjh@buaa.edu.cn>

VOLUME /dataset
VOLUME /benchmark

WORKDIR /db/bin/demo-test

ENV DIR_DATA /dataset
ENV BENCHMARK_FULL_PATH /benchmark/benchmark.json
ENV DATASET energy
ENV CLASS_DATA_GEN edu.buaa.dataset.EnergyWriteTxGenerator
ENV DATA_SIZE 20120101~20150101
ENV QUERY_CNT 10000
ENV APPEND_TX_SIZE 100
ENV RQ_DISTRIBUTION 100,0,0,0,0,0,0,0

# ENTRYPOINT /bin/bash
CMD ["mvn", "-B", "--offline", "exec:java", "-Dexec.mainClass=edu.buaa.common.benchmark.BenchmarkBuilder" ]
