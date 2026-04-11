package redis.user;

import java.security.MessageDigest;
import java.util.Collections;
import java.util.HexFormat;
import java.util.Set;
import java.util.TreeSet;

import lombok.Getter;
import lombok.SneakyThrows;

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

	public void addPassword(String plainPassword) {
		passwords.add(hashPassword(plainPassword));
		flags.remove(FLAG_NOPASS);
	}

	public boolean verifyPassword(String plainPassword) {
		return passwords.contains(hashPassword(plainPassword));
	}

	@SneakyThrows
	public static final String hashPassword(String plainPassword) {
		final var digest = MessageDigest.getInstance("SHA-256");
		final var hash = digest.digest(plainPassword.getBytes());

		return HexFormat.of().formatHex(hash);
	}

}