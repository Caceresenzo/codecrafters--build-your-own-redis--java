package redis.command;

import redis.Redis;
import redis.client.Client;

public interface Command {

	CommandResponse execute(Redis redis, Client client);

	/** @return Whether the command can be queued and executed when executing a transaction. */
	default boolean isQueueable() {
		return true;
	}

	/** @return Whether the command should also be sent to replicas. */
	default boolean isPropagatable() {
		return false;
	}

}