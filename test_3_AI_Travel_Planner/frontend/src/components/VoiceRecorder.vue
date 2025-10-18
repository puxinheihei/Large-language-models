<template>
  <div>
    <el-button type="primary" @click="toggleRecording">
      {{ isRecording ? '停止录音' : '开始录音' }}
    </el-button>
    <!-- 移除行内显示的转写文本，避免与第二行重复 -->
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { transcribeAudio } from '@/services/plannerService'

const emit = defineEmits<{
  (e: 'transcribed', text: string): void
}>()

const isRecording = ref(false)
const transcribedText = ref('')

let audioContext: AudioContext | null = null
let sourceNode: MediaStreamAudioSourceNode | null = null
let processorNode: ScriptProcessorNode | null = null
let stream: MediaStream | null = null
let inputSampleRate = 44100
const audioChunks: Float32Array[] = []

function mergeBuffers(buffers: Float32Array[]) {
  let length = 0
  for (const b of buffers) length += b.length
  const result = new Float32Array(length)
  let offset = 0
  for (const b of buffers) { result.set(b, offset); offset += b.length }
  return result
}

function downsampleBuffer(buffer: Float32Array, sampleRate: number, outRate: number) {
  if (outRate === sampleRate) return buffer
  if (outRate > sampleRate) throw new Error('不能上采样')
  const sampleRateRatio = sampleRate / outRate
  const newLength = Math.round(buffer.length / sampleRateRatio)
  const result = new Float32Array(newLength)
  let offsetResult = 0
  let offsetBuffer = 0
  while (offsetResult < result.length) {
    const nextOffsetBuffer = Math.round((offsetResult + 1) * sampleRateRatio)
    let accum = 0, count = 0
    for (let i = offsetBuffer; i < nextOffsetBuffer && i < buffer.length; i++) {
      accum += buffer[i]
      count++
    }
    result[offsetResult] = accum / count
    offsetResult++
    offsetBuffer = nextOffsetBuffer
  }
  return result
}

function encodeWAV(samples: Float32Array, sampleRate: number) {
  const buffer = new ArrayBuffer(44 + samples.length * 2)
  const view = new DataView(buffer)

  function writeString(view: DataView, offset: number, s: string) {
    for (let i = 0; i < s.length; i++) view.setUint8(offset + i, s.charCodeAt(i))
  }
  function floatTo16BitPCM(view: DataView, offset: number, input: Float32Array) {
    for (let i = 0; i < input.length; i++, offset += 2) {
      let s = Math.max(-1, Math.min(1, input[i]))
      view.setInt16(offset, s < 0 ? s * 0x8000 : s * 0x7FFF, true)
    }
  }

  // RIFF/WAVE Header
  writeString(view, 0, 'RIFF')
  view.setUint32(4, 36 + samples.length * 2, true)
  writeString(view, 8, 'WAVE')
  writeString(view, 12, 'fmt ')
  view.setUint32(16, 16, true) // PCM
  view.setUint16(20, 1, true)  // format
  view.setUint16(22, 1, true)  // mono
  view.setUint32(24, sampleRate, true)
  view.setUint32(28, sampleRate * 2, true)
  view.setUint16(32, 2, true) // block align
  view.setUint16(34, 16, true) // bits per sample
  writeString(view, 36, 'data')
  view.setUint32(40, samples.length * 2, true)

  floatTo16BitPCM(view, 44, samples)
  return buffer
}

function arrayBufferToBase64(buffer: ArrayBuffer) {
  const bytes = new Uint8Array(buffer)
  let binary = ''
  for (let i = 0; i < bytes.byteLength; i++) binary += String.fromCharCode(bytes[i])
  return btoa(binary)
}

const toggleRecording = async () => {
  if (!isRecording.value) {
    try {
      stream = await navigator.mediaDevices.getUserMedia({ audio: { channelCount: 1, echoCancellation: true, noiseSuppression: true } })
      audioContext = new (window.AudioContext || (window as any).webkitAudioContext)()
      inputSampleRate = audioContext.sampleRate
      sourceNode = audioContext.createMediaStreamSource(stream)
      processorNode = audioContext.createScriptProcessor(4096, 1, 1)
      audioChunks.length = 0
      processorNode.onaudioprocess = (e) => {
        const data = e.inputBuffer.getChannelData(0)
        audioChunks.push(new Float32Array(data))
      }
      sourceNode.connect(processorNode)
      processorNode.connect(audioContext.destination)
      isRecording.value = true
    } catch (e) {
      ElMessage.error('无法访问麦克风')
    }
  } else {
    try {
      isRecording.value = false
      processorNode?.disconnect()
      sourceNode?.disconnect()
      await audioContext?.close()
      stream?.getTracks().forEach(t => t.stop())

      const merged = mergeBuffers(audioChunks)
      const down = downsampleBuffer(merged, inputSampleRate, 16000)
      const wavBuffer = encodeWAV(down, 16000)
      const base64 = arrayBufferToBase64(wavBuffer)
      const text = await transcribeAudio(base64)
      transcribedText.value = text
      emit('transcribed', text)
      ElMessage.success('语音识别完成')
    } catch (e) {
      ElMessage.error('识别失败')
    } finally {
      stream = null
      audioContext = null
      sourceNode = null
      processorNode = null
      audioChunks.length = 0
    }
  }
}
</script>