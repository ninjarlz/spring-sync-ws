package org.springframework.sync.diffsync;

import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor
@Data
public class Shadow<T> {
	private T resource;
	private int serverVersion;  // aka serverVersion in the context of a server app
	private int clientVersion; // aka clientVersion in the context of a server app
}
