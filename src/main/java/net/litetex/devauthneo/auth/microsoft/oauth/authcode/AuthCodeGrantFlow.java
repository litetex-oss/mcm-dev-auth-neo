package net.litetex.devauthneo.auth.microsoft.oauth.authcode;

import java.net.URI;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;

import org.apache.commons.codec.digest.DigestUtils;

import net.litetex.devauthneo.auth.microsoft.oauth.BaseGrantFlow;
import net.litetex.devauthneo.auth.microsoft.token.OAuthToken;
import net.litetex.devauthneo.auth.shared.HttpClientUtil;


public abstract class AuthCodeGrantFlow extends BaseGrantFlow
{
	protected static final String OAUTH_AUTHORIZE_URL = "https://login.live.com/oauth20_authorize.srf";
	protected static final URI OAUTH_TOKEN_URI = URI.create("https://login.live.com/oauth20_token.srf");
	
	protected final String redirectUri;
	
	public AuthCodeGrantFlow(final String clientId, final String scopes, final String redirectUri)
	{
		super(clientId, scopes);
		this.redirectUri = redirectUri;
	}
	
	@Override
	public OAuthToken getToken()
	{
		final byte[] bytes = new byte[32];
		new SecureRandom().nextBytes(bytes);
		final String codeVerifier = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
		
		return this.getAuthorizationToken(orderedStringMap(
			"code", this.getAuthorizationCode(codeVerifier),
			"grant_type", "authorization_code",
			"code_verifier", codeVerifier
		));
	}
	
	@Override
	protected Map<String, String> getAuthorizationTokenParams()
	{
		final Map<String, String> params = super.getAuthorizationTokenParams();
		params.put("redirect_uri", this.redirectUri);
		return params;
	}
	
	protected abstract String getAuthorizationCode(final String codeVerifier);
	
	protected String buildAuthorizeUrl(final String codeVerifier)
	{
		return OAUTH_AUTHORIZE_URL + "?" + HttpClientUtil.buildQuery(orderedStringMap(
			"client_id",
			this.clientId,
			"response_type",
			"code",
			"redirect_uri",
			this.redirectUri,
			"scope",
			this.scopes,
			"prompt",
			"select_account",
			"code_challenge",
			Base64.getUrlEncoder().withoutPadding().encodeToString(DigestUtils.sha256(codeVerifier)),
			"code_challenge_method",
			"S256"
		));
	}
	
	@Override
	protected URI refreshTokenUri()
	{
		return OAUTH_TOKEN_URI;
	}
	
	protected OAuthToken getAuthorizationToken(final Map<String, String> extraParams)
	{
		return this.getAuthorizationToken(OAUTH_TOKEN_URI, extraParams);
	}
}
