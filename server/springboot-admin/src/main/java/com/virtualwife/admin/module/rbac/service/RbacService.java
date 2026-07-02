package com.virtualwife.admin.module.rbac.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.virtualwife.admin.module.rbac.entity.Permission;
import com.virtualwife.admin.module.rbac.entity.Role;
import com.virtualwife.admin.module.rbac.entity.RolePermission;
import com.virtualwife.admin.module.rbac.entity.UserRole;
import com.virtualwife.admin.module.rbac.mapper.PermissionMapper;
import com.virtualwife.admin.module.rbac.mapper.RoleMapper;
import com.virtualwife.admin.module.rbac.mapper.RolePermissionMapper;
import com.virtualwife.admin.module.rbac.mapper.UserRoleMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * RBAC权限管理服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RbacService {

    private final RoleMapper roleMapper;
    private final PermissionMapper permissionMapper;
    private final UserRoleMapper userRoleMapper;
    private final RolePermissionMapper rolePermissionMapper;

    // ==================== 角色管理 ====================

    /**
     * 获取所有角色
     */
    public List<Role> getAllRoles() {
        return roleMapper.selectList(
                new LambdaQueryWrapper<Role>().orderByAsc(Role::getId)
        );
    }

    /**
     * 根据ID获取角色
     */
    public Role getRoleById(Long id) {
        return roleMapper.selectById(id);
    }

    /**
     * 根据角色编码获取角色
     */
    public Role getRoleByCode(String roleCode) {
        return roleMapper.selectOne(
                new LambdaQueryWrapper<Role>().eq(Role::getRoleCode, roleCode)
        );
    }

    /**
     * 创建角色
     */
    public Role createRole(Role role) {
        role.setCreateTime(java.time.LocalDateTime.now());
        role.setUpdateTime(java.time.LocalDateTime.now());
        roleMapper.insert(role);
        return role;
    }

    /**
     * 更新角色
     */
    public Role updateRole(Role role) {
        role.setUpdateTime(java.time.LocalDateTime.now());
        roleMapper.updateById(role);
        return role;
    }

    /**
     * 删除角色
     */
    @Transactional
    public void deleteRole(Long roleId) {
        // 删除角色权限关联
        rolePermissionMapper.delete(
                new LambdaQueryWrapper<RolePermission>().eq(RolePermission::getRoleId, roleId)
        );
        // 删除用户角色关联
        userRoleMapper.delete(
                new LambdaQueryWrapper<UserRole>().eq(UserRole::getRoleId, roleId)
        );
        // 删除角色
        roleMapper.deleteById(roleId);
    }

    // ==================== 权限管理 ====================

    /**
     * 获取所有权限
     */
    public List<Permission> getAllPermissions() {
        return permissionMapper.selectList(
                new LambdaQueryWrapper<Permission>().orderByAsc(Permission::getSortOrder)
        );
    }

    /**
     * 根据ID获取权限
     */
    public Permission getPermissionById(Long id) {
        return permissionMapper.selectById(id);
    }

    /**
     * 创建权限
     */
    public Permission createPermission(Permission permission) {
        permission.setCreateTime(java.time.LocalDateTime.now());
        permission.setUpdateTime(java.time.LocalDateTime.now());
        permissionMapper.insert(permission);
        return permission;
    }

    /**
     * 更新权限
     */
    public Permission updatePermission(Permission permission) {
        permission.setUpdateTime(java.time.LocalDateTime.now());
        permissionMapper.updateById(permission);
        return permission;
    }

    /**
     * 删除权限
     */
    @Transactional
    public void deletePermission(Long permissionId) {
        // 删除角色权限关联
        rolePermissionMapper.delete(
                new LambdaQueryWrapper<RolePermission>().eq(RolePermission::getPermissionId, permissionId)
        );
        // 删除权限
        permissionMapper.deleteById(permissionId);
    }

    // ==================== 用户角色管理 ====================

    /**
     * 获取用户的角色列表
     */
    public List<Role> getUserRoles(Long userId) {
        List<UserRole> userRoles = userRoleMapper.selectList(
                new LambdaQueryWrapper<UserRole>().eq(UserRole::getUserId, userId)
        );
        if (userRoles.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> roleIds = userRoles.stream()
                .map(UserRole::getRoleId)
                .collect(Collectors.toList());
        return roleMapper.selectBatchIds(roleIds);
    }

    /**
     * 设置用户角色
     */
    @Transactional
    public void setUserRoles(Long userId, List<Long> roleIds) {
        // 删除原有角色关联
        userRoleMapper.delete(
                new LambdaQueryWrapper<UserRole>().eq(UserRole::getUserId, userId)
        );
        // 添加新的角色关联
        for (Long roleId : roleIds) {
            UserRole userRole = new UserRole();
            userRole.setUserId(userId);
            userRole.setRoleId(roleId);
            userRole.setCreateTime(java.time.LocalDateTime.now());
            userRoleMapper.insert(userRole);
        }
    }

    /**
     * 获取用户的角色编码列表
     */
    public List<String> getUserRoleCodes(Long userId) {
        return getUserRoles(userId).stream()
                .map(Role::getRoleCode)
                .collect(Collectors.toList());
    }

    /**
     * 检查用户是否是管理员
     */
    public boolean isAdmin(Long userId) {
        List<String> roleCodes = getUserRoleCodes(userId);
        return roleCodes.contains("SUPER_ADMIN") || roleCodes.contains("ADMIN");
    }

    /**
     * 检查用户是否是超级管理员
     */
    public boolean isSuperAdmin(Long userId) {
        List<String> roleCodes = getUserRoleCodes(userId);
        return roleCodes.contains("SUPER_ADMIN");
    }

    // ==================== 角色权限管理 ====================

    /**
     * 获取角色的权限列表
     */
    public List<Permission> getRolePermissions(Long roleId) {
        List<RolePermission> rolePermissions = rolePermissionMapper.selectList(
                new LambdaQueryWrapper<RolePermission>().eq(RolePermission::getRoleId, roleId)
        );
        if (rolePermissions.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> permissionIds = rolePermissions.stream()
                .map(RolePermission::getPermissionId)
                .collect(Collectors.toList());
        return permissionMapper.selectBatchIds(permissionIds);
    }

    /**
     * 设置角色权限
     */
    @Transactional
    public void setRolePermissions(Long roleId, List<Long> permissionIds) {
        // 删除原有权限关联
        rolePermissionMapper.delete(
                new LambdaQueryWrapper<RolePermission>().eq(RolePermission::getRoleId, roleId)
        );
        // 添加新的权限关联
        for (Long permissionId : permissionIds) {
            RolePermission rolePermission = new RolePermission();
            rolePermission.setRoleId(roleId);
            rolePermission.setPermissionId(permissionId);
            rolePermission.setCreateTime(java.time.LocalDateTime.now());
            rolePermissionMapper.insert(rolePermission);
        }
    }

    /**
     * 获取用户的权限编码列表
     */
    public List<String> getUserPermissionCodes(Long userId) {
        List<Role> roles = getUserRoles(userId);
        Set<String> permissionCodes = new LinkedHashSet<>();
        for (Role role : roles) {
            List<Permission> permissions = getRolePermissions(role.getId());
            for (Permission permission : permissions) {
                permissionCodes.add(permission.getPermissionCode());
            }
        }
        return new ArrayList<>(permissionCodes);
    }

    /**
     * 获取用户的菜单权限列表
     */
    public List<Permission> getUserMenuPermissions(Long userId) {
        List<Role> roles = getUserRoles(userId);
        Set<Long> permissionIds = new LinkedHashSet<>();
        for (Role role : roles) {
            List<RolePermission> rolePermissions = rolePermissionMapper.selectList(
                    new LambdaQueryWrapper<RolePermission>().eq(RolePermission::getRoleId, role.getId())
            );
            for (RolePermission rp : rolePermissions) {
                permissionIds.add(rp.getPermissionId());
            }
        }
        if (permissionIds.isEmpty()) {
            return Collections.emptyList();
        }
        return permissionMapper.selectBatchIds(permissionIds).stream()
                .filter(p -> "menu".equals(p.getResourceType()))
                .sorted(Comparator.comparing(Permission::getSortOrder))
                .collect(Collectors.toList());
    }

    /**
     * 检查用户是否有指定权限
     */
    public boolean hasPermission(Long userId, String permissionCode) {
        List<String> permissionCodes = getUserPermissionCodes(userId);
        return permissionCodes.contains(permissionCode);
    }
}
