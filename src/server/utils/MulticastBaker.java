package server.utils;

import java.util.ArrayList;

public class MulticastBaker {
    private static int multicastSuffix = 0x000000;
    private static final int[] address = new int[4];
    private static final ArrayList<String> reusableAddresses = new ArrayList<>();


    public static String getNewMulticastAddress() {
        //set first octet to multicast address scope
        address[0] = 0xE0;

        if(reusableAddresses.size() > 0){
            return reusableAddresses.remove(0);
        }

        if (!(multicastSuffix > 0xFF)){
            multicastSuffix++;
            address[3] = (multicastSuffix  & 0xFF);         //mask last eight bits
            address[2] = ((multicastSuffix >> 0x8) & 0xFF); //shift 8 bits right and mask last eight bits
            address[1] = ((multicastSuffix >> 0xF) & 0xFF); //shift 16 bits right and mask last eight bits
            return address[0] + "." + address[1] + "." + address[2] + "." + address[3]; //return 224.x.x.x string
        } else return null;
    }


    public static void releaseAddress(String address){
        reusableAddresses.add(address);
    }
}
