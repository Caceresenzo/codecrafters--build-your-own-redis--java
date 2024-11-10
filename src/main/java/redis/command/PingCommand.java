package redis.command;

public record PingCommand() implements Command {

	public static final String NAME = "PING";

}