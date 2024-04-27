package redis.type.stream.identifier;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class UniqueIdentifierTest {

	@Test
	void compareTo() {
		assertEquals(0, UniqueIdentifier.MIN.compareTo(UniqueIdentifier.MIN));
		assertEquals(1, UniqueIdentifier.MIN.compareTo(new UniqueIdentifier(0, 0)));
		assertEquals(1, new UniqueIdentifier(10, 0).compareTo(UniqueIdentifier.MIN));

		assertEquals(-1, new UniqueIdentifier(5, 0).compareTo(new UniqueIdentifier(10, 0)));
	}

}