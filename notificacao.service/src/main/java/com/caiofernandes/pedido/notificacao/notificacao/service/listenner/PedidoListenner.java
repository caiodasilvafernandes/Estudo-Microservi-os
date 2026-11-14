package com.caiofernandes.pedido.notificacao.notificacao.service.listenner;

import com.caiofernandes.pedido.notificacao.notificacao.service.entity.Pedido;
import com.caiofernandes.pedido.notificacao.notificacao.service.service.EmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class PedidoListenner {
    private final Logger logger = LoggerFactory.getLogger(PedidoListenner.class);

    @Autowired
    private EmailService emailService;

    @RabbitListener(queues = "pedidos.v1.pedido.criado.gerar-notificacao")
    public void notificaPedido(Pedido pedido){
        emailService.enviarEmail(pedido);
        logger.info("Notificação gerada: {}", pedido.toString());
    }
}
