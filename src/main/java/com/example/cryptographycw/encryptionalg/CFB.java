package com.example.cryptographycw.encryptionalg;

import lombok.Data;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Data
public class CFB {

    Serpent serpent;

    public byte[] getInitVector(){
        return new byte[]{1, 2,3,32,3,23,2,3,23,23,23, 12,123, 123,123,55};
    }


    public void decrypt(InputStream inputStream, OutputStream outputStream, byte[] initVectorC0)
    {
        int blockSize = serpent.BLOCK_SIZE / 8;
        int readBytes = 0;
        byte[] buf = null;
        byte[] res = null;
        try {
            while (true)
            {
                buf = inputStream.readNBytes(blockSize);
                // System.out.println("block: " + Arrays.toString(buf));
                res = serpent.encrypt(initVectorC0);
                res = xorBytes(res, buf);
                // System.out.println("decr ecnrypted block: " + Arrays.toString(res));
                initVectorC0 = buf.clone();
                if(inputStream.available() == 0)
                    break;
                outputStream.write(res);
            }
            if(res.length != 0)
            {
                res = deletePadding(res);
                outputStream.write(res);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }


    }

    public void encrypt(InputStream inputStream, OutputStream outputStream, byte[] iv)
    {
        int blockSize = serpent.BLOCK_SIZE/8;
        int readBytes = 0;
        byte[] buf = null;
        byte[] res = new byte[blockSize];
        try {
            while (true)
            {
                buf = inputStream.readNBytes(blockSize);
                // System.out.println("block: " + Arrays.toString(buf));
                if(inputStream.available() == 0)
                    break;
                res = serpent.encrypt(iv);
                res = xorBytes(res, buf);
                // System.out.println("encr ecnrypted block: " + Arrays.toString(res));
                iv = res;
                outputStream.write(res);
            }
            if(buf != null && buf.length != 0)
            {
                buf = addPadding(buf, blockSize);

                for(int i = 0; i < buf.length / blockSize; i++)
                {
                    byte[] tmp = Arrays.copyOfRange(buf, i * blockSize, (i+1) * blockSize);
                    res = serpent.encrypt(iv);
                    res = xorBytes(res, tmp);
                    iv = res.clone();

                    outputStream.write(res);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }


    }


    private byte[] xorBytes(byte[] a, byte[] b)
    {
        byte[] res = new byte[Math.min(a.length, b.length)];
        for (int i = 0; i < Math.min(a.length, b.length); ++i)
        {
            res[i] = (byte) (a[i] ^ b[i]);
        }
        return res;
    }

    private byte[] deletePadding(byte[] input) {
        boolean hasPadding = checkPadding(input);
        if (hasPadding) {
            int padding = input[input.length - 1];
            byte[] tmp = new byte[input.length - padding];
            System.arraycopy(input, 0, tmp, 0, tmp.length);
            return tmp;
        }
        return input;
    }

    private byte[] addPadding(byte[] buf, int blockSize)
    {
        int bytesToPad = blockSize - buf.length;
        if(bytesToPad == 0)
            bytesToPad = blockSize;
        byte[] res = new byte[buf.length + bytesToPad];
        for(int i = 0; i < buf.length; ++i)
        {
            res[i] = buf[i];
        }
        for(int i = buf.length; i < blockSize; ++i)
        {
            res[i] = (byte) bytesToPad;
        }
        return res;
    }

    private boolean checkPadding(byte[] in) {
        int padding = in[in.length - 1];
        int i = in.length - 1;
        int count = 0;
        while (i >= 0 && in[i] == padding) {
            count++;
            i--;
        }
        return count == padding;
    }
}