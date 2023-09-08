module com.example.cryptographycw {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.controlsfx.controls;
    requires org.kordamp.bootstrapfx.core;
    requires com.google.gson;
    requires lombok;
    requires org.apache.commons.io;

    opens com.example.cryptographycw to javafx.fxml;
    exports com.example.cryptographycw;
    exports com.example.cryptographycw.controllers;
    opens com.example.cryptographycw.controllers to javafx.fxml;

    opens com.example.cryptographycw.entities to com.google.gson;
    exports com.example.cryptographycw.entities;

    opens com.example.cryptographycw.encryptionalg;
    exports com.example.cryptographycw.net;
    opens com.example.cryptographycw.net to com.google.gson;
}