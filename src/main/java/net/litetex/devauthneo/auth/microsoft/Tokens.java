package net.litetex.devauthneo.auth.microsoft;

import net.litetex.devauthneo.auth.microsoft.token.OAuthToken;
import net.litetex.devauthneo.auth.microsoft.token.Token;
import net.litetex.devauthneo.auth.microsoft.token.XBLToken;


class Tokens
{
	private OAuthToken oauth;
	private XBLToken xbl;
	private XBLToken xsts;
	private Token session;
	
	OAuthToken getOauth()
	{
		return this.oauth;
	}
	
	void setOauth(final OAuthToken oauth)
	{
		this.oauth = oauth;
	}
	
	XBLToken getXbl()
	{
		return this.xbl;
	}
	
	void setXbl(final XBLToken xbl)
	{
		this.xbl = xbl;
	}
	
	XBLToken getXsts()
	{
		return this.xsts;
	}
	
	void setXsts(final XBLToken xsts)
	{
		this.xsts = xsts;
	}
	
	Token getSession()
	{
		return this.session;
	}
	
	void setSession(final Token session)
	{
		this.session = session;
	}
}
