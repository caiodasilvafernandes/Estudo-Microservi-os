package br.com.caiofernandes.pedidos.processador.service;

import br.com.caiofernandes.pedidos.processador.entity.ItemPedido;
import br.com.caiofernandes.pedidos.processador.repository.ProdutoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProdutoService {
    @Autowired
    private ProdutoRepository produtoRepository;

    public void save(List<ItemPedido> itens) {
        itens.forEach(item -> {
            produtoRepository.save(item.getProduto());
        });
    }
}
