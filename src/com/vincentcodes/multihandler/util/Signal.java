package com.vincentcodes.multihandler.util;

/**
 * This is a wrapper for the {@code class sun.misc.Signal}
 */
public class Signal {
    /**
     * @param sig eg. INT for SIGINT, TSTP for SIGTSTP
     */
    public static sun.misc.Signal create(String sig){
        return new sun.misc.Signal(sig);
    }
}
