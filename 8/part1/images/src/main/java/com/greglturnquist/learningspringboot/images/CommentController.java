/*
 * Copyright 2016 the original author or authors.
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
package com.greglturnquist.learningspringboot.images;

import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;

import org.springframework.boot.actuate.metrics.CounterService;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.annotation.Output;
import org.springframework.cloud.stream.messaging.Source;
import org.springframework.cloud.stream.reactive.FluxSender;
import org.springframework.cloud.stream.reactive.StreamEmitter;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Greg Turnquist
 */
// tag::rest-controller[]
@RestController
@EnableBinding(Source.class)
public class CommentController {
// end::rest-controller[]

	private final CounterService counterService;
	private FluxSink<Message<Comment>> commentSink;
	private Flux<Message<Comment>> flux;

	public CommentController(CounterService counterService) {
		this.counterService = counterService;
		this.flux = Flux.<Message<Comment>>create(
			emitter -> this.commentSink = emitter,
			FluxSink.OverflowStrategy.IGNORE)
			.publish()
			.autoConnect();
	}

	// tag::rest[]
	@PostMapping("/comments")
	public Mono<ResponseEntity<?>> addComment(Mono<Comment> newComment) {
		if (commentSink != null) {
			return newComment
				.map(comment -> {
					commentSink.next(MessageBuilder
						.withPayload(comment)
						.setHeader(MessageHeaders.CONTENT_TYPE,
							MediaType.APPLICATION_JSON_VALUE)
						.build());
					return comment;
				})
				.flatMap(comment -> {
					counterService.increment("comments.total.produced");
					counterService.increment(
						"comments." +
							comment.getImageId() + ".produced");
					return Mono.just(ResponseEntity.noContent().build());
				});
		} else {
			return Mono.just(ResponseEntity.noContent().build());
		}
	}
	// end::rest[]

	@StreamEmitter
	@Output(Source.OUTPUT)
	public void emit(FluxSender output) {
		output.send(this.flux);
	}

}
