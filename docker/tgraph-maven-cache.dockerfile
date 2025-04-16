FROM songjinghe/ubuntu-oraclejdk8-maven:latest
MAINTAINER Jinghe Song <songjh@buaa.edu.cn>

# cache TGraph source code & maven packages & built java classes and jars

RUN mkdir /db
WORKDIR /db

RUN git clone --depth=1 https://github.com/TGraphDB/temporal-storage.git -b TGraph2.3latest --single-branch && \
    cd /db/temporal-storage && \
    mvn -B install -Dmaven.test.skip=true && \
    rm -rf /db/temporal-storage

RUN git clone --depth=1 https://github.com/TGraphDB/temporal-neo4j.git -b TGraph2.3latest --single-branch && \
    cd /db/temporal-neo4j/community && \
    rm /db/temporal-neo4j/community/unsafe/src/test/java/sun/nio/ch/DelegateFileDispatcher.java && \
    rm -rf /db/temporal-neo4j/community/io/src/test/java/org/neo4j/adversaries && \
    rm -rf /db/temporal-neo4j/community/io/src/test/java/org/neo4j/io && \
    rm -rf /db/temporal-neo4j/community/io/src/test/java/org/neo4j/test/LinearHistoryPageCacheTracerTest.java && \
    rm -rf /db/temporal-neo4j/community/logging/src/test/java/org/neo4j/logging/RotatingFileOutputStreamSupplierTest.java && \
    rm -rf /db/temporal-neo4j/community/kernel/src/test/java/org/neo4j/test/PageCacheRule.java && \
    rm -rf /db/temporal-neo4j/community/kernel/src/test/java/org/neo4j/kernel/impl/api/store/StorePropertyPayloadCursorTest.java && \
    rm -rf /db/temporal-neo4j/community/kernel/src/test/java/org/neo4j/kernel/impl/store/RelationshipGroupStoreTest.java && \
    rm -rf /db/temporal-neo4j/community/kernel/src/test/java/org/neo4j/kernel/impl/store/UpgradeStoreIT.java && \
    rm -rf /db/temporal-neo4j/community/kernel/src/test/java/org/neo4j/kernel/impl/store/StoreFactoryTest.java && \
    rm -rf /db/temporal-neo4j/community/kernel/src/test/java/org/neo4j/kernel/impl/store/TestDynamicStore.java && \
    rm -rf /db/temporal-neo4j/community/kernel/src/test/java/org/neo4j/kernel/impl/store/FreeIdsAfterRecoveryTest.java && \
    rm -rf /db/temporal-neo4j/community/kernel/src/test/java/org/neo4j/kernel/impl/store/counts/CountsRotationTest.java && \
    rm -rf /db/temporal-neo4j/community/kernel/src/test/java/org/neo4j/kernel/impl/store/kvstore/KeyValueStoreFileTest.java && \
    rm -rf /db/temporal-neo4j/community/kernel/src/test/java/org/neo4j/kernel/impl/store/kvstore/KeyValueStoreFileFormatTest.java && \
    rm -rf /db/temporal-neo4j/community/kernel/src/test/java/org/neo4j/kernel/impl/store/TestStoreAccess.java && \
    rm -rf /db/temporal-neo4j/community/kernel/src/test/java/org/neo4j/graphdb/RunOutOfDiskSpaceIT.java && \
    rm -rf /db/temporal-neo4j/community/kernel/src/test/java/org/neo4j/kernel/impl/transaction/log/BatchingTransactionAppenderConcurrencyTest.java && \
    rm -rf /db/temporal-neo4j/community/kernel/src/test/java/org/neo4j/kernel/impl/transaction/PartialTransactionFailureIT.java && \
    rm -rf /db/temporal-neo4j/community/kernel/src/test/java/org/neo4j/kernel/impl/transaction/state/NeoStoreTransactionTest.java && \
    rm -rf /db/temporal-neo4j/community/kernel/src/test/java/org/neo4j/kernel/impl/transaction/state/TransactionRecordStateTest.java && \
    rm -rf /db/temporal-neo4j/community/kernel/src/test/java/org/neo4j/unsafe/impl/batchimport/PropertyEncoderStepTest.java && \
    mvn -B install -Dlicense.skip=true -Dlicensing.skip=true -DskipTests=true       -pl org.neo4j:neo4j-kernel -am && \
    mvn -B install -Dlicense.skip=true -Dlicensing.skip=true -Dmaven.test.skip=true -pl org.neo4j:neo4j-cypher -am && \
    rm -rf /db/temporal-neo4j

# mvn -B install -Dlicense.skip=true -Dlicensing.skip=true -DskipTests=true       -pl org.neo4j:neo4j-unsafe -am && \
# mvn -B install -Dlicense.skip=true -Dlicensing.skip=true -DskipTests=true       -pl org.neo4j:neo4j-io -am && \
# mvn -B install -Dlicense.skip=true -Dlicensing.skip=true -DskipTests=true       -pl org.neo4j:neo4j-logging -am && \

RUN git clone --depth=1 https://gitee.com/tgraphdb/demo-test.git -b dev-sjh --single-branch && \
    cd /db/demo-test && \
    mvn -B dependency:resolve

# rm -rf /db/demo-test

ENTRYPOINT /bin/bash
