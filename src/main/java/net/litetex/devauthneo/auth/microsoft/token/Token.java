package net.litetex.devauthneo.auth.microsoft.token;

import java.time.Duration;
import java.time.Instant;

import com.google.gson.JsonObject;


public class Token
{
	protected static final Duration EXPIRY_BUFFER = Duration.ofSeconds(10);
	
	private final String token;
	private final Instant expiry;
	
	public Token(final String token, final Instant expiry)
	{
		this.token = token;
		this.expiry = expiry;
	}
	
	public String getToken()
	{
		return this.token;
	}
	
	public Instant getExpiry()
	{
		return this.expiry;
	}
	
	public boolean isExpired()
	{
		return Instant.now().isAfter(this.expiry);
	}
	
	public static Token fromJson(final JsonObject jsonObject)
	{
		return new Token(
			jsonObject.get("access_token").getAsString(),
			parseExpiresIn(jsonObject));
	}
	
	protected static Instant parseExpiresIn(final JsonObject jsonObject)
	{
		return Instant.now()
			.plusSeconds(jsonObject.get("expires_in").getAsInt())
			.minus(EXPIRY_BUFFER);
	}
}
