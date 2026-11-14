package br.com.caiofernandes.pedidos.processador.listenner;

import br.com.caiofernandes.pedidos.processador.entity.Pedido;
import br.com.caiofernandes.pedidos.processador.entity.enums.Status;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;

@Component
public class PedidoListenner {
    private final Logger logger = LoggerFactory.getLogger(PedidoListenner.class);

    @RabbitListener(queues =  "pedidos.v1.pedido.criado.gerar-processamento")
    public void salvarPedido(Pedido pedido){
        pedido.setStatus(Status.PROCESSADO);
        logger.info("Pedido processado: {}", pedido.toString());

    }
}
