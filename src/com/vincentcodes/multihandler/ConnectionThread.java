package com.vincentcodes.multihandler;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

import com.vincentcodes.multihandler.error.ConnectionClosed;

public class ConnectionThread extends Thread{
    private Socket connection;
    private InputStream is;
    private OutputStream os;
    private String remoteHostIp;

    public ConnectionThread(String name, Socket connection){
        this.setName(name);
        this.connection = connection;
        this.remoteHostIp = connection.getInetAddress().getHostAddress() + ":" + connection.getPort();
    }

    @Override
    public void run(){
        MultiHandler.LOGGER.log(logstr("Connection received from " + remoteHostIp));
        try{
            is = connection.getInputStream();
            os = connection.getOutputStream();
            while(!isDoneFor()){
                if(is.available() > 0){
                    byte[] buf = new byte[is.available()];
                    is.read(buf);
                    System.out.print(new String(buf));
                }else{
                    Thread.sleep(100);
                }
            }
        }catch(IOException | InterruptedException e){
            e.printStackTrace();
        }finally{
            close();
        }
        throw new ConnectionClosed("Connection to " + remoteHostIp + " is closed.");
    }

    public void send(String data) throws IOException{
        try{
            if(!isDoneFor()){
                if(os == null) return;
                os.write(data.getBytes("ascii"));
                os.flush();
            }
        }catch(IOException e){
            connection.close();
            throw e;
        }
    }

    public boolean isDoneFor(){
        return connection.isInputShutdown() || connection.isOutputShutdown() || connection.isClosed();
    }

    public void close(){
        try{
            connection.close();
            MultiHandler.LOGGER.warn(logstr("Connection to " + remoteHostIp + " closed"));
        }catch(IOException e){
            e.printStackTrace();
        }
    }

    private String logstr(String logmsg){
        return "["+this.getName()+"] " + logmsg;
    }
}
