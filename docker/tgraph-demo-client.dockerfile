FROM registry.cn-beijing.aliyuncs.com/songjinghe/tgraph:latest
MAINTAINER Jinghe Song <songjh@act.buaa.edu.cn>

ENTRYPOINT /bin/bash
WORKDIR /tgraph-source/TGraph-demo-test
VOLUME /test
CMD ["mvn", "-B", "--offline", "exec:java", "-Dexec.mainClass=org.act.tgraph.demo.benchmark.BenchmarkRunner", "-Dexec.args=/tgraph" ]