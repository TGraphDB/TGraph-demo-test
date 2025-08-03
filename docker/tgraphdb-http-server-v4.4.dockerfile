FROM songjinghe/tgraph-demo-test:4.4-latest
MAINTAINER Jinghe Song <songjh@buaa.edu.cn>

# ENV MAVEN_OPTS "-Xmx512m"

WORKDIR /db/bin/temporal-storage
RUN git pull --ff-only && mvn -B install -Dmaven.test.skip=true

WORKDIR /db/bin/temporal-neo4j
RUN git pull --ff-only && mvn -B install -Dmaven.test.skip=true -Dlicense.skip=true -Dlicensing.skip=true -Dcheckstyle.skip -Doverwrite -pl org.neo4j:neo4j-kernel -am

WORKDIR /db/bin

RUN git clone --depth=1 https://github.com/zhaohaisun/TGraph-DB-HTTP-Server.git -b master --single-branch && \
    mv TGraph-DB-HTTP-Server tgraphdb-http-server && \
    cd /db/bin/tgraphdb-http-server && \
    mvn -B install -Dmaven.test.skip=true

RUN wget -r https://zhaohaisun.github.io/vue-project/ && \
    mv zhaohaisun.github.io/vue-project/* /db/bin/tgraphdb-http-server/src/main/resources/static/ && \
    rm -rf zhaohaisun.github.io/vue-project/

# RUN git clone --depth=1 https://github.com/zhaohaisun/vue-project.git -b dist --single-branch && \
#     mv vue-project/release/* /db/bin/tgraphdb-http-server/src/main/resources/static/

WORKDIR /db/bin/tgraphdb-http-server
VOLUME /db/bin/tgraphdb-http-server/target
EXPOSE 7474

ENTRYPOINT mvn -B --offline compile exec:java -Dexec.mainClass=app.Application
