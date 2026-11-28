package net.litetex.devauthneo.config.microsoft.oauth2.authcode;

import org.jetbrains.annotations.Nullable;

import net.litetex.devauthneo.config.microsoft.oauth2.OAuth2GrantFlowConfig;


public interface AuthCodeGrantFlowConfig extends OAuth2GrantFlowConfig
{
	@Nullable
	String redirectUri();
}
