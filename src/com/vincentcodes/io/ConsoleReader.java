package com.vincentcodes.io;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * The gist of the class is to create a new thread for user input and retrieve
 * the input using a common storage object (eg. BlockingQueue) to create a non
 * -blocking read while being able to get what is read from the storage. Code 
 * example:
 * <pre>
 * ConsoleScanner sn = new ConsoleScanner(System.in);
 * for(;;)
 *     if(sn.peakLine() != null)
 *         System.out.println("Typed: " + sn.readLine());
 * </pre>
 * @see https://community.oracle.com/tech/developers/discussion/1258631/nonblocking-system-in-read
 */
public class ConsoleReader {
    // BlockingQueue is thread safe
    private final BlockingQueue<String> lines = new LinkedBlockingQueue<String>();
    // private final LinkedList<String> lines = new LinkedList<String>();

    private Thread backgroundReaderThread = null;
    private boolean closed = false;

    public ConsoleReader(InputStream input) {
        this(new InputStreamReader(input));
    }

    // You do not want the devs to invoke init() method manually
    public ConsoleReader(final Reader reader) {
        backgroundReaderThread = new Thread("Console reader"){
            @Override
            public void run(){
                BufferedReader bufferedReader = null;
                try {
                    bufferedReader = (reader instanceof BufferedReader ? (BufferedReader)reader : new BufferedReader(reader));
                    while (!Thread.interrupted()) {
                        String line = bufferedReader.readLine();
                        if (line == null) // maybe EOS, CTRL-Z or others
                            continue;
                        lines.add(line);
                    }
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                } finally {
                    closed = true;
                    if (bufferedReader != null) {
                        try {
                            bufferedReader.close();
                        } catch(IOException e) {
                            throw new UncheckedIOException(e);
                        }
                    }
                }
            }
        };
        // The JVM exits when the only running threads are all daemons.
        backgroundReaderThread.setDaemon(true); 
        backgroundReaderThread.start();
    }

    /**
     * Returns the next line and removes it from the buffer
     * @return null if no input received or the thread is interrupred
     */
    public String readLine() {
        try {
            return closed && lines.size() == 0? null : lines.take();
        } catch (InterruptedException ignored) {}
        return null;
        // return closed && lines.size() == 0? null : lines.pollFirst(); // for LinkedList
    }

    /**
     * Peek next line
     * @return the input line
     */
    public String peakLine() {
        return closed && lines.size() == 0? null : lines.peek();
    }

    /** 
     * Closes this reader (by interrupting the background reader thread).
     */
    public void close() {
        if(backgroundReaderThread != null){ 
            backgroundReaderThread.interrupt();
            backgroundReaderThread = null;
        }
    }
}
