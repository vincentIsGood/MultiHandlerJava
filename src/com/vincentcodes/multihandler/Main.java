package com.vincentcodes.multihandler;

import java.io.IOException;

// kill -l
//
// To enable CTRL-Z on this application
// May need to run "trap '' TSTP"
// to clear it run "trap - TSTP"
public class Main{
    public static void main(String[] args) throws IOException{
        int port = 4444;
        if(args.length > 0){
            try{
                switch(args.length){
                    case 1:
                        port = Integer.parseInt(args[0]);
                        if(args.length == 1) break;
                    case 2:
                        MultiHandler.BETA_ENABLED = Boolean.parseBoolean(args[1]);
                        if(args.length == 2) break;
                }
            }catch(NumberFormatException e){
                MultiHandler.LOGGER.err("'" + args[0] + "' is not a valid port number");
                MultiHandler.LOGGER.warn("Usage: java -jar multihandler-java-vx.x.x.jar [<port>] [<beta - bool>]");
                System.exit(-1);
            }
        }

        MultiHandler multiHandler = new MultiHandler(port);
        multiHandler.start();
    }
}