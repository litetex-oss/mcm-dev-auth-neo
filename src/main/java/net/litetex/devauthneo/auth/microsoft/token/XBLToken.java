package net.litetex.devauthneo.auth.microsoft.token;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

import com.google.gson.JsonObject;


public class XBLToken extends Token
{
	private final String userHash;
	
	public XBLToken(final String token, final String userHash, final Instant expiry)
	{
		super(token, expiry);
		this.userHash = userHash;
	}
	
	public String getUserHash()
	{
		return this.userHash;
	}
	
	public static XBLToken fromJson(final JsonObject jsonObject, final boolean hasUserHash)
	{
		return new XBLToken(
			jsonObject.get("Token").getAsString(),
			hasUserHash
				? jsonObject
				.get("DisplayClaims").getAsJsonObject()
				.get("xui").getAsJsonArray()
				.get(0).getAsJsonObject()
				.get("uhs").getAsString()
				: null,
			Instant.from(OffsetDateTime.parse(
					jsonObject.get("NotAfter").getAsString(),
					DateTimeFormatter.ISO_DATE_TIME))
				.minus(EXPIRY_BUFFER));
	}
}
