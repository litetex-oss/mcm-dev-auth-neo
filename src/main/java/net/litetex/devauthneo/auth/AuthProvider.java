package net.litetex.devauthneo.auth;

import java.util.Map;
import java.util.Set;


public interface AuthProvider
{
	Set<String> possibleArgs();
	
	Map<String, String> getLoginParams(String account);
}
