<template>
  <div>
    <el-card>
      <div style="display:flex; gap:12px; align-items:center; flex-wrap: nowrap; overflow-x: auto; white-space: nowrap;">
        <VoiceRecorder @transcribed="onVoiceText" />
        <el-input v-model="destination" placeholder="目的地" style="width:200px" />
        <el-date-picker v-model="startDate" type="date" placeholder="开始日期" style="width:160px" value-format="YYYY-MM-DD" format="YYYY-MM-DD" />
        <el-input-number v-model="days" :min="1" :max="30" />
        <span>总预算</span>
        <el-input-number v-model="budget" :min="0" :step="100" />
        <span>人数</span>
        <el-input-number v-model="peopleCount" :min="1" :max="20" />
        <el-input v-model="preferences" placeholder="偏好描述" style="width:220px" />
        <el-button type="primary" @click="generate" :disabled="isGenerating">生成行程</el-button>
        <el-button type="success" @click="saveIt" :disabled="isGenerating || !itinerary">保存行程</el-button>
        <el-tag v-if="isGenerating" type="warning">正在生成行程… 预计剩余 {{ etaSeconds }} 秒</el-tag>
      </div>
      <!-- 第二行：语音识别文本展示，不影响第一行 -->
      <div style="margin-top:8px; display:flex; align-items:flex-start; gap:8px;">
        <span style="min-width:100px;">语音识别文本：</span>
        <el-input v-model="lastVoiceText" type="textarea" :autosize="{ minRows: 1, maxRows: 3 }" placeholder="（暂无语音识别文本）" />
      </div>
    </el-card>

    <el-card style="margin-top:16px">
      <template #header>
        <div style="display:flex; justify-content:space-between; align-items:center;">
          <span>行程地图</span>
          <el-button @click="refreshRoute" size="small">刷新线路</el-button>
        </div>
      </template>
      <MapView :routes="dailyRoutes" :points="mapPoints" :destination="itinerary?.destination || destination" />
    </el-card>

    <el-card style="margin-top:16px">
      <template #header>AI 生成的行程</template>
      <div v-if="itinerary && itinerary.schedule?.length">
        <div v-for="(day, idx) in itinerary.schedule" :key="idx" style="margin-bottom:12px">
          <strong>Day {{ idx + 1 }}:</strong>
          <span v-if="day.dailyBudget" style="margin-left:8px;color:#409EFF">预算：{{ day.dailyBudget }} 元</span>
          <div>{{ day.summary }}</div>
          <ul>
            <li v-for="(p, j) in day.places" :key="j">{{ p.name }} {{ p.address }}</li>
          </ul>
        </div>
      </div>
      <div v-else>暂无行程，请先点击“生成行程”。</div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import MapView from '@/components/MapView.vue'
import { ElMessage } from 'element-plus'
import VoiceRecorder from '@/components/VoiceRecorder.vue'
import { saveItinerary, planItinerary, voicePlan, searchPOIs } from '@/services/plannerService'

const userId = localStorage.getItem('userId') || ''
const destination = ref('')
const startDate = ref('')
const days = ref(5)
const budget = ref<number | null>(null)
const peopleCount = ref(1)
const preferences = ref('')

const itinerary = ref<any | null>(null)
// 存储最近一次语音文本
const lastVoiceText = ref('')

// 生成提示与倒计时
const isGenerating = ref(false)
const etaSeconds = ref<number>(0)
let etaTimer: any = null
function startEta(seconds = 60) {
  etaSeconds.value = seconds
  if (etaTimer) clearInterval(etaTimer)
  etaTimer = setInterval(() => {
    etaSeconds.value = Math.max(etaSeconds.value - 1, 0)
    if (etaSeconds.value === 0) {
      clearInterval(etaTimer)
      etaTimer = null
    }
  }, 1000)
}
function stopEta() {
  if (etaTimer) clearInterval(etaTimer)
  etaTimer = null
  etaSeconds.value = 0
}

function parseVoice(text: string) {
  const destMatch = text.match(/(去|到)([\u4e00-\u9fa5A-Za-z]+?)(?:[，,。、\s]|5|\d|天|预算)/)
  if (destMatch) destination.value = destMatch[2]
  const daysMatch = text.match(/(\d+)\s*天/)
  if (daysMatch) days.value = Number(daysMatch[1])
  const budgetMatch = text.match(/(\d+(?:\.\d+)?)\s*(?:元|块|人民币)/)
  if (budgetMatch) budget.value = Number(budgetMatch[1])
  const peopleMatch = text.match(/(\d+)\s*(?:人|位|个)/)
  if (peopleMatch) peopleCount.value = Number(peopleMatch[1])
  const prefs: string[] = []
  const prefKeys = ['美食', '动漫', '亲子', '自然', '文化', '购物', '历史', '摄影']
  prefKeys.forEach(k => { if (text.includes(k)) prefs.push(k) })
  if (prefs.length) preferences.value = prefs.join(',')
}

function onVoiceText(text?: string) {
  if (!text) { ElMessage.warning('未获取到语音文本'); return }
  lastVoiceText.value = text
  ElMessage.success('语音文本已识别，可直接点击“生成行程”')
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

async function geocodePlace(p: any, city: string) {
  const query = (p.address && p.address.trim()) || p.name
  if (!query) return null
  try {
    const resp = await searchPOIs(query, city)
    const data = typeof resp === 'string' ? JSON.parse(resp) : resp
    const loc = data?.pois?.[0]?.location
    if (loc && typeof loc === 'string' && loc.includes(',')) {
      const [lngStr, latStr] = loc.split(',')
      p.lng = Number(lngStr)
      p.lat = Number(latStr)
      return { lng: p.lng, lat: p.lat }
    }
  } catch {}
  return null
}

async function enrichItineraryGeo() {
  if (!itinerary.value?.schedule?.length) return
  const city = itinerary.value?.destination || destination.value
  for (const day of itinerary.value.schedule) {
    for (const p of (day.places || [])) {
      if (!p?.lat || !p?.lng) {
        await geocodePlace(p, city)
      }
    }
  }
}

async function generate() {
  if (isGenerating.value) return
  isGenerating.value = true
  startEta(25)
  try {
    if (lastVoiceText.value.trim()) {
      try {
        const plan = await voicePlan(lastVoiceText.value.trim())
        itinerary.value = plan
        destination.value = plan?.destination || destination.value
        await enrichItineraryGeo()
        refreshRoute()
        ElMessage.success('语音生成完成')
        return
      } catch (e) {
        parseVoice(lastVoiceText.value)
        ElMessage.warning('语音生成失败，已根据语音填充表单，请再试')
      }
    }

    if (!destination.value || !startDate.value || !days.value || !budget.value) {
      ElMessage.warning('请填写目的地、开始日期、天数与总预算，或先录音使用语音生成')
      return
    }
    const req = {
      destination: destination.value,
      startDate: startDate.value,
      days: days.value,
      budget: budget.value,
      peopleCount: peopleCount.value,
      preferences: preferences.value.split(',').map(s => s.trim()).filter(Boolean)
    }
    try {
      const plan = await planItinerary(req as any)
      itinerary.value = plan
      await enrichItineraryGeo()
      refreshRoute()
      ElMessage.success('行程已生成')
    } catch (e) {
      ElMessage.error('生成失败')
    }
  } finally {
    isGenerating.value = false
    stopEta()
  }
}

async function saveIt() {
  if (!userId) { ElMessage.error('请先登录'); return }
  if (!itinerary.value) { ElMessage.warning('请先生成行程'); return }
  try {
    const res = await saveItinerary(userId, itinerary.value)
    if (res.status === 'ok') {
      ElMessage.success('行程已保存')
    } else {
      ElMessage.error(res.message || '保存失败')
    }
  } catch (e) {
    ElMessage.error('保存失败')
  }
}

const mapPoints = computed(() => {
  if (!itinerary.value?.schedule?.length) return []
  return itinerary.value.schedule.map((day: any) => {
    const first = (day.places || []).find((p: any) => p?.lat && p?.lng)
    if (!first) return null
    return { lat: Number(first.lat), lng: Number(first.lng), name: first.name }
  }).filter(Boolean)
})

const dailyRoutes = computed(() => {
  if (!itinerary.value?.schedule?.length) return []
  return itinerary.value.schedule.map((day: any, idx: number) => {
    const sorted = [...(day.places || [])].sort((a: any, b: any) => extractTimeFromNotes(a?.notes) - extractTimeFromNotes(b?.notes))
    const path = sorted
      .filter((p: any) => p?.lat && p?.lng)
      .map((p: any) => [Number(p.lng), Number(p.lat)])
    return { path, dayIndex: day.dayIndex ?? (idx + 1) }
  }).filter((r: any) => r.path && r.path.length >= 2)
})

function refreshRoute() {
  itinerary.value = { ...itinerary.value }
}
</script>
