package br.com.caiofernandes.pedidos.processador.service;

import br.com.caiofernandes.pedidos.processador.entity.ItemPedido;
import br.com.caiofernandes.pedidos.processador.entity.Pedido;
import br.com.caiofernandes.pedidos.processador.repository.PedidoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PedidoService {
    @Autowired
    private PedidoRepository pedidoRepository;

    @Autowired
    private ProdutoService produtoService;

    @Autowired
    private ItemPedidoService itemPedidoService;

    private final Logger logger = LoggerFactory.getLogger(Pedido.class);

    public void SalvarPedido(Pedido pedido){
        produtoService.save(pedido.getItens());
        List<ItemPedido> itensPedido = itemPedidoService. save(pedido.getItens());
        pedidoRepository.save(pedido);
        itemPedidoService.updateItemPedido(itensPedido, pedido);

        logger.info("Pedido salvo: {}", pedido.toString());
    }

}
