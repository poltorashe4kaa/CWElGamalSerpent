package com.example.cryptographycw.net;


import com.example.cryptographycw.entities.Message;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonToken;

import java.io.*;
import java.math.BigInteger;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class Transfering implements Runnable {
    private final Socket firstClient;
    private final Socket secondClient;
    private final Map<String, String> files = new ConcurrentHashMap<>();
    Scanner scannerFirst = null;
    Scanner scannerSecond = null;
    PrintWriter outputFirst = null;
    PrintWriter outputSecond = null;
    private Gson gson;
    private Path sessionDir;
    private List<BigInteger> asymKey;
    private byte[] symKey;

    public Transfering(Socket first, Socket second) {
        try {
            sessionDir = Files.createTempDirectory("session");
            System.out.println("temp file " + sessionDir.getFileName());
            scannerFirst = new Scanner(first.getInputStream());
            scannerSecond = new Scanner(second.getInputStream());
            outputFirst = new PrintWriter(first.getOutputStream(), true);
            outputSecond = new PrintWriter(second.getOutputStream(), true);
        } catch (IOException e) {
            e.printStackTrace();
        }
        gson = new GsonBuilder().create();
        firstClient = first;
        secondClient = second;
    }

    @Override
    public void run() {
        // распределение ключей
        Message genAsymKey = new Message("key_request", List.of("asym"));
        Message genSymKey = new Message("key_request", List.of("sym"));
        sendMessage(genAsymKey, CLIENT.FIRST);
        sendMessage(genSymKey, CLIENT.SECOND);
        System.out.println("requests are sent to clients");
        // ждем асиметричного ключа от первого
        if (scannerFirst.hasNext()) {
            String test = scannerFirst.nextLine();
            // System.out.println(" ++ " + test);
            Message msg = parseMessage(test);
            asymKey = List.of(new BigInteger((String) msg.data.get(0)), new BigInteger((String) msg.data.get(1)), new BigInteger((String) msg.data.get(2)));
            System.out.println("got asym key from first client: " + asymKey);
        }
        // отправляем ключ второму
        if (asymKey == null) {
            System.out.println("отсутствует симметричный ключ");
            return;
        }
        Message asym = new Message("key", List.of(asymKey.get(0).toString(), asymKey.get(1).toString(), asymKey.get(2).toString()));
        sendMessage(asym, CLIENT.SECOND);
        // получаем от второго симметричный ключ
        Message msg = null;
        do {
            //System.out.println("test massage");
            if (scannerSecond.hasNext()) {
                msg = parseMessage(scannerSecond.nextLine());
                //System.out.println("msg = " + msg.type + " " + msg.data);
                sendMessage(msg, CLIENT.FIRST);
            }
        }
        while (msg.type.equals("key"));
        if (!msg.type.equals("keyEnd")) {
            return;
        }
        new Thread(() -> clientListener(CLIENT.FIRST)).start();
        new Thread(() -> clientListener(CLIENT.SECOND)).start();
    }

    public void clientListener(CLIENT client) {
        Scanner scanner = client.equals(CLIENT.FIRST) ? scannerFirst : scannerSecond;
        while (scanner.hasNext() && !Thread.currentThread().isInterrupted()) {
            Message msg = parseMessage(scanner.nextLine());
            //System.out.println(" type " + msg.type);
            if (msg.type.equals("download")) {
                String filename = (String) msg.data.get(0);
                byte[] encodedBuf = Base64.getEncoder().encode(getInitVector(filename));
                sendMessage(new Message("init_vector", List.of(new String(encodedBuf))), client);
                if (files.containsKey(filename)) {
                    downloadFile(filename, client);
                } else {
                    sendMessage(new Message("fail", List.of("doesn't exist")), client);
                }
            } else if (msg.type.equals("load")) {
                System.out.println("запрос на загрузку файла");
                String filename = (String) msg.data.get(0);
                if (files.containsKey(filename)) {
                    sendMessage(new Message("fail", List.of("file with this name already exist")), client);
                } else {
                    byte[] initVector = null;
                    System.out.println("sfdsdfsdf");
                    if (scanner.hasNext())
                    {
                        msg = parseMessage(scanner.nextLine());
                        System.out.println("sdfsdfsdfsdfsdfsdfsdf");
                        if (msg.type.equals("init_vector")){
                            System.out.println("sdfsdfsdfsdfsdfsdfsdf");
                            byte[] decodedBuf = Base64.getDecoder().decode(((String) msg.data.get(0)).getBytes());
                            byte[] bytes = ((String) msg.data.get(0)).getBytes();
                            System.out.println("sdfsdf: " + bytes);
                            initVector = Base64.getDecoder().decode(bytes);
                            System.out.println("dssdf" + initVector);
                        }
                    }
                    System.out.println("dfdsfsdfsddfsddfsddf");
                    if(initVector == null) {
                        sendMessage(new Message("fail", List.of("file with this name already exist")), client);
                        System.out.println("sdfdsdfkhbfdjhbsdkjndalknfskdjfnsdksjfbsdhfbdsf");
                        return;
                    }
                    saveInitVector(filename, initVector);
                    System.out.println("filename" + filename);
                    files.put(filename, "");
                    loadFile(filename, client);
                }
            } else if (msg.type.equals("fileNames")){
                List<Object> list = new ArrayList<>(files.keySet());
                System.out.println(list);
                sendMessage(new Message("fileNames", list), client);
            } else
            {
            }
        }

    }

    private byte[] getInitVector(String fileName){
        Path file = sessionDir.resolve(fileName + "init_vector.txt");
        System.out.println("filename - " + file.getFileName());
        try {
            return Files.readAllBytes(file);
        } catch (IOException e) {
            System.out.println("ERRRRROOOORRR");
            throw new RuntimeException(e);}
    }

    private void saveInitVector(String fileName, byte[] initVector) {
        byte[] arr = new byte[initVector.length];
        for(int i = 0; i < initVector.length; i++){
            arr[i] = initVector[i];
        }
        try (FileOutputStream fos = new FileOutputStream(sessionDir.resolve(fileName + "init_vector.txt").toFile())) {
            fos.write(arr);
        } catch (IOException e) {
            e.printStackTrace();
        }


    }

    private boolean downloadFile(String filename, CLIENT output) {
        Path file = sessionDir.resolve(filename);

        final int BUFSIZ = 1024 * 1024; // Mb
        byte[] buf;
        try (InputStream inputStream = new FileInputStream(file.toString())) {
            do {
                buf = inputStream.readNBytes(BUFSIZ);
                byte[] encodedBuf = Base64.getEncoder().encode(buf);
                if (buf.length != 0)
                    sendMessage(new Message("downloading", List.of(new String(encodedBuf))), output);
            }
            while (buf.length == BUFSIZ);
            sendMessage(new Message("finished", List.of()), output);
            System.out.println("файл был скачан");
        } catch (FileNotFoundException e) {
            sendMessage(new Message("fail", List.of("file " + filename + " doesn't exist")), output);
            return false;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return true;
    }

    private boolean loadFile(String filename, CLIENT client) {
        Path file = sessionDir.resolve(filename);
//        if(Files.exists(file))
//        {
//            sendMessage(new Message("fail", List.of("file " + filename + " already exists")), client);
//            return false;
//        }
        byte[] buf;
        Scanner scanner = client.equals(CLIENT.FIRST) ? scannerFirst : scannerSecond;
        try (OutputStream outputStream = new FileOutputStream(file.toString())) {
            System.out.println("start loading file");

            while (scanner.hasNext() && !Thread.currentThread().isInterrupted()) {
                Message msg = parseMessage(scanner.nextLine());
                if (msg.type.equals("loading")) {
                    byte[] decodedBuf = Base64.getDecoder().decode(((String) msg.data.get(0)).getBytes());
                    outputStream.write(decodedBuf);
                } else if (msg.type.equals("finished")) {
                    System.out.println("файл загружен ");
                    break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return true;
    }

    void sendMessage(Message msg, CLIENT client) {
        String message = gson.toJson(msg);
        PrintWriter pr = client.equals(CLIENT.FIRST) ? outputFirst : outputSecond;
        pr.println(message);
    }

    private Message parseMessage(String msg) {
        return gson.fromJson(msg, Message.class);
    }

    private enum CLIENT {
        FIRST,
        SECOND
    }
}