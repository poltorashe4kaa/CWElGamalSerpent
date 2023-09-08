package com.example.cryptographycw.controllers;

import com.example.cryptographycw.HelloApplication;
import com.example.cryptographycw.net.Client;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Objects;

public class HelloController {

    Client client;
    boolean isPressed = false;

    @FXML
    private Label addressLabel;

    @FXML
    private Button connectBtn;

    @FXML
    private TextField hostTextField;

    @FXML
    private TextField portTextField;

    @FXML
    private Label statusLabel;

    @FXML
    public void initialize() {
        client = new Client("localhost", 8433);
        addressLabel.setText(client.getLocalAddress());
    }

    @FXML
    void connectBtnClick() {
        statusLabel.setText("Подключение...");
        connectBtn.setDisable(true);

        Task<Void> result = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                System.out.println(hostTextField.getText() + ":" + portTextField.getText());
                boolean res = client.requestSession(hostTextField.getText() + ":" + portTextField.getText());
                if (res) {
                    return null;
                }
                connectBtn.setDisable(false);
                statusLabel.setText("Ошибка подключения.");
                return null;
            }
        };

        result.setOnSucceeded(event -> loadTransferWindow());
        new Thread(result).start();
    }

    void loadTransferWindow() {
        try {
            FXMLLoader loader = new FXMLLoader(Objects.requireNonNull(HelloApplication.class.getResource("transferView.fxml")));
            Parent root = loader.load();
            TransferController transferController = loader.getController();
            transferController.init(client);
            Scene scene = new Scene(root);
            Stage stage = (Stage) portTextField.getScene().getWindow();
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}