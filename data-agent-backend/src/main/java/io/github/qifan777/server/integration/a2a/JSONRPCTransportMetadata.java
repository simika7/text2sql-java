package io.github.qifan777.server.integration.a2a;

import io.a2a.server.TransportMetadata;
import io.a2a.spec.TransportProtocol;

public class JSONRPCTransportMetadata implements TransportMetadata {

    @Override
    public String getTransportProtocol() {
        return TransportProtocol.JSONRPC.toString();
    }
}
