export CLASS_DATA_IMPORT=edu.buaa.batch.TGSBulkLoad
export CLASS_DATA_GEN=edu.buaa.dataset.EnergyWriteTxGenerator
export DATASET=energy
export DATA_SIZE=T0.1
export DATA_PATH=/dataset
export DB_PATH=/workspace

mvn -B --offline exec:java -Dexec.mainClass=edu.buaa.common.benchmark.MilestoneBuilder -Dexec.cleanupDaemonThreads=false
