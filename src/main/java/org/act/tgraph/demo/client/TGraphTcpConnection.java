package org.act.tgraph.demo.client;

import org.act.tgraph.demo.utils.TimeMonitor;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class TGraphTcpConnection {
    private Socket conn;
    private BufferedReader in;
    private PrintWriter out;

    public TGraphTcpConnection(String serverHost, int port) throws IOException {
        this.conn = new Socket(serverHost, port);
//            client.setSoTimeout(8000);
        this.conn.setTcpNoDelay(true);
        this.in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        this.out = new PrintWriter(conn.getOutputStream(), true);
    }

    public void close() throws IOException {
        in.close();
        out.close();
        conn.close();
    }

    public String call(String query, TimeMonitor timeMonitor) throws IOException {
        timeMonitor.begin("Send");
        out.println(query);
        timeMonitor.mark("Send", "Wait result");
        String result = in.readLine();
        timeMonitor.end("Wait result");
        return result;
    }
}
