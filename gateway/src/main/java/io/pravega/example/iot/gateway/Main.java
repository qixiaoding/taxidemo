package io.pravega.example.iot.gateway;

import com.fasterxml.jackson.databind.JsonNode;
import io.pravega.client.ClientFactory;
import io.pravega.client.admin.StreamManager;
import io.pravega.client.stream.EventStreamWriter;
import io.pravega.client.stream.EventWriterConfig;
import io.pravega.client.stream.ScalingPolicy;
import io.pravega.client.stream.StreamConfiguration;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;

class Main
{
	private static final Logger Log = LoggerFactory.getLogger(Main.class);
    private static EventStreamWriter<JsonNode> writer;

    /**
     * Starts Grizzly HTTP server exposing JAX-RS resources defined in this application.
     * @return Grizzly HTTP server.
     */
    private static HttpServer startServer() {
        // Create a resource config that scans for JAX-RS resources and providers.
        final ResourceConfig rc = new ResourceConfig().packages("io.pravega.example.iot.gateway");

        // Create and start a new instance of grizzly http server exposing the Jersey application.
        return GrizzlyHttpServerFactory.createHttpServer(Parameters.getGatewayURI(), rc);
    }

    public static void main(String[] args) throws Exception    {
        Log.info("gateway main: BEGIN");

        URI controllerURI = Parameters.getControllerURI();
        StreamManager streamManager = StreamManager.create(controllerURI);
        String scope = Parameters.getScope();
        streamManager.createScope(scope);
        String streamName = Parameters.getStreamName();
        StreamConfiguration streamConfig = StreamConfiguration.builder()
                .scalingPolicy(ScalingPolicy.byEventRate(
                        Parameters.getTargetRateEventsPerSec(), Parameters.getScaleFactor(), Parameters.getMinNumSegments()))
                .build();
        streamManager.createStream(scope, streamName, streamConfig);

        ClientFactory clientFactory = ClientFactory.withScope(scope, controllerURI);
        writer = clientFactory.createEventWriter(
                streamName,
                new JsonNodeSerializer(),
                EventWriterConfig.builder().build());

        final HttpServer server = startServer();
        Log.info("Gateway running at {}", Parameters.getGatewayURI());
        Log.info("gateway main: END");
    }

    public static EventStreamWriter<JsonNode> getWriter() {
        return writer;
    }
}
