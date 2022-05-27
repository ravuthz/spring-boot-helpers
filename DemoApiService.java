import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;


/**
 * @author Vannaravuth YO
 * @since 27-May-22, 9:25 AM
 */
@Service
public class DemoApiService extends ApiService {
    @Autowired
    public DemoApiService(Environment env) {
        super(
                env.getProperty("demo_api.access_token_url", "http://localhost:9090/api/oauth/token"),
                env.getProperty("demo_api.client_id", "admin_id"),
                env.getProperty("demo_api.client_secret", "admin_secret"),
                env.getProperty("demo_api.username", "adminz"),
                env.getProperty("demo_api.password", "123123"));
    }
}
