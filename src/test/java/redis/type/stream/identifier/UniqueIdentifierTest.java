package redis.type.stream.identifier;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class UniqueIdentifierTest {

	@Test
	void compareTo() {
		assertEquals(-1, UniqueIdentifier.ZERO.compareTo(UniqueIdentifier.MINIMUM));
		assertEquals(1, UniqueIdentifier.MINIMUM.compareTo(UniqueIdentifier.ZERO));

		assertEquals(0, UniqueIdentifier.MINIMUM.compareTo(UniqueIdentifier.MINIMUM));
		assertEquals(1, UniqueIdentifier.MINIMUM.compareTo(new UniqueIdentifier(0, 0)));
		assertEquals(1, new UniqueIdentifier(10, 0).compareTo(UniqueIdentifier.MINIMUM));

		assertEquals(-1, new UniqueIdentifier(5, 0).compareTo(new UniqueIdentifier(10, 0)));
	}

}