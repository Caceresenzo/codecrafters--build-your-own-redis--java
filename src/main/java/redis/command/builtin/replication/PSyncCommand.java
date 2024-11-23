package redis.command.builtin.replication;

import java.util.Base64;

import redis.Redis;
import redis.client.Client;
import redis.client.SocketClient;
import redis.command.Command;
import redis.command.CommandResponse;
import redis.type.RBlob;
import redis.type.RError;
import redis.type.RString;

public record PSyncCommand() implements Command {

	private static final RError COULD_NOT_ENABLE = new RError("ERR could not enable replica");

	@Override
	public CommandResponse execute(Redis redis, Client client) {
		final var socketClient = SocketClient.cast(client);

		socketClient.setReplicate(true);

		final var replicas = redis.getReplicas();

		replicas.add(socketClient);
		if (!socketClient.onDisconnect(replicas::remove)) {
			replicas.remove(socketClient);

			throw COULD_NOT_ENABLE.asException();
		}

		socketClient.command(new CommandResponse(RString.simple("FULLRESYNC %s 0".formatted(redis.getMasterReplicationId()))));
		socketClient.command(new CommandResponse(RBlob.bulk(Base64.getDecoder().decode("UkVESVMwMDEx+glyZWRpcy12ZXIFNy4yLjD6CnJlZGlzLWJpdHPAQPoFY3RpbWXCbQi8ZfoIdXNlZC1tZW3CsMQQAPoIYW9mLWJhc2XAAP/wbjv+wP9aog=="))));

		return null;
	}

	@Override
	public boolean isQueueable() {
		return false;
	}

}