package redis.user;

import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;

import lombok.Getter;

public class User {

	public static final String FLAG_NOPASS = "nopass";

	private final @Getter String name;
	private final Set<String> flags;
	private final Set<String> passwords;

	public User(String name) {
		this(name, new TreeSet<>(String.CASE_INSENSITIVE_ORDER), new TreeSet<>(String.CASE_INSENSITIVE_ORDER));
	}

	public User(String name, Set<String> flags, Set<String> passwords) {
		this.name = name;
		this.flags = flags;
		this.passwords = passwords;

		if (passwords.isEmpty()) {
			this.flags.add(FLAG_NOPASS);
		}
	}

	public Set<String> getFlags() {
		return Collections.unmodifiableSet(flags);
	}

	public Set<String> getPasswords() {
		return Collections.unmodifiableSet(passwords);
	}

	public void addPassword(String password) {
		this.passwords.add(password);
		this.flags.remove(FLAG_NOPASS);
	}

}