import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResponseErrorHandler;

import java.io.IOException;

import static org.springframework.http.HttpStatus.Series.CLIENT_ERROR;
import static org.springframework.http.HttpStatus.Series.SERVER_ERROR;

/**
 * @author Vannaravuth YO
 * @since 27-May-22, 12:07 PM
 */

@Component
public class RestTemplateResponseErrorHandler implements ResponseErrorHandler {

    @Override
    public boolean hasError(ClientHttpResponse response) throws IOException {
        return (response.getStatusCode().series() == CLIENT_ERROR || response.getStatusCode().series() == SERVER_ERROR);
    }

    @Override
    public void handleError(ClientHttpResponse response) throws IOException {
        switch (response.getStatusCode().series()) {
            case SERVER_ERROR:
                // handle SERVER_ERROR
                break;
            case CLIENT_ERROR:
                // handle CLIENT_ERROR
                if (response.getStatusCode() == HttpStatus.NOT_FOUND) {
                    throw new IOException("NotFound");
                }
                break;
        }
    }
}
