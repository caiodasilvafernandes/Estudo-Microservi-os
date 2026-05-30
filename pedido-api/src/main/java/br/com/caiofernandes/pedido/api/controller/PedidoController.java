package br.com.caiofernandes.pedido.api.controller;

import br.com.caiofernandes.pedido.api.dto.PedidoReqDTO;
import br.com.caiofernandes.pedido.api.entity.Pedido;
import br.com.caiofernandes.pedido.api.entity.enums.Status;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.UUID;

@Tag(name="Pedido", description = "Rota para requisições dos pedidos")
@RestController
@RequestMapping("/api/v1/pedidos")
public class PedidoController {
    private final Logger logger = LoggerFactory.getLogger(PedidoController.class);

    @Operation(summary = "Cria pedido",
               description = "Contem as operações para serializar e criar o pedido",
               responses = @ApiResponse(responseCode = "201",
                                        description = "Recurso criado com sucesso",
                                        content = @Content(mediaType = "aplicattion/json",
                                                           schema = @Schema(implementation = Pedido.class)))
    )
    @PostMapping
    public ResponseEntity<Pedido> criarPedido(@RequestBody PedidoReqDTO request){
        // Aqui você define os dados que o sistema gera
        Pedido pedido = new Pedido();
        pedido.setIdPedido(UUID.randomUUID());
        pedido.setStatus(Status.EM_PRCESSAMENTO);
        pedido.setDateTime(LocalDateTime.now());

        pedido.setCliente(request.cliente());
        pedido.setItens(request.itens());
        pedido.setValorTotal(request.valorTotal());
        pedido.setEmailNotification(request.emailNotification());

        logger.info("Pedido Recebido");
        return ResponseEntity.status(HttpStatus.CREATED).body(pedido);
    }
}
