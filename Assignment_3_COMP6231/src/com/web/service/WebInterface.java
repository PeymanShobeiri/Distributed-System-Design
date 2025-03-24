package com.web.service;

import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;

@WebService
@SOAPBinding(style = SOAPBinding.Style.RPC)

public interface WebInterface {

	// admin
    String addShare(String shareID, String shareType, int capacity);
    
    String removeShare(String shareID, String shareType);
    
    String listShareAvailability(String shareType);



    //buyers
    String purchaseShare(String buyerID, String shareID, String shareType, int shareCount);
    
    String getShares(String buyerID);
    
    String sellShare(String buyerID, String shareID, int shareCount);

    String swapShare(String buyerID, String newShareID, String newShareType, String oldShareID, String oldShareType);

}



