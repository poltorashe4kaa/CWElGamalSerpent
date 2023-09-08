package com.example.cryptographycw.controllers;

import com.example.cryptographycw.net.Client;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.nio.file.Paths;
import java.util.List;

public class TransferController {

    private Client client;
    @FXML
    private TextField fileNameTextField;
    @FXML
    private VBox filesAvailableVBox;
    @FXML
    private Label labelReceivingStatus;
    @FXML
    private Label labelSendingStatus;
    @FXML
    private TextField pathDirectoryField;
    @FXML
    private TextField pathFileField;
    @FXML
    private Button receiveBtn;
    @FXML
    private Button refreshFilesAvailable;
    @FXML
    private Button reviewDirectoryBtn;
    @FXML
    private Button reviewFileBtn;
    @FXML
    private Button toSend;
    @FXML
    private Button cancel1Btn;

    public void init(Client client) {
        this.client = client;
    }

    @FXML
    void refreshFilesAvailableClick(ActionEvent event) {
        List<String> fileNames = client.getFileNames();
            filesAvailableVBox.getChildren().clear();
        for (String str: fileNames
             ) {
            filesAvailableVBox.getChildren().add(new Label(str));
        }
    }

    @FXML
    void reviewDirectoryBtnClick(ActionEvent event) {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        File selectedDirectory = directoryChooser.showDialog((Stage) receiveBtn.getScene().getWindow());

        if (selectedDirectory != null) {
            pathDirectoryField.setText(selectedDirectory.getAbsolutePath());
        }
    }

    @FXML
    void reviewFileBtnClick(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        File selectedFile = fileChooser.showOpenDialog((Stage) toSend.getScene().getWindow());

        if (selectedFile != null) {
            pathFileField.setText(selectedFile.getAbsolutePath());
        }
    }

    @FXML
    void toSendClick(ActionEvent event) {
        if (client.loadFile(Paths.get(pathFileField.getText()))) {
            labelSendingStatus.setText("Файл загружен на сервер");
        } else {
            labelSendingStatus.setText("Файл не был загружен на сервер");
        }
    }

    @FXML
    void receiveBtnClick(ActionEvent event) {
        if (client.downloadFile(Paths.get(pathDirectoryField.getText()), fileNameTextField.getText())) {
            labelReceivingStatus.setText("Файл скачан");
        } else {
            labelReceivingStatus.setText("Файл не был скачан");
        }

    }

    @FXML
    void cancel1BtnClick(ActionEvent event) {

    }
}
