package redis.command.parser;

import redis.command.builtin.acl.AclGetUserCommand;
import redis.command.builtin.acl.AclWhoamiCommand;

public class AclCommandParser extends CommandParser {

	public AclCommandParser() {
		register("WHOAMI", noArgumentCommand(AclWhoamiCommand::new));
		register("GETUSER", singleArgumentCommand(AclGetUserCommand::new));
	}

}