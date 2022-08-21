package carsharing;

public class MenuCommand {
    private String caption;
    private Command command;

    public MenuCommand(String caption, Command command) {
        this.caption = caption;
        this.command = command;
    }

    public String getCaption() {
        return caption;
    }

    public Command getCommand() {
        return command;
    }
}
