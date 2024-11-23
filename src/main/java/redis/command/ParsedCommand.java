package redis.command;

import redis.type.RArray;
import redis.type.RString;

public record ParsedCommand(
	RArray<RString> raw,
	Command command
) {}