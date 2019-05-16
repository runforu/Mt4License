package mt4.license.com;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

import mt4.license.com.service.SslServer;

@SpringBootApplication
public class Mt4LicenseApplication {

    @Autowired
    private SslServer mServer;

    public static void main(String[] args) {
        ConfigurableApplicationContext ctx = SpringApplication.run(Mt4LicenseApplication.class, args);
    }

}
