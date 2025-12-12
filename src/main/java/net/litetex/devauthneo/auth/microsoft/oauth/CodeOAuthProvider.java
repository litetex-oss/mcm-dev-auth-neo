package net.litetex.devauthneo.auth.microsoft.oauth;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpRequest;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.net.httpserver.HttpServer;

import net.litetex.devauthneo.auth.microsoft.token.OAuthToken;
import net.litetex.devauthneo.auth.shared.HttpClientUtil;
import net.minecraft.util.Util;


public class CodeOAuthProvider implements OAuthProvider
{
	private static final Logger LOG = LoggerFactory.getLogger(CodeOAuthProvider.class);
	
	private static final String CLIENT_ID = "757bb3b3-b7ca-4bcd-a160-c92e6379c263";
	
	private static final String SCOPES = "XboxLive.signin XboxLive.offline_access";
	private static final String REDIRECT_URI = "http://127.0.0.1:3000";
	private static final String OAUTH_URL = "https://login.live.com/oauth20_authorize.srf";
	private static final URI OAUTH_TOKEN_URI = URI.create("https://login.live.com/oauth20_token.srf");
	
	private static final int INT_HTTP_SERVER_PORT = 3000;
	
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
	public OAuthToken refreshToken(final OAuthToken token)
	{
		try
		{
			return this.getAuthorizationToken(orderedStringMap(
				"grant_type", "refresh_token",
				"refresh_token", token.getRefreshToken()
			));
		}
		catch(final Exception e)
		{
			LOG.error("Error refreshing OAuth token, trying to get new token", e);
			return this.getToken();
		}
	}
	
	private OAuthToken getAuthorizationToken(final Map<String, String> extraParams)
	{
		final Map<String, String> params = orderedStringMap(
			"client_id", CLIENT_ID,
			"scope", SCOPES,
			"redirect_uri", REDIRECT_URI
		);
		params.putAll(extraParams);
		
		return OAuthToken.fromJson(HttpClientUtil.jsonPost(
			OAUTH_TOKEN_URI,
			HttpRequest.BodyPublishers.ofString(params.entrySet()
				.stream()
				.map(e -> Stream.of(e.getKey(), e.getValue())
					.map(s -> URLEncoder.encode(s, StandardCharsets.UTF_8))
					.collect(Collectors.joining("=")))
				.collect(Collectors.joining("&"))
			),
			Map.of("Content-Type", "application/x-www-form-urlencoded")
		));
	}
	
	public String getAuthorizationCode(final String codeVerifier)
	{
		final HttpServer server;
		try
		{
			server = HttpServer.create(new InetSocketAddress("0.0.0.0", INT_HTTP_SERVER_PORT), 0);
		}
		catch(final IOException e)
		{
			throw new UncheckedIOException(
				"Failed to start integrated HTTP server[port=" + INT_HTTP_SERVER_PORT + "] to get authentication code",
				e);
		}
		
		try
		{
			final CompletableFuture<String> future = new CompletableFuture<>();
			server.createContext(
				"/", req -> {
					final URI uri = req.getRequestURI();
					
					// all other paths are 404
					if(!uri.getPath().equals("/"))
					{
						req.sendResponseHeaders(404, 0);
						req.getResponseBody().close();
						return;
					}
					
					final byte[] response;
					try(final InputStream is = CodeOAuthProvider.class.getResourceAsStream(
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
			
			if(LOG.isInfoEnabled())
			{
				final String url = OAUTH_URL + "?" + HttpClientUtil.buildQuery(orderedStringMap(
					"client_id",
					CLIENT_ID,
					"response_type",
					"code",
					"redirect_uri",
					REDIRECT_URI,
					"scope",
					SCOPES,
					"prompt",
					"select_account",
					"code_challenge",
					Base64.getUrlEncoder().withoutPadding().encodeToString(DigestUtils.sha256(codeVerifier)),
					"code_challenge_method",
					"S256"
				));
				
				LOG.info(
					"OAuth URL, open this in a browser to complete authentication: {}",
					url);
				
				try
				{
					getPlatform().openUri(URI.create(url));
				}
				catch(final Exception ex)
				{
					LOG.warn("Failed to auto open url in browser", ex);
				}
			}
			
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
	
	private static Map<String, String> orderedStringMap(final String... entries)
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
		else
		{
			return string.contains("unix") ? Util.OS.LINUX : Util.OS.UNKNOWN;
		}
	}
}
