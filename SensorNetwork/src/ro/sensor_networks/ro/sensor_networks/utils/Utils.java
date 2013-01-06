package ro.sensor_networks.ro.sensor_networks.utils;

/**
 * Created with IntelliJ IDEA.
 * User: Marius
 * Date: 12/26/12
 * Time: 10:06 PM
 * To change this template use File | Settings | File Templates.
 */
public class Utils {

    public static String bytesToHex(byte[] bytes) {
        final char[] hexArray = {'0','1','2','3','4','5','6','7','8','9','A','B','C','D','E','F'};
        char[] hexChars = new char[bytes.length * 2];
        int v;
        for ( int j = 0; j < bytes.length; j++ ) {
            v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    public static String intsToHex(int[] ints) {
        final char[] hexArray = {'0','1','2','3','4','5','6','7','8','9','A','B','C','D','E','F'};
        char[] hexChars = new char[ints.length * 2];
        int v;
        for ( int j = 0; j < ints.length; j++ ) {
            v = ints[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    public static int[] bytesToInts(byte[] bytes){
        if(bytes == null || bytes.length == 0){
             throw new IllegalArgumentException("Can't convert null or empty arrays");
        }
        int[] resultArray= new int[bytes.length];

        for(int i=0;i< bytes.length; i++){
           resultArray[i] = bytes[i];
        }

        return resultArray;
    }

    public static byte[] intsToBytes(int[] ints){
        if(ints == null || ints.length == 0){
            throw new IllegalArgumentException("Can't convert null or empty arrays");
        }
        byte[] resultArray= new byte[ints.length];

        for(int i=0;i< ints.length; i++){
            resultArray[i] = (byte)ints[i];
        }

        return resultArray;
    }

}
