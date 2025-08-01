FROM node:22.17.1-bullseye
MAINTAINER Jinghe Song <songjh@buaa.edu.cn>

WORKDIR /code

RUN git clone --depth=1 https://github.com/zhaohaisun/vue-project.git -b main --single-branch && \
    mv vue-project tgraphdb-http-server-front

WORKDIR /code/tgraphdb-http-server-front
RUN npm install

# VOLUME /code/tgraphdb-http-server-front
EXPOSE 5173

ENTRYPOINT npm run dev
# CMD -B --offline compile exec:java -Dexec.mainClass=app.Application
