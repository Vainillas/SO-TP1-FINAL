import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;


/*
   maliberti
*/
public class EjecutorComandos {

    // java EjecutorComandos -s , < comandos.txt

    public static void main(String[] args) { //TODO: Realizar la ejecución de cada comando en hilos diferentes
        String separator; // Valor por defecto
        if (args.length > 0 && args[0].equals("-s") && args.length > 1) {
            separator = args[1]; // Valor especificado por el usuario
        } else { // Si no defino el separador en el else me tira error en el ide
            separator = "\t";
        }

        Map<String, String> environmentVariables = new HashMap<>(System.getenv());
        // La variable HOSTNAME debe ser pasada con el valor “prueba”
        environmentVariables.put("HOSTNAME", "prueba");

        AtomicInteger commandCount = new AtomicInteger(0);

        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
            String line;

            CountDownLatch latch = new CountDownLatch(1); // Contador para esperar a que todos los hilos terminen
            // Manejar señales INT y TERM

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    latch.await();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                System.out.println("Terminando programa por señal...");
                System.out.println("Número de líneas leídas o comandos ejecutados: " + commandCount.get());
                latch.countDown();
            }));

            /*Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("Esperando a que todos los hilos terminen...");

                System.out.println("Programa terminado por señal.");
            }));*/

            while ((line = reader.readLine()) != null) {
                String[] commandParts = line.split(" ");
                if (commandParts.length == 0 || commandParts[0].isEmpty()) {
                    System.err.println("Error: Linea vacía " + line);
                    continue;
                }

                String command = commandParts[0];
                String[] commandArgs = new String[commandParts.length - 1];
                System.arraycopy(commandParts, 1, commandArgs, 0, commandArgs.length);
                Thread commandThread = new Thread(() -> {
                    try {
                        ProcessBuilder processBuilder = new ProcessBuilder();
                        List<String> commandList = new ArrayList<>();
                        commandList.add(command);
                        commandList.addAll(Arrays.asList(commandArgs));
                        processBuilder.command(commandList);
                        processBuilder.environment().putAll(environmentVariables);

                        Process process = processBuilder.start();

                        int exitCode = process.waitFor();
                        // Iimprimir por la salida standar el comando y el valor de retorno, en formato
                        //“comando” separador “retorno”.
                        System.out.println(command + separator + exitCode);

                        if (exitCode != 0) {
                            //imprimir por la salida de errores
                            //el mensaje “El comando {comando}, fallo con valor de retorno {retorno}”.
                            System.err.println("El comando " + command + " fallo con valor de retorno " + exitCode);
                        }
                    } catch (IOException | InterruptedException e) {
                        System.err.println("Error al ejecutar el comando: " + e.getMessage());
                    } finally {
                        latch.countDown(); // Decrementar el contador cuando el hilo termina
                        //En teoría debería hacer que el programa no termine abruptamente, si no que espere a que todos los hilos terminen
                    }
                });
                commandThread.start();
                commandCount.incrementAndGet();
            }

            reader.close();

            System.exit(0);

        } catch (IOException e) {
            System.err.println("Error al leer la entrada estándar: " + e.getMessage());
            System.exit(1);
        }
    }
}
