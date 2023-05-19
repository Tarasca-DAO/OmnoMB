package concept.omno;

//import concept.utility.JsonFunction;
import org.json.simple.JSONObject;
import sun.misc.Signal;

import java.util.HashMap;

public class Main {

    public static void main(String[] args) throws Exception {
        HashMap<String, String> applicationResult = new HashMap<>();

        // Verificar los argumentos
        if (args.length > 1) {
            applicationResult.put("errorDescription", "usage:\nconfiguration\n");
            printApplicationResult(applicationResult);
            System.exit(0);
        }

        // Obtener el nombre del archivo de configuración
        String configurationFileName = (args.length == 1) ? args[0] : "configuration.json";

        // Crear la instancia de ApplicationContext
        ApplicationContext applicationContext = new ApplicationContext(configurationFileName);

        if (applicationContext.isConfigured) {
            Thread applicationContextThread = new Thread(applicationContext);

            Httpd httpd = new Httpd(applicationContext, applicationContext.apiHost, applicationContext.apiPort);
            Thread httpdThread = new Thread(httpd);

            applicationContextThread.start();
            httpdThread.start();

            // Manejar la señal de interrupción
            final ApplicationContext finalApplicationContext = applicationContext;
            Signal.handle(new Signal("INT"), signal -> {
                finalApplicationContext.logInfoMessage("Omno | Shutting down...");
                httpd.stop();
                finalApplicationContext.stop();
            });

            httpdThread.join();
            applicationContextThread.join();
        }

        printApplicationResult(applicationResult);
        System.exit(0);
    }

    private static void printApplicationResult(HashMap<String, String> applicationResult) {
        if (!applicationResult.isEmpty()) {
            JSONObject applicationResultJson = new JSONObject(applicationResult);
            System.out.println(applicationResultJson.toJSONString());
        }
    }
}
