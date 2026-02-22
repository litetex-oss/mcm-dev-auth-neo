package net.litetex.devauthneo.auth.microsoft.token;

public final class TokenKey<T extends Token>
{
	public static final TokenKey<OAuthToken> OAUTH_TOKEN = TokenKey.of("oauth", OAuthToken.class);
	public static final TokenKey<XBLToken> XBL_TOKEN = TokenKey.of("xbl", XBLToken.class);
	public static final TokenKey<XBLToken> XSTS_TOKEN = TokenKey.of("xsts", XBLToken.class);
	public static final TokenKey<Token> SESSION_TOKEN = TokenKey.of("session", Token.class);
	
	private final String name;
	private final Class<T> clazz;
	
	private TokenKey(final String name, final Class<T> clazz)
	{
		this.name = name;
		this.clazz = clazz;
	}
	
	public String getName()
	{
		return this.name;
	}
	
	public Class<T> getClazz()
	{
		return this.clazz;
	}
	
	public static <T extends Token> TokenKey<T> of(final String name, final Class<T> clazz)
	{
		return new TokenKey<>(name, clazz);
	}
}
