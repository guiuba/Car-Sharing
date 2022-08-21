package carsharing;

import java.sql.SQLException;

@FunctionalInterface
public interface Command {
    Command exec() throws SQLException;
}
