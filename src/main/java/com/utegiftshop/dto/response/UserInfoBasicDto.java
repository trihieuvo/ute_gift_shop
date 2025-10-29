package com.utegiftshop.dto.response;

import com.utegiftshop.entity.User;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter @NoArgsConstructor
public class UserInfoBasicDto {
    private Long id;
    private String fullName;
    private String avatarUrl;

    public UserInfoBasicDto(User user) {
        if (user != null) {
            this.id = user.getId();
            this.fullName = user.getFullName();
            this.avatarUrl = user.getAvatarUrl();
        }
    }
}