package net.litetex.devauthneo.auth.microsoft;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.litetex.devauthneo.auth.AuthProvider;
import net.litetex.devauthneo.auth.microsoft.oauth.CodeOAuthProvider;
import net.litetex.devauthneo.auth.microsoft.oauth.OAuthProvider;
import net.litetex.devauthneo.shared.json.JSONSerializer;


public class MicrosoftAuthProvider implements AuthProvider
{
	private static final Logger LOG = LoggerFactory.getLogger(MicrosoftAuthProvider.class);
	
	public static final String IDENTIFIER = "microsoft";
	
	private static final String ACCESS_TOKEN = "accessToken";
	private static final String UUID = "uuid";
	private static final String USERNAME = "username";
	
	private final OAuthProvider oAuthProvider = new CodeOAuthProvider();
	
	private final Path file;
	
	private Map<String, Tokens> nameTokens = new HashMap<>();
	
	public MicrosoftAuthProvider(final Path stateDir)
	{
		this.file = stateDir.resolve("microsoft-accounts.json");
	}
	
	@Override
	public Set<String> possibleArgs()
	{
		return Set.of(ACCESS_TOKEN, UUID, USERNAME);
	}
	
	@Override
	public Map<String, String> login(final String account)
	{
		this.readFile();
		
		final MicrosoftLoginExecutor loginExecutor =
			new MicrosoftLoginExecutor(this.oAuthProvider, this.nameTokens.get(account));
		final LoginData loginData = loginExecutor.login();
		
		loginExecutor.requiresTokenUpdate().ifPresent(t -> {
			this.nameTokens.put(account, t);
			this.saveAsync();
		});
		
		return Map.of(
			ACCESS_TOKEN, loginData.accessToken(),
			UUID, loginData.uuid(),
			USERNAME, loginData.username());
	}
	
	private void readFile()
	{
		if(!Files.exists(this.file))
		{
			return;
		}
		
		final long startMs = System.currentTimeMillis();
		try
		{
			final PersistentState persistentState =
				JSONSerializer.GSON.fromJson(Files.readString(this.file), PersistentState.class);
			
			this.nameTokens = persistentState.nameTokens();
			
			LOG.debug(
				"Took {}ms to read {}x nameTokens",
				System.currentTimeMillis() - startMs,
				this.nameTokens.size());
		}
		catch(final Exception ex)
		{
			LOG.warn("Failed to read file['{}']", this.file, ex);
		}
	}
	
	private void saveAsync()
	{
		CompletableFuture.runAsync(this::saveToFile);
	}
	
	private synchronized void saveToFile()
	{
		final long startMs = System.currentTimeMillis();
		try
		{
			Files.writeString(this.file, JSONSerializer.GSON.toJson(new PersistentState(this.nameTokens)));
			LOG.debug(
				"Took {}ms to write {}x nameTokens",
				System.currentTimeMillis() - startMs,
				this.nameTokens.size());
		}
		catch(final Exception ex)
		{
			LOG.warn("Failed to write file['{}']", this.file, ex);
		}
	}
	
	record PersistentState(
		Map<String, Tokens> nameTokens
	)
	{
	}
}
