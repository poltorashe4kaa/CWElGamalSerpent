package com.example.cryptographycw.encryptionalg;

import lombok.Getter;
import lombok.Setter;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class ElGamal {
    public static BigInteger TWO = new BigInteger("2");

    @Getter
    private List<BigInteger> sk;
    @Getter
    @Setter
    private List<BigInteger> publicKey;


    public void keyGen(int n) {
        // p = 2 * p' + 1 (p') = true
        BigInteger p = getRandomPrimeNum(n, 40);

        BigInteger g = randNum(p, new Random());
        BigInteger pPrime = p.subtract(BigInteger.ONE).divide(ElGamal.TWO);

        while (!g.modPow(pPrime, p).equals(BigInteger.ONE)) {
            if (g.modPow(pPrime.multiply(ElGamal.TWO), p).equals(BigInteger.ONE))
                g = g.modPow(TWO, p);
            else
                g = randNum(p, new Random());
        }

        // x random in [0, p' - 1]
        BigInteger x = randNum(pPrime.subtract(BigInteger.ONE), new Random());
        BigInteger h = g.modPow(x, p);
        // secret key (p, x) and public (p, g, h)
        sk = new ArrayList<>(Arrays.asList(p, x));
        publicKey = new ArrayList<>(Arrays.asList(p, g, h));

    }

    public List<BigInteger> encrypt(byte m) {
        byte[] arr = new byte[1];
        arr[0] = m;
        BigInteger message = new BigInteger(arr);

        BigInteger p = publicKey.get(0);
        BigInteger g = publicKey.get(1);
        BigInteger h = publicKey.get(2);
        BigInteger pPrime = p.subtract(BigInteger.ONE).divide(ElGamal.TWO);
        BigInteger r = randNum(pPrime, new Random());
        // (g^r, m * h^r)
        return new ArrayList<>(Arrays.asList(g.modPow(r, p), message.multiply(h.modPow(r, p))));
    }


    public byte decrypt(BigInteger gr, BigInteger mhr) {
        BigInteger p = sk.get(0);
        BigInteger x = sk.get(1);
        BigInteger hr = gr.modPow(x, p);
        // mhr(gr^x)^-1 mod p
        return mhr.multiply(hr.modInverse(p)).mod(p).byteValue();
    }


    private BigInteger getRandomPrimeNum(int bitCount, int k){
        BigInteger num = new BigInteger(bitCount, new Random());
        if(!num.testBit(0))
            num = num.add(BigInteger.ONE);
        while(!isPrimeMillerRabin(num, k))
        {
            num = num.add(BigInteger.TWO);
        }
        return num;
    }

    private boolean isPrimeMillerRabin(BigInteger n, int k){
        // 2^(-2k)
        if (n.compareTo(BigInteger.ONE) == 0 ||
                n.compareTo(BigInteger.TWO) == 0) {
            return true;
        }

        if (n.mod(BigInteger.TWO).equals(BigInteger.ZERO)) {
            return false;
        }

        // Вычисляем число r и число d, такие что n-1 = 2^r * d, где d - нечетное число.
        int r = 0;
        BigInteger d = n.subtract(BigInteger.ONE);

        while (d.mod(BigInteger.TWO).equals(BigInteger.ZERO)) {
            r++;
            d = d.divide(BigInteger.TWO);
        }

        for (int i = 0; i < k; i++) {
            BigInteger a = new BigInteger(n.bitLength() - 1, new Random()).add(BigInteger.ONE);
            BigInteger x = a.modPow(d, n);


            if (x.compareTo(BigInteger.ONE) == 0 ||
                    x.compareTo(n.subtract(BigInteger.ONE)) == 0) {
                continue; // a^d == 1 mod n
            }
            // r < s, a^(2^r)*d
            int j = 0;
            for (j = 0; j < r; j++) {
                x = x.modPow(BigInteger.TWO, n);
                if (x.compareTo(BigInteger.ONE) == 0) {
                    return false;
                }

                if (x.compareTo(n.subtract(BigInteger.ONE)) == 0) {
                    break;
                }
            }
            if (j == r) {
                return false;
            }
        }

        return true;
    }


    public BigInteger randNum(BigInteger N, Random prg) {
        return new BigInteger(N.bitLength() + 100, prg).mod(N);
    }


    public static void main(String[] args) {
        ElGamal elGamal = new ElGamal();
        elGamal.keyGen(512);

        int b = -115;
        int c = 115;
        int g = -111;

        List<BigInteger> cipher;

        cipher = elGamal.encrypt((byte)g);
        System.out.println(elGamal.decrypt(cipher.get(0), cipher.get(1)));
    }
}
