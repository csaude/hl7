package mz.org.fgh.hl7.web;

import lombok.Getter;
import lombok.ToString;

@ToString
public class Alert {
    enum Type {
        SUCCESS("alert-success"), DANGER("alert-danger");

        @Getter
        String cssClass;

        Type(String cssClass) {
            this.cssClass = cssClass;
        }
    }

    @Getter
    private final Type type;
    @Getter
    private final String message;

    private Alert(Type type, String message) {
        this.type = type;
        this.message = message;
    }

    public static Alert success(String message) {
        return new Alert(Type.SUCCESS, message);
    }

    public static Alert danger(String message) {
        return new Alert(Type.DANGER, message);
    }
}
