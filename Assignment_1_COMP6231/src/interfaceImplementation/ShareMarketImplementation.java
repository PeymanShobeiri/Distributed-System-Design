package ServerImplementation;

import java.io.IOException;
import java.net.*;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import Interface.ShareMarketInterface;
import Logger.Logger;


public class ShareMarketImplementation extends UnicastRemoteObject implements ShareMarketInterface {

    public static final String BONUS    = "Bonus";
    public static final String EQUITY   = "Equity";
    public static final String DIVIDEND = "Dividend";

    // UDP server ports
    public static final int NYK_PORT = 8001;
    public static final int LON_PORT = 8002;
    public static final int TOK_PORT = 8003;
    public static final String MARKET_SERVER_NEWYORK = "NEWYORK";
    public static final String MARKET_SERVER_LONDON  = "LONDON";
    public static final String MARKET_SERVER_TOKYO   = "TOKYO";
    private final String serverID;    // like TOK
    private final String serverName;
    private Map<String, Map<String, ShareData>> allShares; //shareType -> (shareID -> ShareData)
    private Map<String, Map<String, List<String>>> buyerShares; // which buyer bought which share  buyerID -> (shareType -> list of shareIDs)

    // a class for storing the informations of a share
    private static class ShareData {
        private final String shareID;
        private final String shareType;
        private int capacity;
        private final Map<String, Integer> buyerCounts; // buyer id : number of shares it buyes

        public static final int SHARE_FULL         = -1;
        public static final int ALREADY_REGISTERED = 0;
        public static final int ADD_SUCCESS        = 1;

        public ShareData(String shareID, String shareType, int capacity) {
            this.shareID    = shareID;
            this.shareType  = shareType;
            this.capacity   = capacity;
            this.buyerCounts  = new HashMap<>();
        }

        public int getShareCapacity() {
            return capacity;
        }

        public void setShareCapacity(int capacity) {
            this.capacity = capacity;
        }

        public int getUsedCapacity() {
            int sum = 0;
            for (int units : buyerCounts.values()) {
                sum += units;
            }
            return sum;
        }


        public int getShareRemainCapacity() {
            return capacity - getUsedCapacity();
        }


        public int addBuyerPurchase(String buyerID, int count) {
            if (buyerCounts.containsKey(buyerID)) {
                return ALREADY_REGISTERED;
            }
            if (getUsedCapacity() + count > capacity) {
                return SHARE_FULL;
            }
            buyerCounts.put(buyerID, count);
            return ADD_SUCCESS;
        }

        public int removeBuyerPurchase(String buyerID, int count) {
            if (!buyerCounts.containsKey(buyerID)) {
                return 0;
            }
            int privious = buyerCounts.get(buyerID);
            int removed = Math.min(privious, count);
            int remaining = privious - removed;
            if (remaining > 0) {
                buyerCounts.put(buyerID, remaining);
            }
            else {
                buyerCounts.remove(buyerID);
            }
            return removed;
        }

        public boolean isFull() {
            return (getUsedCapacity() >= capacity);
        }

        @Override
        public String toString() {
            return "[ShareID=" + shareID + ", Type=" + shareType + ", Capacity=" + capacity + ", Purchased=" +  getUsedCapacity() + ", Remaining=" + getShareRemainCapacity() + "]";
        }
    }


    public ShareMarketImplementation(String serverID, String serverName) throws RemoteException {
        super();
        this.serverID   = serverID;
        this.serverName = serverName;
        allShares = new ConcurrentHashMap<>();
        allShares.put(BONUS,    new ConcurrentHashMap<>());
        allShares.put(EQUITY,   new ConcurrentHashMap<>());
        allShares.put(DIVIDEND, new ConcurrentHashMap<>());
        buyerShares = new ConcurrentHashMap<>();
    }


    @Override
    public synchronized String addShare(String shareID, String shareType, int capacity) throws RemoteException {
        try {
            Logger.serverLog(serverID, "Admin", "RMI addShare", "shareID=" + shareID + ", shareType=" + shareType + ", capacity=" + capacity, "request received");
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        // is share in your server? if not can't ...
        if (!detectShareServer(shareID).equals(serverName)) {
            String response = "Failed: Cannot add share " + shareID + " from a different server. please contact the server admin";
            logResponse("Admin", "addShare", shareID, shareType, response);
            return response;
        }

        if (allShares.get(shareType).containsKey(shareID)) {
            String resp = "Failed: share " + shareID + " already exists for shareType of " + shareType;
            logResponse("Admin", "addShare", shareID, shareType, resp);
            return resp;
        }

        ShareData newShare = new ShareData(shareID, shareType, capacity);
        allShares.get(shareType).put(shareID, newShare);
        String response = "Success: share " + shareID + " added with capacity=" + capacity;
        logResponse("Admin", "addShare", shareID, shareType, response);
        return response;
    }

    @Override
    public synchronized String removeShare(String shareID, String shareType) throws RemoteException {
        try {
            Logger.serverLog(serverID, "Admin", "RMI removeShare", "shareID=" + shareID + ", shareType=" + shareType, "request received");
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        if (!detectShareServer(shareID).equals(serverName)) {
            String response = "Failed: Cannot remove share " + shareID + " from another server. please contact the server admin";
            logResponse("Admin", "removeShare", shareID, shareType, response);
            return response;
        }

        if (!allShares.get(shareType).containsKey(shareID)) {
            String response = "Failed: share " + shareID + " does not exist on " + serverName;
            logResponse("Admin", "removeShare", shareID, shareType, response);
            return response;
        }

        allShares.get(shareType).remove(shareID);
        // be careful with the power -> if admin remove -> remove from all buyer even if they have it
        removeShareFromBuyers(shareType, shareID);
        String response = "Success: share " + shareID + " removed from " + serverName;
        logResponse("Admin", "removeShare", shareID, shareType, response);
        return response;
    }

    @Override
    public synchronized String listShareAvailability(String shareType) throws RemoteException {
        try {
            Logger.serverLog(serverID, "Admin", "RMI listShareAvailability", "shareType=" + shareType, "request received");
        }
        catch (IOException e) {
            e.printStackTrace();
        }

        StringBuilder sb = new StringBuilder();
        sb.append(serverName).append(" [").append(shareType).append("]:\n");
        Map<String, ShareData> localMap = allShares.get(shareType);
        if (localMap.isEmpty()) {
            sb.append("No shares of type ").append(shareType).append("\n");
        }
        else {
            for (ShareData share : localMap.values()) {
                sb.append(share.toString()).append(" \n");
            }
            sb.append("\n");
        }

        switch (serverID) {
            case "NYK":
                sb.append(sendUDPMessage(LON_PORT, "listShareAvailability", "Admin", shareType, "N/A"));
                sb.append(sendUDPMessage(TOK_PORT, "listShareAvailability", "Admin", shareType, "N/A"));
                break;
            case "LON":
                sb.append(sendUDPMessage(NYK_PORT, "listShareAvailability", "Admin", shareType, "N/A"));
                sb.append(sendUDPMessage(TOK_PORT, "listShareAvailability", "Admin", shareType, "N/A"));
                break;
            case "TOK":
                sb.append(sendUDPMessage(NYK_PORT, "listShareAvailability", "Admin", shareType, "N/A"));
                sb.append(sendUDPMessage(LON_PORT, "listShareAvailability", "Admin", shareType, "N/A"));
                break;
        }
        String response = sb.toString();
        logResponse("Admin", "listShareAvailability", "N/A", shareType, response);
        return response;
    }

    @Override
    public synchronized String purchaseShare(String buyerID, String shareID, String shareType, int shareCount) throws RemoteException {
        try {
            Logger.serverLog(serverID, buyerID, "RMI purchaseShare", "shareID=" + shareID + ", shareType=" + shareType + ", shareCount=" + shareCount, "request received");
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        String shareServer = detectShareServer(shareID);
        // condition: not your own city --> UDP called + check number of purchases
        if (!shareServer.equals(serverName)) {
            if (shareCount > 3){
                String failMsg = "Failed: " + buyerID + " can not buy more than 3 shares from other cities in total per week.";
                logResponse(buyerID, "purchaseShare", shareID, shareType, failMsg);
                return failMsg;
            }
            if (exceededCrossCityLimit(buyerID, shareID)) {
                String failMsg = "Failed: " + buyerID + " exceeded your limit for buying from other cities.";
                logResponse(buyerID, "purchaseShare", shareID, shareType, failMsg);
                return failMsg;
            }
            // condition: one share type per day
            if (TodayPurchased(buyerID, shareType, shareID)) {
                String failMsg = "Failed: " + buyerID + " already purchased this share type today.";
                logResponse(buyerID, "purchaseShare", shareID, shareType, failMsg);
                return failMsg;
            }

            String extra = shareID + "-" + shareCount;
            String res = sendUDPMessage(getServerPort(shareID.substring(0,3)), "purchaseShare", buyerID, shareType, extra);
            if (res.startsWith("Success")) {
                for (int i = 0; i < shareCount; i++) {
                    addBuyerRecord(buyerID, shareType, shareID);
                }
//                System.out.println("buyer id recoards is:" + buyerShares);
            }
            logResponse(buyerID, "purchaseShare", shareID, shareType, res);
            return res;
        }

        // it's your own city
        if (TodayPurchased(buyerID, shareType, shareID)) {
            String failMsg = "Failed: " + buyerID + " already purchased this share type today.";
            logResponse(buyerID, "purchaseShare", shareID, shareType, failMsg);
            return failMsg;
        }
        
        if (!allShares.get(shareType).containsKey(shareID)) {
            String failMsg = "Failed: share " + shareID + " not found in " + serverName;
            logResponse(buyerID, "purchaseShare", shareID, shareType, failMsg);
            return failMsg;
        }
        ShareData share = allShares.get(shareType).get(shareID);
        int available = share.getShareRemainCapacity();
        if (available <= 0) {
            String failMsg = "Failed: share " + shareID + " is full in " + serverName;
            logResponse(buyerID, "purchaseShare", shareID, shareType, failMsg);
            return failMsg;
        }
        int realPurchase = Math.min(shareCount, available);
        int result = share.addBuyerPurchase(buyerID, realPurchase);
        if (result == ShareData.SHARE_FULL) {
            String failMsg = "Failed: share " + shareID + " is full.";
            logResponse(buyerID, "purchaseShare", shareID, shareType, failMsg);
            return failMsg;
        }
        else if (result == ShareData.ALREADY_REGISTERED) {
            String failMsg = "Failed: " + buyerID + " already purchased " + shareID + ".";
            logResponse(buyerID, "purchaseShare", shareID, shareType, failMsg);
            return failMsg;
        }

        // save to buyerShares
        for (int i = 0; i < realPurchase; i++) {
            addBuyerRecord(buyerID, shareType, shareID);
        }
        String successMsg = "Success: " + buyerID + " purchased " + realPurchase + " of share " + shareID;
        logResponse(buyerID, "purchaseShare", shareID, shareType, successMsg);
        return successMsg;
    }

    @Override
    public synchronized String getShares(String buyerID) throws RemoteException {
        try {
            Logger.serverLog(serverID, buyerID, "RMI getShares", "", "request received");
        }
        catch (IOException e) {
            e.printStackTrace();
        }

        if (!buyerShares.containsKey(buyerID)) {
            String res = "No shares found for buyer " + buyerID;
            logResponse(buyerID, "getShares", "N/A", "N/A", res);
            return res;
        }
        StringBuilder sb = new StringBuilder("Shares for " + buyerID + ":\n");
        Map<String, List<String>> stMap = buyerShares.get(buyerID);
//        System.out.println("buyer shares is : " + stMap);
        for (String shareType : stMap.keySet()) {
            sb.append("Type [").append(shareType).append("]:\n");
            Map<String, Integer> counts = new HashMap<>();
            for (String shareID : stMap.get(shareType)) {
                if (!counts.containsKey(shareID)) {
                    counts.put(shareID, 1);
                } else {
                    int tmp = counts.get(shareID);
                    counts.put(shareID, tmp + 1);
                }
            }
            for (Map.Entry<String, Integer> e : counts.entrySet()) {
                sb.append("\t").append(e.getKey()).append("\tcount: ").append(e.getValue()).append("\n");
            }
        }
        String resp = sb.toString();
        logResponse(buyerID, "getShares", "N/A", "N/A", resp);
        return resp;
    }

    @Override
    public synchronized String sellShare(String buyerID, String shareID, int shareCount) throws RemoteException {
        try {
            Logger.serverLog(serverID, buyerID, "RMI sellShare", "shareID=" + shareID + ", shareCount=" + shareCount, "request received");
        }
        catch (IOException e) {
            e.printStackTrace();
        }

        String shareServer = detectShareServer(shareID);
        if (!shareServer.equals(serverName)) {
            String extra = shareID + "-" + shareCount;
            String resp  = sendUDPMessage(getServerPort(shareID.substring(0,3)), "sellShare", buyerID, "N/A", extra);
            if (resp.startsWith("Success")) {
                removeSharesFromBuyer(buyerID, shareID, shareCount);
            }
            logResponse(buyerID, "sellShare", shareID, "N/A", resp);
            return resp;
        }
        //buyer has it?
        String st = findShareTypeForBuyer(buyerID, shareID);
        if (st == null) {
            String failMsg = "Failed: " + buyerID + " does not own " + shareID;
            logResponse(buyerID, "sellShare", shareID, "N/A", failMsg);
            return failMsg;
        }

        int buyerShareRemoved = removeSharesFromBuyer(buyerID, shareID, shareCount);
//        System.out.println("remove from buyer share:  " + buyerShareRemoved);
        ShareData shareData = allShares.get(st).get(shareID);
        int removedCount = shareData.removeBuyerPurchase(buyerID, buyerShareRemoved);
        String successMsg = "Success: " + buyerID + " sold " + buyerShareRemoved + " of share " + shareID;
        logResponse(buyerID, "sellShare", shareID, st, successMsg);
        return successMsg;
    }


    public synchronized String listShareAvailabilityUDP(String shareType) {
        StringBuilder sb = new StringBuilder();
        sb.append(serverName).append(" [").append(shareType).append("]:\n");
        Map<String, ShareData> map = allShares.get(shareType);
        if (map.isEmpty()) {
            sb.append("No shares of type ").append(shareType);
        }
        else {
            for (ShareData sd : map.values()) {
                sb.append(sd.toString()).append(" \n ");
            }
        }
        sb.append("\n");
        return sb.toString();
    }

    public synchronized String purchaseShareUDP(String buyerID, String shareType, String extra) { //extra = shareID;shareCount
        String[] arr = extra.split("-");
        String shareID = arr[0];
        int shareCount = Integer.parseInt(arr[1]);
        try {
//            System.out.println("UDP call for the purchase share -> count here: " + shareCount);
            return purchaseShare(buyerID, shareID, shareType, shareCount);
        }
        catch (RemoteException e) {
            return "Error: " + e.getMessage();
        }
    }

    public synchronized String sellShareUDP(String buyerID, String shareType, String extra) { // same extra
        String[] arr = extra.split("-");
        String shareID = arr[0];
        int shareCount = Integer.parseInt(arr[1]);
        try {
            return sellShare(buyerID, shareID, shareCount);
        }
        catch (RemoteException e) {
            return "Error: " + e.getMessage();
        }
    }

    // find server name from share id
    private String detectShareServer(String shareID) {
        String prefix = shareID.substring(0,3).toUpperCase();
        switch (prefix) {
            case "NYK": return MARKET_SERVER_NEWYORK;
            case "LON": return MARKET_SERVER_LONDON;
            case "TOK": return MARKET_SERVER_TOKYO;
            default:    return "UNKNOWN";
        }
    }

    // find server port from the id
    private int getServerPort(String cityID) {
        switch (cityID.toUpperCase()) {
            case "NYK": return NYK_PORT;
            case "LON": return LON_PORT;
            case "TOK": return TOK_PORT;
            default:    return 0;
        }
    }


    private String sendUDPMessage(int serverPort, String method, String callerID, String shareType, String extra) {
        DatagramSocket socket = null;
        String result = "";
        String data = method + ";" + callerID + ";" + shareType + ";" + extra;

        try {
            Logger.serverLog(serverID, callerID, "UDP request: " + method, "shareType=" + shareType + ", extra=" + extra, "...");
        }
        catch (IOException e) {
            e.printStackTrace();
        }

        try {
            socket = new DatagramSocket();
            InetAddress host = InetAddress.getByName("localhost");
            byte[] buffer = data.getBytes();
            DatagramPacket request = new DatagramPacket(buffer, buffer.length, host, serverPort);
            socket.send(request);
            byte[] recvBuf = new byte[1000];
            DatagramPacket reply = new DatagramPacket(recvBuf, recvBuf.length);
            socket.receive(reply);
            result = new String(reply.getData(), 0, reply.getLength());
//            result = rawdata;
            Logger.serverLog(serverID, callerID, "UDP reply: " + method, "shareType=" + shareType + ", extra=" + extra, result);
        }
        catch (IOException e) {
            result = "UDP Error: " + e.getMessage();
        }
        finally {
            if (socket != null)
                socket.close();
        }
        return result;
    }

    // create share type map -> inseart shareId
    private void addBuyerRecord(String buyerID, String shareType, String shareID) {
        buyerShares.putIfAbsent(buyerID, new ConcurrentHashMap<>());
        buyerShares.get(buyerID).putIfAbsent(shareType, new ArrayList<>());
        buyerShares.get(buyerID).get(shareType).add(shareID);
    }


    private int removeSharesFromBuyer(String buyerID, String shareID, int shareCount) {
        if (!buyerShares.containsKey(buyerID))
            return 0;
        String st = findShareTypeForBuyer(buyerID, shareID);
        if (st == null)
            return 0;
        List<String> shareList = buyerShares.get(buyerID).get(st);
        if (shareList == null)
            return 0;
        int removed = 0;
        while (removed < shareCount && shareList.remove(shareID)) {
            removed++;
        }
        return removed;
    }

    // admin wants to remove it even if the buyer has it
    private void removeShareFromBuyers(String shareType, String shareID) {
        for (String buyerID : buyerShares.keySet()) {
            Map<String, List<String>> typeMap = buyerShares.get(buyerID);
            if (typeMap.containsKey(shareType)) {
                typeMap.get(shareType).removeIf(id -> id.equalsIgnoreCase(shareID));
            }
        }
    }

    // share type for buyer --> with that shareID
    private String findShareTypeForBuyer(String buyerID, String shareID) {
        if (!buyerShares.containsKey(buyerID))
            return null;
        for (String st : buyerShares.get(buyerID).keySet()) {
            if (buyerShares.get(buyerID).get(st).contains(shareID)) {
                return st;
            }
        }
        return null;
    }

    // if buyer bought more than 3 shares in other cities --> true
    private boolean exceededCrossCityLimit(String buyerID, String shareID) {
        int day   = Integer.parseInt(shareID.substring(4, 6));
        int month = Integer.parseInt(shareID.substring(6, 8));
        int year  = Integer.parseInt("20" + shareID.substring(8, 10));
        int thisWeek = (day - 1) / 7;
        int BuyFromOtherCities = 0;
        Map<String, List<String>> stMap = buyerShares.getOrDefault(buyerID, new ConcurrentHashMap<>());
        for (String st : stMap.keySet()) {
//            System.out.println("haaaa : " + st);
            for (String sID : stMap.get(st)) {
                int d2 = Integer.parseInt(sID.substring(4, 6));
                int m2 = Integer.parseInt(sID.substring(6, 8));
                int y2 = Integer.parseInt("20" + sID.substring(8, 10));
                int w2 = (d2 - 1) / 7;
                if (m2 == month && y2 == year && w2 == thisWeek) {
                    BuyFromOtherCities++;
                }
            }
        }
        return (BuyFromOtherCities >= 3);
    }


    private boolean TodayPurchased(String buyerID, String shareType, String shareID) {
        int day   = Integer.parseInt(shareID.substring(4, 6));
        int month = Integer.parseInt(shareID.substring(6, 8));
        int year  = Integer.parseInt("20" + shareID.substring(8, 10));
        Map<String, List<String>> typeMap = buyerShares.getOrDefault(buyerID, new ConcurrentHashMap<>());
        List<String> shareList = typeMap.getOrDefault(shareType, new ArrayList<>());
        for (String existingID : shareList) {
            int d2 = Integer.parseInt(existingID.substring(4, 6));
            int m2 = Integer.parseInt(existingID.substring(6, 8));
            int y2 = Integer.parseInt("20" + existingID.substring(8, 10));
            if (d2 == day && m2 == month && y2 == year) {
                return true;
            }
        }
        return false;
    }


    private void logResponse(String user, String operation, String shareID, String shareType, String response) {
        try {
            Logger.serverLog(serverID, user, "RMI " + operation, "shareID=" + shareID + ", shareType=" + shareType, response);
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }
}
