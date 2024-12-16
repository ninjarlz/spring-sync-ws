# Spring Sync Ws

## Description

The aim of this particular project is to **introduce real-time WebSocket/STOMP communication between clients and the Spring Sync backend**.

Fork of Spring Sync, a project created in order to enable efficient communication and data synchronization between clients and Spring server applications using a [Differential Synchronization algorithm](http://neil.fraser.name/writing/sync/eng047-fraser.pdf) created by Neil Fraser and a [JSON Patch format](https://www.rfc-editor.org/info/rfc6902).
JSON Patch defines a JSON document structure for expressing a sequence of operations to apply to a JavaScript Object Notation (JSON) document; it is suitable for use with the `HTTP PATCH` method. The `application/json-patch+json` media type is used to identify such patch documents.

## Spring Sync

Spring Sync is a prototype module of the Spring framework in the form of an open-source plugin. It was initially developed by Craig Walls and Brian Cavalier and was first introduced at the [SpringOne2GX conference in 2014](https://www.slideshare.net/briancavalier/differential-sync-and-json-patch-s2-gx-2014). Spring Sync aims to provide efficient communication and synchronization between a client and a server (or any set of nodes that share a specific resource). The primary goal of the module was to extend the Spring framework with an implementation of Neil Fraser's Differential Synchronization algorithm. While it natively supports the JSON Patch data exchange format, it is largely independent of any specific patch format - allowing users of the plugin to supply their own implementation.
The original implementation of the plugin enables communication exclusively via REST endpoints.

However, the subject of this project is to extend its functionality to support data synchronization using WebSocket/STOMP protocols. This approach ensures that data synchronization conducted through the Spring Sync plugin occurs in real time. For this purpose, both the framework's built-in message broker and an external solution, RabbitMQ, were utilized.
The original Spring Sync project is now archived and available in a public [Git repository](https://github.com/spring-attic/spring-sync) under the Apache 2.0 license.

## Usage

Here are two repositories presenting usage of the plugin, simulating both sides of the communication:
* [todosyncws](https://github.com/ninjarlz/todosyncws) - simple Spring Boot service simulating application backend powered by Spring Sync Ws plugin.
* [todosyncclientws](https://github.com/ninjarlz/todosyncclientws) - simple Spring Boot service simulating application client powered by Spring Sync Ws plugin.

## Implementation

Further details of the provided implementation are described in author's master thesis available [here](https://drive.google.com/file/d/1yDWy3Z8tfSdYdx1t00atVMBNUdZZNNix/view?usp=sharing).

## Built with

* [Spring Framework](https://spring.io/projects/spring-framework) - the world’s leading Java web app creation platform.
* [Spring Sync](https://spring.io/blog/2014/10/22/introducing-spring-sync) - a Spring module that enables efficient communication and data synchronization between clients and Spring server applications using a [Differential Synchronization algorithm](http://neil.fraser.name/writing/sync/eng047-fraser.pdf) created by Neil Fraser and a [JSON Patch format](https://www.rfc-editor.org/info/rfc6902).
* [Spring Messaging](https://docs.spring.io/spring-boot/reference/messaging/index.html) -  a module of the Spring Framework that provides support for messaging-based applications, including abstraction layers for messaging protocols, integration with message brokers, and support for annotations to handle messages seamlessly.
* [Spring WebSocket](https://docs.spring.io/spring-framework/reference/web/websocket.html) - module of the Spring Framework that provides support for WebSocket-based communication, enabling full-duplex, real-time messaging between clients and servers with seamless integration into the Spring ecosystem.
* [Apache Commons](https://commons.apache.org) - a collection of reusable, open-source Java libraries and components that provide a wide range of utilities to simplify common programming tasks,
  such as file handling, string manipulation, and configuration management.
* [Docker](https://www.docker.com/get-started) - an open-source platform that automates the deployment, scaling, and management of applications using lightweight,
  portable containers that include everything needed to run the software.

## Developers

Authors of the original Spring Sync project:
* **Craig Walls**
* **Brian Cavalier**

Author of Spring Sync Ws extension:
* **Michał Kuśmidrowicz** - [ninjarlz](https://github.com/ninjarlz)

## License
This project is licensed under the Apache License 2.0 - see the [license.txt](license.txt) file for details