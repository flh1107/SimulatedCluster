package com.wrox;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.websocket.*;
import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * @Project: SimulatedCluster
 * @Author: fanlihua
 * @Date: 2018/1/30
 */

@ClientEndpoint
public class ClusterNodeServlet extends HttpServlet {

    private String nodeId;
    private Session session;

    @Override
    public void init() throws ServletException {
        nodeId = this.getInitParameter("nodeId");
        String path = this.getServletContext().getContextPath() + "/clusterNode/" + this.nodeId;
        try {
            URI uri = new URI("ws", "localhost:8080", path,
                    null, null);
            session = ContainerProvider.getWebSocketContainer().connectToServer(this, uri);
        } catch (URISyntaxException | IOException | DeploymentException e) {
            throw new ServletException("can not connect to " + path + "." + e);
        }
    }

    @Override
    public void destroy() {
        try {
            session.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        ClusterMessage message = new ClusterMessage(this.nodeId, " request: {IP: " + req.getRemoteAddr() +
                ", queryString: \"" + req.getQueryString() + "\"}");
        try(OutputStream outputStream = this.session.getBasicRemote().getSendStream();
        ObjectOutputStream objectOutputStream = new ObjectOutputStream(outputStream)) {
            objectOutputStream.writeObject(message);
        }

        resp.getWriter().append("OK");
    }


    @OnMessage
    public void onMessage(InputStream input) {
        try {
            ObjectInputStream objectInputStream = new ObjectInputStream(input);
            ClusterMessage message = (ClusterMessage) objectInputStream.readObject();
            System.out.println("[INFO] Node " + this.nodeId + " received message: Node " +
                    message.getNodeId() + " " + message.getMessage());
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    @OnClose
    public void onClose(Session session, CloseReason closeReason) {
        CloseReason.CloseCode closeCode = closeReason.getCloseCode();
        if (closeCode != CloseReason.CloseCodes.NORMAL_CLOSURE) {
            System.err.println("[ERROR] WebSocket connection closed unexpectedly: code= " + closeCode +
                    ", reason= " + closeReason.getReasonPhrase());
        }
    }


}
