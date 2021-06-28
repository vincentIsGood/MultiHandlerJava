package com.vincentcodes.multihandler;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

import com.vincentcodes.io.ConsoleReader;
import com.vincentcodes.logger.Logger;
import com.vincentcodes.multihandler.util.Signal;
import com.vincentcodes.multihandler.util.SignalUtils;

// ExecutorService is not used. It makes thing harder
/**
 * Inspired by the metasploit multihandler module, you cannot do it on
 * simply netcat since it is single-threaded. However, with a multi-
 * threaded server, it is possible to imitate its behavior.
 * @author Vincent Ko
 */
public class MultiHandler {
    public static Logger LOGGER = new Logger();
    private ServerSocket server;
    private ConsoleReader consoleReader;
    private int port;

    private List<ConnectionThread> threads;
    private volatile ConnectionThread currentThread;
    private boolean receiveConnections = true;
    private volatile boolean done = false;
    private volatile boolean inputRequested = false; // higher priority

    public MultiHandler() throws IOException{
        this(4444);
    }
    
    public MultiHandler(int port) throws IOException{
        this.port = port;
        server = new ServerSocket(port);
        consoleReader = new ConsoleReader(System.in);
        threads = new ArrayList<>();
    }
    
    public void start() throws IOException{
        LOGGER.log("Starting Multihandler on port " + server.getLocalPort());
        registerSignals();
        enableUserInput();
        Socket socket;
        while(!done){
            try{
                if(!receiveConnections){
                    Thread.sleep(100);
                    continue;
                }
                socket = server.accept();
                System.out.println();
                ConnectionThread thread = new ConnectionThread(Integer.toString(threads.size()), socket);
                thread.start();
                threads.add(thread);
                currentThread = thread;
            }catch(IOException | InterruptedException e){
                if(!e.getMessage().contains("Socket closed"))
                    e.printStackTrace();
            }
        }
        LOGGER.log("Bye");
    }

    public void enableUserInput(){
        new Thread("User input handler"){
            @Override
            public void run(){
                boolean printed = false;
                while(!done){
                    try{
                        Thread.sleep(100);
                    }catch(InterruptedException ignored){}
                    if(inputRequested) continue;
                    if(currentThread != null){
                        try{
                            String line = consoleReader.peakLine();
                            if(line == null) continue;

                            consoleReader.readLine(); // remove the line from buffer
                            if(!handleSystemCommand(line)){
                                currentThread.send(line + "\n");
                            }
                        }catch(IOException e){
                            e.printStackTrace();
                            bgCurrentConnection();
                        }
                    }else{
                        if(!printed){
                            System.out.print("multihandler> ");
                            printed = true;
                        }
                        String line = consoleReader.peakLine();
                        if(line == null) continue;

                        consoleReader.readLine(); // remove the line from buffer
                        printed = false;
                        handleSystemCommand(line);
                    }
                }
                return;
            }
        }.start();
    }

    /**
     * @param line
     * @return is command handled
     */
    public boolean handleSystemCommand(String line){
        String[] splited = line.split("\\s{1,}");
        String command = splited[0];
        switch(command){
            case "sysexit": 
                done = true;
                try{ closeAll(); }catch(Exception e){}
                return true;
            case "sessions":
                if(splited.length > 1){
                    int sessionNum = Integer.parseInt(splited[1]);
                    if(sessionNum >= 0 && sessionNum < threads.size()){
                        if(!threads.get(sessionNum).isDoneFor()){
                            LOGGER.log("Changing current thread to thread with ID: " + sessionNum);
                            currentThread = threads.get(sessionNum);
                        }else{
                            LOGGER.warn("The session is closed already");
                        }
                    }else{
                        LOGGER.log("Cannot find the specified session");
                    }
                }else{
                    LOGGER.log("id | state (up)");
                    for(ConnectionThread thread : threads){
                        if(currentThread != null){
                            LOGGER.log(String.format("%2d | %s", 
                                Integer.parseInt(thread.getName()), 
                                Boolean.toString(!thread.isDoneFor()) + " " + (thread.getName().equals(currentThread.getName())? "(current)" : "")));
                        }else{
                            LOGGER.log(String.format("%2d | %s", 
                                Integer.parseInt(thread.getName()), 
                                Boolean.toString(!thread.isDoneFor())));
                        }
                    }
                }
                return true;
            case "togglereceive":
                receiveConnections = !receiveConnections;
                try{
                    if(receiveConnections){
                        LOGGER.warn("Listening on port " + port);
                        server = new ServerSocket(port);
                    }else{
                        LOGGER.warn("Will not receive any other connections");
                        server.close();
                    }
                }catch(IOException e){
                    e.printStackTrace();
                }
                return true;
            case "help":
                LOGGER.log("sysexit");
                LOGGER.log("sessions [<thread_num>]");
                LOGGER.log("togglereceive");
                return true;
        }
        return false;
    }

    public void registerSignals(){
        SignalUtils.handle(Signal.create("INT"), (sig)->{
            // Send Ctrl+C as normal
            if(currentThread != null){
                try{
                    currentThread.send("\u0003");
                    System.out.print("\rClose the current session? (y/N) ");
                    String answer = getImmediateUserInput();
                    if(answer.toLowerCase().trim().equals("y")){
                        currentThread.close();
                        currentThread = null;
                    }
                }catch(IOException e){
                    e.printStackTrace();
                }
            }else{
                System.out.print("\rAre you sure you want to exit? (y/N) ");
                String answer = getImmediateUserInput();
                if(answer.toLowerCase().trim().equals("y")){
                    System.out.println(sig.toString() + " received, quitting");
                    System.exit(-1);
                }
            }
        });
        if(System.getProperty("os.name").toLowerCase().contains("linux")){
            SignalUtils.handle(Signal.create("TSTP"), (sig)->{
                if(currentThread == null){
                    System.out.println(sig.toString() + " received, no operation");
                    return;
                }
                System.out.print("\rBackground the current session? (y/N) ");
                String answer = getImmediateUserInput();
                if(answer.toLowerCase().trim().equals("y")){
                    bgCurrentConnection();
                }
            });
        }
    }

    public void bgCurrentConnection(){
        currentThread = null;
        System.out.println();
        LOGGER.warn("Putting current thread into background");
    }

    /**
     * This is a blocking operation
     * @return user console input
     */
    public String getImmediateUserInput(){
        String answer = null;
        requestInput();
        while((answer = consoleReader.peakLine()) == null);
        answer = consoleReader.readLine();
        cancelInputRequest();
        return answer;
    }

    public void requestInput(){
        inputRequested = true;
    }
    public void cancelInputRequest(){
        inputRequested = false;
    }

    public void closeAll() throws IOException{
        server.close();
        for(ConnectionThread thread : threads){
            thread.close();
        }
    }
}
