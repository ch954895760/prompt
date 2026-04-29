import './assets/main.css'

import { createApp } from 'vue'
import { createPinia } from 'pinia'

import App from './App.vue'
import router from './router'
import ToastContainer from '@/components/ToastContainer.vue'
import { i18n } from '@/i18n'

const app = createApp(App)

app.use(createPinia())
app.use(router)
app.use(i18n)

app.component('ToastContainer', ToastContainer)

app.mount('#app')
