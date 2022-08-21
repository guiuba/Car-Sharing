package carsharing;

import java.sql.*;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Scanner;

public class Controller {   // that's the invoker class
    private final Connection connection;
    Scanner scan = new Scanner(System.in);

    public Controller(Connection connection) {
        this.connection = connection;
    }

    public Command mainMenu() {
        Map<Integer, MenuCommand> menu = new LinkedHashMap<>();
        menu.put(1, new MenuCommand("Log in as a manager", this::managerMenu));
        menu.put(2, new MenuCommand("Log in as a customer", this::getCustomerList));
        menu.put(3, new MenuCommand("Create a customer", this::createCustomer));
        menu.put(0, new MenuCommand("Exit", null));
        return displayMenu(menu);
    }

    public Command managerMenu() {
        Map<Integer, MenuCommand> menu = new LinkedHashMap<>();
        menu.put(1, new MenuCommand("Company list", this::getCompanyList));
        menu.put(2, new MenuCommand("Create a company", this::createCompany));
        menu.put(0, new MenuCommand("Back", this::mainMenu));
        return displayMenu(menu);
    }

    public Command customerMenu(int customerId) {
        System.out.println();
        Map<Integer, MenuCommand> menu = new LinkedHashMap<>();
        menu.put(1, new MenuCommand("Rent a car", () -> rentCar(customerId)));
        menu.put(2, new MenuCommand("Return a rented car", () -> returnCar(customerId)));
        menu.put(3, new MenuCommand("My rented car", () -> checkRentedCar(customerId)));
        menu.put(0, new MenuCommand("Back", this::getCustomerList));
        return displayMenu(menu);
    }

    public Command companyMenu(Company company) {
        Map<Integer, MenuCommand> menu = new LinkedHashMap<>();
        menu.put(1, new MenuCommand("Car list", () -> getCarList(company)));
        menu.put(2, new MenuCommand("Create a car", () -> createCar(company)));
        menu.put(0, new MenuCommand("Back", this::managerMenu));
        return displayMenu(menu);
    }


    public Command companyMenuC(int customerId) {
        Map<Integer, MenuCommand> menu = new LinkedHashMap<>();
        menu.put(1, new MenuCommand("Company list", () -> getCompanyList()));
        menu.put(0, new MenuCommand("Back", () -> customerMenu(customerId)));
        return displayMenu(menu);
    }

    public boolean hasRentedCar(int customerId) {
        final String query = "SELECT RENTED_CAR_ID FROM CUSTOMER WHERE ID = ?;";
        try {
            PreparedStatement ps = connection.prepareStatement(query);
            ps.setInt(1, customerId);
            ResultSet rs = ps.executeQuery();
            rs.next();
            if (rs.getInt("RENTED_CAR_ID") == 0) {
                return false;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return true;
    }

    public Command rentCar(int customerId) {
        if (hasRentedCar(customerId)) {
            System.out.println("You've already rented a car!");
            return customerMenu(customerId);
        }
        return getCompanyListCustomer(customerId);
    }

    public Command returnCar(int customerId) {

        final String query1 = "" +
                "SELECT RENTED_CAR_ID  " +
                "FROM CUSTOMER " +
                "WHERE CUSTOMER.ID = ?;";
        final String query2 = "" +
                "UPDATE CUSTOMER " +
                "SET RENTED_CAR_ID = NULL " +
                "WHERE ID = ?;";
        try {
            PreparedStatement ps = connection.prepareStatement(query1);
            ps.setInt(1, customerId);
            ResultSet rs = ps.executeQuery();
            rs.next();
            if (rs.getInt("RENTED_CAR_ID") == 0) {
                System.out.println(" You didn't rent a car!");
                return customerMenu(customerId);
            } else {
                PreparedStatement ps2 = connection.prepareStatement(query2);
                ps2.setInt(1, customerId);
                ps2.executeUpdate();
                System.out.println("You've returned a rented car!");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return customerMenu(customerId);
    }

    public Command checkRentedCar(int customerId) {
        final String query = "" +
                "SELECT CUSTOMER.RENTED_CAR_ID AS RENTED_CAR_ID, " +
                "CAR.NAME AS car_name, " +
                "COMPANY.NAME AS company_name " +
                "FROM CUSTOMER " +
                "LEFT JOIN CAR " +
                "ON CAR.ID = CUSTOMER.RENTED_CAR_ID " +
                "LEFT JOIN COMPANY " +
                "ON COMPANY.ID = CAR.COMPANY_ID " +
                "WHERE CUSTOMER.ID = ?;";
        try {
            PreparedStatement ps = connection.prepareStatement(query);
            ps.setInt(1, customerId);
            ResultSet rs = ps.executeQuery();
            rs.next();
            if (rs.getInt("RENTED_CAR_ID") == 0) {
                System.out.println(" You didn't rent a car!");
                return customerMenu(customerId);
            } else {
                System.out.printf("Your rented car:\n%s\nCompany:\n%s\n",
                        rs.getString("car_name"),
                        rs.getString("company_name"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return customerMenu(customerId);
    }

    public Command companyCars(int companyId, int customerId) {
        Map<Integer, MenuCommand> menu = new LinkedHashMap<>();
        String query = "SELECT NAME, ID FROM CAR WHERE COMPANY_ID = ? ORDER BY ID;";
        ResultSet rs;
        int counter = 1;
        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setInt(1, companyId);
            rs = ps.executeQuery();
            if (!rs.next()) {
                System.out.println("The car list is empty!");
                System.out.println();
                return getCompanyListCustomer(customerId);
            } else {
                System.out.println("Choose a car:");
                do {
                    int carId = rs.getInt("ID");
                    String carName = rs.getString("NAME");
                    menu.put(counter, new MenuCommand(carName,
                            () -> registerRent(carId, customerId, carName)
                    ));
                    counter++;
                } while (rs.next());
                menu.put(0, new MenuCommand("Back", () -> getCompanyListCustomer(customerId)));
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        return displayMenu(menu);
    }

    public Command registerRent(int carId, int customerId, String carName) throws SQLException {
        String update = "UPDATE CUSTOMER SET RENTED_CAR_ID = ? WHERE ID = ?";
        try (PreparedStatement ps = connection.prepareStatement(update)) {
            ps.setInt(1, carId);
            ps.setInt(2, customerId);
            ps.executeUpdate();
            System.out.printf("You rented '%s'\n", carName);
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        return () -> customerMenu(customerId);
    }


    public Command displayMenu(Map<Integer, MenuCommand> menu) {
        menu.entrySet().stream()
                .map(i -> i.getKey() + ". " + i.getValue().getCaption())
                .forEach(System.out::println);
        System.out.print("> ");
        int key = scan.nextInt();
        if (menu.containsKey(key)) {
            return menu.get(key).getCommand();
        }
        return null;
    }

    public Command getCompanyListCustomer(int customerId) {
        final String query = "SELECT * FROM COMPANY;";
        Map<Integer, MenuCommand> menu = new LinkedHashMap<>();
        try (Statement st = connection.createStatement()) {
            ResultSet rs = st.executeQuery(query);
            if (!rs.next()) {
                System.out.println("The company list is empty!\n");
                return () -> this::managerMenu;
            } else {
                System.out.println("Choose a company:");
                do {
                    int carId = rs.getInt("ID");
                    String carName = rs.getString("NAME");
                    menu.put(carId, new MenuCommand(carName, () -> companyCars(carId, customerId)
                    ));
                } while (rs.next());
                menu.put(0, new MenuCommand("Back", () -> customerMenu(customerId))); // this::customerMenu
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        return displayMenu(menu);
    }

    public Command getCompanyList() {
        final String query = "SELECT * FROM COMPANY;";
        Map<Integer, MenuCommand> menu = new LinkedHashMap<>();
        try (Statement st = connection.createStatement()) {
            ResultSet rs = st.executeQuery(query);
            if (!rs.next()) {
                System.out.println("The company list is empty!\n");
                return () -> this::managerMenu;
            } else {
                System.out.println("Choose a company:");
                do {
                    Company company = new Company(rs.getInt("id"), rs.getString("name"));
                    menu.put(company.getId(), new MenuCommand(company.getName(),
                            () -> companyMenu(company)
                    ));
                } while (rs.next());
                menu.put(0, new MenuCommand("Back", this::managerMenu));
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        return displayMenu(menu);
    }

    public Command getCustomerList() {
        Map<Integer, MenuCommand> menu = new LinkedHashMap<>();
        String query = "SELECT NAME, ID FROM CUSTOMER ORDER BY ID;";
        try (Statement st = connection.createStatement()) {
            ResultSet rs = st.executeQuery(query);
            if (!rs.next()) {
                System.out.println("The customer list is empty!\n");
                return () -> this::mainMenu;
            } else {
                System.out.println("\nThe customer list:");
                do {
                    int customerId = rs.getInt("ID");
                    String customerName = rs.getString("NAME");
                    menu.put(customerId,
                            new MenuCommand(customerName,
                                    () -> customerMenu(customerId)));
                } while (rs.next());
                menu.put(0, new MenuCommand("Back", this::mainMenu));
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }

        return displayMenu(menu);

    }

    public Command createCustomer() {
        System.out.println("Enter the customer name:");
        System.out.print("> ");
        scan.nextLine();
        String customerName = scan.nextLine();
        String insert = "INSERT INTO CUSTOMER (NAME) VALUES (?)";
        try (PreparedStatement ps = connection.prepareStatement(insert)) {
            ps.setString(1, customerName);
            ps.executeUpdate();
            System.out.println("The customer was added!\n");
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        return this::mainMenu;
    }

    public Command createCompany() {
        System.out.println("Enter the company name:");
        System.out.print("> ");
        scan.nextLine();
        String companyName = scan.nextLine();
        String insert = "INSERT INTO COMPANY (NAME) VALUES (?)";
        try (PreparedStatement ps = connection.prepareStatement(insert)) {
            ps.setString(1, companyName);
            ps.executeUpdate();
            System.out.println("The company was created!");
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        return this::managerMenu;
    }

    public Command getCarList(Company company) {
        String query = "SELECT NAME, ID FROM CAR WHERE COMPANY_ID = ? ORDER BY ID;";
        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setInt(1, company.getId());
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) {
                System.out.println("The car list is empty!");
            } else {
                System.out.println("Car list:");
                int counter = 1;
                do {
                    System.out.printf("%d. %s\n", counter,
                            rs.getString("NAME"));
                    counter++;
                } while (rs.next());
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        System.out.println();
        return () -> companyMenu(company);
    }
    public Command createCar(Company company) {
        System.out.println("Enter the car name:");
        System.out.print("> ");
        scan.nextLine();
        String carName = scan.nextLine();
        String insert = "INSERT INTO CAR (NAME, COMPANY_ID) VALUES (?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(insert)) {
            ps.setString(1, carName);
            ps.setInt(2, company.getId());
            ps.executeUpdate();
            System.out.println("The car was added!");
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }

        return () -> companyMenu(company);
    }
}
