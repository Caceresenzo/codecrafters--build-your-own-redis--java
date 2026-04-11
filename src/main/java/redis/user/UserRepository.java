package redis.user;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import redis.type.RError;
import redis.type.RString;

public class UserRepository {

	private final List<User> users = new ArrayList<>();

	public UserRepository() {
		users.add(new User("default"));
	}

	public Optional<User> findByName(String name) {
		return users.stream()
			.filter(user -> user.getName().equals(name))
			.findFirst();
	}

	public User getByName(RString username) {
		final var name = username.content();

		return findByName(name).orElseThrow(() -> RError.noSuchUser(name).asException());
	}

}