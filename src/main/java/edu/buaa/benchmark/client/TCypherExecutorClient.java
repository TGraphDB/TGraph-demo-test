package edu.buaa.benchmark.client;

import com.google.common.util.concurrent.ListenableFuture;
import edu.buaa.benchmark.transaction.*;
import edu.buaa.benchmark.transaction.index.CreateTGraphAggrDurationIndexTx;
import edu.buaa.benchmark.transaction.index.CreateTGraphAggrMaxIndexTx;
import edu.buaa.benchmark.transaction.index.CreateTGraphTemporalValueIndexTx;
import edu.buaa.benchmark.transaction.internal.EarliestArriveTimeAggrTx;
import edu.buaa.client.TGraphSocketClient;
import edu.buaa.model.StatusUpdate;
import edu.buaa.utils.TimeMonitor;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TCypherExecutorClient extends TGraphSocketClient implements DBProxy {
    public TCypherExecutorClient(String serverHost, int parallelCnt, int queueLength) throws IOException{
        super(serverHost, parallelCnt, queueLength);
    }

    @Override
    public String testServerClientCompatibility() throws UnsupportedOperationException {
        return null;
    }

    @Override
    protected ServerResponse onResponse(String query, String response, TimeMonitor timeMonitor, Thread thread) throws Exception {
        return null;
    }

    @Override
    public ListenableFuture<ServerResponse> execute(AbstractTransaction tx) throws Exception {
        switch (tx.getTxType()){
            case tx_import_static_data: return execute((ImportStaticDataTx) tx);
            case tx_import_temporal_data: return execute((ImportTemporalDataTx) tx);
            case tx_query_snapshot_aggr_max: return execute((SnapshotAggrMaxTx) tx);
            case tx_query_snapshot_aggr_duration: return execute((SnapshotAggrDurationTx) tx);
            case tx_query_snapshot: return execute((SnapshotQueryTx) tx);
            case tx_query_road_by_temporal_condition: return execute((EntityTemporalConditionTx) tx);
            case tx_index_tgraph_aggr_max: return execute((CreateTGraphAggrMaxIndexTx) tx);
            case tx_index_tgraph_aggr_duration: return execute((CreateTGraphAggrDurationIndexTx) tx);
            case tx_index_tgraph_temporal_condition: return execute((CreateTGraphTemporalValueIndexTx) tx);
            default:
                throw new UnsupportedOperationException();
        }
    }

    private ListenableFuture<ServerResponse> execute(ImportStaticDataTx tx){
        StringBuilder sb = new StringBuilder();
        for (ImportStaticDataTx.StaticCrossNode p : tx.getCrosses()) {
            sb.append(MessageFormat.format("CREATE ({name: '{0}', tid: {1} });", p.getName(), p.getId()));
        }
        for (ImportStaticDataTx.StaticRoadRel sr : tx.getRoads()) {
            sb.append(MessageFormat.format("CREATE (n)-[r:ROAD_TO {name: '{2}'}]->(m) WHERE id(n)={0} AND id(m)={1}", sr.getStartCrossId(), sr.getEndCrossId(), sr.getId()));
        }
        return addQuery(sb.toString());
    }

    private ListenableFuture<ServerResponse> execute(ImportTemporalDataTx tx){
        StringBuilder sb = new StringBuilder();
        for (StatusUpdate s : tx.data) {
            sb.append(MessageFormat.format("MATCH ()-[r:ROAD_TO]->() WHERE r.name='{0}' SET " +
                    "r.travel_time=TV({1}~NOW:{2}), " +
                    "r.full_status=TV({1}~NOW:{3}), " +
                    "r.segment_count=TV({1}~NOW:{5});"
                    ,s.getRoadId(), s.getTime(), s.getTravelTime(), s.getJamStatus(), s.getSegmentCount()));
        }
        return addQuery(sb.toString());
    }

    private ListenableFuture<ServerResponse> execute(SnapshotAggrMaxTx tx){
        return addQuery(
                MessageFormat.format(
                        "MATCH ()-[r:ROAD_TO]->() RETURN r.name, TAGGR_MAX(r.{0}, {1}~{2})"
            ,tx.getP(), tx.getT0(), tx.getT1()));
    }

    private ListenableFuture<ServerResponse> execute(SnapshotAggrDurationTx tx){
        return addQuery(
                MessageFormat.format(
                        "MATCH ()-[r:ROAD_TO]->() RETURN r.name, TAGGR_DURATION(r.{0}, {1}~{2})"
                        ,tx.getP(), tx.getT0(), tx.getT1()));
    }

    private ListenableFuture<ServerResponse> execute(SnapshotQueryTx tx){
        return addQuery(
                MessageFormat.format(
                        "MATCH ()-[r:ROAD_TO]->() RETURN r.name, TFV(r.{0}, {1})"
                        ,tx.getPropertyName(), tx.getTimestamp()));
    }

    private ListenableFuture<ServerResponse> execute(EntityTemporalConditionTx tx){
        return addQuery(
                MessageFormat.format(
                        "MATCH ()-[r:ROAD_TO]->() WHERE TVRC(r.{0}, any, {1}~{2}, {3}, {4}) RETURN r.name"
                        ,tx.getP(), tx.getT0(), tx.getT1(), tx.getVmin(), tx.getVmax()));
    }

    private ListenableFuture<ServerResponse> execute(CreateTGraphAggrMaxIndexTx tx){
        return addQuery(
                MessageFormat.format(
                        "CREATE TEMPORAL INDEX(min_max) ON ({0}) DURING {1}~{2} ARGS({3}, {4})"
                        ,tx.getProName(), tx.getStart(), tx.getEnd(), tx.getEvery(), tx.getTimeUnit()));
    }

    private ListenableFuture<ServerResponse> execute(CreateTGraphAggrDurationIndexTx tx){
        return addQuery(
                MessageFormat.format(
                        "CREATE TEMPORAL INDEX(duration) ON ({0}) DURING {1}~{2} ARGS({3}, {4})"
                        ,tx.getProName(), tx.getStart(), tx.getEnd(), tx.getEvery(), tx.getTimeUnit()));
    }

    private ListenableFuture<ServerResponse> execute(CreateTGraphTemporalValueIndexTx tx){
        StringBuilder b = new StringBuilder();
        for(String p : tx.getProps()){
            b.append(p).append(',');
        }
        return addQuery(
                MessageFormat.format(
                        "CREATE TEMPORAL INDEX(value) ON ({0}) DURING {1}~{2}"
                        ,b.substring(0, b.length()-1), tx.getStart(), tx.getEnd()));
    }

    @Override
    public void createDB() throws IOException {

    }

    @Override
    public void restartDB() throws IOException {

    }

    @Override
    public void shutdownDB() throws IOException {

    }

    @Override
    public void close() throws IOException, InterruptedException {

    }

    public static class Result extends AbstractTransaction.Result{
        Map<String, List<Object>> results = new HashMap<>();

        public Map<String, List<Object>> getResults() {
            return results;
        }

        public void setResults(Map<String, List<Object>> results) {
            this.results = results;
        }
    }
}
