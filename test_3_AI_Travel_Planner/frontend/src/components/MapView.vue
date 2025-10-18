<template>
  <div ref="mapRef" style="height: 480px; width: 100%"></div>
</template>

<script setup lang="ts">
import { onMounted, ref, watch, nextTick } from 'vue'

const props = defineProps<{ points?: any[], routes?: { path: [number, number][], dayIndex: number }[], destination?: string }>()

const mapRef = ref<HTMLElement | null>(null)
let map: any
let drawTimer: any = null
const DRAW_DELAY = 150

function loadAMapScript() {
  const key = import.meta.env.VITE_AMAP_JS_API_KEY
  const securityJsCode = import.meta.env.VITE_AMAP_SECURITY_JS_CODE
  return new Promise<void>((resolve, reject) => {
    if ((window as any).AMap) return resolve()
    ;(window as any)._AMapSecurityConfig = { securityJsCode }
    const script = document.createElement('script')
    script.src = `https://webapi.amap.com/maps?v=2.0&key=${key}&plugin=AMap.ToolBar`
    script.onload = () => resolve()
    script.onerror = reject
    document.head.appendChild(script)
  })
}

function getColorByDay(day: number) {
  const colors = ['#E74C3C', '#2980B9', '#27AE60', '#8E44AD', '#F39C12', '#16A085', '#2C3E50', '#D35400']
  return colors[(day - 1) % colors.length]
}

function recenterToDestination() {
  if (!map) return
  const dest = (props.destination || '').toLowerCase()
  const centers: Record<string, [number, number]> = {
    '日本': [139.7671, 35.6812],
    'tokyo': [139.7671, 35.6812],
    '东京': [139.7671, 35.6812],
    '大阪': [135.4978, 34.7025],
    'osaka': [135.4978, 34.7025],
    '京都': [135.7681, 35.0116],
    'kyoto': [135.7681, 35.0116],
    '横滨': [139.6380, 35.4437],
    'yokohama': [139.6380, 35.4437],
    '札幌': [141.3545, 43.0621],
    'sapporo': [141.3545, 43.0621],
    '名古屋': [136.9066, 35.1815],
    'nagoya': [136.9066, 35.1815],
    '冲绳': [127.6789, 26.2124],
    'okinawa': [127.6789, 26.2124]
  }
  const key = Object.keys(centers).find(k => dest.includes(k))
  if (key) {
    map.setCenter(centers[key])
    map.setZoom(11)
  } else {
    map.setCenter([116.397428, 39.90923])
    map.setZoom(12)
  }
}

function drawDailyRoutes() {
  if (!map || !props.routes?.length) {
    if (map) {
      map.clearMap()
      recenterToDestination()
    }
    return
  }
  map.clearMap()
  props.routes.forEach((r: any) => {
    const color = getColorByDay(r.dayIndex)
    const polyline = new (window as any).AMap.Polyline({
      path: r.path,
      showDir: true,
      strokeColor: color,
      strokeOpacity: 0.9,
      strokeWeight: 5,
      isOutline: true,
      outlineColor: '#FFFFFF',
      borderWeight: 1,
    })
    map.add(polyline)

    if (r.path.length) {
      const start = r.path[0]
      const end = r.path[r.path.length - 1]
      const startMarker = new (window as any).AMap.Marker({
        position: start,
        title: `第${r.dayIndex}天 起点`,
        label: { content: `第${r.dayIndex}天 起`, offset: new (window as any).AMap.Pixel(0, -18) }
      })
      const endMarker = new (window as any).AMap.Marker({
        position: end,
        title: `第${r.dayIndex}天 终点`,
        label: { content: `第${r.dayIndex}天 终 →`, offset: new (window as any).AMap.Pixel(0, -18) }
      })
      map.add(startMarker)
      map.add(endMarker)
    }

    r.path.forEach((lnglat: [number, number]) => {
      const marker = new (window as any).AMap.Marker({
        position: lnglat,
        title: `第${r.dayIndex}天`,
        label: {
          content: `第${r.dayIndex}天`,
          offset: new (window as any).AMap.Pixel(0, -18)
        }
      })
      map.add(marker)
    })
  })
  map.setFitView()
}

function drawPoints() {
  if (!map || !props.points?.length) {
    if (map) {
      map.clearMap()
      recenterToDestination()
    }
    return
  }
  map.clearMap()
  props.points.forEach((p: any) => {
    const marker = new (window as any).AMap.Marker({ position: [p.lng, p.lat], title: p.name })
    map.add(marker)
  })
  map.setFitView()
}

onMounted(async () => {
  await loadAMapScript()
  map = new (window as any).AMap.Map(mapRef.value!, { zoom: 12 })
  nextTick(() => {
    if (props.routes && props.routes.length) drawDailyRoutes()
    else if (props.points && props.points.length) drawPoints()
    else recenterToDestination()
  })
})

// 合并监听 + 防抖：当 routes 与 points 同时更新时，优先绘制路线，减少频繁重绘导致的闪烁
watch([() => props.routes, () => props.points], ([routes, points]) => {
  if (!map) return
  if (drawTimer) clearTimeout(drawTimer)
  drawTimer = setTimeout(() => {
    if (routes && (routes as any[]).length) {
      drawDailyRoutes()
    } else if (points && (points as any[]).length) {
      drawPoints()
    } else {
      map.clearMap()
      recenterToDestination()
    }
  }, DRAW_DELAY)
}, { deep: true })
</script>