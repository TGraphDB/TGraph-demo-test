package org.act.tgraph.demo.benchmark.client;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import org.act.tgraph.demo.algo.EarliestArriveTime;
import org.act.tgraph.demo.benchmark.transaction.AbstractTransaction;
import org.act.tgraph.demo.benchmark.transaction.ImportStaticDataTx;
import org.act.tgraph.demo.benchmark.transaction.ImportTemporalDataTx;
import org.act.tgraph.demo.benchmark.transaction.ReachableAreaQueryTx;
import org.act.tgraph.demo.utils.TimeMonitor;
import org.apache.commons.lang3.tuple.Pair;

import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.*;

public class SqlServerExecutorClient implements DBProxy {
    private ThreadPoolExecutor exe;
    private BlockingQueue<Connection> connectionPool = new LinkedBlockingQueue<>();
    private ListeningExecutorService service;

    public SqlServerExecutorClient(String serverHost, int parallelCnt, int queueLength) throws IOException, ClassNotFoundException, SQLException {
        Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
        String dbURL = "jdbc:sqlserver://" + serverHost + ":8438; DatabaseName=sample";
        this.exe = new ThreadPoolExecutor(parallelCnt, parallelCnt, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(queueLength), (r, executor) -> {
            if (!executor.isShutdown()) try {
                executor.getQueue().put(r);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
        exe.prestartAllCoreThreads();
        this.service = MoreExecutors.listeningDecorator(exe);
        for(int i = 0; i< parallelCnt; i++) this.connectionPool.offer(DriverManager.getConnection(dbURL, "sa", ""));
    }

    @Override
    public ListenableFuture<JsonObject> execute(AbstractTransaction tx) {
        switch (tx.txType){
            case tx_import_static_data:
                return this.service.submit(execute((ImportStaticDataTx) tx));
            case tx_import_temporal_data:
                return this.service.submit(execute((ImportTemporalDataTx)tx));
            case tx_query_reachable_area:
                return this.service.submit(execute((ReachableAreaQueryTx) tx));
            default:
                throw new UnsupportedOperationException();
        }
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
        try {
            while(connectionPool.size()>0){
                Connection conn = connectionPool.take();
                conn.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
            throw new IOException(e);
        }
    }

    @Override
    public String testServerClientCompatibility() {return "sql-server";}

    private Callable<JsonObject> execute(ImportStaticDataTx tx){
        return new Req(){
            @Override
            protected JsonObject executeQuery(Connection conn) throws Exception{
                conn.setAutoCommit(false);
                PreparedStatement stat1 = conn.prepareStatement("INSERT INTO cross_node VALUES (?,?)");
                for(Pair<Long, String> p : tx.crosses){
                    stat1.setInt(1, Math.toIntExact(p.getLeft()));
                    stat1.setString(2, p.getRight());
                    stat1.addBatch();
                }
                stat1.executeBatch();
                stat1.close();
                PreparedStatement stat2 = conn.prepareStatement("INSERT INTO road VALUES (?,?,?,?,?,?)");
                for(ImportStaticDataTx.StaticRoadRel r : tx.roads){
                    stat2.setInt(1, Math.toIntExact(r.roadId));
                    stat2.setString(2, r.id);
                    stat2.setInt(3, Math.toIntExact(r.startCrossId));
                    stat2.setInt(4, Math.toIntExact(r.endCrossId));
                    stat2.setInt(5, r.length);
                    stat2.setInt(6, r.type);
                    stat2.addBatch();
                }
                stat2.executeBatch();
                stat2.close();
                conn.commit();
                conn.setAutoCommit(true);
                Statement stat = conn.createStatement();
                stat.execute("");
                return new JsonObject();
            }
        };
    }

    private Callable<JsonObject> execute(ImportTemporalDataTx tx){
        return new Req(){
            @Override
            protected JsonObject executeQuery(Connection conn) throws Exception{
                conn.setAutoCommit(false);
                PreparedStatement stat = conn.prepareStatement("INSERT INTO temporal_status VALUES (?,?)");
                for(ImportTemporalDataTx.StatusUpdate s : tx.data){
                    stat.setInt(1, s.time);
                    stat.setInt(2, Math.toIntExact(s.roadId));
                    stat.setInt(3, s.jamStatus);
                    stat.setInt(4, s.travelTime);
                    stat.setInt(5, s.segmentCount);
                    stat.addBatch();
                }
                stat.executeBatch();
                stat.close();
                conn.commit();
                conn.setAutoCommit(true);
                return new JsonObject();
            }
        };
    }

    private Callable<JsonObject> execute(ReachableAreaQueryTx tx){
        return new Req(){
            @Override
            protected JsonObject executeQuery(Connection conn) throws Exception{
                conn.setAutoCommit(true);
                EarliestArriveTime algo = new EarliestArriveTimeSQL(tx.startCrossId, tx.departureTime, tx.travelTime, conn);
                List<EarliestArriveTime.NodeCross> result = new ArrayList<>(algo.run());
                result.sort(Comparator.comparingLong(o -> o.id));
                JsonObject res = Json.object();
                JsonArray nodeIdArr = Json.array();
                JsonArray arriveTimeArr = Json.array();
                JsonArray parentIdArr = Json.array();
                for(EarliestArriveTime.NodeCross node : result){
                    nodeIdArr.add(node.id);
                    arriveTimeArr.add(node.arriveTime);
                    parentIdArr.add(node.parent);
                }
                res.add("nodeId", nodeIdArr);
                res.add("arriveTime", arriveTimeArr);
                res.add("parentId", parentIdArr);
                return res;
            }
        };
    }

    private abstract class Req implements Callable<JsonObject>{
        private final TimeMonitor timeMonitor = new TimeMonitor();

        @Override
        public JsonObject call() throws Exception {
            try {
                Connection conn = connectionPool.take();
                timeMonitor.mark("Wait in queue", "query");
                JsonObject result = executeQuery(conn);
                timeMonitor.end("query");
                if (result == null) throw new RuntimeException("[Got null. Server close connection]");
                connectionPool.put(conn);
                return result;
            }catch (Exception e){
                e.printStackTrace();
                throw e;
            }
        }

        protected abstract JsonObject executeQuery(Connection conn) throws Exception;
    }

    private static class EarliestArriveTimeSQL extends EarliestArriveTime {
        private final PreparedStatement getEndNodeIdStat;
        private final PreparedStatement getCrossOutRoadStat;
        private final PreparedStatement getTemporalStat;
        private final PreparedStatement getStartTStat;

        EarliestArriveTimeSQL(long startId, int startTime, int travelTime, Connection conn) throws SQLException {
            super(startId, startTime, travelTime);
            this.getEndNodeIdStat = conn.prepareStatement("SELECT r_end FROM road WHERE id=?");
            this.getCrossOutRoadStat = conn.prepareStatement("SELECT id FROM road WHERE r_start=?");
            this.getStartTStat = conn.prepareStatement("SELECT MAX(t) as ts FROM temporal_status WHERE rid=? AND t<=? LIMIT 1");
            this.getTemporalStat = conn.prepareStatement("SELECT t, travel_t FROM temporal_status WHERE rid=? AND t>=? AND t<=?");
        }

        @Override
        protected int getEarliestArriveTime(Long roadId, int departureTime) throws UnsupportedOperationException {
            try {
                int startT = maxTimeLessOrEq(roadId, departureTime);
                ResultSet rs = getTT(roadId, startT, departureTime + this.travelTime);
                int minArriveTime = Integer.MAX_VALUE;
                int curT = departureTime;
                while(rs.next() && curT<minArriveTime){
                    curT = rs.getInt("t");
                    int travelT = rs.getInt("travel_t");
                    if(curT<departureTime){
                        minArriveTime = departureTime+travelT;
                    }else if(curT+travelT<minArriveTime){
                        minArriveTime = curT+travelT;
                    }
                }
                if(minArriveTime!=Integer.MAX_VALUE) return minArriveTime;
                else throw new UnsupportedOperationException();
            } catch (SQLException e) {
                e.printStackTrace();
                throw new UnsupportedOperationException(e);
            }
        }

        private int maxTimeLessOrEq(long roadId, int time) throws SQLException {
            this.getStartTStat.setInt(1, Math.toIntExact(roadId));
            this.getStartTStat.setInt(2, time);
            ResultSet rs = this.getStartTStat.executeQuery();
            if(rs.next()){
                return rs.getInt("ts");
            }else{
                throw new UnsupportedOperationException();
            }
        }

        private ResultSet getTT(long roadId, int start, int end) throws SQLException {
            this.getTemporalStat.setInt(1, Math.toIntExact(roadId));
            this.getTemporalStat.setInt(2, start);
            this.getTemporalStat.setInt(3, end);
            return this.getTemporalStat.executeQuery();
        }

        @Override
        protected Iterable<Long> getAllOutRoads(long nodeId) {
            try {
                this.getCrossOutRoadStat.setInt(1, Math.toIntExact(nodeId));
                ResultSet rs = this.getCrossOutRoadStat.executeQuery();
                List<Long> result = new ArrayList<>();
                while(rs.next()){
                    result.add((long) rs.getInt("id"));
                }
                return result;
            } catch (SQLException e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        }

        @Override
        protected long getEndNodeId(long roadId) {
            try {
                this.getEndNodeIdStat.setInt(1, Math.toIntExact(roadId));
                ResultSet rs = this.getEndNodeIdStat.executeQuery();
                if(rs.next()){
                    return rs.getInt("r_end");
                }else{
                    throw new RuntimeException("road not found, should not happen!");
                }
            } catch (SQLException e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        }
    }
}
