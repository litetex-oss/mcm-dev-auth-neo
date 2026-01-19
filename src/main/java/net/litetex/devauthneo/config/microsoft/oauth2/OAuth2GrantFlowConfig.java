package net.litetex.devauthneo.config.microsoft.oauth2;

import java.util.List;

import org.jetbrains.annotations.Nullable;


public interface OAuth2GrantFlowConfig
{
	@Nullable
	String clientId();
	
	List<String> scopes();
}
