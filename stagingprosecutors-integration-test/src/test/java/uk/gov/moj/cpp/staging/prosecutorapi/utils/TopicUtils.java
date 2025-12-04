package uk.gov.moj.cpp.staging.prosecutorapi.utils;

import static java.util.Optional.ofNullable;
import static uk.gov.justice.services.test.utils.core.messaging.QueueUriProvider.queueUri;

import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClient;
import uk.gov.justice.services.messaging.JsonEnvelope;

import java.util.Optional;

import javax.jms.Connection;
import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.jms.Topic;

import org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory;
import org.apache.activemq.artemis.jms.client.ActiveMQTopic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TopicUtils implements AutoCloseable {

    private static final Logger LOGGER = LoggerFactory.getLogger(TopicUtils.class);

    private static final long RETRIEVE_TIMEOUT = 20000;

    private static final String QUEUE_URI = queueUri();
    private Session session;
    private MessageProducer messageProducer;
    private Connection connection;
    private Topic topic;
    private String topicName = null;
    public static final TopicUtils publicEvents = new TopicUtils("jms.topic.public.event");

    public TopicUtils() {
    }

    public TopicUtils(final String topicName) {
        try {
            LOGGER.info("Artemis URI: {}", QUEUE_URI);
            ActiveMQConnectionFactory factory = new ActiveMQConnectionFactory(QUEUE_URI);
            connection = factory.createConnection();
            connection.start();
            session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            topic = new ActiveMQTopic(topicName);
            this.topicName = topicName;
        } catch (JMSException e) {
            LOGGER.error("Fatal error initialising Artemis", e);
            throw new RuntimeException(e);
        }
    }

    public static Optional<String> retrieveMessageAsString(final MessageConsumer consumer) {
        return retrieveMessageAsString(consumer, RETRIEVE_TIMEOUT);
    }

    public static Optional<String> retrieveMessageAsString(final MessageConsumer consumer, long customTimeOutInMillis) {
        try {
            TextMessage message = (TextMessage) consumer.receive(customTimeOutInMillis);
            if (message == null) {
                LOGGER.error("No message retrieved using consumer with selector {}", consumer.getMessageSelector());
                return Optional.empty();
            }
            return Optional.of(message.getText());
        } catch (JMSException e) {
            throw new RuntimeException(e);
        }
    }

    public static Optional<JsonEnvelope> retrieveMessageAsString(final JmsMessageConsumerClient consumer) {
        return retrieveMessageAsString(consumer, RETRIEVE_TIMEOUT);
    }

    public static Optional<JsonEnvelope> retrieveMessageAsString(final JmsMessageConsumerClient consumer, long customTimeOutInMillis) {
        final long startTime = System.currentTimeMillis();
        Optional<JsonEnvelope> message;
        do {
            message = consumer.retrieveMessageAsJsonEnvelope();
            if (ofNullable(message).isPresent()) {
                return message;
            }
        } while (customTimeOutInMillis > (System.currentTimeMillis() - startTime));
        return null;
    }

    @Override
    public void close() {
        close(messageProducer);
        close(session);
        close(connection);

        session = null;
        messageProducer = null;
        connection = null;
    }

    private void close(final AutoCloseable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (Exception ignored) {
            }
        }
    }
}
