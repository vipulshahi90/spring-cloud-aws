/*
 * Copyright 2013-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.aws.autoconfigure.context;

import java.lang.reflect.Field;

import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cloud.aws.context.config.AmazonEc2InstanceDataPropertySourcePostProcessor;
import org.springframework.cloud.aws.context.support.env.AwsCloudEnvironmentCheckUtils;
import org.springframework.util.ReflectionUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Agim Emruli
 */
class ContextInstanceDataAutoConfigurationTest {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(ContextInstanceDataAutoConfiguration.class));

	@BeforeEach
	void restContextInstanceDataCondition() throws IllegalAccessException {
		Field field = ReflectionUtils.findField(AwsCloudEnvironmentCheckUtils.class, "isCloudEnvironment");
		assertThat(field).isNotNull();
		ReflectionUtils.makeAccessible(field);
		field.set(null, null);
	}

	@Test
	void placeHolder_noExplicitConfiguration_createInstanceDataResolverForAwsEnvironment() throws Exception {
		// Arrange
		HttpServer httpServer = MetaDataServer.setupHttpServer();
		HttpContext instanceIdHttpContext = httpServer.createContext("/latest/meta-data/instance-id",
				new MetaDataServer.HttpResponseWriterHandler("testInstanceId"));

		this.contextRunner.run(
				context -> assertThat(context).hasSingleBean(AmazonEc2InstanceDataPropertySourcePostProcessor.class));

		httpServer.removeContext(instanceIdHttpContext);
	}

	@Test
	void placeHolder_noExplicitConfiguration_missingInstanceDataResolverForNotAwsEnvironment() throws Exception {
		// Arrange
		HttpServer httpServer = MetaDataServer.setupHttpServer();
		HttpContext instanceIdHttpContext = httpServer.createContext("/latest/meta-data/instance-id",
				new MetaDataServer.HttpResponseWriterHandler(null));

		this.contextRunner.run(context -> assertThat(context)
				.doesNotHaveBean((AmazonEc2InstanceDataPropertySourcePostProcessor.class)));

		httpServer.removeContext(instanceIdHttpContext);
	}

	@Test
	void placeHolder_noExplicitConfiguration_createInstanceDataResolverThatResolvesWithDefaultAttributes()
			throws Exception {
		// Arrange
		HttpServer httpServer = MetaDataServer.setupHttpServer();
		HttpContext instanceIdHttpContext = httpServer.createContext("/latest/meta-data/instance-id",
				new MetaDataServer.HttpResponseWriterHandler("testInstanceId"));
		HttpContext userDataHttpContext = httpServer.createContext("/latest/user-data",
				new MetaDataServer.HttpResponseWriterHandler("a:b;c:d"));

		this.contextRunner.run(context -> {
			assertThat(context.getEnvironment().getProperty("a")).isEqualTo("b");
			assertThat(context.getEnvironment().getProperty("c")).isEqualTo("d");
		});

		httpServer.removeContext(instanceIdHttpContext);
		httpServer.removeContext(userDataHttpContext);
	}

	@Test
	void placeHolder_customValueSeparator_createInstanceDataResolverThatResolvesWithCustomValueSeparator()
			throws Exception {
		// Arrange
		HttpServer httpServer = MetaDataServer.setupHttpServer();
		HttpContext instanceIdHttpContext = httpServer.createContext("/latest/meta-data/instance-id",
				new MetaDataServer.HttpResponseWriterHandler("testInstanceId"));
		HttpContext userDataHttpContext = httpServer.createContext("/latest/user-data",
				new MetaDataServer.HttpResponseWriterHandler("a=b;c=d"));

		this.contextRunner.withPropertyValues("cloud.aws.instance.data.valueSeparator:=").run(context -> {
			assertThat(context.getEnvironment().getProperty("a")).isEqualTo("b");
			assertThat(context.getEnvironment().getProperty("c")).isEqualTo("d");
		});

		httpServer.removeContext(instanceIdHttpContext);
		httpServer.removeContext(userDataHttpContext);
	}

	@Test
	void placeHolder_customAttributeSeparator_createInstanceDataResolverThatResolvesWithCustomAttribute()
			throws Exception {
		// Arrange
		HttpServer httpServer = MetaDataServer.setupHttpServer();
		HttpContext instanceIdHttpContext = httpServer.createContext("/latest/meta-data/instance-id",
				new MetaDataServer.HttpResponseWriterHandler("testInstanceId"));
		HttpContext userDataHttpContext = httpServer.createContext("/latest/user-data",
				new MetaDataServer.HttpResponseWriterHandler("a:b/c:d"));

		this.contextRunner.withPropertyValues("cloud.aws.instance.data.attributeSeparator:/").run(context -> {
			assertThat(context.getEnvironment().getProperty("a")).isEqualTo("b");
			assertThat(context.getEnvironment().getProperty("c")).isEqualTo("d");
		});

		httpServer.removeContext(instanceIdHttpContext);
		httpServer.removeContext(userDataHttpContext);
	}

}
