package com.vincentcodes.io;

public interface BasicTerminalLineReader {
    String readLine();

    String peekLine();

    void close();
}
