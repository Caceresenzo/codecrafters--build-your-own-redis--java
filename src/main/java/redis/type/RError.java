package redis.type;

public record RError(
	RString message
) implements RValue {

	private static final RError SYNTAX = new RError("ERR syntax error");
	private static final RError STREAM_ID_INVALID = new RError("ERR Invalid stream ID specified as stream command argument");
	private static final RError XADD_ID_EQUAL_OR_SMALLER = new RError("ERR The ID specified in XADD is equal or smaller than the target stream top item");
	private static final RError XADD_ID_GREATER_0_0 = new RError("ERR The ID specified in XADD must be greater than 0-0");
	private static final RError WRONGTYPE_WRONG_KIND_OF_VALUE = new RError("WRONGTYPE Operation against a key holding the wrong kind of value");
	private static final String ERR_INVALID_COMMAND_IN_SUBSCRIBED_CONTEXT_FORMAT = "ERR Can't execute '%s': only (P|S)SUBSCRIBE / (P|S)UNSUBSCRIBE / PING / QUIT / RESET are allowed in this context";
	private static final RError VALUE_NOT_A_FLOAT = new RError("ERR value is not a valid float");

	public RError(String message) {
		this(RString.simple(message));
	}

	public RErrorException asException() {
		throw new RErrorException(this);
	}

	public static RError syntax() {
		return SYNTAX;
	}

	public static RError streamIdInvalid() {
		return STREAM_ID_INVALID;
	}

	public static RError xaddIdEqualOrSmaller() {
		return XADD_ID_EQUAL_OR_SMALLER;
	}

	public static RError xaddIdGreater00() {
		return XADD_ID_GREATER_0_0;
	}

	public static RError wrongtypeWrongKindOfValue() {
		return WRONGTYPE_WRONG_KIND_OF_VALUE;
	}

	public static RError invalidCommandInSubscribedContextFormat(String commandName) {
		return new RError(ERR_INVALID_COMMAND_IN_SUBSCRIBED_CONTEXT_FORMAT.formatted(commandName.toLowerCase()));
	}

	public static RError valueNotAFloat() {
		return VALUE_NOT_A_FLOAT;
	}

}