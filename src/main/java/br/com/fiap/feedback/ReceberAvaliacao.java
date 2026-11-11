package br.com.fiap.feedback;

import com.microsoft.azure.functions.annotation.*;
import com.microsoft.azure.functions.*;
import java.util.Optional;

import com.azure.data.tables.TableClient;
import com.azure.data.tables.TableClientBuilder;
import com.azure.data.tables.models.TableEntity;
import java.time.LocalDateTime;
import java.util.UUID;

import com.azure.storage.queue.QueueClient;
import com.azure.storage.queue.QueueClientBuilder;

public class ReceberAvaliacao {

    @FunctionName("receber-avaliacao")
    public HttpResponseMessage run(
            @HttpTrigger(name = "req", methods = {
                    HttpMethod.POST }, authLevel = AuthorizationLevel.ANONYMOUS) HttpRequestMessage<Optional<Avaliacao>> request,
            final ExecutionContext context) {

        Optional<Avaliacao> body = request.getBody();
        if (body.isEmpty()) {
            return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                    .body("Avaliação não informada!").build();
        }
        Avaliacao avaliacao = body.get();

        if (avaliacao.getNota() < 0 || avaliacao.getNota() > 10) {
            return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                    .body("Nota inválida! deve ser entre 0 e 10").build();
        }

        String urgencia = calcularUrgencia(avaliacao.getNota());
        context.getLogger().info("Recebendo avaliação: " + avaliacao.getDescricao() +
                " com nota: " + avaliacao.getNota() + " e urgência: " + urgencia);

        // gravar no Table Storage -
        String connectionString = System.getenv("AzureWebJobsStorage");
        String rowKey = UUID.randomUUID().toString();

        TableClient tableClient = new TableClientBuilder()
                .connectionString(connectionString)
                .tableName("avaliacoes")
                .buildClient();

        try { tableClient.createTable(); } catch (Exception ignored) {}  

        TableEntity entity = new TableEntity("avaliacao", rowKey);
        entity.addProperty("descricao", avaliacao.getDescricao());
        entity.addProperty("nota", avaliacao.getNota());
        entity.addProperty("urgencia", urgencia);
        entity.addProperty("dataEnvio", LocalDateTime.now().toString());

        tableClient.upsertEntity(entity);

        if ("CRITICA".equals(urgencia)) {
            QueueClient queueClient = new QueueClientBuilder()
                    .connectionString(connectionString)
                    .queueName("avaliacoes-criticas")
                    .buildClient();

            queueClient.createIfNotExists();
            queueClient.sendMessage(rowKey);
        }

        return request.createResponseBuilder(HttpStatus.OK)
                .body("Avaliação recebida com sucesso!").build();
    }

    private String calcularUrgencia(int nota) {
        if (nota <= 3)
            return "CRITICA";
        if (nota <= 6)
            return "MÉDIA";
        return "BAIXA";
    }

}
