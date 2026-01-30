FROM songjinghe/temporal-graph-dbms:aeong-2.2-e11679d
MAINTAINER Jinghe Song <songjh@buaa.edu.cn>

RUN echo Asia/Shanghai > /etc/timezone
RUN mkdir -p /database
WORKDIR /home/AeonG/build
ENTRYPOINT [                                   \
    "/home/AeonG/build/memgraph",              \
    "--bolt-port 7687",                        \
    "--data-directory /database/",             \
    "--log-file=/database/aeong.log",          \
    "--log-level=DEBUG",                       \
    "--also-log-to-stderr",                    \
    "--bolt-server-name-for-init=Neo4j/5.2.0", \
    "--storage-snapshot-on-exit=true",         \
    "--data_recovery_on_startup=true",         \
    "--storage-snapshot-interval-sec 30",      \
    "--storage-properties-on-edges=true",      \
    "--memory-limit 0",                        \
    "--anchor-num 10",                         \
    "--storage-gc-cycle-sec 30",               \
    "--real-time-flag=false"                   \
]
