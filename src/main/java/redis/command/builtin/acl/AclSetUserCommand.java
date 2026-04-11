package redis.command.builtin.acl;

import redis.Redis;
import redis.client.Client;
import redis.command.Command;
import redis.command.CommandResponse;
import redis.type.RError;
import redis.type.ROk;
import redis.type.RString;

public record AclSetUserCommand(
	RString username,
	RString newPassword
) implements Command {

	public static final RError USER_NOT_FOUND = new RError("ERR no such user");

	@Override
	public CommandResponse execute(Redis redis, Client client) {
		final var userRepository = redis.getUserRepository();

		final var user = userRepository.getByName(username);

		final var password = newPassword.content().substring(1); /* remove the leading '>' character */
		user.addPassword(password);

		return new CommandResponse(ROk.OK);
	}

	@Override
	public boolean isQueueable() {
		return false;
	}

}