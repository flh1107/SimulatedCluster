package com.wrox;

import javax.websocket.*;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * @Project: SimulatedCluster
 * @Author: fanlihua
 * @Date: 2018/1/30
 */

@ServerEndpoint("/clusterNode/{nodeId}")
public class ClusterNodeEndPoint {

    private static final List<Session> nodes = new ArrayList<>(2);

    @OnOpen
    public void onOpen(Session session, @PathParam("nodeId") String nodeId) {
        System.out.println("[INFO]: Node " + nodeId + " connected to cluster.");

        ClusterMessage message = new ClusterMessage(nodeId, "joined the cluster");
        try {
            byte[] messageBytes = ClusterNodeEndPoint.toArrayByte(message);
            for (Session node : nodes) {
                node.getBasicRemote().sendBinary(ByteBuffer.wrap(messageBytes));
            }
        } catch (IOException e) {
            System.err.println("ERROR: Exception when notifying of new node");
            e.printStackTrace();
        }
        ClusterNodeEndPoint.nodes.add(session);
    }

    @OnMessage
    public void onMessage(Session session, byte[] message) {
        try {
            for (Session node : this.nodes) {
                if (node != session) {
                    node.getBasicRemote().sendBinary(ByteBuffer.wrap(message));
                }
            }
        } catch (IOException e) {
            System.err.println("ERROR: Exception when handling message on server.");
            e.printStackTrace();
        }

    }

    @OnClose
    public void onClose(Session session, @PathParam("nodeId") String nodeId) {
        System.out.println("[INFO] Node " + nodeId+ "disconnected.");
        ClusterNodeEndPoint.nodes.remove(session);

        ClusterMessage message = new ClusterMessage(nodeId, "Left the cluster");
        try {
            byte[] messageBytes = ClusterNodeEndPoint.toArrayByte(message);
            for (Session node : nodes) {
                session.getBasicRemote().sendBinary(ByteBuffer.wrap(messageBytes));
            }
        } catch (IOException e) {
            System.err.println("Error: Exception when notifying of left node.");
            e.printStackTrace();
        }

    }

    private static byte[] toArrayByte(ClusterMessage message) throws IOException {
        try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
             ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream)) {
            objectOutputStream.writeObject(message);
            return byteArrayOutputStream.toByteArray();
        }
    }
}
