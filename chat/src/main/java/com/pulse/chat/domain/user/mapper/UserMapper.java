package com.pulse.chat.domain.user.mapper;

import com.pulse.chat.domain.user.dto.UserSummary;
import com.pulse.chat.domain.user.entity.UserEntity;

public final class UserMapper {
    private UserMapper() {}

    public static UserSummary toSummary(UserEntity entity) {
        return new UserSummary(entity.getId(), entity.getUsername());
    }
}
