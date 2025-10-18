<template>
  <div class="login">
    <h2>登录</h2>
    <el-form :model="form" label-width="80px" style="max-width: 400px">
      <el-form-item label="用户名">
        <el-input v-model="form.username" />
      </el-form-item>
      <el-form-item label="密码">
        <el-input type="password" v-model="form.password" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="login">登录</el-button>
      </el-form-item>
    </el-form>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import api from '@/services/api'
import { ElMessage } from 'element-plus'

const router = useRouter()
const form = ref({ username: '', password: '' })

const login = async () => {
  const { data } = await api.post('/api/user/login', form.value)
  if (data.status === 'ok') {
    localStorage.setItem('userId', data.userId)
    ElMessage.success('登录成功')
    router.push('/planner')
  } else {
    ElMessage.error(data.message || '登录失败')
  }
}
</script>