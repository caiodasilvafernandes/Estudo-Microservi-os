---
name: java-rabbitmq
description: |
  Use this skill for any task involving Java development with RabbitMQ — including designing
  message-driven architectures, implementing producers and consumers, configuring exchanges,
  queues and bindings, handling dead-letter queues, reviewing RabbitMQ-related code, and
  diagnosing messaging bugs.

  Trigger when the user mentions any of: RabbitMQ, AMQP, message broker, filas de mensagens,
  producers/consumers, exchanges, Spring AMQP, Spring Boot + RabbitMQ, dead-letter queue (DLQ),
  fanout/direct/topic/headers exchange, confirmação de mensagens (ACK/NACK), retry com backoff,
  idempotência em consumers, ou qualquer combinação de Java + mensageria.

  Use this skill even when the request looks like a "simple" question about RabbitMQ in Java —
  the patterns here prevent subtle bugs that trip up experienced developers.
---

# Java + RabbitMQ Development Skill

Este skill cobre o desenvolvimento Java com RabbitMQ, desde configuração básica até padrões
avançados de resiliência e revisão de código. Use-o como guia normativo ao gerar, revisar ou
refatorar código.

---

## Stack de referência

| Camada | Tecnologia recomendada |
|---|---|
| Runtime | Java 17+ (records, sealed classes, text blocks) |
| Framework | Spring Boot 3.x + `spring-boot-starter-amqp` |
| Cliente AMQP | Spring AMQP / `RabbitTemplate` + `@RabbitListener` |
| Testes | JUnit 5, Testcontainers (RabbitMQ), Mockito |
| Build | Maven ou Gradle |

---

## 1. Configuração de infraestrutura

### Dependência Maven
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-amqp</artifactId>
</dependency>
```

### application.yml mínimo
```yaml
spring:
  rabbitmq:
    host: ${RABBITMQ_HOST:localhost}
    port: 5672
    username: ${RABBITMQ_USER:guest}
    password: ${RABBITMQ_PASS:guest}
    virtual-host: /
    listener:
      simple:
        acknowledge-mode: manual        # sempre manual; nunca auto em produção
        prefetch: 10                    # limite de mensagens não-ACKadas por consumer
        retry:
          enabled: false               # use retry customizado via DLQ, não o Spring
    publisher-confirms: true
    publisher-returns: true
```

> **Por que `acknowledge-mode: manual`?** Com `auto`, qualquer exceção não tratada descarta a
> mensagem silenciosamente. Com `manual`, o código controla explicitamente ACK/NACK, garantindo
> que mensagens com erro vão para a DLQ.

---

## 2. Topologia: Exchanges, Queues e Bindings

### Padrão recomendado: Direct Exchange com DLQ

```java
@Configuration
public class RabbitMQTopologyConfig {

    // Nomes como constantes — nunca strings literais espalhadas pelo código
    public static final String MAIN_EXCHANGE   = "pedidos.exchange";
    public static final String MAIN_QUEUE      = "pedidos.queue";
    public static final String DLX_EXCHANGE    = "pedidos.dlx";
    public static final String DLQ_QUEUE       = "pedidos.dlq";
    public static final String ROUTING_KEY     = "pedido.criado";

    @Bean
    public DirectExchange mainExchange() {
        return new DirectExchange(MAIN_EXCHANGE, true, false);
    }

    @Bean
    public DirectExchange deadLetterExchange() {
        return new DirectExchange(DLX_EXCHANGE, true, false);
    }

    @Bean
    public Queue mainQueue() {
        return QueueBuilder.durable(MAIN_QUEUE)
                .withArgument("x-dead-letter-exchange", DLX_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", ROUTING_KEY)
                .withArgument("x-message-ttl", 60_000)        // TTL opcional
                .build();
    }

    @Bean
    public Queue deadLetterQueue() {
        return QueueBuilder.durable(DLQ_QUEUE).build();
    }

    @Bean
    public Binding mainBinding() {
        return BindingBuilder.bind(mainQueue()).to(mainExchange()).with(ROUTING_KEY);
    }

    @Bean
    public Binding dlqBinding() {
        return BindingBuilder.bind(deadLetterQueue()).to(deadLetterExchange()).with(ROUTING_KEY);
    }
}
```

### Quando usar cada tipo de exchange

| Exchange | Use quando |
|---|---|
| **Direct** | Roteamento por routing key exata. Padrão para a maioria dos casos. |
| **Topic** | Routing keys com wildcards (`pedido.*`, `#.erro`). Bom para eventos com sub-tipos. |
| **Fanout** | Broadcast para todos os consumidores sem filtro. Cuidado com volume. |
| **Headers** | Roteamento por headers AMQP. Evite — dificulta observabilidade. |

---

## 3. Produtor (Publisher)

```java
@Service
@RequiredArgsConstructor
public class PedidoPublisher {

    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    public void publicar(PedidoCriadoEvent evento) {
        try {
            String payload = objectMapper.writeValueAsString(evento);

            MessageProperties props = new MessageProperties();
            props.setMessageId(evento.pedidoId().toString());     // idempotência no consumer
            props.setContentType(MessageProperties.CONTENT_TYPE_JSON);
            props.setHeader("x-origem", "pedido-service");

            Message message = new Message(payload.getBytes(StandardCharsets.UTF_8), props);

            rabbitTemplate.convertAndSend(
                RabbitMQTopologyConfig.MAIN_EXCHANGE,
                RabbitMQTopologyConfig.ROUTING_KEY,
                message
            );

        } catch (JsonProcessingException e) {
            throw new MensagemSerializacaoException("Falha ao serializar evento: " + evento, e);
        }
    }
}
```

**Checklist do produtor:**
- [ ] `messageId` definido para rastreamento e idempotência
- [ ] `contentType` definido como `application/json`
- [ ] Exceções de serialização tratadas e convertidas em exceções de domínio
- [ ] Evitar `convertAndSend(Object)` com inferência automática de tipo — preferir `Message` explícito

---

## 4. Consumidor (Consumer)

```java
@Component
@RequiredArgsConstructor
@Slf4j
public class PedidoConsumer {

    private final PedidoService pedidoService;
    private final ObjectMapper objectMapper;

    @RabbitListener(queues = RabbitMQTopologyConfig.MAIN_QUEUE)
    public void consumir(Message message, Channel channel) throws IOException {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        String messageId = message.getMessageProperties().getMessageId();

        try {
            PedidoCriadoEvent evento = objectMapper.readValue(
                message.getBody(), PedidoCriadoEvent.class
            );

            log.info("Processando pedido: messageId={}, pedidoId={}", messageId, evento.pedidoId());

            pedidoService.processar(evento);

            channel.basicAck(deliveryTag, false);   // false = ack apenas esta mensagem
            log.info("ACK enviado: messageId={}", messageId);

        } catch (JsonProcessingException e) {
            // Mensagem malformada — NACK sem requeue (vai para DLQ imediatamente)
            log.error("Mensagem inválida, enviando para DLQ: messageId={}", messageId, e);
            channel.basicNack(deliveryTag, false, false);

        } catch (Exception e) {
            // Erro de negócio — NACK sem requeue (DLQ + análise manual)
            log.error("Erro ao processar pedido: messageId={}", messageId, e);
            channel.basicNack(deliveryTag, false, false);
        }
    }
}
```

**Regras de ACK/NACK:**

| Situação | Ação | Requeue |
|---|---|---|
| Sucesso | `basicAck` | — |
| Mensagem malformada (JSON inválido) | `basicNack` | `false` → DLQ |
| Erro transiente (DB fora, timeout) | `basicNack` | `false` → DLQ com TTL para retry |
| Erro de negócio permanente | `basicNack` | `false` → DLQ |

> **Nunca use `requeue=true` em produção** sem um mecanismo de limite de tentativas
> (`x-death` header). Loops infinitos derrubam o consumer.

---

## 5. Padrão de Retry com Backoff via DLQ

Para retry automático com backoff, use múltiplas DLQs com TTL crescente:

```java
@Bean
public Queue retryQueue1() {
    return QueueBuilder.durable("pedidos.retry.1")
            .withArgument("x-dead-letter-exchange", MAIN_EXCHANGE)
            .withArgument("x-dead-letter-routing-key", ROUTING_KEY)
            .withArgument("x-message-ttl", 5_000)    // 5s → recoloca na fila principal
            .build();
}

// No consumer, após N tentativas (lidas do header x-death), redireciona para retry ou DLQ final
private void requeue(Message message, Channel channel, long deliveryTag) throws IOException {
    Map<String, Object> headers = message.getMessageProperties().getHeaders();
    long tentativas = contarTentativas(headers);  // lê header x-death

    if (tentativas < 3) {
        rabbitTemplate.send("pedidos.retry.exchange", ROUTING_KEY, message);
        channel.basicAck(deliveryTag, false);
    } else {
        channel.basicNack(deliveryTag, false, false);  // DLQ final
    }
}
```

---

## 6. Serialização e Contrato de Mensagem

### Configurar Jackson como serializador padrão

```java
@Bean
public MessageConverter messageConverter(ObjectMapper objectMapper) {
    return new Jackson2JsonMessageConverter(objectMapper);
}

@Bean
public RabbitTemplate rabbitTemplate(ConnectionFactory cf, MessageConverter converter) {
    RabbitTemplate template = new RabbitTemplate(cf);
    template.setMessageConverter(converter);
    template.setMandatory(true);
    return template;
}
```

### Contrato de evento (use records Java 17+)

```java
// Imutável, serializa/deserializa limpo com Jackson
public record PedidoCriadoEvent(
    UUID pedidoId,
    String clienteId,
    List<ItemPedido> itens,
    Instant criadoEm
) {
    // Validação no construtor
    public PedidoCriadoEvent {
        Objects.requireNonNull(pedidoId, "pedidoId obrigatório");
        Objects.requireNonNull(clienteId, "clienteId obrigatório");
        if (itens == null || itens.isEmpty()) throw new IllegalArgumentException("Itens não podem ser vazios");
    }
}
```

**Evolução de schema:** adicione campos novos como `Optional` ou com `@JsonIgnoreProperties(ignoreUnknown = true)`.
Nunca remova ou renomeie campos sem versionamento.

---

## 7. Revisão de Código — Checklist

Ao revisar código Java + RabbitMQ, verifique:

### Topologia
- [ ] Filas declaradas como `durable=true`?
- [ ] DLQ configurada com `x-dead-letter-exchange`?
- [ ] Nomes de fila/exchange em constantes, não literais?
- [ ] Exchange e queue declarados em `@Configuration` centralizada?

### Produtor
- [ ] `messageId` único definido por mensagem?
- [ ] `contentType` explícito?
- [ ] Exceções de serialização tratadas?
- [ ] Publisher confirms ativado para operações críticas?

### Consumidor
- [ ] `acknowledge-mode: manual` configurado?
- [ ] `channel.basicAck/basicNack` chamado em todos os fluxos (inclusive catch)?
- [ ] `requeue=false` em NACKs (sem loop infinito)?
- [ ] Idempotência implementada (verificação por `messageId` antes de processar)?
- [ ] Prefetch configurado (evita que um consumer monopolize a fila)?
- [ ] Log com `messageId` em todos os pontos de decisão?

### Resiliência
- [ ] DLQ monitorada com alertas?
- [ ] TTL definido nas filas de retry?
- [ ] Limite de tentativas lido do header `x-death`?

---

## 8. Testes

### Teste de integração com Testcontainers

```java
@SpringBootTest
@Testcontainers
class PedidoConsumerIntegrationTest {

    @Container
    static RabbitMQContainer rabbitmq = new RabbitMQContainer("rabbitmq:3.12-management");

    @DynamicPropertySource
    static void configureRabbit(DynamicPropertyRegistry registry) {
        registry.add("spring.rabbitmq.host", rabbitmq::getHost);
        registry.add("spring.rabbitmq.port", rabbitmq::getAmqpPort);
    }

    @Autowired RabbitTemplate rabbitTemplate;
    @Autowired PedidoService pedidoService;  // mock ou real

    @Test
    void deveProcessarPedidoComSucesso() throws Exception {
        var evento = new PedidoCriadoEvent(UUID.randomUUID(), "cliente-1",
            List.of(new ItemPedido("SKU-1", 2)), Instant.now());

        rabbitTemplate.convertAndSend(MAIN_EXCHANGE, ROUTING_KEY, evento);

        await().atMost(5, SECONDS)
               .untilAsserted(() -> verify(pedidoService).processar(any()));
    }

    @Test
    void deveMoverMensagemMalformadaParaDLQ() throws Exception {
        rabbitTemplate.send(MAIN_EXCHANGE, ROUTING_KEY,
            new Message("{ invalid json }".getBytes(), new MessageProperties()));

        await().atMost(5, SECONDS)
               .untilAsserted(() -> {
                   Message dlq = rabbitTemplate.receive(DLQ_QUEUE, 1000);
                   assertThat(dlq).isNotNull();
               });
    }
}
```

---

## 9. Anti-padrões frequentes

| Anti-padrão | Problema | Correção |
|---|---|---|
| `acknowledge-mode: auto` | Exceção descarta mensagem silenciosamente | Usar `manual` sempre |
| `basicNack(tag, false, true)` sem limite | Loop infinito, consumer trava | `requeue=false`, usar DLQ + retry |
| Strings literais de fila/exchange | Inconsistências entre produtores e consumers | Constantes compartilhadas |
| Sem `messageId` | Impossível detectar duplicatas ou rastrear mensagens | Definir `messageId` único no produtor |
| Deserializar sem `@JsonIgnoreProperties` | Quebra ao adicionar campo novo | Anotar DTOs com `ignoreUnknown = true` |
| Lógica de negócio pesada no consumer | Consumer lento, backpressure | Delegar para `@Service`, consumer só faz ACK/NACK |
| Sem Testcontainers | Testes dependem de RabbitMQ externo | Usar `RabbitMQContainer` nos testes de integração |

---

## 10. Referências rápidas

- Documentação Spring AMQP: https://docs.spring.io/spring-amqp/docs/current/reference/html/
- Especificação AMQP 0-9-1: https://www.rabbitmq.com/tutorials/amqp-concepts
- RabbitMQ Testcontainers: https://java.testcontainers.org/modules/rabbitmq/
