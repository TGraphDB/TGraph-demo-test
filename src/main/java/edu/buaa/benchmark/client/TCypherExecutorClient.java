package edu.buaa.benchmark.client;

import com.google.common.util.concurrent.ListenableFuture;
import edu.buaa.benchmark.transaction.AbstractTransaction;
import edu.buaa.benchmark.transaction.ImportTemporalDataTx;
import edu.buaa.client.TGraphSocketClient;
import edu.buaa.model.StatusUpdate;
import edu.buaa.utils.TimeMonitor;

import java.io.IOException;
import java.text.MessageFormat;

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
        return null;
    }

    private String importTemporalData(ImportTemporalDataTx tx){
        StringBuilder sb = new StringBuilder();
        for (StatusUpdate s : tx.data) {
            sb.append(MessageFormat.format("MATCH ()-[r:ROAD_TO]->() WHERE id(r)={0} SET " +
                    "r.travel_time=TV({1}~NOW:{2}), " +
                    "r.full_status=TV({1}~NOW:{3}), " +
//                    "r.vehicle_count=TV({1}~NOW:{4}); "
                    "r.segment_count=TV({1}~NOW:{5});"
                    ,s.getRoadId(), s.getTime(), s.getTravelTime(), s.getJamStatus(), s.getSegmentCount()));
        }
        return sb.toString();
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
}
