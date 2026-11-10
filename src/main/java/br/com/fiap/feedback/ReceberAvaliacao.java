package br.com.fiap.feedback;
import com.microsoft.azure.functions.annotation.*;
import com.microsoft.azure.functions.*;
import java.util.Optional;


public class ReceberAvaliacao {

    @FunctionName("receber-avaliacao")
    public HttpResponseMessage run(
        @HttpTrigger (
            name = "req", 
            methods = {HttpMethod.POST}, 
            authLevel = AuthorizationLevel.ANONYMOUS) 
            HttpRequestMessage<Optional<Avaliacao>> request, final ExecutionContext context) {


                Optional<Avaliacao> body = request.getBody();
                if(body.isEmpty())  {
                    return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                    .body("Avaliação não informada!").build();
                }
                Avaliacao avaliacao = body.get();

                if(avaliacao.getNota() <0 || avaliacao.getNota() > 10) {
                    return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                    .body("Nota inválida! deve ser entre 0 e 10").build();
                }

                String urgencia =  calcularUrgencia(avaliacao.getNota());
                context.getLogger().info("Recebendo avaliação: " + avaliacao.getDescricao() + 
                " com nota: " + avaliacao.getNota() + " e urgência: " + urgencia);

                return request.createResponseBuilder(HttpStatus.OK)
                .body("Avaliação recebida com sucesso!").build();
            }

        private String calcularUrgencia(int nota)    {
           if(nota <= 3)  return "CRITICA";
           if(nota <= 6)  return "MÉDIA";
           return "BAIXA";
        }
        
    
}
