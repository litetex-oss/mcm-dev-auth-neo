package net.litetex.devauthneo.auth.shared;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.litetex.devauthneo.shared.json.JSONSerializer;


public final class HttpClientUtil
{
	public static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(30);
	
	public static HttpClient.Builder newHttpClientBuilder()
	{
		return HttpClient.newBuilder()
			.connectTimeout(DEFAULT_TIMEOUT);
	}
	
	public static HttpRequest.Builder newHttpClientRequest(final URI uri)
	{
		return HttpRequest.newBuilder(uri)
			.timeout(DEFAULT_TIMEOUT)
			.setHeader("User-Agent", "Mozilla/5.0 (dev-auth-neo)");
	}
	
	public static String buildQuery(final Map<String, String> params)
	{
		
		return params.entrySet()
			.stream()
			.map(e -> Stream.of(e.getKey(), e.getValue())
				.map(s -> URLEncoder.encode(s, StandardCharsets.UTF_8))
				.collect(Collectors.joining("=")))
			.collect(Collectors.joining("&"));
	}
	
	public static Map<String, String> parseQuery(final String query)
	{
		if(query == null || query.isEmpty())
		{
			return Map.of();
		}
		
		return Arrays.stream(query.split("&"))
			.map(kv -> Arrays.stream(kv.split("="))
				.limit(2)
				.map(s -> URLDecoder.decode(s, StandardCharsets.UTF_8))
				.toList())
			.collect(Collectors.toMap(
				List::getFirst,
				l -> l.size() > 1 ? l.getLast() : ""));
	}
	
	public static JsonObject jsonPost(final URI uri, final JsonObject body)
	{
		return jsonPost(
			uri,
			HttpRequest.BodyPublishers.ofString(JSONSerializer.GSON.toJson(body)),
			Map.of());
	}
	
	public static JsonObject jsonPost(
		final URI uri,
		final HttpRequest.BodyPublisher bodyPublisher,
		final Map<String, String> additionalHeaders)
	{
		final HttpResponse<String> response = post(uri, bodyPublisher, additionalHeaders);
		
		checkStatus(response);
		
		return JsonParser.parseString(response.body()).getAsJsonObject();
	}
	
	public static JsonObject jsonPostForm(
		final URI uri,
		final Map<String, String> formData)
	{
		final HttpResponse<String> response = postForm(uri, formData);
		
		checkStatus(response);
		
		return JsonParser.parseString(response.body()).getAsJsonObject();
	}
	
	public static HttpResponse<String> postForm(
		final URI uri,
		final Map<String, String> formData)
	{
		return post(
			uri,
			formBodyPublisher(formData),
			Map.of("Content-Type", "application/x-www-form-urlencoded"));
	}
	
	public static HttpResponse<String> post(
		final URI uri,
		final HttpRequest.BodyPublisher bodyPublisher,
		final Map<String, String> additionalHeaders)
	{
		try(final HttpClient httpClient = newHttpClientBuilder().build())
		{
			final HttpRequest.Builder builder = newHttpClientRequest(uri)
				.setHeader("Accept", "application/json")
				.POST(bodyPublisher);
			
			additionalHeaders.forEach(builder::setHeader);
			
			return httpClient.send(
				builder.build(),
				HttpResponse.BodyHandlers.ofString()
			);
		}
		catch(final IOException e)
		{
			throw new UncheckedIOException("Failed to execute request to " + uri, e);
		}
		catch(final InterruptedException e)
		{
			Thread.currentThread().interrupt();
			throw new IllegalStateException("Got interrupted", e);
		}
	}
	
	private static HttpRequest.BodyPublisher formBodyPublisher(final Map<String, String> formData)
	{
		return HttpRequest.BodyPublishers.ofString(formData.entrySet()
			.stream()
			.map(e -> Stream.of(e.getKey(), e.getValue())
				.map(s -> URLEncoder.encode(s, StandardCharsets.UTF_8))
				.collect(Collectors.joining("=")))
			.collect(Collectors.joining("&")));
	}
	
	public static HttpResponse<String> checkStatus(final HttpResponse<String> res)
	{
		if(res.statusCode() != 200)
		{
			throw new RuntimeException(
				"Received bad status " + res.statusCode() + " body: " + res.body()
			);
		}
		return res;
	}
	
	private HttpClientUtil()
	{
	}
}
