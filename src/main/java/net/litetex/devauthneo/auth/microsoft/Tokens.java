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
	
	public OAuthToken getOauth()
	{
		return this.oauth;
	}
	
	public void setOauth(final OAuthToken oauth)
	{
		this.oauth = oauth;
	}
	
	public XBLToken getXbl()
	{
		return this.xbl;
	}
	
	public void setXbl(final XBLToken xbl)
	{
		this.xbl = xbl;
	}
	
	public XBLToken getXsts()
	{
		return this.xsts;
	}
	
	public void setXsts(final XBLToken xsts)
	{
		this.xsts = xsts;
	}
	
	public Token getSession()
	{
		return this.session;
	}
	
	public void setSession(final Token session)
	{
		this.session = session;
	}
}
