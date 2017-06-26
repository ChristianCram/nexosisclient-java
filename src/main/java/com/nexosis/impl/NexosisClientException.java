package com.nexosis.impl;

import com.nexosis.model.ErrorResponse;
import org.apache.http.StatusLine;

public class NexosisClientException extends Exception {
    private int statusCode;
    private ErrorResponse errorResponse;

    public NexosisClientException(String message) {
        super(message);
    }

    public NexosisClientException(String message, Exception inner) {
        super(message, inner);
    }


    public NexosisClientException(String message, ErrorResponse response)
    {
        super(message);

        if (response != null) {
            this.statusCode = response.getStatusCode();
        }
        this.errorResponse = response;
    }

    public NexosisClientException(String message, StatusLine status)
    {
        super(message);
        this.statusCode = status.getStatusCode();
    }

    public int getStatusCode() {
        return this.statusCode;
    }
    public void setStatusCode(int statusCode) {

    }
    public ErrorResponse getErrorResponse() {
        return this.errorResponse;
    }

    public void setErrorResponse(ErrorResponse errorResponse) {
        this.errorResponse = errorResponse;
    }
}
