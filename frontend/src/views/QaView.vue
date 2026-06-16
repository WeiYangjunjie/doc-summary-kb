<template>
  <div class="page-container">
    <h2 class="page-title">智能问答</h2>

    <el-row :gutter="16">
      <!-- 左：提问 -->
      <el-col :xs="24" :md="14">
        <div class="section-card">
          <el-input
            v-model="question"
            type="textarea"
            :rows="4"
            placeholder="请输入你的问题，例如：MiniMax M3 的计费规则是什么？"
            :maxlength="500"
            show-word-limit
          />
          <div class="toolbar" style="margin-top:12px">
            <el-select v-model="topK" style="width:120px">
              <el-option :value="3" label="top 3" />
              <el-option :value="5" label="top 5" />
              <el-option :value="10" label="top 10" />
            </el-select>
            <div class="flex-grow" />
            <el-button :icon="Delete" @click="question = ''; currentAnswer = null">清空</el-button>
            <el-button
              type="primary"
              :icon="Promotion"
              :loading="asking"
              :disabled="!question.trim()"
              @click="onAsk"
            >提问</el-button>
          </div>
        </div>

        <div v-if="currentAnswer" class="section-card">
          <div class="card-title">回答</div>
          <div class="qa-answer">{{ currentAnswer.answer }}</div>

          <div class="card-title" style="margin-top:20px">引用来源</div>
          <el-empty v-if="!currentAnswer.citations?.length" description="该回答未引用任何文档" />
          <div v-else>
            <CitationItem
              v-for="(c, i) in currentAnswer.citations"
              :key="i"
              :citation="c"
            />
          </div>
        </div>
      </el-col>

      <!-- 右：历史 -->
      <el-col :xs="24" :md="10">
        <div class="section-card">
          <div class="card-title-row">
            <div class="card-title">历史问答</div>
            <el-button :icon="RefreshRight" link @click="loadHistory">刷新</el-button>
          </div>

          <el-empty v-if="!historyLoading && historyList.length === 0" description="暂无历史问答" />
          <el-scrollbar v-else height="600px">
            <div
              v-for="item in historyList"
              :key="item.id"
              class="history-item"
            >
              <div class="history-q" @click="toggleExpand(item.id)">
                <el-icon class="history-arrow">
                  <component :is="expanded[item.id] ? 'ArrowDown' : 'ArrowRight'" />
                </el-icon>
                <span class="history-question">{{ item.question }}</span>
                <span class="history-time">{{ relativeTime(item.createdAt) }}</span>
              </div>
              <div v-if="expanded[item.id]" class="history-detail">
                <div class="qa-answer">{{ item.answer }}</div>
                <div v-if="item.citations?.length" style="margin-top:10px">
                  <div class="card-subtitle">引用</div>
                  <CitationItem
                    v-for="(c, i) in item.citations"
                    :key="i"
                    :citation="c"
                  />
                </div>
                <div class="history-actions">
                  <el-button link type="primary" size="small" @click="reuse(item)">
                    再次提问
                  </el-button>
                </div>
              </div>
            </div>
          </el-scrollbar>

          <div class="pagination-wrap">
            <el-pagination
              v-model:current-page="historyPage"
              v-model:page-size="historySize"
              :total="historyTotal"
              :page-sizes="[10, 20]"
              layout="total, sizes, prev, pager, next, jumper"
              background
              small
              @size-change="loadHistory"
              @current-change="loadHistory"
            />
          </div>
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Promotion, Delete, RefreshRight } from '@element-plus/icons-vue'
import CitationItem from '@/components/CitationItem.vue'
import { askQuestion, listQaHistory } from '@/api/qa'
import { relativeTime } from '@/utils/format'

// 当前提问
const question = ref('')
const topK = ref(5)
const asking = ref(false)
const currentAnswer = ref(null)

const onAsk = async () => {
  if (!question.value.trim()) return
  asking.value = true
  try {
    const data = await askQuestion({
      question: question.value.trim(),
      topK: topK.value
    })
    currentAnswer.value = data
    ElMessage.success('已生成回答')
    loadHistory() // 刷新历史
  } catch (e) {
    // 拦截器已提示
  } finally {
    asking.value = false
  }
}

// 历史
const historyLoading = ref(false)
const historyList = ref([])
const historyPage = ref(1)
const historySize = ref(10)
const historyTotal = ref(0)
const expanded = reactive({})

const loadHistory = async () => {
  historyLoading.value = true
  try {
    const data = await listQaHistory({ page: historyPage.value, size: historySize.value })
    historyList.value = data.list || []
    historyTotal.value = data.total ?? historyList.value.length
    // 解析 citations：后端存的是 JSON 字符串
    for (const it of historyList.value) {
      if (typeof it.citations === 'string') {
        try { it.citations = JSON.parse(it.citations) } catch { it.citations = [] }
      }
      if (!Array.isArray(it.citations)) it.citations = []
    }
  } catch (e) {
    historyList.value = []
    historyTotal.value = 0
  } finally {
    historyLoading.value = false
  }
}

const toggleExpand = (id) => {
  expanded[id] = !expanded[id]
}

const reuse = (item) => {
  question.value = item.question
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

onMounted(loadHistory)
</script>

<style scoped>
.page-title {
  margin: 0 0 16px;
  font-size: 20px;
  color: #303133;
}
.card-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 12px;
}
.card-subtitle {
  font-size: 13px;
  color: #909399;
  margin: 8px 0 4px;
}
.card-title-row {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  margin-bottom: 12px;
}
.history-item {
  border-bottom: 1px solid #ebeef5;
  padding: 10px 4px;
}
.history-q {
  display: flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
}
.history-question {
  flex: 1;
  font-weight: 500;
  color: #303133;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.history-time {
  font-size: 12px;
  color: #909399;
}
.history-detail {
  margin-top: 10px;
  padding-left: 22px;
}
.history-actions {
  margin-top: 8px;
  display: flex;
  justify-content: flex-end;
}
.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  margin-top: 12px;
}
</style>