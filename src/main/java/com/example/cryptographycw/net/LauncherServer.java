package com.example.cryptographycw.net;

import org.apache.commons.io.FileSystemUtils;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class LauncherServer {

    public static void main(String[] args) {
        ExecutorService exc = Executors.newFixedThreadPool(2);
        Server server = new Server("localhost", 8433);
        exc.execute(server::start);
    }
}
