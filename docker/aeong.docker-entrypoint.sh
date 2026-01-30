#!/bin/bash

/home/AeonG/build/memgraph --bolt-port 7687 \
   --data-directory /database/      \
   --log-file=/database/aeong.log  \
   --log-level=DEBUG           \
   --also-log-to-stderr        \
   --bolt-server-name-for-init=Neo4j/5.2.0 \
   --storage-snapshot-on-exit=true         \
   --storage-recover-on-startup=true       \
   --storage-snapshot-interval-sec 30      \
   --storage-gc-cycle-sec 30   \
   --storage-properties-on-edges=true     \
   --memory-limit 0            \
   --anchor-num 10             \
   --real-time-flag=false
