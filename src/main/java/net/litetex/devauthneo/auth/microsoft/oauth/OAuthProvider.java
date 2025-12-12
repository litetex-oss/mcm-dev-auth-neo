package net.litetex.devauthneo.auth.microsoft.oauth;

import net.litetex.devauthneo.auth.microsoft.token.OAuthToken;


public interface OAuthProvider
{
	OAuthToken getToken();
	
	OAuthToken refreshToken(OAuthToken token);
}
