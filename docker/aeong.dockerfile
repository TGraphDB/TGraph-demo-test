FROM songjinghe/temporal-graph-dbms:aeong-2.2-e11679d
MAINTAINER Jinghe Song <songjh@buaa.edu.cn>

RUN echo Asia/Shanghai > /etc/timezone

COPY docker/aeong.docker-entrypoint.sh /home/AeonG/build/docker-entrypoint.sh

RUN mkdir -p /database

WORKDIR /home/AeonG/build
RUN chmod 755 /home/AeonG/build/docker-entrypoint.sh

ENTRYPOINT [ "/home/AeonG/build/docker-entrypoint.sh" ]
