import com.google.gson.Gson;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import java.util.Scanner;
import java.util.HashMap;
import java.time.Duration;

public class Main {

    private static final String API_KEY = "db2cb0a8fd1c9765fd0ffff0";
    private final String MONEDA_BASE = "USD";
    private Map<String, Double> tasasDeCambio;
    private final Gson gson = new Gson();
    private static final String API_URL_BASE = "https://v6.exchangerate-api.com/v6/" + API_KEY + "/latest/";
    private final String[][] PARES_CONVERSION = {
            {"USD", "GBP", "Dólar a Libra Esterlina"},       // 1. Dólar a Libra Esterlina
            {"CLP", "EUR", "Peso Chileno a Euro"},           // 2. Pesos Chilenos a Euro
            {"CLP", "USD", "Peso Chileno a Dólar"},          // 3. Pesos Chilenos a Dólar
            {"CLP", "ARS", "Peso Chileno a Peso Argentino"},  // 4. Pesos Chilenos a Peso Argentino (ARS)
            {"USD", "CLP", "Dólar a Peso Chileno"},           // 5. Dólar a Pesos Chilenos
            {"USD", "CNY", "Dólar a Yuan"},                   // 6. Dólar a Yuan
            {"CNY", "CLP", "Yuan a Peso Chileno"}             // 7. Yuan a Pesos Chilenos
    };

    private static class RespuestaAPI {
        public String base_code;
        public Map<String, Double> conversion_rates;
    }


    public void iniciar() {
        if (API_KEY.equals("TU_CLAVE_AQUI")) {
            System.err.println("ERROR: Por favor, ingrese su clave API válida.");
            return;
        }

        try {
            System.out.println("Cargando tasas de cambio...");
            tasasDeCambio = obtenerTasasDeAPI();
            guardarTasasEnCache(tasasDeCambio);
            System.out.println("Tasas cargadas. Base: " + MONEDA_BASE);
            ejecutarMenuInteractivo();

        } catch (Exception apiException) {
            System.err.println("Error al obtener tasas de API: " + apiException.getMessage());
            tasasDeCambio = cargarTasasDeCache();

            if (tasasDeCambio != null && !tasasDeCambio.isEmpty()) {
                System.out.println("ADVERTENCIA: Usando tasas de cambio guardadas localmente (Caché).");
                ejecutarMenuInteractivo();
            } else {
                System.err.println("No se pudo cargar, ingrese nuevamente.");
            }
        }
    }

    private Map<String, Double> obtenerTasasDeAPI() throws IOException, InterruptedException {
        String urlCompleta = API_URL_BASE + MONEDA_BASE;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(urlCompleta))
                .timeout(Duration.ofSeconds(10))
                .GET()
                .build();

        HttpClient cliente = HttpClient.newHttpClient();
        HttpResponse<String> respuesta = cliente.send(request, HttpResponse.BodyHandlers.ofString());

        if (respuesta.statusCode() == 200) {
            RespuestaAPI datos = gson.fromJson(respuesta.body(), RespuestaAPI.class);
            return datos.conversion_rates;
        } else {
            throw new IOException("Error HTTP. Código: " + respuesta.statusCode());
        }
    }

    // Manejo básico de Cache

    private static final String NOMBRE_CACHE = "tasas_cache.json";

    private void guardarTasasEnCache(Map<String, Double> tasas) {
        try (FileWriter escritor = new FileWriter(NOMBRE_CACHE)) {
            gson.toJson(tasas, escritor);
        } catch (IOException e) {
            System.err.println("Error al guardar caché: " + e.getMessage());
        }
    }

    private Map<String, Double> cargarTasasDeCache() {
        try (FileReader lector = new FileReader(NOMBRE_CACHE)) {
            java.lang.reflect.Type tipoMapa = new com.google.gson.reflect.TypeToken<Map<String, Double>>(){}.getType();
            return gson.fromJson(lector, tipoMapa);
        } catch (IOException e) {
            return new HashMap<>();
        }
    }

    public double calcularConversion(double monto, String origen, String destino) throws NullPointerException {

        Double tasaOrigen = tasasDeCambio.get(origen);
        Double tasaDestino = tasasDeCambio.get(destino);

        if (tasaOrigen == null || tasaDestino == null) {
            throw new NullPointerException("Tasa no disponible para " + origen + " o " + destino);
        }

        double montoEnUSD = monto / tasaOrigen;
        double resultado = montoEnUSD * tasaDestino;

        return Math.round(resultado * 100.0) / 100.0;
    }


    private void exibirMenu() {
        System.out.println("\n=============================================");
        System.out.println("    sea bienvenido al Conversor de Monedas");
        System.out.println("=============================================");
        System.out.println("Elija la conversión a realizar:");

        for (int i = 0; i < PARES_CONVERSION.length; i++) {
            String[] par = PARES_CONVERSION[i];
            System.out.printf("  %d) %s (%s a %s)\n", (i + 1), par[2], par[0], par[1]);
        }

        System.out.println("---------------------------------------------");
        System.out.println("  0) Salir");
        System.out.println("=============================================");
        System.out.print("Elija una opción: ");
    }

    private void ejecutarMenuInteractivo() {
        Scanner scanner = new Scanner(System.in);
        int opcion = -1;

        while (opcion != 0) {
            exibirMenu();

            try {
                opcion = scanner.nextInt();

                if (opcion == 0) {
                    System.out.println("\n¡Gracias por preferirnos¡");
                    break;
                }

                if (opcion > 0 && opcion <= PARES_CONVERSION.length) {
                    realizarConversionSeleccionada(opcion, scanner);
                } else {
                    System.err.println("\nOpción no válida. Inténtelo de nuevo.");
                }

            } catch (java.util.InputMismatchException e) {
                System.err.println("\nERROR: Ingrese solo el número de opción. Inténtelo de nuevo.");
                scanner.next();
            }
        }

        scanner.close();
    }

    private void realizarConversionSeleccionada(int opcion, Scanner scanner) {

        int indice = opcion - 1;
        String codigoOrigen = PARES_CONVERSION[indice][0];
        String codigoDestino = PARES_CONVERSION[indice][1];

        System.out.print("\nIngrese el monto en " + codigoOrigen + " a convertir: ");

        try {
            double monto = scanner.nextDouble();

            double resultado = calcularConversion(monto, codigoOrigen, codigoDestino);

            java.text.DecimalFormat df = new java.text.DecimalFormat("#,##0.00");
            System.out.println("\n=============================================");
            System.out.printf("  %s %s son: %s %s\n",
                    df.format(monto), codigoOrigen,
                    df.format(resultado), codigoDestino);
            System.out.println("=============================================");

        } catch (java.util.InputMismatchException e) {
            System.err.println("ERROR: Ingrese un número válido para el monto. Vuelva al menú.");
            scanner.next();
        } catch (NullPointerException e) {
            System.err.println("ERROR: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        new Main().iniciar();
    }
}