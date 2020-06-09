/*
 * Copyright 2020-2020 Exactpro (Exactpro Systems Limited)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.exactpro.th2.utility;

import static java.util.concurrent.TimeUnit.SECONDS;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactpro.th2.configuration.Configuration;
import com.exactpro.th2.utility.configuration.UtilityConfiguration;

import io.grpc.Server;
import io.grpc.ServerBuilder;

public class MicroserviceMain {
    private static final Logger LOGGER = LoggerFactory.getLogger(MicroserviceMain.class);

    /**
     * Environment variables:
     *  {@link Configuration#ENV_GRPC_PORT}
     */
    public static void main(String[] args) {
        try {
            Configuration configuration = new UtilityConfiguration();

            Server gRPCServer = ServerBuilder.forPort(configuration.getPort())
                    .addService(new MessageComparatorService())
                    .build();

            configureShutdownHook(gRPCServer);

            gRPCServer.start();
            LOGGER.info("Utility started");
            gRPCServer.awaitTermination();
        } catch (RuntimeException | InterruptedException | IOException e) {
            LOGGER.error("Utility crashed", e);
            System.exit(-1);
        }
    }

    private static void configureShutdownHook(Server gRPCServer) {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                LOGGER.info("Utility is terminating");
                gRPCServer.shutdown();
                if (!gRPCServer.awaitTermination(3, SECONDS)) {
                    LOGGER.info("Utility isn't terminated gracefully");
                    gRPCServer.shutdownNow();
                }
            } catch (InterruptedException e) {
                LOGGER.error("gRPC server shutdown is interrupted", e);
            } finally {
                LOGGER.info("Utility terminated");
            }
        }));
    }
}
