<template>
  <div class="role-page">
    <el-card>
      <div class="page-header">
        <span class="page-title">角色管理</span>
        <el-button type="primary" @click="openRoleDialog(null)">
          <el-icon><Plus /></el-icon>新增角色
        </el-button>
      </div>
    </el-card>

    <el-card style="margin-top:16px">
      <el-table :data="roles" stripe v-loading="loading">
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column prop="roleName" label="角色名称" width="150" />
        <el-table-column prop="roleCode" label="角色编码" width="150">
          <template #default="{ row }">
            <el-tag>{{ row.roleCode }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="description" label="描述" />
        <el-table-column prop="status" label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'">
              {{ row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="280" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="openRoleDialog(row)">编辑</el-button>
            <el-button size="small" type="primary" @click="openPermissionDialog(row)">权限</el-button>
            <el-button size="small" type="danger" @click="deleteRole(row)" :disabled="row.roleCode === 'SUPER_ADMIN'">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 角色编辑弹窗 -->
    <el-dialog v-model="roleDialogVisible" :title="roleForm.id ? '编辑角色' : '新增角色'" width="500px">
      <el-form :model="roleForm" label-width="80px">
        <el-form-item label="角色名称" required>
          <el-input v-model="roleForm.roleName" placeholder="如：管理员" />
        </el-form-item>
        <el-form-item label="角色编码" required>
          <el-input v-model="roleForm.roleCode" placeholder="如：ADMIN" :disabled="!!roleForm.id" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="roleForm.description" type="textarea" :rows="3" placeholder="角色描述" />
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="roleForm.status" :active-value="1" :inactive-value="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="roleDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveRole" :loading="saving">保存</el-button>
      </template>
    </el-dialog>

    <!-- 权限设置弹窗 -->
    <el-dialog v-model="permissionDialogVisible" :title="`设置权限 - ${currentRole?.roleName}`" width="600px">
      <el-checkbox-group v-model="selectedPermissions">
        <div v-for="perm in allPermissions" :key="perm.id" style="margin-bottom:8px">
          <el-checkbox :value="perm.id">
            {{ perm.permissionName }}
            <span style="color:#909399;font-size:12px;margin-left:8px">{{ perm.permissionCode }}</span>
          </el-checkbox>
        </div>
      </el-checkbox-group>
      <template #footer>
        <el-button @click="permissionDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="savePermissions" :loading="saving">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import request from '@/utils/request'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'

const roles = ref([])
const allPermissions = ref([])
const loading = ref(false)
const saving = ref(false)

const roleDialogVisible = ref(false)
const roleForm = ref({ id: null, roleName: '', roleCode: '', description: '', status: 1 })

const permissionDialogVisible = ref(false)
const currentRole = ref(null)
const selectedPermissions = ref([])

const loadRoles = async () => {
  loading.value = true
  try {
    const res = await request.get('/rbac/roles')
    roles.value = res.data.data || []
  } catch (e) {
    ElMessage.error('加载角色失败')
  } finally {
    loading.value = false
  }
}

const loadPermissions = async () => {
  try {
    const res = await request.get('/rbac/permissions')
    allPermissions.value = res.data.data || []
  } catch (e) {
    ElMessage.error('加载权限失败')
  }
}

const openRoleDialog = (role) => {
  if (role) {
    roleForm.value = { ...role }
  } else {
    roleForm.value = { id: null, roleName: '', roleCode: '', description: '', status: 1 }
  }
  roleDialogVisible.value = true
}

const saveRole = async () => {
  if (!roleForm.value.roleName || !roleForm.value.roleCode) {
    ElMessage.warning('请填写角色名称和编码')
    return
  }

  saving.value = true
  try {
    if (roleForm.value.id) {
      await request.put(`/rbac/roles/${roleForm.value.id}`, roleForm.value)
    } else {
      await request.post('/rbac/roles', roleForm.value)
    }
    ElMessage.success('保存成功')
    roleDialogVisible.value = false
    loadRoles()
  } catch (e) {
    ElMessage.error('保存失败')
  } finally {
    saving.value = false
  }
}

const deleteRole = async (role) => {
  try {
    await ElMessageBox.confirm(`确定删除角色 "${role.roleName}"？`, '警告', { type: 'warning' })
    await request.delete(`/rbac/roles/${role.id}`)
    ElMessage.success('删除成功')
    loadRoles()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error('删除失败')
  }
}

const openPermissionDialog = async (role) => {
  currentRole.value = role
  try {
    const res = await request.get(`/rbac/roles/${role.id}/permissions`)
    selectedPermissions.value = (res.data.data || []).map(p => p.id)
  } catch (e) {
    selectedPermissions.value = []
  }
  permissionDialogVisible.value = true
}

const savePermissions = async () => {
  saving.value = true
  try {
    await request.post(`/rbac/roles/${currentRole.value.id}/permissions`, {
      permissionIds: selectedPermissions.value
    })
    ElMessage.success('权限设置成功')
    permissionDialogVisible.value = false
  } catch (e) {
    ElMessage.error('权限设置失败')
  } finally {
    saving.value = false
  }
}

onMounted(() => {
  loadRoles()
  loadPermissions()
})
</script>

<style scoped>
.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.page-title {
  font-size: 18px;
  font-weight: 600;
  color: var(--text-primary);
}
</style>
