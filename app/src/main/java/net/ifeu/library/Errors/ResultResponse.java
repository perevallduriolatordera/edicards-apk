package net.ifeu.library.Errors;

public class ResultResponse {

    public ResultResponse(boolean success, String message) {
        Success = success;
        Message = message;
    }

    public boolean Success;
    public String Message;
}
