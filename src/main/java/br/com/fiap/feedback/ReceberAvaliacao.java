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

                return request.createResponseBuilder(HttpStatus.OK)
                .body("Avaliação recebida com sucesso!").build();
            }
        
    
}
