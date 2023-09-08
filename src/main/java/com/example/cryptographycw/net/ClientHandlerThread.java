package com.example.cryptographycw.net;

import com.example.cryptographycw.entities.Message;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;
// обрабатывает подключение клиентов друг к другу
public class ClientHandlerThread implements Runnable {
    private static final Map<String, Socket> waitingSessions = new ConcurrentHashMap<>();

    public final String CONNECTION = "connection";
    PrintWriter output = null;
    Scanner input = null;

    private Gson gson;
    private Socket client;

    public ClientHandlerThread(Socket client) {
        try {
            input = new Scanner(client.getInputStream());
            output = new PrintWriter(client.getOutputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
        gson = new GsonBuilder().create();
        this.client = client;
    }

    @Override
    public void run() {
        // подключился пользователь
        try {
            //Scanner input = new Scanner(client.getInputStream());
            // принимаем адрес клиента, с которым хочет взаимодействовать другой клиент
            if (input.hasNext()) {

                // проверяем, ждет ли нас кто-то

                String address = client.getInetAddress().getHostAddress() + ":" + client.getPort();
                Message msg = parseMessage(input.nextLine());
                String buddiesAddress = (String) msg.data.get(0); // host + port
                System.out.println("адрес клиента, с которым хочет взаимодействовать другой клиент: " + buddiesAddress);
                System.out.println("адрес клиента: " + address);
                if (waitingSessions.containsKey(address)) {
                    //System.out.println("find pair!!!");
                    Socket buddiesSocket = waitingSessions.get(address);
                    // проверяем ждет ли он нас
                    if (buddiesAddress.equals(buddiesSocket.getInetAddress().getHostAddress() + ":" + buddiesSocket.getPort())) {
                        Transfering session = new Transfering(waitingSessions.get(address), client);
                        Thread thread = new Thread(session);
                        thread.start();
                        thread.join();
                    }
                } else {
                    waitingSessions.put(buddiesAddress, client);
                    //sendMessage(new Message("wait", List.of("hell")), client);
                }
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    void sendMessage(Message msg, Socket client) {
        output.println(new GsonBuilder().create().toJson(msg));
    }

    private Message parseMessage(String msg) {
        //System.out.println("debug: " + msg);
        return gson.fromJson(msg, Message.class);
    }
}
