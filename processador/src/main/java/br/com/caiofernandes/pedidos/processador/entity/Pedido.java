package br.com.caiofernandes.pedidos.processador.entity;

import br.com.caiofernandes.pedidos.processador.entity.enums.Status;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "pedido")
public class Pedido {
    @Id
    private UUID idPedido = UUID.randomUUID();
    private  String cliente;
    @OneToMany(mappedBy = "pedido")
    private List<ItemPedido> itens = new ArrayList<>();
    @Column(name = "valor_total")
    private Double valorTotal;
    @Column(name = "email_notification")
    private String emailNotification;
    @Enumerated(EnumType.STRING)
    private Status status = Status.EM_PRCESSAMENTO;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Column(name = "datetime")
    private LocalDateTime dateTime = LocalDateTime.now();
}
