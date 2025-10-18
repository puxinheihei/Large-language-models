import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    { path: '/', redirect: '/planner' },
    { path: '/planner', name: 'planner', component: () => import('@/views/Planner.vue') },
    { path: '/budget', name: 'budget', component: () => import('@/views/Budget.vue') },
    { path: '/itinerary', name: 'itinerary', component: () => import('@/views/Itinerary.vue') },
    { path: '/login', name: 'login', component: () => import('@/views/Login.vue') },
    { path: '/register', name: 'register', component: () => import('@/views/Register.vue') },
  ],
})

export default router
