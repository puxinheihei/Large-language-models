<template>
  <div class="budget">
    <h2>旅行预算</h2>

    <el-card style="margin-bottom: 16px">
      <template #header>
        <div style="display:flex; align-items:center; justify-content:space-between">
          <span>快速录入</span>
          <div style="display:flex; align-items:center; gap:8px">
            <el-select v-model="selectedItineraryId" placeholder="选择行程" style="width: 220px" @change="onItineraryChange">
              <el-option v-for="it in itineraries" :key="it.id" :label="`${it.destination} · ${it.startDate}`" :value="it.id" />
            </el-select>
            <el-button type="primary" @click="analyze">AI分析</el-button>
          </div>
        </div>
      </template>

      <div style="display: flex; gap: 12px; align-items: flex-start">
        <VoiceRecorder @transcribed="onVoiceText" style="flex: 1" />
        <!-- 新增：语音识别与对话面板 -->
        <div style="flex: 1">
          <el-card>
            <template #header>语音识别与对话</template>
            <div style="max-height: 220px; overflow: auto">
              <div v-for="(m,i) in chatLog" :key="i"><strong>{{ m.role }}：</strong>{{ m.text }}</div>
            </div>
          </el-card>
        </div>
      </div>
    </el-card>

    <el-card v-if="summary" style="margin-bottom: 16px">
      <template #header>
        <div style="display:flex; align-items:center; justify-content:space-between; gap:12px">
          <span>预算总览</span>
          <div style="display:flex; align-items:center; gap:8px">
            <!-- 恢复：手动调整总预算控件 -->
            <el-input-number v-model="newTotal" :min="0" :step="100" :precision="0" placeholder="总预算(元)" />
            <!-- 简化：保留两个快捷控件：平均分配、智能重分配 -->
            <el-button type="primary" plain @click="equalReallocate">平均分配</el-button>
            <el-button type="success" @click="aiReallocate">智能重分配</el-button>
          </div>
        </div>
      </template>
      <div style="display:flex; gap:24px; flex-wrap:wrap; margin-bottom:12px">
        <div>总预算：<strong>{{ summary.totalBudget }}</strong> 元</div>
        <div>总已消费：<strong>{{ summary.totalSpent }}</strong> 元</div>
        <div>总剩余：<strong style="color:#67C23A">{{ summary.totalRemaining }}</strong> 元</div>
      </div>
      <el-table :data="summaryDays" size="small" style="width:100%">
        <el-table-column prop="dayIndex" label="天" width="80" />
        <el-table-column prop="date" label="日期" width="140" />
        <el-table-column prop="dailyBudget" label="日预算" width="120" />
        <el-table-column prop="spent" label="已消费" width="120" />
        <el-table-column prop="remaining" label="剩余" width="120" />
        <el-table-column label="操作" width="160">
          <template #default="{ row }">
            <el-button size="small" type="primary" @click="openAddExpense(row)">添加消费</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-card v-if="analysis" style="margin-bottom: 16px">
      <template #header>
        <span>AI预算分析</span>
      </template>
      <div>
        <p>总支出：{{ analysis.total }} 元</p>
        <p>日均支出：{{ analysis.dailyAvg }} 元</p>
        <p>类别汇总：</p>
        <ul>
          <li v-for="(val, key) in analysis.byCategory" :key="key">{{ key }}：{{ val }} 元</li>
        </ul>
        <p>建议：</p>
        <ul>
          <li v-for="(s, i) in analysis.suggestions" :key="i">{{ s }}</li>
        </ul>
      </div>
    </el-card>

    <el-card>
      <template #header><span>记录列表</span></template>
      <el-table :data="records" style="width: 100%">
        <el-table-column prop="date" label="日期" width="120" />
        <el-table-column prop="category" label="类别" width="120" />
        <!-- 金额显示为+/−并按颜色区分 -->
        <el-table-column label="金额" width="140">
          <template #default="{ row }">
            <span :style="{ color: row.amount < 0 ? '#F56C6C' : '#67C23A' }">
              {{ row.amount < 0 ? '-' : '+' }}{{ Math.abs(row.amount) }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="description" label="描述" />
        <el-table-column label="操作" width="120">
          <template #default="{ row }">
            <el-button type="danger" size="small" @click="removeRecord(row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialogVisible" title="添加当日消费" width="480px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="日期">
          <el-input v-model="form.date" disabled />
        </el-form-item>
        <el-form-item label="类别">
          <el-select v-model="form.category" placeholder="请选择">
            <el-option label="交通" value="交通" />
            <el-option label="住宿" value="住宿" />
            <el-option label="餐饮" value="餐饮" />
            <el-option label="门票" value="门票" />
            <el-option label="购物" value="购物" />
            <el-option label="其他" value="其他" />
          </el-select>
        </el-form-item>
        <el-form-item label="金额">
          <el-input v-model.number="form.amount" placeholder="例如：88" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" placeholder="可选" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible=false">取消</el-button>
        <el-button type="primary" @click="add">确认添加</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import api from '@/services/api'
import { ElMessage } from 'element-plus'
import VoiceRecorder from '@/components/VoiceRecorder.vue'
import { listItineraries } from '@/services/plannerService'

interface BudgetRecord {
  id?: string
  category: string
  amount: number
  description?: string
  date: string
  itineraryId?: string
}

const form = ref<BudgetRecord>({ category: '', amount: 0, description: '', date: '' })
const records = ref<BudgetRecord[]>([])
const analysis = ref<{ total: number; dailyAvg: number; byCategory: Record<string, number>; suggestions: string[] } | null>(null)

const itineraries = ref<Array<{ id: string; destination: string; startDate: string }>>([])
const selectedItineraryId = ref<string>('')
const currentItinerary = computed(() => itineraries.value.find(it => it.id === selectedItineraryId.value) || null)

const summary = ref<{ totalBudget: number; totalSpent: number; totalRemaining: number; days?: Array<{ dayIndex: number; date: string; dailyBudget: number; spent: number; remaining: number }>; daySummaries?: Array<{ dayIndex: number; date: string; dailyBudget: number; spent: number; remaining: number }>; } | null>(null)
const summaryDays = computed(() => summary.value ? ((summary.value as any).days || (summary.value as any).daySummaries || []) : [])
const newTotal = ref<number | null>(null)
const reallocateMode = ref<'equal' | 'proportional'>('equal')

// 新增：语音识别对话日志
const chatLog = ref<Array<{ role: '用户' | '系统'; text: string }>>([])

const dialogVisible = ref(false)
const dialogDay = ref<any | null>(null)

function formatYmd(d: Date): string {
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const dd = String(d.getDate()).padStart(2, '0')
  return `${y}-${m}-${dd}`
}

// 语音指令解析与执行
async function handleVoiceCommand(text: string): Promise<string> {
  text = text.trim()
  // 预算相关关键词
  const hasBudgetWord = /预算|总预算|经费|钱/.test(text)
  const isAddVerb = /(增加|调高|加)/.test(text)
  const isDecVerb = /(减少|降低|减)/.test(text)
  const hasDayIndicator = /第\d+天|第[一二三四五六七八九十]+天|今天|昨天|明天|\d{4}-\d{2}-\d{2}/.test(text)

  // 解析日期辅助
  const zhNumToInt = (zh: string) => {
    const map: Record<string, number> = { 一: 1, 二: 2, 三: 3, 四: 4, 五: 5, 六: 6, 七: 7, 八: 8, 九: 9, 十: 10 }
    if (zh.length === 1) return map[zh] || NaN
    if (zh.length === 2 && zh[0] === '十') return 10 + (map[zh[1]] || 0)
    if (zh.length === 2 && zh[1] === '十') return (map[zh[0]] || 0) * 10
    if (zh.length === 3 && zh[1] === '十') return (map[zh[0]] || 0) * 10 + (map[zh[2]] || 0)
    return NaN
  }
  const parseDate = (s: string): string | null => {
    if (/今天/.test(s)) return formatYmd(new Date())
    if (/昨天/.test(s)) { const d = new Date(); d.setDate(d.getDate() - 1); return formatYmd(d) }
    if (/明天/.test(s)) { const d = new Date(); d.setDate(d.getDate() + 1); return formatYmd(d) }
    const dmy = s.match(/(\d{4})-(\d{2})-(\d{2})/)
    if (dmy) return `${dmy[1]}-${dmy[2]}-${dmy[3]}`
    const di1 = s.match(/第(\d+)天/)
    if (di1 && currentItinerary.value?.startDate) {
      const start = new Date(currentItinerary.value.startDate)
      const day = Number(di1[1])
      const d = new Date(start.getTime())
      d.setDate(d.getDate() + (day - 1))
      return formatYmd(d)
    }
    const di2 = s.match(/第([一二三四五六七八九十]+)天/)
    if (di2 && currentItinerary.value?.startDate) {
      const start = new Date(currentItinerary.value.startDate)
      const day = zhNumToInt(di2[1])
      const d = new Date(start.getTime())
      d.setDate(d.getDate() + (day - 1))
      return formatYmd(d)
    }
    return null
  }

  // 解析预算设置语句
  const budgetCmdA = text.match(/(设置|设为|调整|改成)(?:总)?预算(?:为)?\s*(\d+(?:\.\d+)?)/)
  const budgetCmdB = text.match(/(?:总)?预算(?:为)?\s*(\d+(?:\.\d+)?)/)

  // 降低总预算：在当前总预算基础上 -amount
  if (hasBudgetWord && !hasDayIndicator && isDecVerb && (budgetCmdA || budgetCmdB)) {
    const amount = Number(budgetCmdA ? budgetCmdA[2] : budgetCmdB![1])
    // 如果未加载摘要，先拉取当前总预算
    if (!summary.value) { await loadSummary() }
    const baseTotal = summary.value?.totalBudget ?? 0
    const newTotalVal = Math.max(0, baseTotal - amount)
    const mode: 'equal' | 'proportional' = /平均|均分/.test(text) ? 'equal' : (/比例|按比例/.test(text) ? 'proportional' : 'equal')
    const { data } = await api.post('/api/budget/reallocate', {
      itineraryId: selectedItineraryId.value,
      newTotal: newTotalVal,
      mode
    })
    summary.value = data
    newTotal.value = data.totalBudget
    ElMessage.success(`已在原有总预算基础上减少 ${amount} 元，新的总预算：${data.totalBudget} 元`)
    return `已在原有总预算基础上减少 ${amount} 元，新的总预算：${data.totalBudget} 元。`
  }

  // 增量添加预算：在当前总预算基础上 +amount
  if (hasBudgetWord && !hasDayIndicator && isAddVerb && (budgetCmdA || budgetCmdB)) {
    const amount = Number(budgetCmdA ? budgetCmdA[2] : budgetCmdB![1])
    // 如果未加载摘要，先拉取当前总预算
    if (!summary.value) { await loadSummary() }
    const baseTotal = summary.value?.totalBudget ?? 0
    const newTotal = baseTotal + amount
    const mode: 'equal' | 'proportional' = /平均|均分/.test(text) ? 'equal' : (/比例|按比例/.test(text) ? 'proportional' : 'equal')
    const { data } = await api.post('/api/budget/reallocate', {
      itineraryId: selectedItineraryId.value,
      newTotal,
      mode
    })
    summary.value = data
    newTotal.value = data.totalBudget
    // 记录正号的预算增加
    const addRecordPayload = {
      category: '预算',
      amount: amount,
      description: `语音增加总预算 ${amount} 元`,
      date: formatYmd(new Date()),
      itineraryId: selectedItineraryId.value
    }
    try {
      await api.post('/api/budget/record', addRecordPayload)
      await load()
    } catch {}
    ElMessage.success(`已在原有总预算基础上增加 ${amount} 元，新的总预算：${data.totalBudget} 元`)
    return `已在原有总预算基础上增加 ${amount} 元，新的总预算：${data.totalBudget} 元。`
  }

  // 按天增减预算：例如“增加第二天预算200元”“减少10-20预算100”
  if (hasBudgetWord && hasDayIndicator && (isAddVerb || isDecVerb)) {
    // 尽量在“预算”附近抓金额，避免把“第2天”的数字当金额
    const nearBudget = text.match(/预算\s*(\d+(?:\.\d+)?)/)
    const amount = nearBudget ? Number(nearBudget[1]) : (text.match(/(\d+(?:\.\d+)?)(?:元|块|人民币)?/) ? Number(text.match(/(\d+(?:\.\d+)?)(?:元|块|人民币)?/)![1]) : NaN)
    if (!isFinite(amount)) {
      return '未识别到需要增减的金额，请再说一次。'
    }
    const sign = isDecVerb ? -1 : 1
    // 提取天索引或具体日期
    let dayIndex: number | null = null
    const di1 = text.match(/第(\d+)天/)
    if (di1) dayIndex = Number(di1[1])
    const di2 = text.match(/第([一二三四五六七八九十]+)天/)
    if (!dayIndex && di2) dayIndex = zhNumToInt(di2[1])
    const dayDate = parseDate(text) // 今天/昨天/具体日期/第X天 -> 日期

    const payload: any = { itineraryId: selectedItineraryId.value, delta: sign * amount }
    if (dayIndex && dayIndex > 0) payload.dayIndex = dayIndex
    else if (dayDate) payload.date = dayDate
    else return '请指出要调整的具体日期或第几天。'

    const { data } = await api.post('/api/budget/day/adjust', payload)
    summary.value = data
    ElMessage.success(`${dayIndex ? '第 ' + dayIndex + ' 天' : dayDate}预算已${sign < 0 ? '减少' : '增加'} ${amount} 元`)
    return `${dayIndex ? '第 ' + dayIndex + ' 天' : dayDate}预算已${sign < 0 ? '减少' : '增加'} ${amount} 元。`
  }

  // 按天重置预算：例如“重置第三天预算为平均/按比例”
  if (hasBudgetWord && hasDayIndicator && /(重置|归零|恢复默认)/.test(text)) {
    let dayIndex: number | null = null
    const di1 = text.match(/第(\d+)天/)
    if (di1) dayIndex = Number(di1[1])
    const di2 = text.match(/第([一二三四五六七八九十]+)天/)
    if (!dayIndex && di2) dayIndex = zhNumToInt(di2[1])
    const dayDate = parseDate(text)
    const mode: 'equal' | 'proportional' = /比例|按比例/.test(text) ? 'proportional' : 'equal'

    const payload: any = { itineraryId: selectedItineraryId.value, mode }
    if (dayIndex && dayIndex > 0) payload.dayIndex = dayIndex
    else if (dayDate) payload.date = dayDate
    else return '请指出要重置的具体日期或第几天。'

    const { data } = await api.post('/api/budget/day/reset', payload)
    summary.value = data
    ElMessage.success(`${dayIndex ? '第 ' + dayIndex + ' 天' : dayDate}预算已重置为${mode === 'equal' ? '平均' : '按比例'}份额`)
    return `${dayIndex ? '第 ' + dayIndex + ' 天' : dayDate}预算已重置为${mode === 'equal' ? '平均' : '按比例'}份额。`
  }

  // 绝对设置预算：将总预算设为 total
  if (budgetCmdA || budgetCmdB) {
    const total = Number(budgetCmdA ? budgetCmdA[2] : budgetCmdB![1])
    const prevTotal = summary.value?.totalBudget ?? 0
    const mode: 'equal' | 'proportional' = /平均|均分/.test(text) ? 'equal' : (/比例|按比例/.test(text) ? 'proportional' : 'equal')
    const { data } = await api.post('/api/budget/reallocate', {
      itineraryId: selectedItineraryId.value,
      newTotal: total,
      mode
    })
    // 添加预算增加记录（若总额提升）
    const diff = data.totalBudget - prevTotal
    if (diff > 0) {
      try {
        await api.post('/api/budget/record', {
          category: '预算', amount: diff, description: `语音设置总预算到 ${total} 元`, date: formatYmd(new Date()), itineraryId: selectedItineraryId.value
        })
        await load()
      } catch {}
    }
    summary.value = data
    newTotal.value = data.totalBudget
    ElMessage.success(`已${mode === 'equal' ? '平均' : '按比例'}重分配，总预算：${data.totalBudget} 元`)
    return `已${mode === 'equal' ? '平均' : '按比例'}重分配，总预算：${data.totalBudget} 元。`
  }

  // 添加消费（解析类别/金额/日期），补充常用同义词到类别
  const catMap = ['交通', '住宿', '餐饮', '门票', '购物', '其他']
  const synonyms: Record<string, string> = {
    '吃饭': '餐饮', '饭': '餐饮', '早餐': '餐饮', '午餐': '餐饮', '晚餐': '餐饮',
    '打车': '交通', '出租车': '交通', '公交': '交通', '地铁': '交通',
    '酒店': '住宿', '旅馆': '住宿',
    '买东西': '购物'
  }
  const foundSyn = Object.keys(synonyms).find(k => text.includes(k))
  const foundCat = foundSyn ? synonyms[foundSyn] : (catMap.find(c => text.includes(c)) || '其他')

  const amountMatch = text.match(/(\d+(?:\.\d+)?)(?:元|块|人民币)?/)
  const dt = parseDate(text) || formatYmd(new Date())
  if (amountMatch) {
    const payload = {
      category: foundCat,
      amount: -Math.abs(Number(amountMatch[1])),
      description: text.replace(/\s+/g, ' ').trim(),
      date: dt,
      itineraryId: selectedItineraryId.value
    }
    const { data } = await api.post('/api/budget/record', payload)
    if (data.status === 'ok') {
      ElMessage.success(`已添加${dt}的消费：${foundCat} ${Math.abs(payload.amount)} 元`)
      await load()
      await loadSummary()
      return `已添加${dt}的消费：${foundCat} ${Math.abs(payload.amount)} 元。`
    } else {
      ElMessage.error(data.message || '添加失败')
      return `添加失败：${data.message || '未知错误'}`
    }
  }
  ElMessage.info('未识别为预算或消费指令，可尝试：设置总预算为5000；添加第二天餐饮80元。')
  return '未识别为预算或消费指令，可尝试：设置总预算为5000；添加第二天餐饮80元。'
}

const onVoiceText = async (text?: string) => {
  if (!text) {
    ElMessage.warning('未获取到语音文本')
    return
  }
  chatLog.value.push({ role: '用户', text })
  const reply = await handleVoiceCommand(text)
  if (reply) chatLog.value.push({ role: '系统', text: reply })
}

const add = async () => {
  if (!selectedItineraryId.value) {
    ElMessage.warning('请先选择一个行程')
    return
  }
  if (!form.value.category || !form.value.amount || !form.value.date) {
    ElMessage.warning('请填写类别、金额和日期')
    return
  }
  const payload = { ...form.value, itineraryId: selectedItineraryId.value, amount: -Math.abs(form.value.amount) }
  const { data } = await api.post('/api/budget/record', payload)
  if (data.status === 'ok') {
    ElMessage.success('添加成功')
    dialogVisible.value = false
    form.value = { category: '', amount: 0, description: '', date: '' }
    load()
    loadSummary()
  } else {
    ElMessage.error(data.message || '添加失败')
  }
}

const removeRecord = async (id?: string) => {
  if (!id) return
  try {
    const { data } = await api.delete('/api/budget/record/delete', { params: { id } })
    if (data.status === 'ok') {
      ElMessage.success('删除成功')
      await load()
      await loadSummary()
    } else {
      ElMessage.error(data.message || '删除失败')
    }
  } catch (e) {
    ElMessage.error('删除失败')
  }
}

const load = async () => {
  if (!selectedItineraryId.value) {
    records.value = []
    return
  }
  const { data } = await api.get('/api/budget/records', {
    params: { itineraryId: selectedItineraryId.value }
  })
  records.value = data.records || []
}

const analyze = async () => {
  const { data } = await api.get('/api/budget/analyze', {
    params: selectedItineraryId.value ? { itineraryId: selectedItineraryId.value } : {}
  })
  analysis.value = data
  ElMessage.success('分析完成')
}

const loadItineraries = async () => {
  const userId = localStorage.getItem('userId') || ''
  if (!userId) return
  const list = await listItineraries(userId)
  itineraries.value = list.map(it => ({ id: it.id, destination: it.destination, startDate: it.startDate }))
}

const loadSummary = async () => {
  if (!selectedItineraryId.value) { summary.value = null; return }
  const { data } = await api.get('/api/budget/summary', { params: { itineraryId: selectedItineraryId.value } })
  summary.value = data
  newTotal.value = data.totalBudget
}

// 平均分配：直接使用总预算均分
const equalReallocate = async () => {
  if (!selectedItineraryId.value || !summary.value) {
    ElMessage.warning('请选择行程')
    return
  }
  const { data } = await api.post('/api/budget/reallocate', {
    itineraryId: selectedItineraryId.value,
    newTotal: summary.value.totalBudget,
    mode: 'equal'
  })
  summary.value = data
  ElMessage.success('已平均分配总预算到每日')
}

// 智能重分配：发送完整预算数据调用后端 LLM
const aiReallocate = async () => {
  if (!selectedItineraryId.value) {
    ElMessage.warning('请选择行程')
    return
  }
  const dayBudgets = summaryDays.value.map((d: any) => d.dailyBudget)
  const { data } = await api.post('/api/budget/reallocate', {
    itineraryId: selectedItineraryId.value,
    mode: 'ai',
    totalBudget: summary.value?.totalBudget ?? null,
    dayBudgets,
    records: records.value.map(r => ({ category: r.category, amount: r.amount, date: r.date, description: r.description }))
  })
  summary.value = data
  ElMessage.success('已智能重分配每日预算')
}

const reallocate = async () => {
  if (!selectedItineraryId.value || newTotal.value == null) {
    ElMessage.warning('请选择行程并填写新总预算')
    return
  }
  const prevTotal = summary.value?.totalBudget ?? 0
  const { data } = await api.post('/api/budget/reallocate', {
    itineraryId: selectedItineraryId.value,
    newTotal: newTotal.value,
    mode: reallocateMode.value
  })
  // 若总额提升则记录+号预算记录
  if (data.totalBudget > prevTotal) {
    try {
      await api.post('/api/budget/record', {
        category: '预算', amount: data.totalBudget - prevTotal, description: `手动调整总预算到 ${data.totalBudget} 元`, date: formatYmd(new Date()), itineraryId: selectedItineraryId.value
      })
      await load()
    } catch {}
  }
  summary.value = data
  ElMessage.success('已重分配日预算')
}

const openAddExpense = (day: any) => {
  dialogDay.value = day
  form.value.date = day?.date || formatYmd(new Date())
  form.value.category = ''
  form.value.amount = 0
  form.value.description = ''
  dialogVisible.value = true
}

const onItineraryChange = () => {
  load()
  loadSummary()
  analysis.value = null
}

onMounted(async () => {
  await loadItineraries()
  // 初始不加载记录与摘要，等待用户选择行程
  // await load()
  // await loadSummary()
})
</script>

<style scoped>
.budget {
  max-width: 900px;
  margin: 0 auto;
}
</style>