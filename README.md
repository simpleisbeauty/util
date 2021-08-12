# util tool repository

actor
  - concurrent: Non-blocking buffered generic actor to create data pipeline, messaging, or workflow with simple Function/Consume interface
  - nio: Simple same NioChannel for both Server and Client socket io based on non-blocking single thread nio select. User just need to provide concrete
         read(byte[] in) call back function with Actor flow to transform/process incoming socket bytes, then write response to NioChannel.
  - util
      chunk: Split large volume io stream to compressed chunks to distributed in cloud, and join them for read.
      LatestConfig: Generic config with changing last_modified and poll_interval from abstract refresh method to determine centralized config
                    changed or not.
      DynamicPool: Abstract auto increase/decrease pool size by balancing among max_busy_time, max_idle time and max_connection,
                   auto refreshed in Latest Config.
  - example: Dummy DynamicPool to illstrate DynamicPool with LatestConfig, Echo Server/Client using concurrent and nio modules.

spring-cache-guava - spring-context 5.1.8 cache support for guava cache, alternative to spring caffeine

ParamParser.java - simple program arguments parser and validator by regex pattern, allow optional and --name argument


