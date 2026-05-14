package com.pulse.chat.app.service;

import com.pulse.chat.app.dto.response.Meta;
import com.pulse.chat.app.dto.response.ResponseDto;
import com.pulse.chat.domain.common.constant.ResponseCode;
import org.springframework.stereotype.Service;

@Service
public class ResponseFactory {

    private final String appName = "pulse-chat";

    public ResponseDto success(ResponseCode responseCode) {
        var meta = Meta.builder()
                .serviceId(appName)
                .status(responseCode.getCode())
                .message(responseCode.getDefaultMessage())
                .build();
        return new ResponseDto(meta, null);
    }

    public ResponseDto success(Object data) {
        var meta = Meta.builder()
                .serviceId(appName)
                .status(ResponseCode.SUCCESS.getCode())
                .build();
        return new ResponseDto(meta, data);
    }
}
