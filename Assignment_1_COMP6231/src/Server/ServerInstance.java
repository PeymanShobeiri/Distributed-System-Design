package Server;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import ServerImplementation.ShareMarketImplementation;
import Logger.Logger;


public class ServerInstance {

    public static final int SERVER_NEWYORK = 3001;
    public static final int SERVER_LONDON  = 3002;
    public static final int SERVER_TOKYO   = 3003;
    public static final String SHARE_MARKET_REGISTERED_NAME = "SHARE_MARKET";
    private final String serverID;
    private String serverName;
    private int serverRegistryPort;
    private int serverUdpPort;

    public ServerInstance(String serverID) throws Exception {
        this.serverID = serverID;

        switch (serverID) {
            case "NYK":
                serverName = ShareMarketImplementation.MARKET_SERVER_NEWYORK;
                serverRegistryPort = SERVER_NEWYORK;
                serverUdpPort = ShareMarketImplementation.NYK_PORT;
                break;
            case "LON":
                serverName = ShareMarketImplementation.MARKET_SERVER_LONDON;
                serverRegistryPort = SERVER_LONDON;
                serverUdpPort = ShareMarketImplementation.LON_PORT;
                break;
            case "TOK":
                serverName = ShareMarketImplementation.MARKET_SERVER_TOKYO;
                serverRegistryPort = SERVER_TOKYO;
                serverUdpPort = ShareMarketImplementation.TOK_PORT;
                break;
            default:
                throw new IllegalArgumentException("Unknown server id: " + serverID);
        }

        ShareMarketImplementation remoteObject = new ShareMarketImplementation(serverID, serverName);
        Registry registry = LocateRegistry.createRegistry(serverRegistryPort);
        registry.bind(SHARE_MARKET_REGISTERED_NAME, remoteObject);
        System.out.println(serverName + " Server is Up & Running on RMI port " + serverRegistryPort);
        Logger.serverLog(serverID, serverName + " Server is Up & Running");
        Runnable task = () -> listenForRequest(remoteObject, serverUdpPort, serverName, serverID);
        Thread t = new Thread(task);
        t.start();
    }


    private static void listenForRequest(ShareMarketImplementation obj, int serverUdpPort, String serverName, String serverID) {
        DatagramSocket socket = null;
        try {
            socket = new DatagramSocket(serverUdpPort);
            System.out.println(serverName + " UDP Server Started at port " + serverUdpPort);
            Logger.serverLog(serverID, "UDP Server Started at port " + serverUdpPort);

            byte[] buffer = new byte[1000];

            while (true) {
                DatagramPacket request = new DatagramPacket(buffer, buffer.length);
                socket.receive(request);
                String sentence = new String(request.getData(), 0, request.getLength()).trim();
//                System.out.println("our sentence is : " + sentence);
                String[] parts = sentence.split(";");
                String method   = parts[0];
                String callerID = parts[1];
                String shareType = parts[2];
                String extra = "";
                if (parts.length > 3){
                    extra = parts[3];
                }
                Logger.serverLog(serverID, callerID, "UDP request received: " + method, "shareType=" + shareType + ", extra=" + extra, "...");

                String result;
                switch (method.toLowerCase()) {
                    case "listshareavailability":
                        result = obj.listShareAvailabilityUDP(shareType);
                        break;
                    case "purchaseshare":
//                        System.out.println("our extra here is : " + extra);
                        result = obj.purchaseShareUDP(callerID, shareType, extra);
                        break;
                    case "sellshare":
                        result = obj.sellShareUDP(callerID, shareType, extra);
                        break;
                    default:
                        result = "ERROR: unknown method " + method;
                }
                String finalResult = result;
                byte[] sendData = finalResult.getBytes();
                DatagramPacket reply = new DatagramPacket(sendData, sendData.length, request.getAddress(), request.getPort());
                socket.send(reply);
                Logger.serverLog(serverID, callerID, "UDP reply sent: " + method, "shareType=" + shareType + ", extra=" + extra, finalResult);
            }
        }
        catch (SocketException e) {
            System.out.println(serverName + " SocketException: " + e.getMessage());
        }
        catch (IOException e) {
            System.out.println(serverName + " IOException: " + e.getMessage());
        }
        finally {
            if (socket != null) {
                socket.close();
            }
        }
    }
}
