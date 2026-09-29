package fr.spectatorplus.mod;

import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

/**
 * Le code commun écrit avec java.util.logging : ses messages sont renvoyés vers le logger du loader
 * (SLF4J à partir de 1.18, Log4j avant), pour apparaître normalement dans la console et latest.log.
 */
public final class ModLogger {

    //? if >=1.18 {
    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger("SpectatorPlus");
    //?} else
    /*private static final org.apache.logging.log4j.Logger LOG = org.apache.logging.log4j.LogManager.getLogger("SpectatorPlus");*/

    private ModLogger() {
    }

    public static Logger create() {
        Logger jul = Logger.getLogger("SpectatorPlus.mod");
        jul.setUseParentHandlers(false);
        for (Handler h : jul.getHandlers()) jul.removeHandler(h);
        jul.addHandler(new Handler() {
            @Override
            public void publish(LogRecord r) {
                String msg = r.getMessage();
                Throwable t = r.getThrown();
                int level = r.getLevel().intValue();
                if (level >= Level.SEVERE.intValue()) {
                    if (t != null) LOG.error(msg, t);
                    else LOG.error(msg);
                } else if (level >= Level.WARNING.intValue()) {
                    if (t != null) LOG.warn(msg, t);
                    else LOG.warn(msg);
                } else {
                    LOG.info(msg);
                }
            }

            @Override
            public void flush() {
            }

            @Override
            public void close() {
            }
        });
        return jul;
    }
}
