# Twinder - A Distributed Systems Service

> Twinder is a revolutionary gender-free dating service built on advanced technologies and a scalable distributed system architecture and Twinder provides a seamless and efficient dating platform. This project consists of four assignments, with the fourth assignment being a collaborative teamwork project. This app supports swipe events for users and stores information such as potential matches and the number of likes and dislikes.
> Assignment4 presentation checked [_here_](https://docs.google.com/presentation/d/1hFm6TPAzC2Zw7cwiCJnpDvBH_1iI77Kl8chhuudVlHo/edit#slide=id.g219bc6154e0_0_115).

## Table of Contents

- [General Info](#general-information)
- [Technologies Used](#technologies-used)
- [System Architecture and Improvements](#System-Architecture-and-Improvements)
- [Room for Improvement](#room-for-improvement)
- [Summarization](#Summarization)
- [Acknowledgements](#acknowledgements)
- [License](#license)

## General Information

At the core of Twinder is a RESTful service that we have meticulously designed using the CQRS pattern. This service efficiently handles user swipes, optimizes matches, and seamlessly facilitates communication between users. With our expertise in load balancing, RabbitMQ, Kafka, and Redis, Twinder ensures efficient scaling and enables asynchronous messaging.

To handle the high volumes of user data, we have implemented distributed databases, including MongoDB Shard Cluster. Additionally, we leverage the power of Redis to ensure fast retrieval of frequently accessed information. By optimizing various components such as multi-threading, fine-tuning the Tomcat Server, and optimizing Kafka partitions, we have significantly enhanced Twinder's performance.

For deployment, we have chosen the reliable infrastructure of AWS EC2, complemented by load balancing mechanisms. This setup guarantees fault tolerance and scalability as the user base grows. Through rigorous stress testing and meticulous fine-tuning of our system's components and configurations, we have achieved an impressive throughput, enabling Twinder to handle a large number of requests per second.

## Technologies Used

- **CQRS** (Command Query Responsibility Segregation): Ensures scalability, maintainability, and efficient data processing in Twinder's RESTful service
- **Load Balancing**: Distributes incoming requests across multiple servers to prevent bottlenecks and ensure responsiveness
- **RabbitMQ and Kafka**: Asynchronous messaging protocols that enable efficient communication between system components
- **MongoDB Shard Cluster**: Distributed databases that accommodate high volumes of user data in a scalable manner
- **Redis**: High-performance in-memory data store used for caching and fast retrieval of frequently accessed information
- **Optimization Techniques**: Multi-threading, Tomcat Server tuning, and Kafka partition optimization for enhanced system performance
- **AWS EC2**: Deployment on the Amazon Web Services infrastructure, leveraging load balancing mechanisms for fault tolerance and scalability
- **Stress Testing**: Rigorous testing to evaluate and optimize system performance, achieving high throughput

## System Architecture and Improvements

[System Architecture](./img/screenshot.png)

[Tuning Kafka Producer parameters](./img/screenshot1.png)

[Tuning Kafka partitions](./img/screenshot2.png)

[Tuning Kafka Consumer threads](./img/screenshot3.png)

[Test within same VPC & scaled out structure](./img/screenshot4.png)

[Incorporate Load Balancer](./img/screenshot5.png)

[Improved Producer from Sync to Async write](./img/screenshot6.png)

## Room for Improvement

Room for improvement - Done:

- Tuning Kafka Producer parameters
- Tuning Kafka partitions
- Tuning Kafka Consumer threads
- Incorporate Load Balancer
- Test within same VPC & scaled out structure
- Improved Producer from Sync to Async write

Room for improvement - To do:

- Implement a circuit breaker to monitor the connection of the Tomcat server and the consumer component
- Implement a data backup mechanism to improve data safety and enable fast recovery in the event of server crashes

## Summarization

In conclusion, we have successfully optimized our system through various approaches, including adjusting Kafka producer parameters, increasing the number of Kafka partitions, modifying Kafka consumer threads, and testing both within the same and across VPCs. Additionally, we scaled out key components by adding extra servlets, Mongos instances, and dedicated EC2 instances for each member of the MongoDB sharded cluster replica set. We also incorporated a load balancer and implemented asynchronous messaging for the producer's communication with the broker. After conducting numerous experiments and tests, we are proud to report that our efforts have led to a substantial increase in throughput and significantly reduced latency compared to our initial design.

## Acknowledgements

- The project drew inspiration and guidance from various resources, including online tutorials, research papers, and discussions within the development community.
- We would like to express our gratitude to the authors and contributors of the resources and tutorials that provided valuable insights and helped shape the development of Twinder.
- Many thanks to Cindy [@cindy-hsin](https://github.com/cindy-hsin) and other team members Jack and Justin who dedicated their time and expertise to bring this project to life, contributing their skills in software development, system architecture, and design.
- Many thanks to professor Gorton [@gortonator](https://github.com/gortonator) who provided feedback and insights throughout the development process, helping us refine and improve Twinder to meet the needs and expectations of our assignment requirement.

## License

This project is open source and available under the [MIT License](https://opensource.org/licenses/MIT).
