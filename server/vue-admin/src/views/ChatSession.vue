<template>
  <div class="chat-session">
    <div class="sticky-header">
      <el-page-header @back="$router.push('/chat')" title="返回">
        <template #content>
          <span>会话回放：{{ sessionId }}</span>
        </template>
      </el-page-header>
    </div>

    <div class="messages" style="margin-top:20px">
      <div v-for="msg in messages" :key="msg.id" :class="['message', msg.messageType === 'user' ? 'user' : 'ai']">
        <div class="message-bubble">
          <div class="message-meta">
            <el-tag :type="msg.messageType === 'text' ? 'success' : 'warning'" size="small">
              {{ msg.messageType === 'text' ? '游客' : msg.avatarName || 'AI' }}
            </el-tag>
            <span class="time">{{ msg.createTime }}</span>
          </div>
          <div class="message-content">{{ msg.content }}</div>
          <div class="message-footer" v-if="msg.emotion || msg.tokenCount">
            <span v-if="msg.emotion">情感：{{ msg.emotion }}</span>
            <span v-if="msg.tokenCount">Token：{{ msg.tokenCount }}</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import request from '@/utils/request'

const route = useRoute()
const sessionId = ref(route.params.sessionId)
const messages = ref([])

onMounted(async () => {
  const res = await request.get(`/chat/session/${sessionId.value}`)
  messages.value = res.data.data
})
</script>

<style scoped>
.sticky-header {
  position: sticky;
  top: 0;
  z-index: 10;
  background: #fff;
  padding: 12px 0;
  border-bottom: 1px solid #ebeef5;
}

.messages {
  max-width: 800px;
  margin: 0 auto;
}

.message {
  margin-bottom: 16px;
  display: flex;
}

.message.user {
  justify-content: flex-end;
}

.message-bubble {
  max-width: 70%;
  padding: 12px 16px;
  border-radius: 12px;
  background: #fff;
  box-shadow: 0 1px 4px rgba(0,0,0,0.08);
}

.message.user .message-bubble {
  background: #409EFF;
  color: #fff;
}

.message-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.time {
  font-size: 12px;
  color: #909399;
}

.message.user .time {
  color: rgba(255,255,255,0.7);
}

.message-content {
  line-height: 1.6;
  word-break: break-word;
}

.message-footer {
  margin-top: 8px;
  font-size: 12px;
  color: #909399;
  display: flex;
  gap: 12px;
}
</style>
