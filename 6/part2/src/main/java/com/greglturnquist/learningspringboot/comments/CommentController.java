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
package com.greglturnquist.learningspringboot.comments;

import reactor.core.publisher.Mono;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.actuate.metrics.CounterService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;

/**
 * @author Greg Turnquist
 */
// tag::code[]
@Controller
public class CommentController {

	private final RabbitTemplate rabbitTemplate;

	private final CounterService counterService;

	public CommentController(RabbitTemplate rabbitTemplate,
							 CounterService counterService) {
		this.rabbitTemplate = rabbitTemplate;
		this.counterService = counterService;
	}

	@PostMapping("/comments")
	public Mono<String> addComment(Mono<Comment> newComment) {
		return newComment.flatMap(comment ->
			Mono.fromRunnable(() ->
				rabbitTemplate
					.convertAndSend(
						"learning-spring-boot",
						"comments.new",
						comment))
			.then(Mono.just(comment)))
			.log("commentService-publish")
			.flatMap(comment -> {
				counterService.increment("comments.total.produced");
				counterService.increment(
					"comments." + comment.getImageId() + ".produced");
				return Mono.just("redirect:/");
			});
	}
}
// end::code[]