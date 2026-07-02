<template>
  <div class="rag-chunks">
    <el-page-header @back="$router.push('/rag')" title="返回">
      <template #content>文档块列表 - DocID: {{ docId }}</template>
    </el-page-header>

    <el-card style="margin-top:16px">
      <el-table :data="tableData" v-loading="loading" stripe>
        <el-table-column prop="chunkIndex" label="序号" width="70" />
        <el-table-column prop="content" label="内容" min-width="400" show-overflow-tooltip>
          <template #default="{ row }">
            <div>{{ row.content?.substring(0, 200) }}{{ row.content?.length > 200 ? '...' : '' }}</div>
          </template>
        </el-table-column>
        <el-table-column prop="enhancedContent" label="增强内容" min-width="300" show-overflow-tooltip>
          <template #default="{ row }">
            <div>{{ row.enhancedContent?.substring(0, 150) || '—' }}</div>
          </template>
        </el-table-column>
        <el-table-column prop="summary" label="摘要" min-width="200" show-overflow-tooltip />
        <el-table-column prop="tokenCount" label="Token" width="80" />
        <el-table-column prop="sectionPath" label="章节" width="150" show-overflow-tooltip />
        <el-table-column prop="vectorStatus" label="向量化" width="80">
          <template #default="{ row }">
            <el-tag :type="row.vectorStatus === 1 ? 'success' : 'warning'" size="small">
              {{ row.vectorStatus === 1 ? '是' : '否' }}
            </el-tag>
          </template>
        </el-table-column>
      </el-table>

      <div style="margin-top:16px;display:flex;justify-content:flex-end">
        <el-pagination v-model:current-page="pageNum" v-model:page-size="pageSize" :total="total"
          layout="total, prev, pager, next" @change="fetchData" />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import request from '@/utils/request'

const route = useRoute()
const docId = ref(route.params.docId)
const tableData = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)
const loading = ref(false)

const fetchData = async () => {
  loading.value = true
  try {
    const res = await request.get(`/rag/${docId.value}/chunk/page`, {
      params: { pageNum: pageNum.value, pageSize: pageSize.value }
    })
    tableData.value = res.data.data.records
    total.value = res.data.data.total
  } finally { loading.value = false }
}

onMounted(fetchData)
</script>
