/*
 * Copyright 2017 the original author or authors.
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
package org.springframework.data.elasticsearch.entities;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Documents;

/**
 * @author zzt
 */
@Setter
@Getter
@NoArgsConstructor
@Builder
@Documents(indexPattern = "chat_#{indexNameVar}", type = "chat")
public class Chat {

	@Id private String id;
	private String sender;
	private String receiver;
	private String content;

	public Chat(String id, String sender, String receiver, String content) {
		this.id = id;
		this.sender = sender;
		this.receiver = receiver;
		this.content = content;
	}
}
