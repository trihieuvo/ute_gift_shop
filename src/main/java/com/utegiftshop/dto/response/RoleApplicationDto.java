package com.utegiftshop.dto.response;

import com.utegiftshop.entity.RoleApplication;
import com.utegiftshop.entity.User;
import lombok.Getter;
import lombok.Setter;
import java.sql.Timestamp;

@Getter
@Setter
public class RoleApplicationDto {
    private Long id;
    private Long userId;
    private String userFullName;
    private String userEmail;
    private String requestedRole;
    private String status;
    private String message;
    private Timestamp createdAt;

    public RoleApplicationDto(RoleApplication app) {
        this.id = app.getId();
        this.requestedRole = app.getRequestedRole();
        this.status = app.getStatus();
        this.message = app.getMessage();
        this.createdAt = app.getCreatedAt();
        
        if (app.getUser() != null) {
            User user = app.getUser();
            this.userId = user.getId();
            this.userFullName = user.getFullName();
            this.userEmail = user.getEmail();
        }
    }
}