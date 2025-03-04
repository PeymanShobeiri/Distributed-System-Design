package Interface;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface ShareMarketInterface extends Remote {

    String addShare(String shareID, String shareType, int capacity) throws RemoteException;
    
    String removeShare(String shareID, String shareType) throws RemoteException;
    
    String listShareAvailability(String shareType) throws RemoteException;

    String purchaseShare(String buyerID, String shareID, String shareType, int shareCount) throws RemoteException;
    
    String getShares(String buyerID) throws RemoteException;
    
    String sellShare(String buyerID, String shareID, int shareCount) throws RemoteException;
}


