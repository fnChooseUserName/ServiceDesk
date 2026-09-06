package com.servicedesk.ticketing.ticket;

public class InvalidTicketRequestException extends RuntimeException {

    public InvalidTicketRequestException(String message) {
        super(message);
    }
}
