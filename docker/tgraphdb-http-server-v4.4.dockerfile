FROM songjinghe/tgraph-demo-test:4.4-latest
MAINTAINER Jinghe Song <songjh@buaa.edu.cn>

# ENV MAVEN_OPTS "-Xmx512m"

WORKDIR /db/bin

RUN git clone --depth=1 https://github.com/zhaohaisun/TGraph-DB-HTTP-Server.git -b master --single-branch && \
    mv TGraph-DB-HTTP-Server tgraphdb-http-server & \
    cd /db/bin/tgraphdb-http-server && \
    mvn -B install -Dmaven.test.skip=true

WORKDIR /db/bin/tgraphdb-http-server
VOLUME /db/bin/tgraphdb-http-server/target
EXPOSE 7474

ENTRYPOINT mvn
CMD -B --offline compile exec:java -Dexec.mainClass=app.Application
