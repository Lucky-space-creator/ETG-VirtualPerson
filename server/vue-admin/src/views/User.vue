<template>
  <div class="user-page">
    <el-card>
      <div class="page-header">
        <span>用户管理</span>
      </div>
    </el-card>

    <el-card style="margin-top:16px">
      <el-form :inline="true" :model="query" size="default">
        <el-form-item>
          <el-input v-model="query.keyword" placeholder="搜索用户名/昵称" clearable />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="fetchData">查询</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card style="margin-top:16px">
      <el-table :data="tableData" v-loading="loading" stripe>
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column prop="username" label="用户名" width="120" />
        <el-table-column prop="nickname" label="昵称" width="120" />
        <el-table-column prop="phone" label="手机号" width="130" />
        <el-table-column prop="role" label="角色" width="80">
          <template #default="{ row }">
            <el-tag :type="row.role === 'ADMIN' ? 'danger' : 'info'" size="small">{{ row.role }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
              {{ row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="lastLoginTime" label="最后登录" width="170" />
        <el-table-column prop="createTime" label="创建时间" width="170" />
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button size="small" :type="row.status===1?'warning':'success'" @click="handleToggleStatus(row.id)">
              {{ row.status === 1 ? '禁用' : '启用' }}
            </el-button>
            <el-button size="small" type="danger" @click="handleDelete(row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div style="margin-top:16px;display:flex;justify-content:flex-end">
        <el-pagination v-model:current-page="pageNum" v-model:page-size="pageSize" :total="total"
          layout="total, sizes, prev, pager, next" @change="fetchData" />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import request from '@/utils/request'
import { ElMessage, ElMessageBox } from 'element-plus'

const query = ref({ keyword: '' })
const tableData = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)
const loading = ref(false)

const fetchData = async () => {
  loading.value = true
  try {
    const res = await request.get('/user/page', {
      params: { pageNum: pageNum.value, pageSize: pageSize.value, keyword: query.value.keyword }
    })
    tableData.value = res.data.data.records
    total.value = res.data.data.total
  } finally { loading.value = false }
}

const handleToggleStatus = async (id) => {
  await request.put(`/user/${id}/status`)
  ElMessage.success('操作成功')
  fetchData()
}

const handleDelete = async (id) => {
  await ElMessageBox.confirm('确定删除？', '提示', { type: 'warning' })
  await request.delete(`/user/${id}`)
  ElMessage.success('删除成功')
  fetchData()
}

onMounted(fetchData)
</script>

<style scoped>
.page-header { display: flex; align-items: center; justify-content: space-between; }
</style>
