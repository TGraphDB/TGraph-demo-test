# export CLASS_SERVER=edu.buaa.server.TGraphKernelSnappyServer
# export DB_PATH=/database
# export DB_PORT=8441

mvn -B --offline exec:java -Dexec.mainClass=$CLASS_SERVER -Dexec.cleanupDaemonThreads=false
