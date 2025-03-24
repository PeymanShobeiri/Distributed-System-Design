package Server;

import Logger.Logger;
import com.web.service.implimentation.ShareMarketImplementation;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import javax.xml.ws.Endpoint;

public class ServerInstance {

    private String serverID;
    private String serverName;
    private String serverEndpoint;
    private int serverUdpPort;

    public ServerInstance(String serverID, String[] args) throws Exception {
        this.serverID = serverID;

        switch (serverID.toUpperCase()) {
            case "NYK":
                serverName    = ShareMarketImplementation.MARKET_SERVER_NEWYORK;
                serverUdpPort = ShareMarketImplementation.NYK_PORT;
                serverEndpoint = "http://localhost:8080/newyork";
                break;
            case "LON":
                serverName    = ShareMarketImplementation.MARKET_SERVER_LONDON;
                serverUdpPort = ShareMarketImplementation.LON_PORT;
                serverEndpoint = "http://localhost:8080/london";
                break;
            case "TOK":
                serverName    = ShareMarketImplementation.MARKET_SERVER_TOKYO;
                serverUdpPort = ShareMarketImplementation.TOK_PORT;
                serverEndpoint = "http://localhost:8080/tokyo";
                break;
            default:
                throw new IllegalArgumentException("Unknown server ID: " + serverID);
        }

        try {
            System.out.println(serverName + " Server Started...");
            Logger.serverLog(serverID, " Server Started...");
            ShareMarketImplementation service = new ShareMarketImplementation(serverID, serverName);

            Endpoint endpoint = Endpoint.publish(serverEndpoint, service);

            System.out.println(serverName + " Server is Up & Running");
            Logger.serverLog(serverID, " Server is Up & Running");

            Runnable task = () -> {
                listenForRequest(service, serverUdpPort, serverName, serverID);
            };
            Thread thread = new Thread(task);
            thread.start();

        }
        catch (Exception e) {
            e.printStackTrace(System.out);
            Logger.serverLog(serverID, "Exception in ServerInstance: " + e.getMessage());
        }
//        System.out.println(serverName + " Server Shutting down");
        Logger.serverLog(serverID, "Server Shutting down");
    }


    private static void listenForRequest(ShareMarketImplementation servant, int serverUdpPort, String serverName, String serverID) {
        DatagramSocket socket = null;
        try {
            socket = new DatagramSocket(serverUdpPort);
            byte[] buffer = new byte[1000];
            System.out.println(serverName + " UDP Server started at port " + socket.getLocalPort() + " ...");
            Logger.serverLog(serverID, "UDP Server started at port " + socket.getLocalPort());

            while (true) {
                DatagramPacket request = new DatagramPacket(buffer, buffer.length);
                socket.receive(request);
                String sentence = new String(request.getData(), 0, request.getLength());
                String[] parts = sentence.split(";");
                String method    = parts[0];
                String buyerID   = parts[1];
                String shareType = parts[2];
                String raw       = parts[3];
                String result = "";
                switch (method.toLowerCase()) {
                    case "listshareavailability":
                        Logger.serverLog(serverID, buyerID, "UDP " + method, "shareType=" + shareType, "...");
                        result = servant.listShareAvailabilityUDP(shareType);
                        break;

                    case "purchaseshare":
                        Logger.serverLog(serverID, buyerID, "UDP " + method, "shareType=" + shareType + ", raw=" + raw, "...");
                        String[] arrPur = raw.split("-");
                        if (arrPur.length == 2) {
                            String shareID = arrPur[0];
                            int shareCount = Integer.parseInt(arrPur[1]);
                            result = servant.purchaseShare(buyerID, shareID, shareType, shareCount);
                        }
                        else {
                            result = servant.purchaseShare(buyerID, raw, shareType, 1);
                        }
                        break;

                    case "sellshare":
                        Logger.serverLog(serverID, buyerID, "UDP " + method, "shareType=" + shareType + ", raw=" + raw, "...");
                        String[] arrSell = raw.split("-");
                        if (arrSell.length == 2) {
                            String sID = arrSell[0];
                            int sCount = Integer.parseInt(arrSell[1]);
                            result = servant.sellShare(buyerID, sID, sCount);
                        }
                        else {
                            result = servant.sellShare(buyerID, raw, 1);
                        }
                        break;

                    default:
                        result = "ERROR: Unknown UDP method " + method;
                        break;
                }

                String sendingResult = result + ";";
                byte[] sendData = sendingResult.getBytes();
                DatagramPacket reply = new DatagramPacket(sendData, sendData.length, request.getAddress(), request.getPort());
                socket.send(reply);
                Logger.serverLog(serverID, buyerID, "UDP reply " + method, "shareType=" + shareType + ", raw=" + raw, sendingResult);
            }

        }
        catch (SocketException e) {
            System.err.println("SocketException in listenForRequest: " + e.getMessage());
            e.printStackTrace(System.out);
        }
        catch (IOException e) {
            System.err.println("IOException in listenForRequest: " + e.getMessage());
            e.printStackTrace(System.out);
        }
        finally {
            if (socket != null) socket.close();
        }
    }
}
