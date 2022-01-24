package org.springframework.data.elasticsearch.core.index.reindex;

import org.elasticsearch.index.reindex.RemoteInfo;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

import java.time.Duration;

/**
 * Remote info {@link RemoteInfo}
 *
 * @author Sijia Liu
 */
public class Remote {
	private final String scheme;
	private final String host;
	private final int port;

	@Nullable private final String pathPrefix;
	@Nullable private final String username;
	@Nullable private final String password;
	@Nullable private final Duration socketTimeout;
	@Nullable private final Duration connectTimeout;

	private Remote(String scheme, String host, int port, @Nullable String pathPrefix, @Nullable String username, @Nullable String password, @Nullable Duration socketTimeout, @Nullable Duration connectTimeout) {

		Assert.notNull(scheme, "scheme must not be null");
		Assert.notNull(host, "host must not be null");

		this.scheme = scheme;
		this.host = host;
		this.port = port;
		this.pathPrefix = pathPrefix;
		this.username = username;
		this.password = password;
		this.socketTimeout = socketTimeout;
		this.connectTimeout = connectTimeout;
	}

	public String getHost() {
		return host;
	}

	@Nullable
	public String getUsername() {
		return username;
	}

	@Nullable
	public String getPassword() {
		return password;
	}

	@Nullable
	public Duration getSocketTimeout() {
		return socketTimeout;
	}

	@Nullable
	public Duration getConnectTimeout() {
		return connectTimeout;
	}

	public String getScheme() {
		return scheme;
	}

	public int getPort() {
		return port;
	}

	@Nullable
	public String getPathPrefix() {
		return pathPrefix;
	}

	public static RemoteBuilder builder(String scheme, String host, int port){
		return new RemoteBuilder(scheme, host, port);
	}

	public static class RemoteBuilder{
		private final String scheme;
		private final String host;
		private final int port;
		@Nullable private String pathPrefix;
		@Nullable private String username;
		@Nullable private String password;
		@Nullable private Duration socketTimeout;
		@Nullable private Duration connectTimeout;

		public RemoteBuilder(String scheme, String host, int port) {
			this.scheme = scheme;
			this.host = host;
			this.port = port;
		}

		public RemoteBuilder withPathPrefix(String pathPrefix){
			this.pathPrefix = pathPrefix;
			return this;
		}

		public RemoteBuilder withUsername(String username){
			this.username = username;
			return this;
		}

		public RemoteBuilder withPassword(String password){
			this.password = password;
			return this;
		}

		public RemoteBuilder withSocketTimeout(Duration socketTimeout){
			this.socketTimeout = socketTimeout;
			return this;
		}

		public RemoteBuilder withConnectTimeout(Duration connectTimeout){
			this.connectTimeout = connectTimeout;
			return this;
		}

		public Remote build(){
			return new Remote(scheme, host, port , pathPrefix, username, password, socketTimeout, connectTimeout);
		}
	}
}
