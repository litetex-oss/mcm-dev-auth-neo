package net.litetex.devauthneo.auth.microsoft.oauth.authcode;

import static java.util.Objects.requireNonNullElse;
import static java.util.Objects.requireNonNullElseGet;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.jetbrains.annotations.Nullable;

import com.sun.net.httpserver.HttpServer;

import net.litetex.devauthneo.auth.microsoft.oauth.OAuthGrantFlow;
import net.litetex.devauthneo.auth.shared.HttpClientUtil;
import net.litetex.devauthneo.config.microsoft.oauth2.authcode.ExternalAuthCodeGrantFlowConfig;
import net.minecraft.util.Util;


public class ExternalAuthCodeGrantFlow extends AuthCodeGrantFlow
{
	public static final String DEV_AUTH_ID = "devauth";
	private static final Map<String, PredefinedProvider> PRE_DEFINED_PROVIDERS = Map.of(
		DEV_AUTH_ID, new PredefinedProvider("757bb3b3-b7ca-4bcd-a160-c92e6379c263", 3000)
	);
	
	private final boolean openSystemBrowser;
	private final int httpPort;
	
	ExternalAuthCodeGrantFlow(
		final ExternalAuthCodeGrantFlowConfig config,
		final PredefinedProvider predefinedProvider,
		final int httpPort)
	{
		super(
			requireNonNullElse(
				config.clientId(),
				predefinedProvider.clientId()),
			createScopes(config.scopes(), DEFAULT_SCOPES),
			requireNonNullElseGet(
				config.redirectUri(),
				() -> Optional.ofNullable(predefinedProvider.redirectUri())
					.orElse("http://127.0.0.1:{port}"))
				.replace("{port}", String.valueOf(httpPort)));
		this.openSystemBrowser = config.openSystemBrowser();
		this.httpPort = httpPort;
	}
	
	public static ExternalAuthCodeGrantFlow create(final ExternalAuthCodeGrantFlowConfig config)
	{
		final PredefinedProvider predefinedProvider = Optional.ofNullable(config.predefinedProvider())
			.map(String::toLowerCase)
			.map(PRE_DEFINED_PROVIDERS::get)
			.orElseGet(() -> PRE_DEFINED_PROVIDERS.get(DEV_AUTH_ID));
		return new ExternalAuthCodeGrantFlow(
			config,
			predefinedProvider,
			requireNonNullElse(config.portForRedirect(), predefinedProvider.port()));
	}
	
	@SuppressWarnings({"checkstyle:MagicNumber", "PMD.AvoidUsingHardCodedIP"})
	@Override
	protected String getAuthorizationCode(final String codeVerifier)
	{
		final HttpServer server;
		try
		{
			server = HttpServer.create(new InetSocketAddress("0.0.0.0", this.httpPort), 0);
		}
		catch(final IOException e)
		{
			throw new UncheckedIOException(
				"Failed to start integrated HTTP server[port=" + this.httpPort + "] to get authentication code",
				e);
		}
		
		try
		{
			final CompletableFuture<String> future = new CompletableFuture<>();
			server.createContext(
				"/", req -> {
					final URI uri = req.getRequestURI();
					
					// all other paths are 404
					if(!"/".equals(uri.getPath()))
					{
						req.sendResponseHeaders(404, 0);
						req.getResponseBody().close();
						return;
					}
					
					final byte[] response;
					try(final InputStream is = OAuthGrantFlow.class.getResourceAsStream(
						"/assets/oauth_redirect.html"))
					{
						Objects.requireNonNull(is);
						response = is.readAllBytes();
					}
					req.getResponseHeaders().add("Content-Type", "text/html");
					req.sendResponseHeaders(200, response.length);
					req.getResponseBody().write(response);
					req.getResponseBody().close();
					
					final Map<String, String> query = HttpClientUtil.parseQuery(req.getRequestURI().getRawQuery());
					
					if(query.containsKey("error"))
					{
						future.completeExceptionally(
							new RuntimeException(
								"OAuth error: " + query.get("error") + ": " + query.get("error_description"))
						);
					}
					
					future.complete(query.get("code"));
				});
			server.start();
			
			final String url = this.buildAuthorizeUrl(codeVerifier);
			
			this.logger.warn(
				"OAuth URL, open this in a browser to complete authentication:\n{}",
				url);
			
			this.maybeOpenSystemBrowser(url);
			
			return future.join();
		}
		catch(final Exception e)
		{
			throw new RuntimeException("Failed to get authorization code", e);
		}
		finally
		{
			server.stop(0);
		}
	}
	
	private void maybeOpenSystemBrowser(final String url)
	{
		if(this.openSystemBrowser)
		{
			try
			{
				getPlatform().openUri(URI.create(url));
			}
			catch(final Exception ex)
			{
				this.logger.warn("Failed to auto open url in browser", ex);
			}
		}
	}
	
	private static Util.OS getPlatform()
	{
		final String string = System.getProperty("os.name").toLowerCase(Locale.ROOT);
		if(string.contains("win"))
		{
			return Util.OS.WINDOWS;
		}
		else if(string.contains("mac"))
		{
			return Util.OS.OSX;
		}
		else if(string.contains("linux"))
		{
			return Util.OS.LINUX;
		}
		return string.contains("unix") ? Util.OS.LINUX : Util.OS.UNKNOWN;
	}
	
	record PredefinedProvider(
		String clientId,
		int port,
		@Nullable
		String redirectUri
	)
	{
		public PredefinedProvider(final String clientId, final int port)
		{
			this(clientId, port, null);
		}
	}
}
