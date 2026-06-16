<template>
  <el-container class="main-layout">
    <el-header class="layout-header">
      <div class="nav-title">
        <el-icon><Document /></el-icon>
        <span>文档摘要与知识库</span>
      </div>
      <el-menu
        mode="horizontal"
        :default-active="activeMenu"
        :ellipsis="false"
        router
        background-color="#001529"
        text-color="#bfcbd9"
        active-text-color="#ffd04b"
      >
        <el-menu-item index="/">
          <el-icon><House /></el-icon>
          <span>概览</span>
        </el-menu-item>
        <el-menu-item index="/documents">
          <el-icon><Folder /></el-icon>
          <span>文档管理</span>
        </el-menu-item>
        <el-menu-item index="/qa">
          <el-icon><ChatLineRound /></el-icon>
          <span>智能问答</span>
        </el-menu-item>
      </el-menu>
      <div class="header-right">
        <el-tag :type="health.ok ? 'success' : 'danger'" size="small">
          {{ health.ok ? '后端在线' : '后端离线' }}
        </el-tag>
      </div>
    </el-header>
    <el-main class="layout-main">
      <router-view v-slot="{ Component }">
        <transition name="fade" mode="out-in">
          <component :is="Component" />
        </transition>
      </router-view>
    </el-main>
  </el-container>
</template>

<script setup>
import { computed, onMounted, onBeforeUnmount, reactive } from 'vue'
import { useRoute } from 'vue-router'
import { Document, House, Folder, ChatLineRound } from '@element-plus/icons-vue'
import { checkHealth } from '@/api/health'

const route = useRoute()
const activeMenu = computed(() => {
  // 详情页仍高亮"文档管理"
  if (route.path.startsWith('/documents')) return '/documents'
  if (route.path.startsWith('/qa')) return '/qa'
  return route.path
})

const health = reactive({ ok: false, info: null })
let healthTimer = null

const refreshHealth = async () => {
  try {
    const data = await checkHealth()
    health.info = data
    health.ok = data?.status === 'up'
  } catch (e) {
    health.ok = false
  }
}

onMounted(() => {
  refreshHealth()
  healthTimer = setInterval(refreshHealth, 30000)
})

onBeforeUnmount(() => {
  if (healthTimer) clearInterval(healthTimer)
})
</script>

<style scoped>
.main-layout {
  min-height: 100vh;
}
.layout-header {
  display: flex;
  align-items: center;
  background-color: #001529;
  padding: 0 20px;
  height: 60px;
}
.layout-header :deep(.el-menu) {
  border-bottom: none;
  flex: 1;
}
.header-right {
  display: flex;
  align-items: center;
  gap: 12px;
}
.layout-main {
  background: var(--app-bg);
  padding: 0;
}
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s;
}
.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>