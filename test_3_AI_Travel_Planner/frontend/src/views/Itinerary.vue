<template>
  <div class="itinerary">
    <h2>我的行程</h2>
    <el-row :gutter="16">
      <el-col :span="10">
        <el-card>
          <div style="margin-bottom: 12px; display:flex; justify-content: space-between; align-items:center;">
            <span>行程列表</span>
            <el-button size="small" @click="load">刷新</el-button>
          </div>
          <el-table :data="list" size="small">
            <el-table-column prop="destination" label="目的地" width="160" />
            <el-table-column prop="startDate" label="出发日期" width="120" />
            <el-table-column prop="days" label="天数" width="80" />
            <el-table-column label="操作" width="200">
              <template #default="{ row }">
                <el-button type="primary" size="small" @click="view(row.id)">详情</el-button>
                <el-button type="danger" size="small" @click="remove(row.id)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
      <el-col :span="14">
        <el-card>
          <div v-if="detail">
            <h3>行程详情：{{ detail.destination }} · {{ detail.days }} 天</h3>
            <MapView :key="detail?.id || `${detail?.destination}-${detail?.startDate}`" :routes="detailRoutes" :points="detailPoints" :destination="detail?.destination" />
            <el-timeline style="margin-top: 12px">
              <el-timeline-item
                v-for="(day, i) in detail.schedule"
                :key="day.dayIndex ?? i"
              >
                <div class="day-title">第{{ day.dayIndex ?? (i + 1) }}天</div>
                <div>{{ day.summary }}</div>
                <ul>
                  <li v-for="p in day.places" :key="p.name">{{ p.type }}：{{ p.name }}</li>
                </ul>
              </el-timeline-item>
            </el-timeline>
          </div>
          <div v-else>
            <el-empty description="请选择左侧行程查看详情" />
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { listItineraries, getItinerary, deleteItinerary } from '@/services/plannerService'
import MapView from '@/components/MapView.vue'

const list = ref<any[]>([])
const detail = ref<any | null>(null)

const load = async () => {
  const userId = localStorage.getItem('userId') || ''
  if (!userId) {
    ElMessage.warning('请先登录')
    return
  }
  try {
    const data = await listItineraries(userId)
    list.value = data
  } catch (e) {
    ElMessage.error('加载行程失败')
  }
}

const view = async (id: string) => {
  try {
    detail.value = await getItinerary(id)
  } catch (e) {
    ElMessage.error('加载详情失败')
  }
}

const remove = async (id: string) => {
  try {
    const { status } = await deleteItinerary(id)
    if (status === 'ok') {
      ElMessage.success('已删除')
      if (detail.value?.id === id) detail.value = null
      load()
    } else {
      ElMessage.error('删除失败')
    }
  } catch (e) {
    ElMessage.error('删除失败')
  }
}

function extractTimeFromNotes(notes?: string): number {
  if (!notes) return Number.POSITIVE_INFINITY
  const s = notes.trim().toLowerCase()
  const isPM = s.includes('下午') || s.includes('pm')
  const isAM = s.includes('上午') || s.includes('am')
  const hhmm = s.match(/(\d{1,2})\s*[:：]\s*(\d{1,2})/)
  if (hhmm) {
    let h = parseInt(hhmm[1], 10)
    const m = parseInt(hhmm[2], 10)
    if (isPM && h < 12) h += 12
    if (isAM && h === 12) h = 0
    return h * 60 + m
  }
  const cn = s.match(/(\d{1,2})\s*点\s*(\d{1,2})?/)
  if (cn) {
    let h = parseInt(cn[1], 10)
    const m = cn[2] ? parseInt(cn[2], 10) : 0
    if (isPM && h < 12) h += 12
    if (isAM && h === 12) h = 0
    return h * 60 + m
  }
  const hourOnly = s.match(/(?:\b|^)(\d{1,2})(?:点|时|hour|小时|h)(?:\b|$)/)
  if (hourOnly) {
    let h = parseInt(hourOnly[1], 10)
    if (isPM && h < 12) h += 12
    if (isAM && h === 12) h = 0
    return h * 60
  }
  return Number.POSITIVE_INFINITY
}

const detailPoints = computed(() => {
  if (!detail.value?.schedule?.length) return []
  return detail.value.schedule.map((day: any) => {
    const first = (day.places || []).find((p: any) => p?.lat && p?.lng)
    if (!first) return null
    return { lat: Number(first.lat), lng: Number(first.lng), name: first.name }
  }).filter(Boolean)
})

const detailRoutes = computed(() => {
  if (!detail.value?.schedule?.length) return []
  return detail.value.schedule.map((day: any, idx: number) => {
    const sorted = [...(day.places || [])].sort((a: any, b: any) => extractTimeFromNotes(a?.notes) - extractTimeFromNotes(b?.notes))
    const path = sorted
      .filter((p: any) => p?.lat && p?.lng)
      .map((p: any) => [Number(p.lng), Number(p.lat)])
    return { path, dayIndex: day.dayIndex ?? (idx + 1) }
  }).filter((r: any) => r.path && r.path.length >= 2)
})
onMounted(load)
</script>

<style scoped>
.itinerary { padding: 16px; }
.day-title { font-weight: 600; margin-bottom: 6px; }
</style>