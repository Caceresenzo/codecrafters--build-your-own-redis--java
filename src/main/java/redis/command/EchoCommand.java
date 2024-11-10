package redis.command;

public record EchoCommand(
	String message
) implements Command {

	public static final String NAME = "ECHO";

}