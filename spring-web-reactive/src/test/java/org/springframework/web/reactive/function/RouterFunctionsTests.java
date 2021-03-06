/*
 * Copyright 2002-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.web.reactive.function;

import java.util.Collections;
import java.util.Optional;

import org.junit.Test;
import reactor.core.publisher.Mono;

import org.springframework.http.HttpMethod;
import org.springframework.http.codec.HttpMessageReader;
import org.springframework.http.codec.HttpMessageWriter;
import org.springframework.http.server.reactive.HttpHandler;
import org.springframework.mock.http.server.reactive.test.MockServerHttpRequest;
import org.springframework.mock.http.server.reactive.test.MockServerHttpResponse;
import org.springframework.web.reactive.result.view.ViewResolver;
import org.springframework.web.server.ServerWebExchange;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * @author Arjen Poutsma
 */
@SuppressWarnings("unchecked")
public class RouterFunctionsTests {

	@Test
	public void routeMatch() throws Exception {
		HandlerFunction<Void> handlerFunction = request -> ServerResponse.ok().build();

		MockServerRequest request = MockServerRequest.builder().build();
		RequestPredicate requestPredicate = mock(RequestPredicate.class);
		when(requestPredicate.test(request)).thenReturn(true);

		RouterFunction<Void> result = RouterFunctions.route(requestPredicate, handlerFunction);
		assertNotNull(result);

		Optional<HandlerFunction<Void>> resultHandlerFunction = result.route(request);
		assertTrue(resultHandlerFunction.isPresent());
		assertEquals(handlerFunction, resultHandlerFunction.get());
	}

	@Test
	public void routeNoMatch() throws Exception {
		HandlerFunction<Void> handlerFunction = request -> ServerResponse.ok().build();

		MockServerRequest request = MockServerRequest.builder().build();
		RequestPredicate requestPredicate = mock(RequestPredicate.class);
		when(requestPredicate.test(request)).thenReturn(false);

		RouterFunction<Void> result = RouterFunctions.route(requestPredicate, handlerFunction);
		assertNotNull(result);

		Optional<HandlerFunction<Void>> resultHandlerFunction = result.route(request);
		assertFalse(resultHandlerFunction.isPresent());
	}

	@Test
	public void subrouteMatch() throws Exception {
		HandlerFunction<Void> handlerFunction = request -> ServerResponse.ok().build();
		RouterFunction<Void> routerFunction = request -> Optional.of(handlerFunction);

		MockServerRequest request = MockServerRequest.builder().build();
		RequestPredicate requestPredicate = mock(RequestPredicate.class);
		when(requestPredicate.test(request)).thenReturn(true);

		RouterFunction<Void> result = RouterFunctions.subroute(requestPredicate, routerFunction);
		assertNotNull(result);

		Optional<HandlerFunction<Void>> resultHandlerFunction = result.route(request);
		assertTrue(resultHandlerFunction.isPresent());
		assertEquals(handlerFunction, resultHandlerFunction.get());
	}

	@Test
	public void subrouteNoMatch() throws Exception {
		HandlerFunction<Void> handlerFunction = request -> ServerResponse.ok().build();
		RouterFunction<Void> routerFunction = request -> Optional.of(handlerFunction);

		MockServerRequest request = MockServerRequest.builder().build();
		RequestPredicate requestPredicate = mock(RequestPredicate.class);
		when(requestPredicate.test(request)).thenReturn(false);

		RouterFunction<Void> result = RouterFunctions.subroute(requestPredicate, routerFunction);
		assertNotNull(result);

		Optional<HandlerFunction<Void>> resultHandlerFunction = result.route(request);
		assertFalse(resultHandlerFunction.isPresent());
	}

	@Test
	public void toHttpHandler() throws Exception {
		HandlerStrategies strategies = mock(HandlerStrategies.class);
		when(strategies.messageReaders()).thenReturn(
				() -> Collections.<HttpMessageReader<?>>emptyList().stream());
		when(strategies.messageWriters()).thenReturn(
				() -> Collections.<HttpMessageWriter<?>>emptyList().stream());
		when(strategies.viewResolvers()).thenReturn(
				() -> Collections.<ViewResolver>emptyList().stream());

		ServerRequest request = mock(ServerRequest.class);
		ServerResponse response = mock(ServerResponse.class);
		when(response.writeTo(any(ServerWebExchange.class), eq(strategies))).thenReturn(Mono.empty());

		HandlerFunction handlerFunction = mock(HandlerFunction.class);
		when(handlerFunction.handle(any(ServerRequest.class))).thenReturn(response);

		RouterFunction routerFunction = mock(RouterFunction.class);
		when(routerFunction.route(any(ServerRequest.class))).thenReturn(Optional.of(handlerFunction));

		RequestPredicate requestPredicate = mock(RequestPredicate.class);
		when(requestPredicate.test(request)).thenReturn(false);


		HttpHandler result = RouterFunctions.toHttpHandler(routerFunction, strategies);
		assertNotNull(result);

		MockServerHttpRequest httpRequest =
				new MockServerHttpRequest(HttpMethod.GET, "http://localhost");
		MockServerHttpResponse serverHttpResponse = new MockServerHttpResponse();
		result.handle(httpRequest, serverHttpResponse);
	}

}
