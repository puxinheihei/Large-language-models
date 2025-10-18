import api from './api'

export interface PlanRequest {
  destination: string
  startDate: string
  days: number
  budget: number
  peopleCount: number
  preferences: string[]
  notes?: string
}

// 改为走 /api/itinerary/generate
export const planItinerary = async (req: PlanRequest) => {
  const { data } = await api.post('/api/itinerary/generate', req)
  return data
}

export const searchPOIs = async (keywords: string, city?: string) => {
  const { data } = await api.get('/api/planner/pois', {
    params: { keywords, city },
  })
  return data
}

export const transcribeAudio = async (audioBase64: string) => {
  const { data } = await api.post('/api/planner/voice/transcribe', { audioBase64 })
  return data.text as string
}

export const saveItinerary = async (userId: string, itinerary: any) => {
  const { data } = await api.post('/api/itinerary/save', { userId, itinerary })
  return data as { status: string; id?: string; message?: string }
}

export const listItineraries = async (userId: string) => {
  const { data } = await api.get('/api/itinerary/list', { params: { userId } })
  return data as Array<{ id: string; destination: string; startDate: string; days: number; summary: string }>
}

export const getItinerary = async (id: string) => {
  const { data } = await api.get('/api/itinerary/get', { params: { id } })
  return data
}

export const deleteItinerary = async (id: string) => {
  const { data } = await api.delete('/api/itinerary/delete', { params: { id } })
  return data as { status: string }
}

// 新增：语音文本直接生成行程
export const voicePlan = async (text: string) => {
  const { data } = await api.post('/api/itinerary/generate', { voiceText: text })
  return data
}