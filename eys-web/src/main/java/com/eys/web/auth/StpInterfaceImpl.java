package com.eys.web.auth;

import cn.dev33.satoken.stp.StpInterface;
import com.eys.mapper.SysUserMapper;
import com.eys.model.entity.SysUser;
import com.eys.model.enums.RoleType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Sa-Token 权限数据加载接口实现
 * 
 * 角色设计：
 * - ADMIN: 管理员，可以访问管理端，也可以作为DM和玩家
 * - DM:    主持人，可以创建房间、管理游戏
 * - PLAYER: 普通玩家
 */
@Component
@RequiredArgsConstructor
public class StpInterfaceImpl implements StpInterface {

    private final SysUserMapper sysUserMapper;

    /**
     * 获取用户权限列表（本项目暂不使用细粒度权限）
     */
    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        return new ArrayList<>();
    }

    /**
     * 获取用户角色列表
     * 
     * 角色层级设计（高级角色包含低级角色的权限）：
     * - ADMIN 返回 ["ADMIN", "DM", "PLAYER"]
     * - DM 返回 ["DM", "PLAYER"]
     * - PLAYER 返回 ["PLAYER"]
     */
    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        List<String> roles = new ArrayList<>();
        
        Long userId = Long.parseLong(loginId.toString());
        SysUser user = sysUserMapper.selectById(userId);
        
        if (user == null || user.getRoleType() == null) {
            return roles;
        }

        RoleType roleType = user.getRoleType();
        
        // 角色层级：高级角色自动拥有低级角色的权限
        switch (roleType) {
            case ADMIN:
                roles.add("ADMIN");
                roles.add("DM");
                roles.add("PLAYER");
                break;
            case DM:
                roles.add("DM");
                roles.add("PLAYER");
                break;
            case PLAYER:
                roles.add("PLAYER");
                break;
        }
        
        return roles;
    }
}
