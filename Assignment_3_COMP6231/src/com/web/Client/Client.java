package com.web.Client;

import Logger.Logger;
import com.web.service.WebInterface;
import javax.xml.namespace.QName;
import javax.xml.ws.Service;
import java.net.URL;
import java.util.Scanner;
import com.web.service.implimentation.ShareMarketImplementation;


public class Client {

    public static final int BUYER_PURCHASE_SHARE = 1;
    public static final int BUYER_GET_SHARES = 2;
    public static final int BUYER_SELL_SHARE = 3;
    public static final int BUYER_SWAP_SHARE = 4;
    public static final int BUYER_LOGOUT = 5;
    public static final int ADMIN_ADD_SHARE = 1;
    public static final int ADMIN_REMOVE_SHARE = 2;
    public static final int ADMIN_LIST_SHARE_AVAILABILITY = 3;
    public static final int ADMIN_LOGOUT = 4;

    private static Scanner input;
    private static Service NewYorkService;
    private static Service TokyoService;
    private static Service LondonService;
    private static WebInterface ref;

    public static void main(String[] args) {
        try {
            URL NewYorkURL = new URL("http://localhost:8080/newyork?wsdl");
            QName NewYorkQName = new QName("http://implimentation.service.web.com/", "ShareMarketImplementationService");
            NewYorkService = Service.create(NewYorkURL, NewYorkQName);

            URL TokyoURL = new URL("http://localhost:8080/tokyo?wsdl");
            QName TokyoQName = new QName("http://implimentation.service.web.com/", "ShareMarketImplementationService");
            TokyoService = Service.create(TokyoURL, TokyoQName);

            URL LondonURL = new URL("http://localhost:8080/london?wsdl");
            QName LondonQName = new QName("http://implimentation.service.web.com/", "ShareMarketImplementationService");
            LondonService = Service.create(LondonURL, LondonQName);
            init();
        }
        catch(Exception e) {
            e.printStackTrace();
        }

    }

    private static void init() throws Exception {
        input = new Scanner(System.in);
        System.out.println("Please Enter your UserID (city ID + A or B + user code)::");
        String userID = input.nextLine().trim().toUpperCase();

        if (userID.length() < 4) {
            System.out.println("Invalid userID format");
            return;
        }

        char role = userID.charAt(3);
        if (role == 'A' || role == 'a') {
            adminMenu(userID);
        }
        else {
            buyerMenu(userID);
        }
    }

    private static String getServerCode(String userID) {
        String x = userID.substring(0,3).toUpperCase();
        if (x.equalsIgnoreCase("NYK")) {
            ref = NewYorkService.getPort(WebInterface.class);
            return x;
        }
        else if (x.equalsIgnoreCase("TOK")) {
            ref = TokyoService.getPort(WebInterface.class);
            return x;
        }
        else if (x.equalsIgnoreCase("LON")) {
            ref = LondonService.getPort(WebInterface.class);
            return x;
        }
        return "1";
    }

    private static void buyerMenu(String buyerID) throws Exception {
        // we are at whcih city
        String cityCode = getServerCode(buyerID);

        if (cityCode.equals("1")){
            init();
        }

        boolean loop = true;
        while(loop) {
            System.out.println("Buyer Menu:");
            System.out.println("1. Purchase Share");
            System.out.println("2. Get Shares");
            System.out.println("3. Sell Share");
            System.out.println("4. Swap Share");
            System.out.println("5. Logout");
            int choice = input.nextInt(); input.nextLine();
            String shareType, shareID, serverResponse;
            int shareCount;
            switch(choice) {
                case BUYER_PURCHASE_SHARE:
                    shareType = promptForShareType();
                    shareID   = promptForShareID();
                    shareCount = promptForshareCount();
                    Logger.clientLog(buyerID, " attempting to purchaseShare");
                    serverResponse = ref.purchaseShare(buyerID, shareID, shareType, shareCount);
                    System.out.println(serverResponse);
                    Logger.clientLog(buyerID, " purchaseShare", " shareID: " + shareID + " shareType: " + shareType + " shareCount: " + shareCount + " ", serverResponse);
                    break;

                case BUYER_GET_SHARES:
                    Logger.clientLog(buyerID, " attempting to getShares");
                    serverResponse = ref.getShares(buyerID);
                    System.out.println(serverResponse);
                    Logger.clientLog(buyerID, " getShares", " null ", serverResponse);
                    break;

                case BUYER_SELL_SHARE:
                    shareID = promptForShareID();
                    shareCount = promptForshareCount();
                    Logger.clientLog(buyerID, " attempting to sellShare");
                    serverResponse = ref.sellShare(buyerID, shareID, shareCount);
                    System.out.println(serverResponse);
                    Logger.clientLog(buyerID, " sellShare", " shareID: " + shareID + " shareCount: " + shareCount + " ", serverResponse);
                    break;

                case BUYER_SWAP_SHARE:
                    System.out.println("\nEnter OLD share:");
                    String oldShareType = promptForShareType();
                    String oldShareID = promptForShareID();
                    System.out.println("\nEnter NEW share:");
                    String newShareType = promptForShareType();
                    String newShareID = promptForShareID();
                    serverResponse = ref.swapShare(buyerID, oldShareID, oldShareType, newShareID,newShareType);
                    System.out.println(serverResponse);
                    Logger.clientLog(buyerID, "swapShare", oldShareID+"=>"+newShareID, serverResponse);
                    break;

                case BUYER_LOGOUT:
                    loop = false;
                    Logger.clientLog(buyerID, " buyer attemped to Logout");
                    break;
            }
        }
    }

    private static void adminMenu(String adminID) throws Exception {
        String cityCode = getServerCode(adminID);

        if (cityCode.equals("1")){
            init();
        }

        boolean loop = true;
        while(loop) {
            System.out.println("Admin Menu:");
            System.out.println("1. Add Share");
            System.out.println("2. Remove Share");
            System.out.println("3. List Share Availability");
            System.out.println("4. Logout");
            int choice = input.nextInt(); input.nextLine();
            String shareType, shareID, buyerID, serverResponse;
            int capacity;
            switch(choice) {
                case ADMIN_ADD_SHARE:
                    shareType = promptForShareType();
                    shareID   = promptForShareID();
                    capacity  = promptForCapacity();
                    Logger.clientLog(adminID, " attempting to addShare");
                    serverResponse = ref.addShare(shareID, shareType, capacity);
                    System.out.println(serverResponse);
                    Logger.clientLog(adminID, " addShare", " shareID: " + shareID + " shareType: " + shareType + " capacity: " + capacity + " ", serverResponse);
                    break;

                case ADMIN_REMOVE_SHARE:
                    shareType = promptForShareType();
                    shareID   = promptForShareID();
                    Logger.clientLog(adminID, " attempting to removeShare");
                    serverResponse = ref.removeShare(shareID, shareType);
                    System.out.println(serverResponse);
                    Logger.clientLog(adminID, " removeShare", " shareID: " + shareID + " shareType: " + shareType + " ", serverResponse);
                    break;

                case ADMIN_LIST_SHARE_AVAILABILITY:
                    shareType = promptForShareType();
                    Logger.clientLog(adminID, " attempting to listShareAvailability");
                    serverResponse = ref.listShareAvailability(shareType);
                    System.out.println(serverResponse);
                    Logger.clientLog(adminID, " listShareAvailability", " shareType: " + shareType + " ", serverResponse);
                    break;

                case ADMIN_LOGOUT:
                    loop = false;
                    Logger.clientLog(adminID, " admin attemped to Logout");
                    break;
            }
        }
    }

    private static String promptBuyerID(String cityPrefix) {
        System.out.println("Enter buyerID (must start with " + cityPrefix + "):");
        String bid = input.nextLine().trim().toUpperCase();
        if (!bid.startsWith(cityPrefix)) {
            return promptBuyerID(cityPrefix);
        }
        return bid;
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
    

    private static int promptForshareCount() {
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
