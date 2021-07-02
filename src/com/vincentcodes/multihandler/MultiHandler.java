package com.vincentcodes.multihandler;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

import com.vincentcodes.io.BasicTerminalLineReader;
import com.vincentcodes.io.ConsoleReader;
import com.vincentcodes.logger.Logger;
import com.vincentcodes.multihandler.util.Signal;
import com.vincentcodes.multihandler.util.SignalUtils;
import com.vincentcodes.terminal.UnblockedTerminalReader;
import com.vincentcodes.terminal.keyevent.BackspaceKeyPressed;

// ExecutorService is not used. It makes thing harder
/**
 * Inspired by the metasploit multihandler module, you cannot do it on
 * simply netcat since it is single-threaded. However, with a multi-
 * threaded server, it is possible to imitate its behavior.
 * @author Vincent Ko
 */
public class MultiHandler {
    public static Logger LOGGER = new Logger();
    public static boolean BETA_ENABLED = false;

    private ServerSocket server;
    private BasicTerminalLineReader consoleReader;
    private int port;

    private List<ConnectionThread> threads;
    private volatile ConnectionThread currentThread;
    private boolean receiveConnections = true;
    private boolean done = false;
    private boolean inputRequested = false; // higher priority
    private int caretResetTime = 150;

    public MultiHandler() throws IOException{
        this(4444);
    }
    
    public MultiHandler(int port) throws IOException{
        this.port = port;
        server = new ServerSocket(port);
        if(BETA_ENABLED){
            LOGGER.warn("Enabling beta features, bugs may present inside the application");
            LOGGER.warn("For example, the starts from STX if a program takes time to process");
            LOGGER.warn("(pressing enter a couple of times MAY fix it)");
            consoleReader = new UnblockedTerminalReader();
        }else
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
                    wrappedSleep(100);
                    continue;
                }
                if(server.isClosed())
                    continue;
                socket = server.accept();
                System.out.println();
                ConnectionThread thread = new ConnectionThread(Integer.toString(threads.size()), socket);
                thread.start();
                threads.add(thread);
                currentThread = thread;
            }catch(IOException e){
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
                    wrappedSleep(100);
                    if(inputRequested) continue;
                    if(currentThread != null){
                        try{
                            String line = consoleReader.peekLine();
                            if(line == null) continue;

                            consoleReader.readLine(); // remove the line from buffer
                            if(!handleSystemCommand(line)){
                                currentThread.send(line + "\n");
                                if(BETA_ENABLED){
                                    wrappedSleep(caretResetTime);
                                    ((UnblockedTerminalReader)consoleReader).resetCaretStartPos();
                                }
                            }
                        }catch(IOException e){
                            e.printStackTrace();
                            bgCurrentConnection();
                        }
                    }else{
                        if(!printed){
                            System.out.print("multihandler> ");
                            if(BETA_ENABLED){
                                ((UnblockedTerminalReader)consoleReader).resetCaretStartPos();
                            }
                            printed = true;
                        }
                        String line = consoleReader.peekLine();
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
            case "caretresettime":
                if(splited.length > 1){
                    int time = Integer.parseInt(splited[1]);
                    if(time >= 0){
                        LOGGER.log("Setting caret reset time from " + caretResetTime + " -> " + time);
                        caretResetTime = time;
                    }
                }else{
                    LOGGER.err("time is needed as an argument");
                }
                return true;
            case "seterasechar":
                if(splited.length > 1){
                    int eraseChar = Integer.parseInt(splited[1]);
                    if(eraseChar >= 0){
                        LOGGER.log("Setting erase char to from to " + eraseChar);
                        BackspaceKeyPressed.setBackSpaceKey((char)eraseChar);
                    }
                }else{
                    LOGGER.err("A number is needed as an argument");
                }
                return true;
            case "help":
                LOGGER.log("sysexit");
                LOGGER.log("sessions [<thread_num>]");
                LOGGER.log("togglereceive");
                LOGGER.log("caretresettime <ms>");
                LOGGER.log("seterasechar <8 / 127>");
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
                    closeAll();
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
        while((answer = consoleReader.peekLine()) == null);
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

    public void closeAll(){
        try{
            done = true;
            if(consoleReader instanceof UnblockedTerminalReader)
                ((UnblockedTerminalReader)consoleReader).close();
            server.close();
            for(ConnectionThread thread : threads){
                thread.close();
            }
        }catch(IOException e){
            throw new UncheckedIOException(e);
        }
    }

    private void wrappedSleep(int ms){
        try{
            Thread.sleep(ms);
        }catch(InterruptedException ignored){}
    }
}
