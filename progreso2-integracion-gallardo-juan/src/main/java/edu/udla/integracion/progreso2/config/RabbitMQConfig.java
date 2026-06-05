package edu.udla.integracion.progreso2.config;

import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String BILLING_EXCHANGE = "billing.exchange";
    public static final String BILLING_QUEUE = "billing.queue";

    public static final String APPOINTMENTS_EVENTS_EXCHANGE = "appointments.events";
    public static final String NOTIFICATIONS_QUEUE = "notifications.queue";
    public static final String ANALYTICS_QUEUE = "analytics.queue";

    @Bean
    public Queue billingQueue() {
        return new Queue(BILLING_QUEUE, true);
    }

    @Bean
    public DirectExchange billingExchange() {
        return new DirectExchange(BILLING_EXCHANGE, true, false);
    }

    @Bean
    public Binding billingBinding(
            @Qualifier("billingQueue") Queue billingQueue,
            DirectExchange billingExchange
    ) {
        return BindingBuilder
                .bind(billingQueue)
                .to(billingExchange)
                .with(BILLING_QUEUE);
    }

    @Bean
    public Queue notificationsQueue() {
        return new Queue(NOTIFICATIONS_QUEUE, true);
    }

    @Bean
    public Queue analyticsQueue() {
        return new Queue(ANALYTICS_QUEUE, true);
    }

    @Bean
    public FanoutExchange appointmentsEventsExchange() {
        return new FanoutExchange(APPOINTMENTS_EVENTS_EXCHANGE, true, false);
    }

    @Bean
    public Binding notificationsBinding(
            @Qualifier("notificationsQueue") Queue notificationsQueue,
            FanoutExchange appointmentsEventsExchange
    ) {
        return BindingBuilder
                .bind(notificationsQueue)
                .to(appointmentsEventsExchange);
    }

    @Bean
    public Binding analyticsBinding(
            @Qualifier("analyticsQueue") Queue analyticsQueue,
            FanoutExchange appointmentsEventsExchange
    ) {
        return BindingBuilder
                .bind(analyticsQueue)
                .to(appointmentsEventsExchange);
    }

    @Bean
    public CommandLineRunner declararRabbitMQ(
            AmqpAdmin amqpAdmin,
            @Qualifier("billingQueue") Queue billingQueue,
            @Qualifier("notificationsQueue") Queue notificationsQueue,
            @Qualifier("analyticsQueue") Queue analyticsQueue,
            DirectExchange billingExchange,
            FanoutExchange appointmentsEventsExchange,
            @Qualifier("billingBinding") Binding billingBinding,
            @Qualifier("notificationsBinding") Binding notificationsBinding,
            @Qualifier("analyticsBinding") Binding analyticsBinding
    ) {
        return args -> {
            amqpAdmin.declareExchange(billingExchange);
            amqpAdmin.declareExchange(appointmentsEventsExchange);

            amqpAdmin.declareQueue(billingQueue);
            amqpAdmin.declareQueue(notificationsQueue);
            amqpAdmin.declareQueue(analyticsQueue);

            amqpAdmin.declareBinding(billingBinding);
            amqpAdmin.declareBinding(notificationsBinding);
            amqpAdmin.declareBinding(analyticsBinding);

            System.out.println("RabbitMQ configurado: exchanges, colas y bindings creados correctamente.");
        };
    }
}