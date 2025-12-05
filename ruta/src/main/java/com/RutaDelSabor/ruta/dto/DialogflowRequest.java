package com.RutaDelSabor.ruta.dto;

import java.util.Map;

public class DialogflowRequest {
    private FulfillmentInfo fulfillmentInfo;
    private SessionInfo sessionInfo;

    public FulfillmentInfo getFulfillmentInfo() { return fulfillmentInfo; }
    public void setFulfillmentInfo(FulfillmentInfo fulfillmentInfo) { this.fulfillmentInfo = fulfillmentInfo; }

    public SessionInfo getSessionInfo() { return sessionInfo; }
    public void setSessionInfo(SessionInfo sessionInfo) { this.sessionInfo = sessionInfo; }

    // --- CLASES INTERNAS ---
    public static class FulfillmentInfo {
        private String tag;
        public String getTag() { return tag; }
        public void setTag(String tag) { this.tag = tag; }
    }

    public static class SessionInfo {
        private Map<String, Object> parameters;
        public Map<String, Object> getParameters() { return parameters; }
        public void setParameters(Map<String, Object> parameters) { this.parameters = parameters; }
    }
}