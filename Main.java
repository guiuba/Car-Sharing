package carsharing;


import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class Main {

    public static void main(String[] args) throws ClassNotFoundException, SQLException {

        String dbFileName = "jdbc:h2:./src/carsharing/db/";
        String localDbFileName = "jdbc:h2:D:\\variousTests\\h2Test\\carSharing5.4, sa, ";
        if (args.length > 0) {
            dbFileName += args[1];
        } else {
            dbFileName += "carsharing";
        }
        //     new CompanyDaoImpl(dbFileName);

        try (Connection cn = initConnection(localDbFileName)) { //   dbFileName
            Controller controller = new Controller(cn);
            Command command = controller::mainMenu;
            while (command != null) {
                command = command.exec();
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    private static Connection initConnection(String dbFileName) throws Exception {
        Class.forName("org.h2.Driver");
        Connection cn = DriverManager.getConnection(dbFileName);
        cn.setAutoCommit(true);
        initDb(cn);
        return cn;
    }

    private static void initDb(Connection cn) {
    //    final String dropFormerCompanyTable = "DROP TABLE IF EXISTS COMPANY";
        final String companyTableQuery = "CREATE TABLE IF NOT EXISTS COMPANY (" + //
                "ID INTEGER AUTO_INCREMENT, " +
                "NAME VARCHAR(20) NOT NULL UNIQUE," +
                "PRIMARY KEY (ID));";

      //  final String dropFormerCarTable = "DROP TABLE IF EXISTS CAR";

        final String carTableQuery = "CREATE TABLE IF NOT EXISTS CAR (" + //
                "ID INTEGER AUTO_INCREMENT, " +
                "NAME VARCHAR(20) NOT NULL UNIQUE, " +
                "COMPANY_ID INTEGER NOT NULL, " +
                "FOREIGN KEY(COMPANY_ID) REFERENCES COMPANY(ID), " +
                "PRIMARY KEY(ID));";

        final String customerTableQuery = "CREATE TABLE IF NOT EXISTS CUSTOMER (" + //
                "ID INTEGER AUTO_INCREMENT, " +
                "NAME VARCHAR(20) NOT NULL UNIQUE, " +
                "RENTED_CAR_ID INTEGER DEFAULT NULL, " +
                "FOREIGN KEY(RENTED_CAR_ID) REFERENCES CAR(ID), " +
                "PRIMARY KEY(ID));";

        final String companyResetIdQuery = "ALTER TABLE COMPANY ALTER COLUMN ID RESTART WITH 1;";
        final String carResetIdQuery = "ALTER TABLE COMPANY ALTER COLUMN ID RESTART WITH 1;";
        final String customerResetIdQuery = "ALTER TABLE CUSTOMER ALTER COLUMN ID RESTART WITH 1;";

        try {
         //   execDsl(cn, dropFormerCompanyTable);

            execDsl(cn, companyTableQuery);
            execDsl(cn, companyResetIdQuery);

          //  execDsl(cn, dropFormerCarTable);

            execDsl(cn, carTableQuery);
            execDsl(cn, carResetIdQuery);

            execDsl(cn, customerTableQuery);
            execDsl(cn, customerResetIdQuery); // mexi aqui

        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    private static void execDsl(Connection cn, String query) throws SQLException {
        try (Statement st = cn.createStatement()) {
            st.executeUpdate(query);
        }
    }
}