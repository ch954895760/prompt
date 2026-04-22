<script setup lang="ts">
import { RouterView, useRoute } from 'vue-router'
import { computed } from 'vue'

const route = useRoute()
const isLoginPage = computed(() => route.path === '/login')
</script>

<template>
  <div class="min-h-screen">
    <RouterView />
  </div>
</template>
