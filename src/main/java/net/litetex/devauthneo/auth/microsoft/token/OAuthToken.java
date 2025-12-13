package net.litetex.devauthneo.auth.microsoft.token;

import java.time.Instant;

import com.google.gson.JsonObject;


public class OAuthToken extends Token
{
	private final String refreshToken;
	
	public OAuthToken(final String token, final String refreshToken, final Instant expiry)
	{
		super(token, expiry);
		this.refreshToken = refreshToken;
	}
	
	public String getRefreshToken()
	{
		return this.refreshToken;
	}
	
	public static OAuthToken fromJson(final JsonObject jsonObject)
	{
		return new OAuthToken(
			jsonObject.get("access_token").getAsString(),
			jsonObject.get("refresh_token").getAsString(),
			parseExpiresIn(jsonObject)
		);
	}
}
