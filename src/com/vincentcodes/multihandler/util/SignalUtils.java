package com.vincentcodes.multihandler.util;

import java.util.function.Consumer;

import sun.misc.Signal;
import sun.misc.SignalHandler;

public class SignalUtils {
    public static void handle(Signal signal, Consumer<Signal> callback){
        Signal.handle(signal, new SignalHandler(){
            @Override
            public void handle(Signal sig) {
                callback.accept(sig);
            }
        });
    }
}
