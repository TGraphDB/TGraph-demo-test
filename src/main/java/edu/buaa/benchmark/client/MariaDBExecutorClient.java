//package edu.buaa.benchmark.client;
//
//import com.google.common.util.concurrent.ListenableFuture;
//import com.google.common.util.concurrent.ListeningExecutorService;
//import com.google.common.util.concurrent.MoreExecutors;
//import edu.buaa.algo.EarliestArriveTime;
//import edu.buaa.benchmark.transaction.*;
//import edu.buaa.model.StatusUpdate;
//import edu.buaa.utils.TimeMonitor;
//import org.apache.commons.lang3.tuple.Pair;
//import org.apache.commons.lang3.tuple.Triple;
//import org.neo4j.register.Register;
//import scala.Tuple4;
//
//import java.io.IOException;
//import java.sql.Connection;
//import java.sql.DriverManager;
//import java.sql.PreparedStatement;
//import java.sql.ResultSet;
//import java.sql.SQLException;
//import java.sql.Statement;
//import java.text.ParseException;
//import java.text.SimpleDateFormat;
//import java.util.*;
//import java.util.concurrent.BlockingQueue;
//import java.util.concurrent.Callable;
//import java.util.concurrent.LinkedBlockingQueue;
//import java.util.concurrent.ThreadPoolExecutor;
//import java.util.concurrent.TimeUnit;
//
//public class MariaDBExecutorClient implements DBProxy {
//
//    private ThreadPoolExecutor exe;
//    private BlockingQueue<Connection> connectionPool = new LinkedBlockingQueue<>();
//    private ListeningExecutorService service;
//    private Map<Connection, Integer> connIdMap = new HashMap<>();
//
//
//    public MariaDBExecutorClient(String serverHost, int parallelCnt, int queueLength) throws IOException, ClassNotFoundException, SQLException {
//        Class.forName("org.mariadb.jdbc.Driver");
//        String dbURL = "jdbc:mariadb://" + serverHost + ":3306";//; DatabaseName=sample
//        this.exe = new ThreadPoolExecutor(parallelCnt, parallelCnt, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(queueLength), (r, executor) -> {
//            if (!executor.isShutdown()) {
//                try {
//                    executor.getQueue().put(r);
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
//            }
//        });
//        exe.prestartAllCoreThreads();
//        this.service = MoreExecutors.listeningDecorator(exe);
//        for(int i = 0; i< parallelCnt; i++) {
//            Connection conn = DriverManager.getConnection(dbURL, "root", "root");
//            this.connectionPool.offer(conn);
//            connIdMap.put(conn, i);
//        }
//    }
//
//    @Override
//    public String testServerClientCompatibility() throws UnsupportedOperationException {
//        return null;
//    }
//
//    @Override
//    public ListenableFuture<DBProxy.ServerResponse> execute(AbstractTransaction tx) {
//        switch (tx.getTxType()) {
//            case tx_import_static_data:
//                return this.service.submit(execute((ImportStaticDataTx) tx));
//            case tx_import_temporal_data:
//                return this.service.submit(execute((ImportTemporalDataTx) tx));
//            case tx_query_reachable_area:
//                return this.service.submit(execute((ReachableAreaQueryTx) tx));
////            case tx_query_road_earliest_arrive_time_aggr:
////                return this.service.submit(execute((EarliestArriveTimeAggrTx)tx));
//            case tx_query_node_neighbor_road:
//                return this.service.submit(execute((NodeNeighborRoadTx) tx));
//            default:
//                throw new UnsupportedOperationException();
//        }
//    }
//
//    @Override
//    public void createDB() throws IOException {
//
//        try {
//            Connection con = connectionPool.take();
//            Statement stmt = con.createStatement();
//            con.setAutoCommit(true);
//            stmt.execute("CREATE DATABASE beijing_traffic");
//            stmt.execute("USE beijing_traffic");
//            stmt.execute("CREATE TABLE cross_node(cn_id INT PRIMARY KEY, name CHAR(255))");
//            stmt.execute("CREATE TABLE road(r_id INT PRIMARY KEY, r_name CHAR(16), r_start INT, r_end INT, r_length INT, r_type INT)");
//            stmt.execute("CREATE TABLE temporal_status(ts_id INT PRIMARY KEY, st_time TIMESTAMP(0), en_time TIMESTAMP(0), r_id INT, status INT, travel_t INT, seg_cnt INT, PERIOD FOR time_period(st_time, en_time))");
//            //time format : timestamp(0) xxxx-xx-xx xx:xx:xx
//            //transform int to datetime while inserting or updating
//            stmt.close();
//            connectionPool.put(con);
//        } catch (SQLException | InterruptedException ex) {
//            ex.printStackTrace();
//            throw new IOException(ex);
//        }
//
//    }
//
//    @Override
//    public void restartDB() throws IOException {
//
//    }
//
//    @Override
//    public void shutdownDB() throws IOException {
//
//    }
//
//    @Override
//    public void close() throws IOException, InterruptedException {
//        try {
//            service.shutdown();
//            while(!service.isTerminated()) {
//                service.awaitTermination(10, TimeUnit.SECONDS);
//                long completeCnt = exe.getCompletedTaskCount();
//                int remains = exe.getQueue().size();
//                System.out.println( completeCnt+"/"+ (completeCnt+remains)+" query completed.");
//            }
//            while(!connectionPool.isEmpty()){
//                Connection conn = connectionPool.take();
//                conn.close();
//            }
//            System.out.println("Client exit. send "+ exe.getCompletedTaskCount() +" lines.");
//        } catch (SQLException e) {
//            e.printStackTrace();
//            throw new IOException(e);
//        }
//    }
//
//    private Callable<DBProxy.ServerResponse> execute(ImportStaticDataTx tx) {
//        return new MariaDBExecutorClient.Req() {
//            @Override
//            protected AbstractTransaction.Result executeQuery(Connection conn) throws Exception {
//                conn.setAutoCommit(false);
//                PreparedStatement stat1 = conn.prepareStatement("INSERT INTO cross_node VALUES (?, ?)");
//                for(ImportStaticDataTx.StaticCrossNode p : tx.getCrosses()){
//                    stat1.setInt(1, Math.toIntExact(p.getId()));
//                    stat1.setString(2, p.getName());
//                    stat1.addBatch();
//                }
//                stat1.executeBatch();
//                stat1.close();
//                PreparedStatement stat2 = conn.prepareStatement("INSERT INTO road VALUES (?, ?, ?, ?, ?, ?)");
//                for(ImportStaticDataTx.StaticRoadRel r : tx.getRoads()){
//                    stat2.setInt(1, Math.toIntExact(r.getRoadId()));
//                    stat2.setString(2, r.getId());
//                    stat2.setInt(3, Math.toIntExact(r.getStartCrossId()));
//                    stat2.setInt(4, Math.toIntExact(r.getEndCrossId()));
//                    stat2.setInt(5, r.getLength());
//                    stat2.setInt(6, r.getType());
//                    stat2.addBatch();
//                }
//                stat2.executeBatch();
//                stat2.close();
//                conn.commit();
//                conn.setAutoCommit(true);
//                return new AbstractTransaction.Result();
//            }
//        };
//    }
//
//    private Callable<DBProxy.ServerResponse> execute(ImportTemporalDataTx tx) {
//        return new MariaDBExecutorClient.Req() {
//            @Override
//            protected AbstractTransaction.Result executeQuery(Connection conn) throws Exception {
//                conn.setAutoCommit(false);
//                PreparedStatement stat = conn.prepareStatement("INSERT INTO temporal_status VALUES (?, ?, ?, ?, ?, ?, ?)");
//                PreparedStatement preparedStatement = conn.prepareStatement("SELECT ts_id, st_time, status, travle_t, seg_cnt, MAX(st_time) FROM temporal_status WHERE r_id = ?");
//                Statement statement = conn.createStatement();
//                int index = 1;
//                ResultSet rs = null;
//                int tsId = 0, status = 0, travelT = 0, segCnt = 0;
//                String stTime = null;
//                for(StatusUpdate s : tx.data) {
//                    stat.setInt(1, index++);
//                    int rId = Math.toIntExact(Long.valueOf(s.getRoadId()));
//                    String st = timestamp2Datetime(s.getTime());
//                    stat.setString(2, st);
//                    stat.setString(3, "2037-12-31 00:00:00");
//                    stat.setInt(4, rId);
//                    // update endTime of last same r_id
//                    preparedStatement.setInt(1, rId);
//                    rs = preparedStatement.executeQuery();
//                    SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
//                    if (rs.next()) { // delete the item and reinsert.
//                        tsId = rs.getInt(1);
//                        stTime = rs.getString(2);
//                        status = rs.getInt(3);
//                        travelT = rs.getInt(4);
//                        segCnt = rs.getInt(5);
//                        //st - 1 be the end_time
//                        statement.execute("DELETE FROM temporal_status WHERE ts_id = " + tsId);
//                        Date date = format.parse(st);
//                        date.setTime(date.getTime() - 1000);
//                        st = format.format(date);
//                        statement.execute("INSERT INTO temporal_status VALUES(" + tsId + ", " + stTime + ", " + st + ", " + rId + ", " + status + ", " + travelT + ", " + segCnt + ")");
//                        conn.commit();
//                    }
//                    stat.setInt(5, s.getJamStatus());
//                    stat.setInt(6, s.getTravelTime());
//                    stat.setInt(7, s.getSegmentCount());
//                    stat.addBatch();
//                }
//                stat.executeBatch();
//                stat.close();
//                conn.commit();
//                conn.setAutoCommit(true);
//                return new AbstractTransaction.Result();
//            }
//        };
//    }
//
//
//
//    private Callable<DBProxy.ServerResponse> execute(UpdateTemporalDataTx tx) {
//        return new MariaDBExecutorClient.Req() {
//            @Override
//            protected AbstractTransaction.Result executeQuery(Connection conn) throws Exception {
//                conn.setAutoCommit(false);
//                //UPDATE temporal_status FOR PORTION OF time_period FROM tx.getStartTime() TO tx.getEndTime() SET status = tx.getJamStatus(), travel_t = tx.getTravelTime(), seg_cnt = tx.getSegmentCount();
//                String sql = "UPDATE temporal_status FOR PORTION OF time_period FROM " + timestamp2Datetime(tx.getStartTime()) + " TO " + timestamp2Datetime(tx.getEndTime()) +
//                        " SET status = " + tx.getJamStatus() + ", travel_t = " + tx.getTravelTime() + ", seg_cnt = " + tx.getSegmentCount();
//                conn.createStatement().execute(sql);
//                conn.commit();
//                conn.setAutoCommit(true);
//                return new AbstractTransaction.Result();
//            }
//        };
//    }
//
//    private Callable<DBProxy.ServerResponse> execute(SnapshotQueryTx tx) {
//        return new MariaDBExecutorClient.Req() {
//            @Override
//            protected AbstractTransaction.Result executeQuery(Connection conn) throws Exception {
//                conn.setAutoCommit(true);
//                String snapshotTime = timestamp2Datetime(tx.getTimestamp());
//                String sql = "SELECT r_id, MAX(en_time), status, travel_t, seg_cnt FROM temporal_status WHERE " + snapshotTime + " >= st_time GROUP BY r_id";
//                ResultSet rs = conn.createStatement().executeQuery(sql);
//                List<Pair<Long, Integer>> res = new ArrayList<>();
//                while (rs.next()){
//                    long rId = rs.getInt("r_id");
//                    int val = rs.getInt(tx.getPropertyName());
//                    res.add(Pair.of(rId, val));
//                }
//                SnapshotQueryTx.Result result = new SnapshotQueryTx.Result();
//                result.setRoadStatus(res);
//                return result;
//            }
//        };
//    }
//
//    private Callable<DBProxy.ServerResponse> execute(SnapshotAggrMaxTx tx) {
//        return new MariaDBExecutorClient.Req() {
//            @Override
//            protected AbstractTransaction.Result executeQuery(Connection conn) throws Exception {
//                conn.setAutoCommit(false);
//                // SELECT MAX(p) FROM temporal_status WHERE en_time >= tx.getT0() AND st_time <= tx.getT1() GROUP BY r_id;
//                String sql = "SELECT MAX(?) as max_v, r_id FROM temporal_status WHERE en_time >= ? AND st_time <= ? GROUP BY r_id";
//                PreparedStatement preparedStatement = conn.prepareStatement(sql);
//                preparedStatement.setString(1, tx.getP());
//                preparedStatement.setString(2, timestamp2Datetime(tx.getT0()));
//                preparedStatement.setString(3, timestamp2Datetime(tx.getT1()));
//                ResultSet rs = preparedStatement.executeQuery();
//                List<Pair<Long, Integer>> res = new ArrayList<>();
//                while (rs.next()) {
//                    res.add(Pair.of((long)rs.getInt("r_id"), rs.getInt("max_v")));
//
//                }
//                SnapshotAggrMaxTx.Result result = new SnapshotAggrMaxTx.Result();
//                result.setRoadTravelTime(res);
//                return result;
//            }
//        };
//    }
//
//    private Callable<DBProxy.ServerResponse> execute(SnapshotAggrDurationTx tx) {
//        return new MariaDBExecutorClient.Req() {
//            @Override
//            protected AbstractTransaction.Result executeQuery(Connection conn) throws Exception {
//                // SELECT r_id, st_time, en_time, status FROM temporal_status WHERE en_time >= tx.getT0() AND st_time <= tx.getT1();
//                String sql = "SELECT r_id, st_time, en_time FROM temporal_status WHERE en_time >= ? AND st_time <= ?";
//                PreparedStatement preparedStatement = conn.prepareStatement(sql);
//                preparedStatement.setString(1, timestamp2Datetime(tx.getT0()));
//                preparedStatement.setString(2, timestamp2Datetime(tx.getT1()));
//                ResultSet rs = preparedStatement.executeQuery();
//                List<Triple<Long, Integer, Integer>> res = new ArrayList<>();
//                int rId, status, duration, stTime, enTime;
//                int t0 = tx.getT0(), t1 = tx.getT1();
//                while(rs.next()) {
//                    rId = rs.getInt("r_id");
//                    status = rs.getInt("status");
//                    stTime = datetime2Int(rs.getString("st_time"));
//                    enTime = datetime2Int((rs.getString("en_time")));
//                    if (t0 > stTime) {
//                        if (t1 <= enTime) {
//                            duration = t1 - t0;
//                        } else {
//                            duration = enTime - t0;
//                        }
//                    } else {
//                        if (t1 <= enTime) {
//                            duration = t1 - stTime;
//                        } else {
//                            duration = enTime - stTime;
//                        }
//                    }
//                    res.add(Triple.of((long)rId, status, duration));
//                }
//                SnapshotAggrDurationTx.Result result = new SnapshotAggrDurationTx.Result();
//                result.setRoadStatDuration(res);
//
//                return result;
//            }
//        };
//    }
//
//
//    private Callable<DBProxy.ServerResponse> execute(EntityTemporalConditionTx tx) {
//        return new MariaDBExecutorClient.Req() {
//            @Override
//            protected AbstractTransaction.Result executeQuery(Connection conn) throws Exception {
//                // SELECT r_id FROM temporal_status WHERE en_time >= tx.getT0() AND st_time <= tx.getT1() AND travel_t >= tx.getVmin() AND travel_t <= tx.getVmax();
//                String sql = "SELECT r_id FROM temporal_status WHERE en_time >= ? AND st_time <= ? AND travel_t >= ? AND travel_t <= ?";
//                PreparedStatement preparedStatement = conn.prepareStatement(sql);
//                preparedStatement.setString(1, timestamp2Datetime(tx.getT0()));
//                preparedStatement.setString(2, timestamp2Datetime(tx.getT1()));
//                preparedStatement.setInt(3, tx.getVmin());
//                preparedStatement.setInt(4, tx.getVmax());
//                ResultSet rs = preparedStatement.executeQuery();
//                List<Long> res = new ArrayList<>();
//                while (rs.next()) {
//                    res.add((long)rs.getInt("r_id"));
//                }
//                EntityTemporalConditionTx.Result result = new EntityTemporalConditionTx.Result();
//                result.setRoads(res);
//                return result;
//            }
//        };
//    }
//
//    private Callable<DBProxy.ServerResponse> execute(NodeNeighborRoadTx tx) {
//        return new MariaDBExecutorClient.Req() {
//            @Override
//            protected AbstractTransaction.Result executeQuery(Connection conn) throws Exception {
//                conn.setAutoCommit(true);
//                // SELECT r_id FROM road WHERE r_start = tx.getNodeId() OR r_end = tx.getNodeId();
//                String sql = "SELECT r_id FROM road WHERE r_start = " + tx.getNodeId() + " OR r_end = " + tx.getNodeId();
//                Statement statement = conn.createStatement();
//                ResultSet rs = statement.executeQuery(sql);
//                List<Long> res = new ArrayList<>();
//                while(rs.next()) {
//                    res.add((long)rs.getInt("r_id"));
//                }
//                NodeNeighborRoadTx.Result result = new NodeNeighborRoadTx.Result();
//                result.setRoadIds(res);
//                metrics.setReturnSize(res.size());
//                return result;
//            }
//        };
//    }
//
//
//    private Callable<DBProxy.ServerResponse> execute(ReachableAreaQueryTx tx) {
//        return new MariaDBExecutorClient.Req() {
//            @Override
//            protected AbstractTransaction.Result executeQuery(Connection conn) throws Exception {
//                conn.setAutoCommit(true);
//                //cross_id, cost_time, remain_time, cur_time
//                PriorityQueue<Tuple4<Integer, Integer, Integer, Integer>> que = new PriorityQueue<>(Comparator.comparingInt(Tuple4::_2));
//                //cross_id, arrive_time
//                HashMap<Integer, Integer> dis = new HashMap<>();
//                int curCrossId = (int)tx.getStartCrossId(), cost;
//                int currentTime = tx.getDepartureTime(), remainTime = tx.getTravelTime();
//                int rId, endId, travelTime;
//                que.add(Tuple4.apply(curCrossId, 0, remainTime, currentTime));
//                Tuple4<Integer, Integer, Integer, Integer> top;
//                ResultSet rs;
//                // r_id, r_start, r_end, travel_t_at_the_current_time
//                List<Tuple4<Integer, Integer, Integer, Integer>> currentPath = new ArrayList<>();
//                while (!que.isEmpty()) {
//                    currentPath.clear();
//                    top = que.poll();
//                    curCrossId = top._1();
//                    cost = top._2();
//                    remainTime = top._3();
//                    currentTime = top._4();
//                    rs = conn.createStatement().executeQuery("SELECT r_id, r_end FROM road WHERE r_start = " + curCrossId);
//                    while (rs.next()) {
//                        rId = rs.getInt("r_id");
//                        endId = rs.getInt("r_end");
//                        travelTime = leastTimeFromStartToEnd(rId, currentTime, currentTime + remainTime, conn);
//                        if (remainTime >= travelTime) {
//                            currentPath.add(Tuple4.apply(rId, curCrossId, endId, travelTime));
//                        }
//                    }
//                    for (Tuple4<Integer, Integer, Integer, Integer> item : currentPath) {
//                        if ((dis.get(item._3()) == null) || dis.get(item._3()) > cost + item._4()) {
//                            dis.put(item._3(), cost + item._4());
//                            que.add(Tuple4.apply(item._3(), cost + item._4(), remainTime - item._4(), currentTime + item._4()));
//                        }
//                    }
//
//                }
//                ReachableAreaQueryTx.Result result = new ReachableAreaQueryTx.Result();
//                List<EarliestArriveTime.NodeCross> ans = new ArrayList<>();
//                dis.forEach((k, v) -> ans.add(new EarliestArriveTime.NodeCross(k, v)));
//                result.setNodeArriveTime(ans);
//                metrics.setReturnSize(ans.size());
//                return result;
//            }
//        };
//    }
//
//    private static int leastTimeFromStartToEnd(int rId, int dePartureTime, int endTime, Connection conn) throws SQLException, ParseException {
//        conn.setAutoCommit(true);
//        String sql = "SELECT st_time, en_time, travel_t FROM temporal_status WHERE r_id = ? AND en_time >= ? AND st_time <= ?";
//        PreparedStatement preparedStatement = conn.prepareStatement(sql);
//        preparedStatement.setInt(1, rId);
//        preparedStatement.setString(2, timestamp2Datetime(dePartureTime));
//        preparedStatement.setString(3, timestamp2Datetime(endTime));
//        int stTime, enTime, travelTime;
//        ResultSet rs = preparedStatement.executeQuery();
//        int ret = Integer.MAX_VALUE, tmp;
//        while (rs.next()) {
//            stTime = datetime2Int(rs.getString("st_time"));
//            enTime = datetime2Int(rs.getString("en_time"));
//            tmp = rs.getInt("travel_t");
//            travelTime = Integer.MAX_VALUE;
//            if (stTime + tmp > enTime) continue;
//            if (dePartureTime > stTime && enTime < endTime) {
//                // no need to wait
//                travelTime = tmp;
//            } else if (stTime >= dePartureTime && enTime <= endTime) {
//                travelTime = tmp + stTime - dePartureTime;
//            } else if (stTime > dePartureTime && endTime < enTime) {
//                if (stTime + tmp <= endTime)
//                    travelTime = tmp + stTime - dePartureTime;
//            }
//            if (ret > travelTime) ret = travelTime;
//
//        }
//        return ret;
//    }
//
//    private abstract class Req implements Callable<DBProxy.ServerResponse> {
//        private final TimeMonitor timeMonitor = new TimeMonitor();
//        final AbstractTransaction.Metrics metrics = new AbstractTransaction.Metrics();
//
//        private Req() {
//            timeMonitor.begin("Wait in queue");
//        }
//
//        @Override
//        public DBProxy.ServerResponse call() throws Exception {
//            try {
//                Connection conn = connectionPool.take();
//                timeMonitor.mark("Wait in queue", "query");
//                AbstractTransaction.Result result = executeQuery(conn);
//                timeMonitor.end("query");
//                if (result == null) throw new RuntimeException("[Got null. Server close connection]");
//                connectionPool.put(conn);
//                metrics.setWaitTime(Math.toIntExact(timeMonitor.duration("Wait in queue")));
//                metrics.setSendTime(timeMonitor.beginT("query"));
//                metrics.setExeTime(Math.toIntExact(timeMonitor.duration("query")));
//                metrics.setConnId(connIdMap.get(conn));
//                ServerResponse response = new ServerResponse();
//                response.setMetrics(metrics);
//                response.setResult(result);
//                return response;
//            } catch (Exception e) {
//                e.printStackTrace();
//                throw e;
//            }
//        }
//        protected abstract AbstractTransaction.Result executeQuery(Connection conn) throws Exception;
//    }
//
//    private static String timestamp2Datetime(int timestamp) {
//        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
//        return format.format(new Date(Long.parseLong(String.valueOf(timestamp * 1000))));
//    }
//
//    private static int datetime2Int(String date) throws ParseException {
//        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
//        return (int)(format.parse(date).getTime() / 1000);
//    }
//
//
//}
