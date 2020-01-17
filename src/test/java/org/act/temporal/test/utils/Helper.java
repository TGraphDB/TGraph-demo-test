package org.act.temporal.test.utils;

import org.act.tgraph.demo.client.Config;

import java.io.File;
import java.util.Arrays;
import java.util.List;

/**
 * Created by song on 16-2-23.
 */
public class Helper {
    public static void deleteAllFilesOfDir(File path) {
        if (!path.exists())
            return;
        if (path.isFile()) {
            path.delete();
            return;
        }
        File[] files = path.listFiles();
        for (int i = 0; i < files.length; i++) {
            deleteAllFilesOfDir(files[i]);
        }
        path.delete();
    }

    public static String getString(int size) {
        char[] chars = new char[size];
        Arrays.fill(chars, 'a');
        return new String(chars);
    }

    public static byte[] intToByteArray(int value) {
        return new byte[] {
                (byte)(value >>> 24),
                (byte)(value >>> 16),
                (byte)(value >>> 8),
                (byte)value};
    }

    public static void getFileRecursive(File dir,List<File> fileList, int level){
        if(dir.isDirectory()){
            for (File file : dir.listFiles()) {
                if(!file.isDirectory() && file.getName().startsWith("TJamData_201") && file.getName().endsWith(".csv")){
                    fileList.add(file);
                }
            }
            if(level>0) {
                for (File file : dir.listFiles()) {
                    if (file.isDirectory()) {
                        getFileRecursive(file, fileList, level - 1);
                    }
                }
            }
        }
    }

    public static int[] calcSplit(int nthPart, int totalPartCount, int size) {
        if(!(0<nthPart && nthPart <= totalPartCount && totalPartCount <= size)){
            throw new RuntimeException("bad argument");
        }
        int[] lengthList = new int[totalPartCount];
        int base = size / totalPartCount;
        int remain=size % totalPartCount;
        int start=0;
        int end=0;

//        System.out.println((base+1)+"["+remain+" times], "+base+"["+(totalPartCount-remain)+" times]");
        for(int i=0;i<totalPartCount;i++) {
            lengthList[i] = base;
            if (remain > 0) {
                remain--;
                lengthList[i]++;
            }
        }
        int j;
        for( j=0;j<nthPart-1;j++){
            start+=lengthList[j];
        }
        end=start+lengthList[j]-1;
        return new int[]{start,end};
    }

    public static int getFileTime(File file){
        return Integer.parseInt(file.getName().substring(10, 21));
    }

    public static void deleteExistDB(String dbPath){
        File dir = new File(dbPath);
        if (dir.exists()){
            Helper.deleteAllFilesOfDir(dir);
        }
        dir.mkdir();
    }

    private static final char[] hexArray = "0123456789ABCDEF".toCharArray();
    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for ( int j = 0; j < bytes.length; j++ ) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }
}


//    public static <T> List<T> shuffle(List<T> list) {
//        int[] order = Helper.shuffle(list.size());
//        List<T> shuffledFileList = new ArrayList<>();
//        for(int i=0;i<order.length;i++){
//            shuffledFileList.add(list.get(order[i]));
//        }
//        return shuffledFileList;
//    }
//
//    private static int[] shuffle(int length) {
//        int [] arr = new int[length];
//        for(int i=0;i<length;i++){
//            arr[i]=i;
//        }
//        int [] arr2 =new int[length];
//        int count = length;
//        int cbRandCount = 0;// 索引
//        int cbPosition = 0;// 位置
//        int k =0;
//        do {
//            Random rand = new Random(8888);
//            int r = count - cbRandCount;
//            cbPosition = rand.nextInt(r);
//            arr2[k++] = arr[cbPosition];
//            cbRandCount++;
//            arr[cbPosition] = arr[r - 1];// 将最后一位数值赋值给已经被使用的cbPosition
//        } while (cbRandCount < count);
//        return arr2;
//    }