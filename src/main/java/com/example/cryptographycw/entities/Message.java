package com.example.cryptographycw.entities;

import java.math.BigInteger;
import java.util.List;

public class Message {
    public String type;
    public List<Object> data;

    public Message(String type, List<Object> data) {
        this.type = type;
        this.data = data;

    }
}
