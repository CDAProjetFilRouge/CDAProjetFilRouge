package fr.diginamic.hubevenementiel.logging;

import java.util.List;

import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

/** Écoute les logs de l'application le temps d'un test (à fermer, ou dans un try-with-resources). */
public class LogCapture implements AutoCloseable {

    private final Logger logger = (Logger) LoggerFactory.getLogger("fr.diginamic.hubevenementiel");
    private final ListAppender<ILoggingEvent> appender = new ListAppender<>();

    public LogCapture() {
        appender.start();
        logger.addAppender(appender);
        logger.setLevel(Level.INFO);
    }

    public List<ILoggingEvent> events() {
        return List.copyOf(appender.list);
    }

    public List<String> messages() {
        return events().stream().map(ILoggingEvent::getFormattedMessage).toList();
    }

    public List<String> warnings() {
        return events().stream().filter(event -> event.getLevel() == Level.WARN)
                .map(ILoggingEvent::getFormattedMessage).toList();
    }

    @Override
    public void close() {
        logger.detachAppender(appender);
    }
}
