package helloworld;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.amazonaws.services.secretsmanager.AWSSecretsManager;
import com.amazonaws.services.secretsmanager.AWSSecretsManagerClientBuilder;
import com.amazonaws.services.secretsmanager.model.GetSecretValueRequest;
import com.amazonaws.services.secretsmanager.model.GetSecretValueResult;
import com.amazonaws.services.simplesystemsmanagement.AWSSimpleSystemsManagement;
import com.amazonaws.services.simplesystemsmanagement.AWSSimpleSystemsManagementClientBuilder;
import com.amazonaws.services.simplesystemsmanagement.model.GetParameterRequest;
import com.amazonaws.services.simplesystemsmanagement.model.GetParameterResult;

/**
 * Handler for requests to Lambda function.
 */
public class App implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    public APIGatewayProxyResponseEvent handleRequest(final APIGatewayProxyRequestEvent input, final Context context) {
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        headers.put("X-Custom-Header", "application/json");

        AWSSimpleSystemsManagement client = AWSSimpleSystemsManagementClientBuilder.standard().build();
        String env_var = System.getenv("SSM_PARAM_ENV");
        System.out.println("ENV" + env_var);
        GetParameterRequest getParameterRequest = new GetParameterRequest();
        getParameterRequest.setName(env_var);
        GetParameterResult result = client.getParameter(getParameterRequest);
        System.out.println("token:" + result.getParameter().getValue());

        String secret_arn = System.getenv("SECRET_ARN");
        System.out.println("secret_arn" + secret_arn);
        AWSSecretsManagerClientBuilder awsSecretsManagerClientBuilder = AWSSecretsManagerClientBuilder.standard();
        AWSSecretsManager awsSecretsManager = awsSecretsManagerClientBuilder.build();
        GetSecretValueRequest getSecretValueRequest = new GetSecretValueRequest().withSecretId(secret_arn);
        try {
            GetSecretValueResult secretValue = awsSecretsManager.getSecretValue(getSecretValueRequest);
            System.out.println(secretValue.getSecretString());
        } catch(Exception e) {
            System.out.println(e);
        }

        APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent()
                .withHeaders(headers);
        try {
            final String pageContents = this.getPageContents("https://checkip.amazonaws.com");
            String output = String.format("{ \"message\": \"hello world!!!\", \"location\": \"%s\" }", pageContents);

            return response
                    .withStatusCode(200)
                    .withBody(output);
        } catch (IOException e) {
            return response
                    .withBody("{}")
                    .withStatusCode(500);
        }
    }

    private String getPageContents(String address) throws IOException{
        URL url = new URL(address);
        try(BufferedReader br = new BufferedReader(new InputStreamReader(url.openStream()))) {
            return br.lines().collect(Collectors.joining(System.lineSeparator()));
        }
    }
}
