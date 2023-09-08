package com.example.cryptographycw.net;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class Server {
    private String host = "localhost";
    private Integer port = 8833;

    public Server(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public void start() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {

            while (!Thread.currentThread().isInterrupted()) {
                Socket client = serverSocket.accept();
                // System.out.println("Test");
                ClientHandlerThread clientHandler = new ClientHandlerThread(client);
                System.out.println("Клиент подключился");

                new Thread(clientHandler).start();
            }
        } catch (IOException ex) {

        }
    }
}
