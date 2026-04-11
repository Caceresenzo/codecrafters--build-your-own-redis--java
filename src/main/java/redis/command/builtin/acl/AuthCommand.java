package redis.command.builtin.acl;

import redis.Redis;
import redis.client.Client;
import redis.client.SocketClient;
import redis.command.Command;
import redis.command.CommandResponse;
import redis.type.RError;
import redis.type.ROk;
import redis.type.RString;

public record AuthCommand(
	RString username,
	RString password
) implements Command {

	public static final RError WRONG_CREDENTIALS = new RError("WRONGPASS invalid username-password pair or user is disabled.");

	@Override
	public CommandResponse execute(Redis redis, Client client) {
		final var socketClient = SocketClient.cast(client);

		final var userRepository = redis.getUserRepository();

		final var user = userRepository.authenticate(username, password);
		if (user.isEmpty()) {
			throw WRONG_CREDENTIALS.asException();
		}

		socketClient.setUser(user.get());
		return new CommandResponse(ROk.OK);
	}

	@Override
	public boolean isQueueable() {
		return false;
	}

	@Override
	public boolean isAuthenticationRequired() {
		return false;
	}

}