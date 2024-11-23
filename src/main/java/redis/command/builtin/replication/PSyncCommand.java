package redis.command.builtin.replication;

import java.util.Base64;

import redis.Redis;
import redis.client.Client;
import redis.command.Command;
import redis.command.CommandResponse;
import redis.type.RBlob;
import redis.type.RError;
import redis.type.RString;

public record PSyncCommand() implements Command {

	private static final RError COULD_NOT_ENABLE = new RError("ERR could not enable replica");

	@Override
	public CommandResponse execute(Redis redis, Client client) {
		client.setReplicate(true);

		final var replicas = redis.getReplicas();

		replicas.add(client);
		if (!client.onDisconnect(replicas::remove)) {
			replicas.remove(client);

			throw COULD_NOT_ENABLE.asException();
		}

		client.command(new CommandResponse(RString.simple("FULLRESYNC %s 0".formatted(redis.getMasterReplicationId()))));
		client.command(new CommandResponse(RBlob.bulk(Base64.getDecoder().decode("UkVESVMwMDEx+glyZWRpcy12ZXIFNy4yLjD6CnJlZGlzLWJpdHPAQPoFY3RpbWXCbQi8ZfoIdXNlZC1tZW3CsMQQAPoIYW9mLWJhc2XAAP/wbjv+wP9aog=="))));

		return null;
	}

	@Override
	public boolean isQueueable() {
		return false;
	}

}