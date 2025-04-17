FROM songjinghe/tgraph-demo-test:2.3
MAINTAINER Jinghe Song <songjh@buaa.edu.cn>
# ENV MAVEN_OPTS "-Xmx512m"

WORKDIR /db/bin/demo-test

# input dataset folder & output db milestone folder
VOLUME /dataset
VOLUME /workspace

ENV CLASS_DATA_IMPORT edu.buaa.batch.TGSBulkLoad
ENV CLASS_DATA_GEN edu.buaa.dataset.EnergyWriteTxGenerator
ENV DATASET energy
ENV DATA_SIZE T0.1
ENV DATA_PATH /dataset
ENV DB_PATH /workspace

ENTRYPOINT /bin/bash
CMD ["mvn", "-B", "--offline", "exec:java", "-Dexec.mainClass=edu.buaa.common.benchmark.MilestoneBuilder", "-Dexec.cleanupDaemonThreads=false" ]
