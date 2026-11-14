package br.com.caiofernandes.pedidos.processador.service;

import br.com.caiofernandes.pedidos.processador.entity.ItemPedido;
import br.com.caiofernandes.pedidos.processador.entity.Pedido;
import br.com.caiofernandes.pedidos.processador.repository.ItemPedidoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ItemPedidoService {
    @Autowired
    private ProdutoService produtoService;
    @Autowired
    private ItemPedidoRepository itemPedidoRepository;

    public List<ItemPedido> save(List<ItemPedido> itens) {
        return itemPedidoRepository.saveAll(itens);
    }

    public void save(ItemPedido itemPedido){
        itemPedidoRepository.save(itemPedido);
    }
    public void updateItemPedido(List<ItemPedido> itensPedido, Pedido pedido) {
        itensPedido.forEach(item -> {
            item.setPedido(pedido);
            this.save(item);
        });
    }
}
