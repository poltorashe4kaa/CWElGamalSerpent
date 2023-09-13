package com.example.cryptographycw.net;

import com.example.cryptographycw.encryptionalg.*;
import com.example.cryptographycw.entities.Message;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.*;
import java.math.BigInteger;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class Client {
    Scanner serverStream;
    int[] symKey;
    private Gson gson;
    private String host = "localhost";
    private Integer port = 8833;
    private Socket server = null;

    public Client(String host, Integer port) {
        this.host = host;
        this.port = port;
        try {
            server = new Socket(host, port);
        } catch (UnknownHostException e) {
            e.printStackTrace();
            return;
        } catch (IOException e) {
            e.printStackTrace();
        }
        gson = new GsonBuilder().create();
        try {
            serverStream = new Scanner(server.getInputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getLocalAddress() {
        return server.getLocalAddress().getHostAddress() + ":" + server.getLocalPort();
    }

    public boolean requestSession(String address) {
        System.out.println("requesSession");
        sendMessage(new Message("address", List.of(address)), server);
        if (serverStream.hasNext()) {
            Message keyRequest = parseMessage(serverStream.nextLine());
            if (!keyRequest.type.equals("key_request")) {
                return false;
            } else {
                ElGamal elGamal = new ElGamal();
                elGamal.keyGen(512);
                if (keyRequest.data.get(0).toString().equals("asym")) {
                    List<BigInteger> publicKey = elGamal.getPublicKey();

                    sendMessage(new Message("key", List.of(publicKey.get(0).toString(), publicKey.get(1).toString(), publicKey.get(2).toString())), server);
//                    sendMessage(new Message("key", List.of("111111", "222222", "333333")), server);
                    System.out.println("Асим ключ: " + publicKey.toString());
                    // получаем симметричный
                    Message symKeyMsg = new Message("key", List.of());
                    ArrayList<Byte> bytesTemp = new ArrayList<Byte>();
                    while (symKeyMsg.type.equals("key")) {
                        if (serverStream.hasNext()) {
                            symKeyMsg = parseMessage(serverStream.nextLine());
                            if (!symKeyMsg.type.equals("key")) {
                                break;
                            }
//                            System.out.println("msg = " + symKeyMsg.data);
                            BigInteger encryptedSymKey1 = new BigInteger((String) symKeyMsg.data.get(0));
                            BigInteger encryptedSymKey2 = new BigInteger((String) symKeyMsg.data.get(1));
                            byte tempByte = elGamal.decrypt(encryptedSymKey1, encryptedSymKey2);
                            // System.out.println("byte: " + tempByte);
                            bytesTemp.add(tempByte);
                        }
                    }
                    if (symKeyMsg.type.equals("keyEnd")) {

                        symKey = bytesToIntegers( bytesTemp);
                        System.out.println("Приняли сим ключ: " + symKey);
                        for (int oneb:
                             symKey) {
                            System.out.print(oneb);
                        }
                    } else {
                       return false;
                    }
                } else {
                    // получаем ассиметричный ключ
                    if (serverStream.hasNext()) {
                        Message asymKeyMsg = parseMessage(serverStream.nextLine());
                        List<BigInteger> asymKey = List.of(new BigInteger((String) asymKeyMsg.data.get(0)), new BigInteger((String) asymKeyMsg.data.get(1)), new BigInteger((String) asymKeyMsg.data.get(2)));
                        System.out.println("Асим ключ: " + asymKey.toString());
                        elGamal.setPublicKey(List.of(new BigInteger((String) asymKeyMsg.data.get(0)), new BigInteger((String) asymKeyMsg.data.get(1)), new BigInteger((String) asymKeyMsg.data.get(2))));

                        symKey = Serpent.generateKey(128);
                        List<BigInteger> tempEncryptedSymKey;
                        System.out.println("Сим ключ: " + symKey);

                        byte[] tempByteArray = integersToBytes(symKey);
                        for (int i = 0; i < tempByteArray.length; i++) {
                            // System.out.println("temp[i] ; i = " + i + "; - " + tempByteArray[i]);
                            tempEncryptedSymKey = elGamal.encrypt(tempByteArray[i]);


                            // отправляем на сервер часть зашифрованного сим ключа(пару значений)
                            sendMessage(new Message("key", List.of(tempEncryptedSymKey.get(0).toString(), tempEncryptedSymKey.get(1).toString())), server);
                        }
                        sendMessage(new Message("keyEnd", List.of()), server);
                    }
                }
            }
        }
        return true;
    }

    public boolean downloadFile(Path dir, String filename) {

        Path encryptedTargetFile = null;
        try {
            encryptedTargetFile = Files.createTempFile("encrypted", "temp");
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        long downloadedBytes = 0;
        Message downloadRequest = new Message("download", List.of(filename));
        sendMessage(downloadRequest, server);

        byte[] initVector = new byte[0];
        if (serverStream.hasNext()) {
            Message msg = parseMessage(serverStream.nextLine());
            if (msg.type.equals("init_vector")) {
                byte[] bytes = ((String) msg.data.get(0)).getBytes();
                System.out.println("sdfsdf: " + bytes);
                initVector = Base64.getDecoder().decode(bytes);
            }
        }
        
        Serpent serpent = new Serpent(symKey);

        try (OutputStream fileOutStream = new FileOutputStream(encryptedTargetFile.toFile())) {
            while (serverStream.hasNext()) {
                Message msg = parseMessage(serverStream.nextLine());
                if (msg.type.equals("fail")) {
                    Files.deleteIfExists(encryptedTargetFile);
                    return false;
                } else if (msg.type.equals("downloading")) {
                    byte[] bytes = ((String) msg.data.get(0)).getBytes();
                    byte[] decodedBytes = Base64.getDecoder().decode(bytes);
                    downloadedBytes = downloadedBytes + decodedBytes.length;
                    fileOutStream.write(decodedBytes);
                } else if (msg.type.equals("finished")) {
                    // закончили
                    System.out.println("файл загружен");
                    break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        String targetFile = dir.resolve(filename).toString();
        decryptFile(targetFile, encryptedTargetFile.toString(), initVector);
        try {
            Files.deleteIfExists(encryptedTargetFile);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public boolean loadFile(Path file) {
        byte[] initVector = CFB.getInitVector();
        Message downloadRequest = new Message("load", List.of(file.getFileName().toString()));
        sendMessage(downloadRequest, server);
        byte[] encodedBuf = Base64.getEncoder().encode(initVector);
        String strBuf = new String(encodedBuf);
        downloadRequest = new Message("init_vector", List.of(strBuf));
        System.out.println("test strbuf: " + strBuf);
        sendMessage(downloadRequest, server);

        Path encryptedFile = null;
        try {
            encryptedFile =  Files.createTempFile("encrypted", "tmp");
            encryptFile(file.toString(), encryptedFile.toString(), initVector);
        } catch (IOException e) {
           return false;
        }
        long downloadedBytes = 0;

        Serpent serpent = new Serpent(symKey);

        final int BUFSIZ = 1024 * 1024;
        byte[] buf;
        try (InputStream inputStream = new FileInputStream(encryptedFile.toFile())) {
            do {
                buf = inputStream.readNBytes(BUFSIZ);
                if (buf.length != 0) {
//                    buf = cfb.encrypt(buf, cfb.getInitVector());
                    encodedBuf = Base64.getEncoder().encode(buf);
                    strBuf = new String(encodedBuf);
                    sendMessage(new Message("loading", List.of(strBuf)), server);
                }
            }
            while (buf.length == BUFSIZ);
            sendMessage(new Message("finished", List.of()), server);
            System.out.println("загрузка файла завершена");
        } catch (FileNotFoundException e) {
            sendMessage(new Message("fail", List.of("file " + file.getFileName() + " doesn't exist")), server);
            e.printStackTrace();
            return false;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return true;
    }

    private void decryptFile(String targetFile, String encryptedFile, byte[] initVector) {
        try (InputStream iStream = new FileInputStream(encryptedFile);
             OutputStream oStream = new FileOutputStream(targetFile)) {
            Serpent serpent = new Serpent(symKey);
            CFB cfb = new CFB();
            cfb.setSerpent(serpent);
            cfb.decrypt(iStream, oStream, initVector);
        } catch (FileNotFoundException e) {
            return;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public List<String> getFileNames(){
        Message downloadRequest = new Message("fileNames", List.of());
        sendMessage(downloadRequest, server);

        if (serverStream.hasNext()){
            Message msg = parseMessage(serverStream.nextLine());
            if (msg.type.equals("fail")) {
                System.out.println("Ошибка с получением файлов на сервере");
                return null;
            } else if (msg.type.equals("fileNames")) {
                return msg.data.stream()
                        .map(object -> Objects.toString(object, null))
                        .toList();
            }
        }
        return null;
    }

    private void encryptFile(String fileToEncrypt, String encryptedFile, byte[] initVector)
    {
        try (InputStream iStream = new FileInputStream(fileToEncrypt);
             OutputStream oStream = new FileOutputStream(encryptedFile)) {
            Serpent serpent = new Serpent(symKey);
            CFB cfb = new CFB();
            cfb.setSerpent(serpent);
            cfb.encrypt(iStream, oStream, initVector);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    void sendMessage(Message msg, Socket server) {
        PrintWriter pr = null;
        try {
            pr = new PrintWriter(server.getOutputStream(), true);
            pr.println(new GsonBuilder().create().toJson(msg));
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private Message parseMessage(String msg) {
        return gson.fromJson(msg, Message.class);
    }


    byte[] integersToBytes(int[] values) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);
        try{
            for(int i=0; i < values.length; ++i)
            {
                dos.writeInt(values[i]);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return baos.toByteArray();
    }

    public static int[] bytesToIntegers(ArrayList<Byte> src) {
        int[] ints = new int[src.size() / 4];
        for (int i = 0, j = 0; i < src.size(); i += 4, j++) {
            ints[j] = ((src.get(i) & 0xFF) << 24) | ((src.get(i+1) & 0xFF) << 16) | ((src.get(i + 2) & 0xFF) << 8) | (src.get(i+3) & 0xFF);
        }
        return ints;
    }


}