<template>
  <div class="avatar-page">
    <el-card>
      <div class="page-header">
        <span>数字人形象管理</span>
        <el-button type="primary" @click="$router.push('/avatar/edit')">新增形象</el-button>
      </div>
    </el-card>

    <el-card style="margin-top:16px">
      <el-row :gutter="20">
        <el-col v-for="avatar in tableData" :key="avatar.id" :span="8" style="margin-bottom:20px">
          <el-card shadow="hover">
            <div class="avatar-card">
              <img v-if="avatar.thumbnailDisplayUrl" :src="avatar.thumbnailDisplayUrl" class="avatar-thumb" />
              <div v-else class="avatar-placeholder"><el-icon :size="48"><UserFilled /></el-icon></div>
              <div class="avatar-info">
                <h4>
                  {{ avatar.avatarName }}
                  <el-tag v-if="avatar.isDefault" type="success" size="small">默认</el-tag>
                  <el-tag v-if="avatar.isSystem" type="warning" size="small">系统</el-tag>
                </h4>
                <p>{{ avatar.voiceType || '未设置语音' }}</p>
                <p class="sort">排序: {{ avatar.sortOrder }}</p>
                <div class="avatar-actions">
                  <el-button size="small" @click="$router.push(`/avatar/edit/${avatar.id}`)">编辑</el-button>
                  <el-button v-if="!avatar.isDefault" size="small" type="success" plain @click="handleSetDefault(avatar.id)">设为默认</el-button>
                  <el-button v-if="!avatar.isSystem" size="small" type="danger" @click="handleDelete(avatar.id)">删除</el-button>
                </div>
              </div>
            </div>
          </el-card>
        </el-col>
      </el-row>

      <div style="display:flex;justify-content:flex-end">
        <el-pagination v-model:current-page="pageNum" v-model:page-size="pageSize" :total="total"
          layout="total, prev, pager, next" @change="fetchData" />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import request from '@/utils/request'
import { ElMessage, ElMessageBox } from 'element-plus'

const tableData = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(9)

const fetchData = async () => {
  const res = await request.get('/avatar/page', { params: { pageNum: pageNum.value, pageSize: pageSize.value } })
  tableData.value = res.data.data.records
  total.value = res.data.data.total
}

const handleDelete = async (id) => {
  await ElMessageBox.confirm('确定删除？', '提示', { type: 'warning' })
  await request.delete(`/avatar/${id}`)
  ElMessage.success('删除成功')
  fetchData()
}

const handleSetDefault = async (id) => {
  await request.put(`/avatar/${id}/default`)
  ElMessage.success('已设为默认形象')
  fetchData()
}

onMounted(fetchData)
</script>

<style scoped>
.page-header { display: flex; align-items: center; justify-content: space-between; }
.avatar-card { display: flex; gap: 16px; }
.avatar-thumb, .avatar-placeholder { width: 80px; height: 80px; border-radius: 8px; background: #f0f2f5; display: flex; align-items: center; justify-content: center; object-fit: cover; }
.avatar-info h4 { margin: 0 0 4px; font-size: 16px; }
.avatar-info p { margin: 0 0 4px; font-size: 13px; color: #909399; }
.avatar-info .sort { color: #c0c4cc; font-size: 12px; }
.avatar-actions { margin-top: 8px; display: flex; gap: 8px; }
</style>
