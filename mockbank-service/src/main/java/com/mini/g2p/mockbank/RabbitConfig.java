package com.mini.g2p.mockbank;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {
  public static final String EXCHANGE = "g2p.payments";
  public static final String RK_INSTR = "payment.instruction";
  public static final String RK_STATUS = "payment.status";
  public static final String Q_INSTR = "q.payment.instructions"; // consume this

  // --- Topology ---
  @Bean
  public TopicExchange paymentsExchange() { return new TopicExchange(EXCHANGE, true, false); }

  @Bean
  public Queue instructionsQueue() { return QueueBuilder.durable(Q_INSTR).build(); }

  @Bean
  public Binding bindInstructions(TopicExchange ex, Queue instructionsQueue) {
    return BindingBuilder.bind(instructionsQueue).to(ex).with(RK_INSTR);
  }

  // --- JSON converter for template & listeners ---
  @Bean
  public MessageConverter jsonMessageConverter() {
    return new Jackson2JsonMessageConverter();
  }

  @Bean
  public RabbitTemplate rabbitTemplate(ConnectionFactory cf, MessageConverter mc) {
    RabbitTemplate tpl = new RabbitTemplate(cf);
    tpl.setMessageConverter(mc);
    return tpl;
  }

  @Bean
  public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
      ConnectionFactory cf, MessageConverter mc) {
    SimpleRabbitListenerContainerFactory f = new SimpleRabbitListenerContainerFactory();
    f.setConnectionFactory(cf);
    f.setMessageConverter(mc);
    return f;
  }
}
