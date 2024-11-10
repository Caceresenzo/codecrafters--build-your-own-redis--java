package redis.command;

import java.util.OptionalLong;

public record SetCommand(
	String key,
	Object value,
	OptionalLong millisecondes
) implements Command {

	public static final String NAME = "SET";

}