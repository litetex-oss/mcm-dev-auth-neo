package net.litetex.devauthneo.auth.microsoft.oauth;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonObject;

import net.litetex.devauthneo.auth.microsoft.token.OAuthToken;
import net.litetex.devauthneo.auth.shared.HttpClientUtil;


public abstract class BaseGrantFlow implements OAuthGrantFlow
{
	protected final Logger logger;
	
	protected static final String[] DEFAULT_SCOPES = {"XboxLive.signin", "offline_access"};
	
	protected final String clientId;
	protected final String scopes;
	
	public BaseGrantFlow(final String clientId, final String scopes)
	{
		this.logger = LoggerFactory.getLogger(this.getClass());
		
		this.clientId = clientId;
		this.scopes = scopes;
	}
	
	protected static String createScopes(final List<String> scopes, final String... defaultScopes)
	{
		if(scopes.isEmpty())
		{
			return String.join(" ", defaultScopes);
		}
		return String.join(" ", scopes);
	}
	
	protected abstract URI refreshTokenUri();
	
	@Override
	public OAuthToken refreshToken(final OAuthToken token)
	{
		try
		{
			return this.getAuthorizationToken(
				this.refreshTokenUri(),
				orderedStringMap(
					"grant_type", "refresh_token",
					"refresh_token", token.getRefreshToken()
				));
		}
		catch(final Exception e)
		{
			this.logger.error("Error refreshing OAuth token, trying to get new token", e);
			return this.getToken();
		}
	}
	
	protected Map<String, String> getAuthorizationTokenParams()
	{
		return orderedStringMap(
			"client_id", this.clientId,
			"scope", this.scopes);
	}
	
	protected OAuthToken getAuthorizationToken(final URI uri, final Map<String, String> extraParams)
	{
		return OAuthToken.fromJson(this.authorizationRequest(uri, extraParams));
	}
	
	protected JsonObject authorizationRequest(final URI uri, final Map<String, String> extraParams)
	{
		final Map<String, String> params = this.getAuthorizationTokenParams();
		params.putAll(extraParams);
		return HttpClientUtil.jsonPostForm(uri, params);
	}
	
	protected static Map<String, String> orderedStringMap(final String... entries)
	{
		if(entries.length % 2 != 0)
		{
			throw new IllegalArgumentException("Number of entries must be even");
		}
		
		final Map<String, String> map = new LinkedHashMap<>();
		for(int i = 0; i < entries.length; i += 2)
		{
			map.put(entries[i], entries[i + 1]);
		}
		
		return map;
	}
}
