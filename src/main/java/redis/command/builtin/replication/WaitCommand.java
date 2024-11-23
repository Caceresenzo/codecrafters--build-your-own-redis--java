package redis.command.builtin.replication;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import redis.Redis;
import redis.client.Client;
import redis.client.SocketClient;
import redis.command.Command;
import redis.command.CommandResponse;
import redis.type.RArray;
import redis.type.RInteger;
import redis.type.RString;

public record WaitCommand(
	int numberOfReplicas,
	Duration timeout
) implements Command {

	@Override
	public CommandResponse execute(Redis redis, Client client) {
		final var replicas = redis.getReplicas();

		if (redis.getReplicationOffset().get() == 0) {
			return new CommandResponse(RInteger.of(replicas.size()));
		}

		final var acks = new AtomicInteger();

		final var futures = new ArrayList<Map.Entry<SocketClient, Future<Integer>>>(replicas.size());
		replicas.forEach((replica) -> {
			if (replica.getOffset() == 0) {
				acks.incrementAndGet();
				return;
			}

			final var future = new CompletableFuture<Integer>();

			replica.command(new CommandResponse(
				RArray.of(
					RString.bulk("REPLCONF"),
					RString.bulk("GETACK"),
					RString.bulk("*")
				),
				false
			));

			replica.setReplicateConsumer((x) -> future.complete(1));
			futures.add(Map.entry(replica, future));
		});

		var remaining = (long) timeout.toSeconds();

		for (final var entry : futures) {
			if (acks.get() >= numberOfReplicas) {
				break;
			}

			final var future = entry.getValue();

			if (remaining <= 0) {
				final var isDone = future.isDone();
				if (isDone) {
					acks.incrementAndGet();
				}

				Redis.log("no time left isDone=%s acks=%d".formatted(isDone, acks.get()));
				continue;
			}

			final var start = System.currentTimeMillis();
			try {
				future.get(remaining, TimeUnit.MILLISECONDS);
				acks.incrementAndGet();

				final var took = System.currentTimeMillis() - start;
				remaining -= took;

				Redis.log("future ended took=%d remaining=%d acks=%d".formatted(took, remaining, acks.get()));
			} catch (TimeoutException exception) {
				final var took = System.currentTimeMillis() - start;

				remaining = 0;
				Redis.log("future timeout took=%d".formatted(took));
			} catch (Exception exception) {
				exception.printStackTrace();
			}
		}

		futures.forEach((entry) -> entry.getKey().setReplicateConsumer(null));

		return new CommandResponse(RInteger.of(acks.get()));
	}

	@Override
	public boolean isQueueable() {
		return false;
	}

}