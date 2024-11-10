package redis.type;

public sealed interface RValue permits RArray, RBlob, RError, RInteger, RNil, ROk, RString {

}