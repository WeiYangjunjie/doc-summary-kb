package com.example.dockb.config;

import java.lang.annotation.*;

/**
 * 标注在 Controller 方法 Long/String 参数上，自动注入当前登录用户信息。
 *
 * <p>Long 类型 → userId；
 * <p>String 类型 → role；
 * <p>加 {@link com.example.dockb.util.AuthContext.Username} → username。
 *
 * <p>示例：
 * <pre>
 * &#64;PostMapping("/upload")
 * public Result&lt;...&gt; upload(&#64;CurrentUser Long userId) { ... }
 *
 * // 注入用户名：
 * public Result&lt;...&gt; foo(&#64;CurrentUser &#64;AuthContext.Username String username) { ... }
 * </pre>
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface CurrentUser {
}
