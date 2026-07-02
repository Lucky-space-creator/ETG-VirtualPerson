package com.virtualwife.admin.module.rbac.controller;

import com.virtualwife.admin.common.result.Result;
import com.virtualwife.admin.module.rbac.entity.Permission;
import com.virtualwife.admin.module.rbac.entity.Role;
import com.virtualwife.admin.module.rbac.service.RbacService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * RBAC权限管理Controller
 */
@Slf4j
@RestController
@RequestMapping("/rbac")
@RequiredArgsConstructor
public class RbacController {

    private final RbacService rbacService;

    // ==================== 角色管理 ====================

    @GetMapping("/roles")
    public Result<List<Role>> getRoles() {
        return Result.success(rbacService.getAllRoles());
    }

    @GetMapping("/roles/{id}")
    public Result<Role> getRole(@PathVariable Long id) {
        Role role = rbacService.getRoleById(id);
        if (role == null) {
            return Result.error("角色不存在");
        }
        return Result.success(role);
    }

    @PostMapping("/roles")
    public Result<Role> createRole(@RequestBody Role role) {
        return Result.success("创建成功", rbacService.createRole(role));
    }

    @PutMapping("/roles/{id}")
    public Result<Role> updateRole(@PathVariable Long id, @RequestBody Role role) {
        role.setId(id);
        return Result.success("更新成功", rbacService.updateRole(role));
    }

    @DeleteMapping("/roles/{id}")
    public Result<?> deleteRole(@PathVariable Long id) {
        rbacService.deleteRole(id);
        return Result.success("删除成功");
    }

    // ==================== 权限管理 ====================

    @GetMapping("/permissions")
    public Result<List<Permission>> getPermissions() {
        return Result.success(rbacService.getAllPermissions());
    }

    @GetMapping("/permissions/{id}")
    public Result<Permission> getPermission(@PathVariable Long id) {
        Permission permission = rbacService.getPermissionById(id);
        if (permission == null) {
            return Result.error("权限不存在");
        }
        return Result.success(permission);
    }

    @PostMapping("/permissions")
    public Result<Permission> createPermission(@RequestBody Permission permission) {
        return Result.success("创建成功", rbacService.createPermission(permission));
    }

    @PutMapping("/permissions/{id}")
    public Result<Permission> updatePermission(@PathVariable Long id, @RequestBody Permission permission) {
        permission.setId(id);
        return Result.success("更新成功", rbacService.updatePermission(permission));
    }

    @DeleteMapping("/permissions/{id}")
    public Result<?> deletePermission(@PathVariable Long id) {
        rbacService.deletePermission(id);
        return Result.success("删除成功");
    }

    // ==================== 用户角色管理 ====================

    @GetMapping("/users/{userId}/roles")
    public Result<List<Role>> getUserRoles(@PathVariable Long userId) {
        return Result.success(rbacService.getUserRoles(userId));
    }

    @PostMapping("/users/{userId}/roles")
    public Result<?> setUserRoles(@PathVariable Long userId, @RequestBody Map<String, List<Long>> request) {
        List<Long> roleIds = request.get("roleIds");
        rbacService.setUserRoles(userId, roleIds);
        return Result.success("设置成功");
    }

    @GetMapping("/users/{userId}/permissions")
    public Result<List<String>> getUserPermissions(@PathVariable Long userId) {
        return Result.success(rbacService.getUserPermissionCodes(userId));
    }

    // ==================== 角色权限管理 ====================

    @GetMapping("/roles/{roleId}/permissions")
    public Result<List<Permission>> getRolePermissions(@PathVariable Long roleId) {
        return Result.success(rbacService.getRolePermissions(roleId));
    }

    @PostMapping("/roles/{roleId}/permissions")
    public Result<?> setRolePermissions(@PathVariable Long roleId, @RequestBody Map<String, List<Long>> request) {
        List<Long> permissionIds = request.get("permissionIds");
        rbacService.setRolePermissions(roleId, permissionIds);
        return Result.success("设置成功");
    }
}
