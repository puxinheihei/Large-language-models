<template>
  <el-container style="min-height: 100vh">
    <el-header>
      <div class="header">
        <h1>AI 旅行规划师</h1>
        <el-menu mode="horizontal" router :default-active="$route.path">
          <el-menu-item index="/planner">行程规划</el-menu-item>
          <el-menu-item index="/budget">预算管理</el-menu-item>
          <el-menu-item index="/itinerary">我的行程</el-menu-item>
          <el-menu-item index="/login">登录</el-menu-item>
          <el-menu-item index="/register">注册</el-menu-item>
        </el-menu>
      </div>
    </el-header>
    <el-main>
      <router-view />
    </el-main>
  </el-container>
</template>

<script setup lang="ts">
</script>

<style scoped>
.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
h1 {
  margin: 0;
}
</style>
