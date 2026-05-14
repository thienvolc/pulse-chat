package com.pulse.chat.app.aop;

import com.pulse.chat.domain.common.constant.ResponseCode;
import jakarta.annotation.Nullable;
import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {

    private final ResponseCode responseCode;
    private final Object[] args;

    public BusinessException(ResponseCode responseCode, @Nullable Object... args) {
        super(getFormattedMessage(responseCode, args));
        this.responseCode = responseCode;
        this.args = args;
    }

    private static String getFormattedMessage(ResponseCode responseCode,
                                              @Nullable Object... args) {

        if (hasArguments(args)) {
            return String.format(responseCode.getDefaultMessage(), args);
        }
        return responseCode.getDefaultMessage();
    }

    private static boolean hasArguments(Object[] args) {
        return args != null && args.length > 0;
    }
}
