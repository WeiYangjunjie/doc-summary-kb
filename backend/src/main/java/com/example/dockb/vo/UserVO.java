package com.example.dockb.vo;

import lombok.Data;

/**
 * 用户信息 VO（不含密码）。
 */
@Data
public class UserVO {

    private Long id;
    private String username;
    private String role;
    private String createdAt;
}
