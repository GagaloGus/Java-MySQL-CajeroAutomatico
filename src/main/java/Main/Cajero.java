package Main;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Scanner;

import Excepciones.DineroExcedido_Excepcion;
import Excepciones.DineroNoValido_Excepcion;
import Excepciones.MonedaNoValida_Excepcion;
import Excepciones.NoSuficienteDinero_Excepcion;
import Excepciones.NoSuficienteMoneda_Excepcion;

public class Cajero {

    Scanner sc = new Scanner(System.in);

    String BBDD;
    String Tabla_uso;
    Connection conn;
    ArrayList<String> TablasCreadas;
    ArrayList<Double> monedasDisponibles;
    HashMap<Double, Integer> valoresCajero;
    
    public Cajero(String BBDD) throws Exception {
        Conf conf = new Conf();

        TablasCreadas = new ArrayList<>();
        valoresCajero = new HashMap<>();

        // Establece la conexion con la base de datos
        conn = DriverManager.getConnection(
                "jdbc:mysql://localhost:3306/sys?serverTimezone=UTC",
                conf.User,
                conf.Password);

        Statement statement = conn.createStatement();

        statement.execute("create database if not exists " + BBDD);
        statement.execute("use " + BBDD);
        this.BBDD = BBDD;

        monedasDisponibles = new ArrayList<>();
        int[] valores = { 5, 2, 1 };
        for (int i = -2; i <= 2; i++) {
            for (int j = 0; j < 3; j++) {

                double moneda = valores[j] * Math.pow(10, i);
                monedasDisponibles.add(moneda);
            }
        }
    }

    public Cajero() throws Exception {
        this("CAJERO");
    }

    boolean ExisteTabla(String Tabla) throws Exception {
        ResultSet resultSet = conn.createStatement().executeQuery(
                String.format("show tables like '%s'", Tabla));

        // Si hay algun dato devuelto es true, si no es false
        return resultSet.isBeforeFirst();
    }

    public void CambiarTabla(String Tabla) throws Exception {
        if (ExisteTabla(Tabla)) {
            System.out.println("Existe el cajero");
        } else {
            System.out.println("No existe el cajero");
            ThreadSleep(0.5);
            CrearTabla(Tabla);
        }

        // Añade los valores de la tabla actual a un hashmap para iterar mas facilmente
        valoresCajero.clear();
        ResultSet resultset = conn.createStatement().executeQuery("select * from " + Tabla + " order by moneda desc");
        while (resultset.next()) {
            double moneda = resultset.getDouble("moneda");
            int cantidad = resultset.getInt("cantidad");
            valoresCajero.put(moneda, cantidad);
        }

        Tabla_uso = Tabla;
    }

    void CrearTabla(String Tabla) throws Exception {
        // Crear la tabla
        ImprimirPuntosSuspensivos("Creando cajero " + Tabla, 4, 2);
        conn.createStatement().execute(
                "create table if not exists " + Tabla + " (moneda double primary key not null, cantidad int not null)");

        // Inserta los valores de todas las monedas
        PreparedStatement prepStatement = conn.prepareStatement(
                "INSERT INTO " + Tabla + " (moneda, cantidad) VALUES (?, ?)");

        conn.setAutoCommit(false);

        int[] valores = { 5, 2, 1 };

        for (int i = -2; i <= 2; i++) {
            for (int j = 0; j < 3; j++) {
                double moneda = valores[j] * Math.pow(10, i);
                prepStatement.setDouble(1, moneda);
                prepStatement.setInt(2, 100);
                prepStatement.addBatch();
            }
        }

        prepStatement.executeBatch();
        conn.commit();
        prepStatement.close();
        conn.setAutoCommit(true);

        TablasCreadas.add(Tabla);
        System.out.println("Cajero " + Tabla + " y monedas creadas");
    }

    public void ListarTabla() throws Exception {
        int i = 1;
        ResultSet resultset = conn.createStatement().executeQuery(
                "select * from " + Tabla_uso + " order by moneda desc");

        while (resultset.next()) {
            double moneda = resultset.getDouble("moneda");
            int cantidad = resultset.getInt("cantidad");

            ThreadSleep(0.2);
            System.out.printf("%6.2f€ -> %-3d", moneda, cantidad);

            if (i % 4 == 0) {
                System.out.println();
            } else {
                System.out.print("  |  ");
            }
            i++;
        }
        ThreadSleep(0.5);
        System.out.printf("\nTu saldo total en este cajero es %.2f €\n", getDineroMaximo());
    }

    public HashMap<Double, Integer> ProcesoAlterarDinero(double dineroPedido, String Tabla, boolean anadir)
            throws Exception {
        return ProcesoAlterarDinero(dineroPedido, Tabla, anadir, null);
    }

    public HashMap<Double, Integer> ProcesoAlterarDinero(double dineroPedido, String Tabla, boolean anadir,
            HashMap<Double, Integer> dm) throws Exception {
        double dinPed_original = dineroPedido;

        HashMap<Double, Integer> DineroMap = (dm == null ? new HashMap<>() : new HashMap<>(dm));

        if (dm == null) {
            System.out.println("¿Que monedas quieres usar?\n(Escribe en formato-> 500.00: 1)");
            while (dineroPedido > 0) {
                System.out.print(" ->");
                // Borra los espacios en blanco y divide en 2 el string
                String[] partes = sc.nextLine().replaceAll("\\s+", "").replace(",", ".").split(":");
                System.out.println(Arrays.toString(partes));

                if (partes.length == 2) {
                    double mon = Double.parseDouble(partes[0]);
                    int canti = Integer.parseInt(partes[1]);

                    if (!monedasDisponibles.contains(mon)) {
                        throw new MonedaNoValida_Excepcion();
                    }

                    DineroMap.put(mon, canti);

                    // Altera todo lo pedido a la cantidad pedida
                    dineroPedido = dinPed_original;
                    for (double moneda : DineroMap.keySet()) {
                        dineroPedido -= moneda * DineroMap.get(moneda);
                    }

                    if (dineroPedido < 0) {
                        throw new DineroExcedido_Excepcion();
                    } else if (valoresCajero.get(mon) < canti && !anadir) {
                        throw new NoSuficienteMoneda_Excepcion();
                    } else {
                        System.out.println("Te quedan " + dineroPedido + "€ por asignar");
                    }
                } else {
                    throw new NumberFormatException();
                }
            }
        }

        for (Double moneda : DineroMap.keySet()) {

            String query = String.format(
                    "update %s set cantidad = cantidad %s %d where moneda = %s",
                    Tabla, (anadir ? "+" : "-"), DineroMap.get(moneda), String.format("%f", moneda).replace(",", "."));
            conn.createStatement().executeUpdate(query);
        }

        return DineroMap;
    }

    public HashMap<Double, Integer> SacarDinero(double dineroPedido) throws Exception {
        return SacarDinero(dineroPedido, Tabla_uso);
    }

    public HashMap<Double, Integer> SacarDinero(double dineroPedido, String Tabla) throws Exception {

        if (!ExisteTabla(Tabla)) {
            System.out.println("\n-- ERROR: El cajero " + Tabla + " no existe --");
            ThreadSleep(1);
            CrearTabla(Tabla);
            ThreadSleep(1);
            System.out.println("-- Cajero " + Tabla + " creado --\n");
            ThreadSleep(1);
        }

        if (dineroPedido > getDineroMaximo()) {
            throw new NoSuficienteDinero_Excepcion();
        }

        if (dineroPedido <= 0) {
            throw new DineroNoValido_Excepcion();
        }

        return ProcesoAlterarDinero(dineroPedido, Tabla, false);
    }

    public HashMap<Double, Integer> IntroducirDinero(double dineroInsertado) throws Exception {
        return IntroducirDinero(dineroInsertado, Tabla_uso, null);
    }

    public HashMap<Double, Integer> IntroducirDinero(double dineroInsertado, HashMap<Double, Integer> dm)
            throws Exception {
        return IntroducirDinero(dineroInsertado, Tabla_uso, dm);
    }

    public HashMap<Double, Integer> IntroducirDinero(double dineroInsertado, String Tabla, HashMap<Double, Integer> dm)
            throws Exception {

        if (!ExisteTabla(Tabla)) {
            System.out.println("\n-- ERROR: El cajero " + Tabla + " no existe --");
            ThreadSleep(1);
            ImprimirPuntosSuspensivos("Creando", 5, 2);
            CrearTabla(Tabla);
            System.out.println("-- Cajero " + Tabla + " creado --\n");
            ThreadSleep(2);
        }

        if (dineroInsertado <= 0) {
            throw new DineroNoValido_Excepcion();
        }

        return ProcesoAlterarDinero(dineroInsertado, Tabla, true, dm);
    }

    public double getDineroMaximo() {
        double dineroMaximo = 0;
        try {

            ResultSet resultset = conn.createStatement().executeQuery(
                    "select * from " + Tabla_uso + " order by moneda desc");

            while (resultset.next()) {
                double moneda = resultset.getDouble("moneda");
                int cantidad = resultset.getInt("cantidad");

                dineroMaximo += moneda * cantidad;
            }
        } catch (SQLException ex) {
        }

        return dineroMaximo;
    }

    public void CloseConnection(boolean dropTablas) {
        try {
            if (dropTablas) {
                for (String tabla : TablasCreadas) {
                    conn.createStatement().execute("drop table " + tabla);
                    System.out.println("Cajero " + tabla + " borrado..");
                    ThreadSleep(0.7);
                }
            }

            conn.close();
        } catch (SQLException ex) {
        }
    }

    void ThreadSleep(double tiempo) {
        try {
            Thread.sleep((long) (tiempo * 1000));
        } catch (InterruptedException e) {
        }
    }

    void ImprimirPuntosSuspensivos(String texto, int cantidad, double tiempo) {
        System.out.print(texto);
        for (int i = 0; i < cantidad; i++) {
            System.out.print(".");
            ThreadSleep(tiempo / (double) cantidad);
        }
        System.out.println();
    }

    public String getTabla_uso() {
        return Tabla_uso;
    }
}
