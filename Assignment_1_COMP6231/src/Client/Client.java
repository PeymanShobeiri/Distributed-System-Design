package Client;

import java.io.IOException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Scanner;
import Interface.ShareMarketInterface;
import ServerImplementation.ShareMarketImplementation;
import Logger.Logger;

public class Client {

    public static final int USER_TYPE_BUYER = 1;
    public static final int USER_TYPE_ADMIN = 2;
    public static final int BUYER_PURCHASE_SHARE = 1;
    public static final int BUYER_GET_SHARES = 2;
    public static final int BUYER_SELL_SHARE = 3;
    public static final int BUYER_LOGOUT = 4;
    public static final int ADMIN_ADD_SHARE = 1;
    public static final int ADMIN_REMOVE_SHARE = 2;
    public static final int ADMIN_LIST_SHARE_AVAILABILITY = 3;
    public static final int ADMIN_LOGOUT = 4;

    public static final int SERVER_NEWYORK = 3001;
    public static final int SERVER_LONDON  = 3002;
    public static final int SERVER_TOKYO   = 3003;
    public static final String SHARE_MARKET_REGISTERED_NAME = "SHARE_MARKET";
    static Scanner input;

    public static void main(String[] args) throws Exception {
        init();
    }

    public static void init() throws IOException {
        input = new Scanner(System.in);
        System.out.println("Please Enter your UserID (city ID + A or B + user code):");
        String userID = input.next().trim().toUpperCase();
        Logger.clientLog(userID, " login attempted");
        
        int flag = 0;
        if (userID.length() >= 5) {
            String prefix = userID.substring(0, 3).toUpperCase();
            String userType   = userID.substring(3, 4).toUpperCase();
            if ((prefix.equals("NYK") || prefix.equals("LON") || prefix.equals("TOK"))) {
                if (userType.equals("B")) {
                    flag = USER_TYPE_BUYER;
                }
                else if (userType.equals("A")) {
                    flag = USER_TYPE_ADMIN;
                }
            }
        }
        
        switch (flag) {
            case USER_TYPE_BUYER:
                try {
                    System.out.println("Buyer Login successful (" + userID + ")");
                    Logger.clientLog(userID, " Buyer Login successful");
                    buyerMenu(userID, getServerPort(userID.substring(0, 3)));
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
                break;
                
            case USER_TYPE_ADMIN:
                try {
                    System.out.println("Admin Login successful (" + userID + ")");
                    Logger.clientLog(userID, " Admin Login successful");
                    adminMenu(userID, getServerPort(userID.substring(0, 3)));
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
                break;
            default:
                System.out.println("UserID is not in correct format !!!)");
                Logger.clientLog(userID, " UserID is not in correct format");
                Logger.deleteALogFile(userID);
                init();
        }
    }

    private static int getServerPort(String prefix) {
        switch (prefix.toUpperCase()) {
            case "NYK":
                return SERVER_NEWYORK;
            case "LON":
                return SERVER_LONDON;
            case "TOK":
                return SERVER_TOKYO;
            default:
                return 0;
        }
    }

    private static void buyerMenu(String buyerID, int serverPort) throws Exception {
        if (serverPort != SERVER_NEWYORK && serverPort != SERVER_LONDON && serverPort != SERVER_TOKYO) {
            System.out.println("Error: Could not find the correct server for " + buyerID);
            return;
        }
        // calling RMI ...
        Registry registry = LocateRegistry.getRegistry(serverPort);
        ShareMarketInterface remoteObject = (ShareMarketInterface) registry.lookup(SHARE_MARKET_REGISTERED_NAME);

        boolean repeat = true;
        printMenu(USER_TYPE_BUYER);
        int selected = input.nextInt();
        String shareType, shareID;
        String serverResponse;
        int shareCount;

        switch (selected) {
            case BUYER_PURCHASE_SHARE:
                shareType = promptForShareType();
                shareID   = promptForShareID();
                shareCount = promptForShareCount();
                Logger.clientLog(buyerID, " attempting to purchaseShare");
                serverResponse = remoteObject.purchaseShare(buyerID, shareID, shareType, shareCount);
                System.out.println(serverResponse);
                Logger.clientLog(buyerID, " purchaseShare", " shareID: " + shareID + " shareType: " + shareType + " shareCount: " + shareCount + " ", serverResponse);
                break;

            case BUYER_GET_SHARES:
                Logger.clientLog(buyerID, " attempting to getShares");
                serverResponse = remoteObject.getShares(buyerID);
                System.out.println(serverResponse);
                Logger.clientLog(buyerID, " getShares", " null ", serverResponse);
                break;

            case BUYER_SELL_SHARE:
                shareID   = promptForShareID();
                shareCount = promptForShareCount();
                Logger.clientLog(buyerID, " attempting to sellShare");
                serverResponse = remoteObject.sellShare(buyerID, shareID, shareCount);
                System.out.println(serverResponse);
                Logger.clientLog(buyerID, " sellShare", " shareID: " + shareID + " shareCount: " + shareCount + " ", serverResponse);
                break;

            case BUYER_LOGOUT:
                repeat = false;
                Logger.clientLog(buyerID, " buyer attemped to Logout");
                init();
                break;
        }
        if (repeat) {
            buyerMenu(buyerID, serverPort);
        }
    }

    private static void adminMenu(String adminID, int serverPort) throws Exception {
        if ((serverPort != SERVER_NEWYORK && serverPort != SERVER_LONDON && serverPort != SERVER_TOKYO)) {
            System.out.println("Error: Could not find correct server for " + adminID);
            return;
        }
        Registry registry = LocateRegistry.getRegistry(serverPort);
        ShareMarketInterface remoteObject = (ShareMarketInterface) registry.lookup(SHARE_MARKET_REGISTERED_NAME);
        boolean repeat = true;
        printMenu(USER_TYPE_ADMIN);
        int selected = input.nextInt();
        String shareType, shareID;
        String serverResponse;
        int capacity;

        switch (selected) {
            case ADMIN_ADD_SHARE:
                shareType = promptForShareType();
                shareID   = promptForShareID();
                capacity  = promptForCapacity();
                Logger.clientLog(adminID, " attempting to addShare");
                serverResponse = remoteObject.addShare(shareID, shareType, capacity);
                System.out.println(serverResponse);
                Logger.clientLog(adminID, " addShare", " shareID: " + shareID + " shareType: " + shareType + " capacity: " + capacity + " ", serverResponse);
                break;

            case ADMIN_REMOVE_SHARE:
                shareType = promptForShareType();
                shareID   = promptForShareID();
                Logger.clientLog(adminID, " attempting to removeShare");
                serverResponse = remoteObject.removeShare(shareID, shareType);
                System.out.println(serverResponse);
                Logger.clientLog(adminID, " removeShare", " shareID: " + shareID + " shareType: " + shareType + " ", serverResponse);
                break;

            case ADMIN_LIST_SHARE_AVAILABILITY:
                shareType = promptForShareType();
                Logger.clientLog(adminID, " attempting to listShareAvailability");
                serverResponse = remoteObject.listShareAvailability(shareType);
                System.out.println(serverResponse);
                Logger.clientLog(adminID, " listShareAvailability", " shareType: " + shareType + " ", serverResponse);
                break;

            case ADMIN_LOGOUT:
                repeat = false;
                Logger.clientLog(adminID, " admin attemped to Logout");
                init();
                break;
        }
        if (repeat) {
            adminMenu(adminID, serverPort);
        }
    }




    private static void printMenu(int userType) {
        System.out.println("-----------------------------------------");
        System.out.println("Please choose an option below:");
        if (userType == USER_TYPE_BUYER) {
            System.out.println("1. Purchase Share");
            System.out.println("2. Get Shares");
            System.out.println("3. Sell Share");
            System.out.println("4. Logout");
        }
        else if (userType == USER_TYPE_ADMIN) {
            System.out.println("1. Add Share");
            System.out.println("2. Remove Share");
            System.out.println("3. List Share Availability");
            System.out.println("4. Logout");
        }
    }

    private static String promptForShareType() {
        System.out.println("-----------------------------------------");
        System.out.println("Please choose a shareType below:");
        System.out.println("1. Bonus");
        System.out.println("2. Equity");
        System.out.println("3. Dividend");
        switch (input.nextInt()) {
            case 1:
                return ShareMarketImplementation.BONUS;
            case 2:
                return ShareMarketImplementation.EQUITY;
            case 3:
                return ShareMarketImplementation.DIVIDEND;
        }
        return promptForShareType();
    }


    private static String promptForShareID() {
        System.out.println("-----------------------------------------");
        System.out.println("Please enter the ShareID (city + M or A or E + time slot (dd+mm+yy)");
        String shareID = input.next().trim().toUpperCase();
        String prefix = shareID.substring(0, 3).toUpperCase();
        String slot   = shareID.substring(3, 4).toUpperCase();
        if ((prefix.equals("NYK") || prefix.equals("LON") || prefix.equals("TOK")) && (slot.equals("M") || slot.equals("A") || slot.equals("E"))) {
            return shareID;
        }
        System.out.println("Invalid shareID format. Try again...");
        return promptForShareID();
    }

    private static int promptForShareCount() {
        System.out.println("-----------------------------------------");
        System.out.println("Please enter the number of shares:");
        return input.nextInt();
    }

    private static int promptForCapacity() {
        System.out.println("-----------------------------------------");
        System.out.println("Please enter the capacity for this new share:");
        return input.nextInt();
    }
}

