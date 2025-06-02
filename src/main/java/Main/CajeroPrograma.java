package Main;

import java.util.HashMap;
import java.util.Scanner;

import Excepciones.DineroExcedido_Excepcion;
import Excepciones.DineroNoValido_Excepcion;
import Excepciones.MonedaNoValida_Excepcion;
import Excepciones.NoSuficienteDinero_Excepcion;
import Excepciones.NoSuficienteMoneda_Excepcion;

public class CajeroPrograma {

    static Scanner sc = new Scanner(System.in);

    static String[] opciones = {
            "Listar dinero",
            "Realizar compra",
            "Introducir dinero",
            "Transferir fondos",
            "Cambiar Tabla",
            "Salir"
    };

    static void ImprimirOpciones() {
        System.out.println("Escribe la operacion a realizar:");
        for (int i = 1; i <= opciones.length; i++) {

            System.out.println("   " + i + "- " + opciones[i - 1]);
        }
    }

    public static void main(String[] args) {
        Cajero cajero;
        try {
            cajero = new Cajero();
        } catch (Exception ex) {
            System.out.println(
                    "No se pudo establecer una conexion con el sistema\nIntentelo mas tarde\n" + ex.getMessage());
            return;
        }

        System.out.print("Indique el nombre del cajero: ");
        try {
            cajero.CambiarTabla(sc.next());
        } catch (Exception ex) {
            System.out.println("No se pudo crear el cajero, intente otra operacion\n" + ex.getMessage());
        }

        ThreadSleep(1.5);

        int operacion = 0;
        while (operacion != opciones.length) {
            System.out.println("");

            ImprimirOpciones();
            operacion = sc.nextInt();

            if (operacion < 1 || opciones.length < operacion) {
                System.out.println("Elija una opcion valida");
                continue;
            }

            System.out.println("\n-- " + opciones[operacion - 1].toUpperCase() + " --\n");

            // Salir
            if (operacion == opciones.length) {
                break;
            }

            try {
                operaciones(cajero, operacion);
            } catch (NoSuficienteDinero_Excepcion e) {
                System.out.println("Error: No hay tanto dinero en este cajero\n MAX: " + cajero.getDineroMaximo());
            } catch (NoSuficienteMoneda_Excepcion e) {
                System.out.println("Error: No hay tanta cantidad de esta moneda");
            } catch (DineroNoValido_Excepcion e) {
                System.out.println("Error: Escribe una cantidad de dinero valida");
            } catch (MonedaNoValida_Excepcion e) {
                System.out.println("Error: Escribe una moneda valida");
            } catch (DineroExcedido_Excepcion e) {
                System.out.println("Error: Te pasaste de dinero");
            } catch (NumberFormatException e) {
                System.out.println("Error: Escriba los datos en el formato solicitado");
            } catch (Exception ex) {
                System.out.println("Error: intentelo mas tarde\n" + ex);
            }

            ThreadSleep(1);
            System.out.println("\n" + "-".repeat(20));

        }

        System.out.print("Te gustaria borrar los datos de los cajeros creados en esta sesion? [Y/N]: ");
        boolean borrarTablas = sc.next().toUpperCase().charAt(0) == 'Y';

        cajero.CloseConnection(borrarTablas);

        System.out.println("\n-- Cajero cerrado --");
    }

    public static void operaciones(Cajero cajero, int operacion) throws Exception {
        // Listar
        if (operacion == 1) {
            cajero.ListarTabla();
        } // Realizar compra
        else if (operacion == 2) {

            System.out.print("Cuanto dinero cuesta?: ");
            cajero.SacarDinero(sc.nextDouble());
            System.out.println("Compra realizada!");

        } // Introducir dinero
        else if (operacion == 3) {

            System.out.print("Cuanto dinero quiere introducir?: ");
            cajero.IntroducirDinero(sc.nextDouble());
            System.out.println("Dinero añadido a la cuenta!");

        } // Transferir fondos
        else if (operacion == 4) {
            System.out.print("A que cajero quieres transferir?: ");
            String Tabla_transferir = sc.next();

            if (Tabla_transferir.equals(cajero.getTabla_uso())) {
                System.out.println("No puedes transferir dinero al mismo cajero");
                return;
            }

            System.out.print("Cuanto dinero quieres transferir?: ");
            double dineroTransferir = sc.nextDouble();

            HashMap<Double, Integer> dineroMap = cajero.SacarDinero(dineroTransferir);
            ImprimirPuntosSuspensivos("Transfiriendo", 4, 2);
            ThreadSleep(0.5);
            ImprimirPuntosSuspensivos("Dinero retirado de " + cajero.getTabla_uso(), 3, 1);

            ThreadSleep(1);
            ImprimirPuntosSuspensivos("Añadiendo dinero a " + Tabla_transferir, 3, 1);

            cajero.IntroducirDinero(dineroTransferir, Tabla_transferir, dineroMap);

            ThreadSleep(1);
            System.out.println("Dinero añadido a " + Tabla_transferir);
            ThreadSleep(1);

            System.out.print("\nTransferencia realizada!");

        } // Cambiar
        else if (operacion == 5) {

            System.out.print("Indique el cajero: ");
            cajero.CambiarTabla(sc.next());
            System.out.println("Cajero cambiado correctamente");
        }
    }

    static void ThreadSleep(double tiempo) {
        try {
            Thread.sleep((long) (tiempo * 1000));
        } catch (InterruptedException e) {
        }
    }

    static void ImprimirPuntosSuspensivos(String texto, int cantidad, double tiempo) {
        System.out.print(texto);
        for (int i = 0; i < cantidad; i++) {
            System.out.print(".");
            ThreadSleep(tiempo / (double) cantidad);
        }
        System.out.println();
    }
}
