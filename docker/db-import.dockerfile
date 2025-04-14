FROM registry.cn-beijing.aliyuncs.com/songjinghe/tgraph-cache:latest
MAINTAINER Jinghe Song <songjh@buaa.edu.cn>
# ENV MAVEN_OPTS "-Xmx512m"
# build latest version

WORKDIR /db/temporal-storage
RUN git pull
RUN mvn -B install -Dmaven.test.skip=true

WORKDIR /db/temporal-neo4j
RUN git pull
RUN mvn -B install -Dmaven.test.skip=true -Dlicense.skip=true -Dlicensing.skip=true -pl org.neo4j:neo4j-cypher -am

WORKDIR /db/demo-test
RUN git pull
RUN mvn -B install -Dmaven.test.skip=true

WORKDIR /db/demo-test
ENTRYPOINT /bin/bash

VOLUME /dataset # input dataset folder
VOLUME /workspace # output db milestone folder

ENV CLASS_DATA_IMPORT edu.buaa.batch.TGSBulkLoad
ENV CLASS_DATA_GEN edu.buaa.dataset.EnergyWriteTxGenerator
ENV DATASET energy
ENV DATA_SIZE T0.1
ENV DATA_PATH /dataset
ENV DB_PATH /workspace

CMD ["mvn", "-B", "--offline", "exec:java", "-Dexec.mainClass=edu.buaa.common.benchmark.MilestoneBuilder", "-Dexec.cleanupDaemonThreads=false" ]
