package com.vincentcodes.multihandler.error;

public class ConnectionClosed extends RuntimeException{
    public ConnectionClosed() {
        super();
    }
    public ConnectionClosed(String message) {
        super(message);
    }
}
