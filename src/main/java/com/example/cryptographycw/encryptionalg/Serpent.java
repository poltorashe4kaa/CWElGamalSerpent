package com.example.cryptographycw.encryptionalg;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Random;


import static com.example.cryptographycw.encryptionalg.SerpentHelper.*;

public class Serpent {
    private static int[] key;
    public static int BLOCK_SIZE = 128;

    private byte[][] K = new byte[33][16];


    public Serpent(int[] key) {
        this.key = key.clone();
        getPuddedKey();
        generateRoundKeys(generatePreKeys());
    }

    public static byte[] permutationBits(byte[] array, int[] pPermutationBlock){
        byte[] resArray = new byte[pPermutationBlock.length / 8];
        for (int i = 0; i < pPermutationBlock.length; i++){
            int pos = pPermutationBlock[i];
            int bit = getBitFromArray(array, pos);
            setBitIntoArray(resArray, i, bit);
        }
        return resArray;
    }
    public static int getBitFromArray(byte[] array, int pos){
        int bytePos = pos / 8;
        int bitPos = pos % 8;
        return ((array[bytePos] >>> (8 - bitPos - 1)) & 0b1);
    }
    public static void setBitIntoArray(byte[] array, int pos, int bit){
        int bytePos = pos / 8;
        int bitPos = pos % 8;
        byte oldByte = array[bytePos];
        oldByte |= bit << (8 - bitPos - 1);
        array[bytePos] = oldByte;
    }
    public void getPuddedKey() {
        int[] pudded = new int[8];
        int len = key.length;
        System.arraycopy(key, 0, pudded, 0, len);
        if (len < 8) {
            pudded[len] = 1 << 31;
            for (int i = len + 1; i < 8; i++) {
                pudded[i] = 0;
            }
        }
        key = pudded;
    }

    public static int[] generateKey(int k){
        BigInteger a = new BigInteger(k, new Random());
        byte arr[] = a.toByteArray();
        for (int i = 0; i < arr.length; i++) {
            arr[i] = arr[i] < 0? (byte) -arr[i] : arr[i];
        }
        return bytesToIntegers(arr);
    }

    public static int[] bytesToIntegers(byte[] src) {
        int dstLength = src.length >>> 2;
        int[]dst = new int[dstLength];

        for (int i=0; i<dstLength; i++) {
            int j = i << 2;
            int x = 0;
            x += (src[j++] & 0xff) << 0;
            x += (src[j++] & 0xff) << 8;
            x += (src[j++] & 0xff) << 16;
            x += (src[j++] & 0xff) << 24;
            dst[i] = x;
        }
        return dst;
    }

    public static int[] generatePreKeys() {
        int goldenRatio = 0x9e3779b9;
        int[] w = new int[132];
        System.arraycopy(key, 0, w, 0, 8);
        for (int i = 8; i < 132; i++) {
            int temp = w[i - 8] ^ w[i - 5] ^ w[i - 3] ^ w[i - 1] ^ goldenRatio ^ (i - 8);
            w[i] = (temp << 11) | (temp >>> 21);
        }
        return w;
    }

    public void generateRoundKeys(int w[]) {
        int j = 3;
        byte[][] keys = new byte[132][4];
        for (int i = 0; i < 33; i++) {
            for (int k = i * 4; k < i * 4 + 4; k++) {
                var key_left = permutationBits(new byte[] {(byte) (w[k] >>> 24 & 0xff), (byte) (w[k] >>> 16 & 0x00ff)}, sBoxTable[j]);
                var key_right = permutationBits(new byte[] {(byte) (w[k] >>> 8 & 0x0000ff), (byte) (w[k] & 0x000000ff)}, sBoxTable[j]);
                keys[k][0] = (key_left[0]);
                keys[k][1] = (key_left[1]);
                keys[k][2] = (key_right[0]);
                keys[k][3] = (key_right[1]);
                j--;
                if (j == -1){
                    j = 7;
                }
            }
        }
        for (int i = 0; i < 33; i++) {
            for (int t = 0; t < 16; t++) {
                K[i][t] = keys[i * 4 + t / 4][t % 4];
            }
        }

    }

    public byte[] decrypt(byte[] input) {
        input = permutationBits(input, IP);
        for (int i = 31; i >= 0; i--) {

            if (i == 31){
                for (int j = 0; j < 16; j++){
                    input[j] = (byte) (K[i + 1][j] ^ input[j]);
                }
            }
            else {
                inverseLinearTransform(input);
            }

            for (int j = 0; j < 16; j++){
                byte tempL = (byte) (input[j] & 0x0f);
                byte tempR = (byte) ((input[j] & 0xf0) >> 4);
                input[j] = (byte) ((replaceWithInverseSBox(tempL, i % 8) << 4) | replaceWithInverseSBox(tempR, i % 8) & 0xff);
            }
            for (int j = 0; j < 16; j++){
                input[j] = (byte) (K[i][j] ^ input[j]);
            }
        }

        input = permutationBits(input, FP);
        return input;
    }

    public byte[] encrypt(byte[] input) {//128 на вход
        input = permutationBits(input, IP);

        for (int i = 0; i < 32; i++){
            //rounds
            for (int j = 0; j < 16; j++){
                input[j] = (byte) (K[i][j] ^ input[j]);
            }


            for (int j = 0; j < 16; j++){
                byte tempL = (byte) (input[j] & 0x0f);
                byte tempR = (byte) ((input[j] & 0xf0) >> 4);
                input[j] = (byte) ((replaceWithSBox(tempL, i % 8) << 4) | replaceWithSBox(tempR, i % 8) & 0xff);
            }

            if (i != 31){
                linearTransform(input);
            } else {
                for (int j = 0; j < 16; j++){
                    input[j] = (byte) (K[i + 1][j] ^ input[j]);
                }
            }
        }
        input = permutationBits(input, FP);
        return input;
    }

    public byte replaceWithSBox(byte b, int i){
        return (byte) sBoxTable[i][b];
    }

    public byte replaceWithInverseSBox(byte b, int i){
        return (byte) sBoxInverseTable[i][b];
    }

    public void linearTransform(byte[] input) {
        int[] x = new int[4];
        for (int i = 0; i < 4; i++) {
            x[i] = (input[i*4] & 0xff)<< 24 | (input[i*4 + 1]& 0xff) << 16 | (input[i*4 + 2] & 0xff) << 8 | (input[i*4 + 3] & 0xff);
        }
        x[0] = ((x[0] << 13) | (x[0] >>> (32 - 13)));
        x[2] = x[2] << 3 | x[2] >>> 29;
        x[1] = x[1] ^ x[0] ^ x[2];
        x[3] = x[3] ^ x[2] ^ (x[0] << 3);
        x[1] = x[1] << 1 | x[1] >>> 31;
        x[3] = x[3] << 7 | x[3] >>> 25;
        x[0] = x[0] ^ x[1] ^ x[3];
        x[2] = x[2] ^ x[3] ^ (x[1] << 7);
        x[0] = x[0] << 5 | x[0] >>> 27;
        x[2] = x[2] << 22 | x[2] >>> 10;

        ByteBuffer buffer = ByteBuffer.allocate(16);
        IntBuffer intBuf = IntBuffer.wrap(x);
        buffer.asIntBuffer().put(intBuf);
        System.arraycopy(buffer.array(), 0, input, 0, 16);
    }

    public void inverseLinearTransform(byte[] input) {
        int[] x = new int[4];
        for (int i = 0; i < 4; i++) {
            x[i] = (input[i * 4] & 0xff) << 24 | (input[i * 4 + 1] & 0xff) << 16 | (input[i * 4 + 2] & 0xff) << 8 | (input[i * 4 + 3] & 0xff);
        }
        x[2] = x[2] >>> 22 | x[2] << 10;
        x[0] = x[0] >>> 5 | x[0] << 27;
        x[2] = x[2] ^ x[3] ^ (x[1] << 7);
        x[0] = x[0] ^ x[1] ^ x[3];
        x[3] = x[3] >>> 7 | x[3] << 25;
        x[1] = x[1] >>> 1 | x[1] << 31;
        x[3] = x[3] ^ x[2] ^ (x[0] << 3);
        x[1] = x[1] ^ x[0] ^ x[2];
        x[2] = x[2] >>> 3 | x[2] << 29;
        x[0] = x[0] >>> 13 | x[0] << 19;
        ByteBuffer buffer = ByteBuffer.allocate(16);
        IntBuffer intBuf = IntBuffer.wrap(x);
        buffer.asIntBuffer().put(intBuf);
        System.arraycopy(buffer.array(), 0, input, 0, 16);
    }

    public static void main(String[] args) {
        Serpent serpent = new Serpent(Serpent.generateKey(128));
        BigInteger a = new BigInteger(128, new Random());
        byte arr[] = a.toByteArray();
        System.out.println(arr.length);
        for (int i = 0; i < arr.length; i++) {
            arr[i] = arr[i] < 0? (byte) -arr[i] : arr[i];
        }

        for (byte oneb:
                arr) {
            System.out.print(oneb + " ");
        }

        byte[] cipher = serpent.encrypt(arr);

        byte[] res = serpent.decrypt(cipher);

        System.out.println("------------------------------");
        for (byte oneb:
             res) {
            System.out.print(oneb + " ");
        }
    }
}
