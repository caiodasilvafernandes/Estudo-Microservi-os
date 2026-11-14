package br.com.caiofernandes.pedidos.processador.listenner;

import br.com.caiofernandes.pedidos.processador.entity.Pedido;
import br.com.caiofernandes.pedidos.processador.entity.enums.Status;
import br.com.caiofernandes.pedidos.processador.service.PedidoService;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;

@Component
public class PedidoListenner {
    private final Logger logger = LoggerFactory.getLogger(PedidoListenner.class);

    @Autowired
    private PedidoService pedidoService;

    @RabbitListener(queues =  "pedidos.v1.pedido.criado.gerar-processamento")
    public void salvarPedido(Pedido pedido){
        logger.info("Pedido chegou no serviço de processamento: {}", pedido.toString());
        pedido.setStatus(Status.PROCESSADO);
        pedidoService.SalvarPedido(pedido);
        logger.info("Pedido processado: {}", pedido.toString());

    }
}
