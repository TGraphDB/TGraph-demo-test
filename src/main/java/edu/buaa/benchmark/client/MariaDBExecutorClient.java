package edu.buaa.benchmark.client;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import edu.buaa.algo.EarliestArriveTime;
import edu.buaa.benchmark.transaction.*;
import edu.buaa.utils.TimeMonitor;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class MariaDBExecutorClient implements DBProxy {

    private ThreadPoolExecutor exe;
    private BlockingQueue<Connection> connectionPool = new LinkedBlockingQueue<>();
    private ListeningExecutorService service;
    private Map<Connection, Integer> connIdMap = new HashMap<>();


    public MariaDBExecutorClient(String serverHost, int parallelCnt, int queueLength) throws IOException, ClassNotFoundException, SQLException {
        Class.forName("org.mariadb.jdbc.Driver");
        String dbURL = "jdbc:mariadb://" + serverHost + ":3306";//; DatabaseName=sample
        this.exe = new ThreadPoolExecutor(parallelCnt, parallelCnt, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(queueLength), (r, executor) -> {
            if (!executor.isShutdown()) {
                try {
                    executor.getQueue().put(r);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
        exe.prestartAllCoreThreads();
        this.service = MoreExecutors.listeningDecorator(exe);
        for(int i = 0; i< parallelCnt; i++) {
            Connection conn = DriverManager.getConnection(dbURL, "root", "root");
            this.connectionPool.offer(conn);
            connIdMap.put(conn, i);
        }
    }

    @Override
    public String testServerClientCompatibility() throws UnsupportedOperationException {
        return null;
    }

    @Override
    public ListenableFuture<DBProxy.ServerResponse> execute(AbstractTransaction tx) {
        switch (tx.getTxType()) {
            case tx_import_static_data:
                return this.service.submit(execute((ImportStaticDataTx) tx));
            case tx_import_temporal_data:
                return this.service.submit(execute((ImportTemporalDataTx) tx));
            case tx_query_reachable_area:
                return this.service.submit(execute((ReachableAreaQueryTx) tx));
            case tx_query_road_earliest_arrive_time_aggr:
                return this.service.submit(execute((EarliestArriveTimeAggrTx)tx));
            case tx_query_node_neighbor_road:
                return this.service.submit(execute((NodeNeighborRoadTx) tx));
            default:
                throw new UnsupportedOperationException();
        }
    }

    @Override
    public void createDB() throws IOException {

        try {
            Connection con = connectionPool.take();
            Statement stmt = con.createStatement();
            con.setAutoCommit(true);
            stmt.execute("CREATE DATABASE beijing_traffic");
            stmt.execute("USE beijing_traffic");
            stmt.execute("CREATE TABLE cross_node(cn_id INT PRIMARY KEY, name CHAR(255))");
            stmt.execute("CREATE TABLE road(r_id INT PRIMARY KEY, r_name CHAR(16), r_start INT, r_end INT, r_length INT, r_type INT)");
            stmt.execute("CREATE TABLE temporal_status(ts_id INT PRIMARY KEY, st_time TIMESTAMP(0), en_time TIMESTAMP(0), r_id INT, status INT, travel_t INT, seg_cnt INT, PERIOD FOR time_period(st_time, en_time))");
            //time format : timestamp(0) xxxx-xx-xx xx:xx:xx
            //transform int to datetime while inserting or updating
            stmt.close();
            connectionPool.put(con);
        } catch (SQLException | InterruptedException ex) {
            ex.printStackTrace();
            throw new IOException(ex);
        }

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
            service.shutdown();
            while(!service.isTerminated()) {
                service.awaitTermination(10, TimeUnit.SECONDS);
                long completeCnt = exe.getCompletedTaskCount();
                int remains = exe.getQueue().size();
                System.out.println( completeCnt+"/"+ (completeCnt+remains)+" query completed.");
            }
            while(!connectionPool.isEmpty()){
                Connection conn = connectionPool.take();
                conn.close();
            }
            System.out.println("Client exit. send "+ exe.getCompletedTaskCount() +" lines.");
        } catch (SQLException e) {
            e.printStackTrace();
            throw new IOException(e);
        }
    }

    private Callable<DBProxy.ServerResponse> execute(ImportStaticDataTx tx) {
        return new MariaDBExecutorClient.Req() {
            @Override
            protected AbstractTransaction.Result executeQuery(Connection conn) throws Exception{
                conn.setAutoCommit(false);
                PreparedStatement stat1 = conn.prepareStatement("INSERT INTO cross_node VALUES (?, ?)");
                for(ImportStaticDataTx.StaticCrossNode p : tx.getCrosses()){
                    stat1.setInt(1, Math.toIntExact(p.getId()));
                    stat1.setString(2, p.getName());
                    stat1.addBatch();
                }
                stat1.executeBatch();
                stat1.close();
                PreparedStatement stat2 = conn.prepareStatement("INSERT INTO road VALUES (?, ?, ?, ?, ?, ?)");
                for(ImportStaticDataTx.StaticRoadRel r : tx.getRoads()){
                    stat2.setInt(1, Math.toIntExact(r.getRoadId()));
                    stat2.setString(2, r.getId());
                    stat2.setInt(3, Math.toIntExact(r.getStartCrossId()));
                    stat2.setInt(4, Math.toIntExact(r.getEndCrossId()));
                    stat2.setInt(5, r.getLength());
                    stat2.setInt(6, r.getType());
                    stat2.addBatch();
                }
                stat2.executeBatch();
                stat2.close();
                conn.commit();
                conn.setAutoCommit(true);
                return new AbstractTransaction.Result();
            }
        };
    }

    private Callable<DBProxy.ServerResponse> execute(ImportTemporalDataTx tx) {
        return new MariaDBExecutorClient.Req() {
            @Override
            protected AbstractTransaction.Result executeQuery(Connection conn) throws Exception {
                conn.setAutoCommit(false);
                PreparedStatement stat = conn.prepareStatement("INSERT INTO temporal_status VALUES (?, ?, ?, ?, ?, ?, ?)");
                PreparedStatement preparedStatement = conn.prepareStatement("SELECT ts_id, st_time, status, travle_t, seg_cnt, MAX(st_time) FROM temporal_status WHERE r_id = ?");
                Statement statement = conn.createStatement();
                int index = 1;
                ResultSet rs = null;
                int tsId = 0, status = 0, travelT = 0, segCnt = 0;
                String stTime = null;
                for(ImportTemporalDataTx.StatusUpdate s : tx.data) {
                    stat.setInt(1, index++);
                    int rId = Math.toIntExact(s.getRoadId());
                    SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    String st = format.format(new Date(Long.parseLong(String.valueOf(s.getTime()))));
                    stat.setString(2, st);
                    stat.setString(3, "2037-12-31 00:00:00");
                    stat.setInt(4, rId);
                    // update endTime of last same r_id
                    preparedStatement.setInt(1, rId);
                    rs = preparedStatement.executeQuery();
                    if (rs.next()) { // delete the item and reinsert.
                        tsId = rs.getInt(1);
                        stTime = rs.getString(2);
                        status = rs.getInt(3);
                        travelT = rs.getInt(4);
                        segCnt = rs.getInt(5);
                        //st - 1 be the end_time
                        statement.execute("DELETE FROM temporal_status WHERE ts_id = " + tsId);
                        Date date = format.parse(st);
                        date.setTime(date.getTime() - 1000);
                        st = format.format(date);
                        statement.execute("INSERT INTO temporal_status VALUES(" + tsId + ", " + stTime + ", " + st + ", " + rId + ", " + status + ", " + travelT + ", " + segCnt + ")");
                        conn.commit();
                    }
                    stat.setInt(5, s.getJamStatus());
                    stat.setInt(6, s.getTravelTime());
                    stat.setInt(7, s.getSegmentCount());
                    stat.addBatch();
                }
                stat.executeBatch();
                stat.close();
                conn.commit();
                conn.setAutoCommit(true);
                return new AbstractTransaction.Result();
            }
        };
    }



    private Callable<DBProxy.ServerResponse> execute(UpdateTemporalDataTx tx) {
        return new MariaDBExecutorClient.Req() {
            @Override
            protected AbstractTransaction.Result executeQuery(Connection conn) throws Exception {
                conn.setAutoCommit(false);
                //UPDATE temporal_status FOR PORTION OF time_period FROM tx.getStartTime() TO tx.getEndTime() SET status = tx.getJamStatus(), travel_t = tx.getTravelTime(), seg_cnt = tx.getSegmentCount();
                String sql = "UPDATE temporal_status FOR PORTION OF time_period FROM " + tx.getStartTime() + " TO " + tx.getEndTime() +
                        "SET status = " + tx.getJamStatus() + ", travel_t = " + tx.getTravelTime() + ", seg_cnt = " + tx.getSegmentCount();
                conn.createStatement().execute(sql);
                conn.commit();
                conn.setAutoCommit(true);
                return new AbstractTransaction.Result();
            }
        };
    }

    private Callable<DBProxy.ServerResponse> execute(SnapshotAggrMaxTx tx) {
        return new MariaDBExecutorClient.Req() {
            @Override
            protected AbstractTransaction.Result executeQuery(Connection conn) throws Exception {
                MariaDBExecutorClient.AggrMaxSQL aggr = new MariaDBExecutorClient.AggrMaxSQL(conn, tx.getP());
                aggr.getAggrMaxSQL(tx.getT0(), tx.getT1());
                return new AbstractTransaction.Result();
            }
        };
    }

    private Callable<DBProxy.ServerResponse> execute(ReachableAreaQueryTx tx) {
        return new MariaDBExecutorClient.Req() {
            @Override
            protected AbstractTransaction.Result executeQuery(Connection conn) throws Exception {
                conn.setAutoCommit(true);
                EarliestArriveTime algo = new MariaDBExecutorClient.EarliestArriveTimeSQL(tx.getStartCrossId(), tx.getDepartureTime(), tx.getTravelTime(), conn);
                List<EarliestArriveTime.NodeCross> answer = new ArrayList<>(algo.run());
                answer.sort(Comparator.comparingLong(EarliestArriveTime.NodeCross::getId));
                ReachableAreaQueryTx.Result result = new ReachableAreaQueryTx.Result();
                result.setNodeArriveTime(answer);
                metrics.setReturnSize(answer.size());
                return result;
            }
        };
    }

    private Callable<DBProxy.ServerResponse> execute(SnapshotQueryTx tx) {
        return new MariaDBExecutorClient.Req() {
            @Override
            protected AbstractTransaction.Result executeQuery(Connection conn) throws Exception {
                conn.setAutoCommit(true);
                SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH-mm:ss");
                String snapshotTime = format.format(new Date((Long.parseLong((String.valueOf(tx.getTimestamp()))))));
                String sql = "SELECT MAX(en_time), status, travel_t, seg_cnt FROM temporal_status WHERE " + snapshotTime + " >= st_time GROUP BY r_id";
                ResultSet rs = conn.createStatement().executeQuery(sql);
                while(rs.next()) {
                    // nop
                }
                return new AbstractTransaction.Result();
            }
        };
    }

    private Callable<DBProxy.ServerResponse> execute(EntityTemporalConditionTx tx) {
        return new MariaDBExecutorClient.Req() {
            @Override
            protected AbstractTransaction.Result executeQuery(Connection conn) throws Exception{
                MariaDBExecutorClient.EntityTemporlSQL aggr = new MariaDBExecutorClient.EntityTemporlSQL(conn, tx.getP());
                aggr.getEntityTemporlSQL(tx.getT0(), tx.getT1(), tx.getVmin(), tx.getVmax());
                return new AbstractTransaction.Result();
            }
        };
    }

    private Callable<DBProxy.ServerResponse> execute(SnapshotAggrDurationTx tx) {
        return new MariaDBExecutorClient.Req() {
            @Override
            protected AbstractTransaction.Result executeQuery(Connection conn) throws Exception{
                MariaDBExecutorClient.AggrMaxDurationSQL aggr = new MariaDBExecutorClient.AggrMaxDurationSQL(conn, tx.getP());
                aggr.getAggrMaxDurationSQL(tx.getT0(), tx.getT1());
                return new AbstractTransaction.Result();
            }
        };
    }

    private Callable<DBProxy.ServerResponse> execute(NodeNeighborRoadTx tx) {
        return new MariaDBExecutorClient.Req() {
            @Override
            protected AbstractTransaction.Result executeQuery(Connection conn) throws Exception {
                conn.setAutoCommit(true);
                MariaDBExecutorClient.NodeNeighborRoadSQL algo = new MariaDBExecutorClient.NodeNeighborRoadSQL(conn);
                List<Long> answer = algo.getNeighborRoad(tx.getNodeId());
                NodeNeighborRoadTx.Result result = new NodeNeighborRoadTx.Result();
                result.setRoadIds(answer);
                metrics.setReturnSize(answer.size());
                return result;
            }
        };
    }

    private Callable<DBProxy.ServerResponse> execute(EarliestArriveTimeAggrTx tx) {
        return new MariaDBExecutorClient.Req() {
            @Override
            protected AbstractTransaction.Result executeQuery(Connection conn) throws Exception {
                conn.setAutoCommit(true);
                MariaDBExecutorClient.RoadEarliestArriveTimeSQL algo = new MariaDBExecutorClient.RoadEarliestArriveTimeSQL(conn);
                try {
                    return new EarliestArriveTimeAggrTx.Result(algo.getEarliestArriveTime(tx.getRoadId(), tx.getDepartureTime(), tx.getEndTime()));
                } catch(UnsupportedOperationException e){
                    return new EarliestArriveTimeAggrTx.Result(-1);
                }
            }
        };
    }

    private static class EarliestArriveTimeSQL extends EarliestArriveTime {
        private final PreparedStatement getEndNodeIdStat;
        private final MariaDBExecutorClient.NodeNeighborRoadSQL nodeNeighborRoadSQL;
        private final MariaDBExecutorClient.RoadEarliestArriveTimeSQL earliestTime;

        EarliestArriveTimeSQL(long startId, int startTime, int travelTime, Connection conn) throws SQLException {
            super(startId, startTime, travelTime);
            this.getEndNodeIdStat = conn.prepareStatement("SELECT r_end FROM road WHERE r_id = ?");
            this.nodeNeighborRoadSQL = new MariaDBExecutorClient.NodeNeighborRoadSQL(conn);
            this.earliestTime = new MariaDBExecutorClient.RoadEarliestArriveTimeSQL(conn);
        }

        @Override
        protected int getEarliestArriveTime(Long roadId, int departureTime) throws UnsupportedOperationException {
            return earliestTime.getEarliestArriveTime(roadId, departureTime, endTime);
        }

        @Override
        protected Iterable<Long> getAllOutRoads(long nodeId) {
            try {
                return nodeNeighborRoadSQL.getNeighborRoad(nodeId);
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
                if(rs.next()) {
                    return rs.getInt("r_end");
                } else {
                    throw new RuntimeException("road not found, should not happen!");
                }
            } catch (SQLException e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        }
    }

    private static class NodeNeighborRoadSQL{
        private final PreparedStatement getCrossOutRoadStat;
        private NodeNeighborRoadSQL(Connection conn) throws SQLException {
            this.getCrossOutRoadStat = conn.prepareStatement("SELECT r_id FROM road WHERE r_start = ?");
        }

        public List<Long> getNeighborRoad(long nodeId) throws SQLException {
            this.getCrossOutRoadStat.setInt(1, Math.toIntExact(nodeId));
            ResultSet rs = this.getCrossOutRoadStat.executeQuery();
            List<Long> result = new ArrayList<>();
            while(rs.next()) {
                result.add((long) rs.getInt("r_ id"));
            }
            return result;
        }
    }

    private static class AggrMaxSQL {
        private final PreparedStatement getStartTStat;
        private final Statement getMaxTemporalStat;
        private final String p;

        AggrMaxSQL(Connection con, String p) throws SQLException {
            this.getStartTStat = con.prepareStatement("SELECT MAX(en_time) as max_en_time, st_time, r_id, status, travel_t, seg_cnt FROM temporal_status WHERE ? >= st_time GROUP BY r_id;");
            this.p = p;
            this.getMaxTemporalStat = con.createStatement();
        }

        protected void getAggrMaxSQL(int start, int endTime) throws UnsupportedOperationException {
            try {
                this.getStartTStat.setInt(1, start);
                ResultSet rs = this.getStartTStat.executeQuery();
                while(rs.next()) {
                    Object ts = rs.getObject("max_en_time");
                    Object rId = rs.getObject("r_id");
                    if(ts == null || rId == null) continue;
                    //SELECT MAX(p) FROM temporal_status WHERE r_id = ? AND ((en_time <= endTime AND st_time >= start) OR (start >= st_time AND endTime <= en_time) OR (endTime >= st_time AND start <= st_time) OR (endTime >= en_time AND start <= en_time))
                    String sql = "SELECT MAX(" + this.p + ") FROM temporal_status WHERE r_id = " + rId + " AND ((en_time <= " + endTime + " AND st_time >= " + start + ") OR (" + start + " >= st_time AND " + endTime + " <= en_time) OR (" + endTime + " >= st_time AND " + start + " <= st_time) OR (" + endTime + " >= en_time AND " + start + " <= en_time))";
                    ResultSet maxP = this.getMaxTemporalStat.executeQuery(sql);
                }
            } catch (SQLException e) {
                e.printStackTrace();
                throw new UnsupportedOperationException(e);
            }
        }
    }

    private static class EntityTemporlSQL {
        private final PreparedStatement getStartTStat;
        private final Statement getTemporalStat;
        private final String p;

        EntityTemporlSQL(Connection con, String p) throws SQLException {
            this.getStartTStat = con.prepareStatement("SELECT MAX(en_time) as max_en_time, r_id FROM temporal_status WHERE ? >= st_time GROUP BY r_id");
            this.getTemporalStat = con.createStatement();
            this.p = p;
        }

        protected void getEntityTemporlSQL(int start, int endTime, int vmin, int vmax) throws UnsupportedOperationException {
            try {
                this.getStartTStat.setInt(1, start);
                ResultSet rs = this.getStartTStat.executeQuery();
                while(rs.next()) {
                    Object ts = rs.getObject("max_en_time");
                    Object rId = rs.getObject("r_id");
                    String sql = "SELECT p FROM temporal_status WHERE r_id = " + rId + " AND ((en_time <= endTime AND st_time >= start) OR (start >= st_time AND endTime <= en_time) OR (endTime >= st_time AND start <= st_time) OR (endTime >= en_time AND start <= en_time))";
                    if(ts == null || rId == null) continue;
                    ResultSet dup = this.getTemporalStat.executeQuery(sql);
                    boolean isSat = true;
                    while(dup.next()){
                        Object status = rs.getObject(p);
                        if(status == null) continue;
                        if((int)status > vmax || (int)status < vmin){
                            //don't save rid
                            isSat = false;
                            break;
                        }
                    }
                    if (isSat) {
                        //save rid
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
                throw new UnsupportedOperationException(e);
            }
        }
    }

    private static class AggrMaxDurationSQL {
        private final PreparedStatement getStartTStat;
        private final Statement getTemporalStat;
        private final String p;

        AggrMaxDurationSQL(Connection con, String p) throws SQLException {
            this.getStartTStat = con.prepareStatement("SELECT MAX(en_time) as max_en_time, r_id FROM temporal_status WHERE ? >= st_time GROUP BY r_id");
            this.getTemporalStat = con.createStatement();
            this.p = p;
        }

        protected void getAggrMaxDurationSQL(int start, int endTime) throws UnsupportedOperationException {
            try {
                this.getStartTStat.setInt(1, start);
                ResultSet rs = this.getStartTStat.executeQuery();
                while(rs.next()){
                    Object ts = rs.getObject("max_en_time");
                    Object rId = rs.getObject("r_id");
                    if(ts == null || rId == null) continue;
                    String sql = "SELECT p, st_time, en_time FROM temporal_status WHERE r_id = " + rId + " AND ((en_time <= endTime AND st_time >= start) OR (start >= st_time AND endTime <= en_time) OR (endTime >= st_time AND start <= st_time) OR (endTime >= en_time AND start <= en_time))";
                    ResultSet dup = this.getTemporalStat.executeQuery(sql);
                    while(dup.next()) {
                        Object t = rs.getObject("t");
                        Object status = rs.getObject(p);
                        String stTime = rs.getString("st_time");
                        String enTime = rs.getString("en_time");
                        if(t == null || status == null) continue;
                        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                        long lastTime = format.parse(enTime).getTime() - format.parse(stTime).getTime();
                    }
                }
            } catch (SQLException | ParseException e) {
                e.printStackTrace();
                throw new UnsupportedOperationException(e);
            }
        }
    }

    private static class RoadEarliestArriveTimeSQL {
        private final PreparedStatement getStartTStat;
        private final Statement getTemporalStat;

        RoadEarliestArriveTimeSQL(Connection con) throws SQLException {
            this.getStartTStat = con.prepareStatement("SELECT st_time, MAX(en_time) as max_en_time FROM temporal_status WHERE ? >= st_time and r_id = ?");
            this.getTemporalStat = con.createStatement();
        }

        protected int getEarliestArriveTime(long roadId, int departureTime, int endTime) throws UnsupportedOperationException {
            try {
                int startT = maxTimeLessOrEq(roadId, departureTime);
                SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH-mm-ss");
                ResultSet rs = getTT(roadId, startT, endTime);
                int minArriveTime = Integer.MAX_VALUE;
                int curT = departureTime;
                while(rs.next() && curT < minArriveTime) {
                    curT = (int)format.parse(rs.getString("st_time")).getTime();
                    int travelT = rs.getInt("travel_t");
                    if(curT < departureTime) {
                        minArriveTime = departureTime + travelT;
                    } else if(curT + travelT < minArriveTime) {
                        minArriveTime = curT + travelT;
                    }
                }
                if(minArriveTime != Integer.MAX_VALUE) return minArriveTime;
                else throw new UnsupportedOperationException();
            } catch (SQLException | ParseException e) {
                e.printStackTrace();
                throw new UnsupportedOperationException(e);
            }
        }

        private int maxTimeLessOrEq(long roadId, int time) throws SQLException, ParseException {
            this.getStartTStat.setInt(1, Math.toIntExact(roadId));
            this.getStartTStat.setInt(2, time);
            ResultSet rs = this.getStartTStat.executeQuery();
            if(rs.next()) {
                String result = rs.getString("st_time");
                if(result != null) {
                    SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH-mm-ss");
                    return (int)format.parse(result).getTime();
                }
                else throw new UnsupportedOperationException();
            } else {
                throw new UnsupportedOperationException();
            }
        }

        private ResultSet getTT(long roadId, int start, int end) throws SQLException {
            return this.getTemporalStat.executeQuery("SELECT st_time, en_time, travel_t FROM temporal_status WHERE r_id = " + roadId + " AND ((en_time <= end AND st_time >= start) OR (start >= st_time AND end <= en_time) OR (end >= st_time AND start <= st_time) OR (end >= en_time AND start <= en_time))");
        }

    }


    private abstract class Req implements Callable<DBProxy.ServerResponse> {
        private final TimeMonitor timeMonitor = new TimeMonitor();
        final AbstractTransaction.Metrics metrics = new AbstractTransaction.Metrics();

        private Req() {
            timeMonitor.begin("Wait in queue");
        }

        @Override
        public DBProxy.ServerResponse call() throws Exception {
            try {
                Connection conn = connectionPool.take();
                timeMonitor.mark("Wait in queue", "query");
                AbstractTransaction.Result result = executeQuery(conn);
                timeMonitor.end("query");
                if (result == null) throw new RuntimeException("[Got null. Server close connection]");
                connectionPool.put(conn);
                metrics.setWaitTime(Math.toIntExact(timeMonitor.duration("Wait in queue")));
                metrics.setSendTime(timeMonitor.beginT("query"));
                metrics.setExeTime(Math.toIntExact(timeMonitor.duration("query")));
                metrics.setConnId(connIdMap.get(conn));
                ServerResponse response = new ServerResponse();
                response.setMetrics(metrics);
                response.setResult(result);
                return response;
            } catch (Exception e) {
                e.printStackTrace();
                throw e;
            }
        }
        protected abstract AbstractTransaction.Result executeQuery(Connection conn) throws Exception;
    }


}
