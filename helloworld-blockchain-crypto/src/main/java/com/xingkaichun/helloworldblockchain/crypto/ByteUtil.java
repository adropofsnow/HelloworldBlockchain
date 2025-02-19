package com.xingkaichun.helloworldblockchain.crypto;

import com.google.common.primitives.Bytes;
import com.google.common.primitives.Longs;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

/**
 * 字节工具类
 *
 * @author 邢开春 409060350@qq.com
 */
public class ByteUtil {

    public static byte[] stringToUtf8Bytes(String stringValue) {
        return stringValue.getBytes(StandardCharsets.UTF_8);
    }
    public static String utf8BytesToString(byte[] bytesValue) {
        return new String(bytesValue, StandardCharsets.UTF_8);
    }

    /**
     * (8个的字节)long转换为(大端模式)8个字节的字节数组。
     */
    public static byte[] long8ToByte8(long number) {
        return Longs.toByteArray(number);
    }
    /**
     * (大端模式)8个字节的字节数组转换为(8个的字节)long。
     */
    public static long byte8ToLong8(byte[] bytes) {
        return Longs.fromByteArray(bytes);
    }



    /**
     * 拼接数组。
     */
    public static byte[] concat(byte[]... arrays) {
        return Bytes.concat(arrays);
    }

    /**
     * 拼接长度。
     * 计算[传入字节数组]的长度，然后将长度转为8个字节的字节数组(大端)，然后将长度字节数组拼接在[传入字节数组]前，然后返回。
     */
    public static byte[] concatLength(byte[] value) {
        return concat(long8ToByte8(value.length),value);
    }

    /**
     * 碾平字节数组列表为字节数组。
     */
    public static byte[] flat(List<byte[]> values) {
        byte[] concatBytes = new byte[0];
        for(byte[] value:values){
            concatBytes = concat(concatBytes,value);
        }
        return concatBytes;
    }

    /**
     * 碾平字节数组列表为新的字节数组，然后拼接长度并返回。
     */
    public static byte[] flatAndConcatLength(List<byte[]> values) {
        byte[] flatBytes = flat(values);
        return concatLength(flatBytes);
    }

    public static boolean equals(byte[] a, byte[] a2) {
        return Arrays.equals(a,a2);
    }

    public static byte[] get(byte[] src, int srcPos, int destPos) {
        int length = destPos - srcPos;
        byte[] dest = new byte[length];
        System.arraycopy(src,srcPos,dest,0,length);
        return dest;
    }

    public static void copy(Object src, int srcPos, Object dest, int destPos, int length){
        System.arraycopy(src,srcPos,dest,destPos,length);
    }
}