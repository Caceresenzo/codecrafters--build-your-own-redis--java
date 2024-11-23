package redis.command;

import redis.Redis;
import redis.client.Client;

public interface Command {

	CommandResponse execute(Redis redis, Client client);

	default boolean isQueueable() {
		return true;
	}

	default boolean isPropagatable() {
		return false;
	}

}