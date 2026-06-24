package io.github.qifan777.server.integration.a2a;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.io.JsonEOFException;
import com.fasterxml.jackson.databind.JsonNode;
import io.a2a.common.A2AHeaders;
import io.a2a.server.ServerCallContext;
import io.a2a.server.auth.UnauthenticatedUser;
import io.a2a.server.extensions.A2AExtensions;
import io.a2a.spec.CancelTaskRequest;
import io.a2a.spec.DeleteTaskPushNotificationConfigRequest;
import io.a2a.spec.GetAuthenticatedExtendedCardRequest;
import io.a2a.spec.GetTaskPushNotificationConfigRequest;
import io.a2a.spec.GetTaskRequest;
import io.a2a.spec.IdJsonMappingException;
import io.a2a.spec.InternalError;
import io.a2a.spec.InvalidParamsError;
import io.a2a.spec.InvalidParamsJsonMappingException;
import io.a2a.spec.InvalidRequestError;
import io.a2a.spec.JSONParseError;
import io.a2a.spec.JSONRPCError;
import io.a2a.spec.JSONRPCErrorResponse;
import io.a2a.spec.JSONRPCRequest;
import io.a2a.spec.JSONRPCResponse;
import io.a2a.spec.ListTaskPushNotificationConfigRequest;
import io.a2a.spec.MethodNotFoundError;
import io.a2a.spec.MethodNotFoundJsonMappingException;
import io.a2a.spec.NonStreamingJSONRPCRequest;
import io.a2a.spec.SendMessageRequest;
import io.a2a.spec.SendStreamingMessageRequest;
import io.a2a.spec.SetTaskPushNotificationConfigRequest;
import io.a2a.spec.StreamingJSONRPCRequest;
import io.a2a.spec.TaskResubscriptionRequest;
import io.a2a.spec.UnsupportedOperationError;
import io.a2a.util.Utils;
import io.a2a.transport.jsonrpc.handler.JSONRPCHandler;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.servlet.http.HttpServletRequest;
import org.reactivestreams.FlowAdapters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Flow;

@RestController
public class A2AController {

    private static final Logger log = LoggerFactory.getLogger(A2AController.class);
    private static final String METHOD_NAME_KEY = "methodName";
    private static final String HEADERS_KEY = "headers";

    private final HttpServletRequest servletRequest;
    private final JSONRPCHandler jsonRpcHandler;

    public A2AController(HttpServletRequest servletRequest, JSONRPCHandler jsonRpcHandler) {
        this.servletRequest = servletRequest;
        this.jsonRpcHandler = jsonRpcHandler;
    }

    @GetMapping(value = "/.well-known/agent-card.json", produces = MediaType.APPLICATION_JSON_VALUE)
    public Object agentJson() {
        return jsonRpcHandler.getAgentCard();
    }

    @Hidden
    @PostMapping(
            value = "/a2a/jsonrpc",
            produces = {MediaType.TEXT_EVENT_STREAM_VALUE, MediaType.APPLICATION_JSON_VALUE}
    )
    public Object handleRequest(@RequestBody String body) {
        boolean streaming = false;
        ServerCallContext context = createCallContext();
        JSONRPCResponse<?> nonStreamingResponse = null;
        Flux<? extends JSONRPCResponse<?>> streamingResponse = null;
        JSONRPCErrorResponse error = null;

        try {
            JsonNode node = Utils.OBJECT_MAPPER.readTree(body);
            JsonNode method = node == null ? null : node.get("method");
            streaming = method != null && (
                    SendStreamingMessageRequest.METHOD.equals(method.asText())
                            || TaskResubscriptionRequest.METHOD.equals(method.asText())
            );
            if (method != null && method.isTextual()) {
                context.getState().put(METHOD_NAME_KEY, method.asText());
            }

            if (streaming) {
                StreamingJSONRPCRequest<?> request = Utils.OBJECT_MAPPER.treeToValue(
                        node,
                        StreamingJSONRPCRequest.class
                );
                streamingResponse = processStreamingRequest(request, context);
            } else {
                NonStreamingJSONRPCRequest<?> request = Utils.OBJECT_MAPPER.treeToValue(
                        node,
                        NonStreamingJSONRPCRequest.class
                );
                nonStreamingResponse = processNonStreamingRequest(request, context);
            }
        } catch (JsonProcessingException exception) {
            error = handleError(exception);
        } catch (Throwable throwable) {
            error = new JSONRPCErrorResponse(new InternalError(throwable.getMessage()));
        }

        if (error != null) {
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Utils.toJsonString(error));
        }

        if (streaming) {
            SseEmitter emitter = new SseEmitter(0L);
            Flux<? extends JSONRPCResponse<?>> flux = streamingResponse == null
                    ? Flux.just(new JSONRPCErrorResponse(new InternalError("Streaming response is null")))
                    : streamingResponse;
            flux.subscribe(
                    response -> {
                        try {
                            emitter.send(SseEmitter.event().data(Utils.toJsonString(response)));
                        } catch (Exception exception) {
                            emitter.completeWithError(exception);
                        }
                    },
                    exception -> {
                        log.error("SSE transport failed", exception);
                        emitter.completeWithError(exception);
                    },
                    emitter::complete
            );
            return emitter;
        }

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(Utils.toJsonString(nonStreamingResponse));
    }

    private JSONRPCErrorResponse handleError(JsonProcessingException exception) {
        Object id = null;
        JSONRPCError jsonRpcError;
        if (exception instanceof JsonEOFException) {
            jsonRpcError = new JSONParseError(exception.getMessage());
        } else if (exception.getCause() instanceof JsonParseException) {
            jsonRpcError = new JSONParseError();
        } else if (exception instanceof MethodNotFoundJsonMappingException methodNotFound) {
            id = methodNotFound.getId();
            jsonRpcError = new MethodNotFoundError();
        } else if (exception instanceof InvalidParamsJsonMappingException invalidParams) {
            id = invalidParams.getId();
            jsonRpcError = new InvalidParamsError();
        } else if (exception instanceof IdJsonMappingException idMappingException) {
            id = idMappingException.getId();
            jsonRpcError = new InvalidRequestError();
        } else {
            jsonRpcError = new InvalidRequestError();
        }
        return new JSONRPCErrorResponse(id, jsonRpcError);
    }

    private JSONRPCResponse<?> processNonStreamingRequest(
            NonStreamingJSONRPCRequest<?> request,
            ServerCallContext context
    ) {
        if (request instanceof GetTaskRequest getTaskRequest) {
            return jsonRpcHandler.onGetTask(getTaskRequest, context);
        }
        if (request instanceof CancelTaskRequest cancelTaskRequest) {
            return jsonRpcHandler.onCancelTask(cancelTaskRequest, context);
        }
        if (request instanceof SetTaskPushNotificationConfigRequest setRequest) {
            return jsonRpcHandler.setPushNotificationConfig(setRequest, context);
        }
        if (request instanceof GetTaskPushNotificationConfigRequest getRequest) {
            return jsonRpcHandler.getPushNotificationConfig(getRequest, context);
        }
        if (request instanceof SendMessageRequest sendMessageRequest) {
            return jsonRpcHandler.onMessageSend(sendMessageRequest, context);
        }
        if (request instanceof ListTaskPushNotificationConfigRequest listRequest) {
            return jsonRpcHandler.listPushNotificationConfig(listRequest, context);
        }
        if (request instanceof DeleteTaskPushNotificationConfigRequest deleteRequest) {
            return jsonRpcHandler.deletePushNotificationConfig(deleteRequest, context);
        }
        if (request instanceof GetAuthenticatedExtendedCardRequest authenticatedCardRequest) {
            return jsonRpcHandler.onGetAuthenticatedExtendedCardRequest(authenticatedCardRequest, context);
        }
        return generateErrorResponse(request, new UnsupportedOperationError());
    }

    private Flux<? extends JSONRPCResponse<?>> processStreamingRequest(
            JSONRPCRequest<?> request,
            ServerCallContext context
    ) {
        Flow.Publisher<? extends JSONRPCResponse<?>> publisher;
        if (request instanceof SendStreamingMessageRequest sendStreamingMessageRequest) {
            publisher = jsonRpcHandler.onMessageSendStream(sendStreamingMessageRequest, context);
        } else if (request instanceof TaskResubscriptionRequest taskResubscriptionRequest) {
            publisher = jsonRpcHandler.onResubscribeToTask(taskResubscriptionRequest, context);
        } else {
            return Flux.just(generateErrorResponse(request, new UnsupportedOperationError()));
        }
        return Flux.from(FlowAdapters.toPublisher(publisher));
    }

    private JSONRPCResponse<?> generateErrorResponse(JSONRPCRequest<?> request, JSONRPCError error) {
        return new JSONRPCErrorResponse(request.getId(), error);
    }

    private ServerCallContext createCallContext() {
        Map<String, Object> state = new HashMap<>();
        Map<String, String> requestHeaders = new HashMap<>();

        Enumeration<String> headerNames = servletRequest.getHeaderNames();
        while (headerNames != null && headerNames.hasMoreElements()) {
            String name = headerNames.nextElement();
            requestHeaders.put(name, servletRequest.getHeader(name));
        }
        state.put(HEADERS_KEY, requestHeaders);

        List<String> extensionHeaderValues = new ArrayList<>();
        Enumeration<String> extensionHeaders = servletRequest.getHeaders(A2AHeaders.X_A2A_EXTENSIONS);
        while (extensionHeaders != null && extensionHeaders.hasMoreElements()) {
            extensionHeaderValues.add(extensionHeaders.nextElement());
        }

        return new ServerCallContext(
                UnauthenticatedUser.INSTANCE,
                state,
                A2AExtensions.getRequestedExtensions(extensionHeaderValues)
        );
    }
}
