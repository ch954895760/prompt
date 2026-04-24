<template>
  <div class="avatar-upload">
    <div class="avatar-preview" @click="handleClick">
      <img v-if="modelValue" :src="getAvatarUrl(modelValue)" alt="avatar" />
      <div v-else class="avatar-placeholder">
        <span class="avatar-letter">{{ username?.charAt(0)?.toUpperCase() || 'U' }}</span>
      </div>
      <div class="avatar-overlay">
        <el-icon><Camera /></el-icon>
      </div>
    </div>
    <input
      ref="fileInput"
      type="file"
      accept="image/*"
      style="display: none"
      @change="handleFileChange"
    />
    <p class="upload-tip">支持 jpg、png、gif 格式，大小不超过 5MB</p>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { User, Camera } from '@element-plus/icons-vue'
import { uploadAvatar } from '@/api/file'

const props = defineProps<{
  modelValue?: string
  username?: string
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: string): void
  (e: 'success', value: string): void
}>()

const fileInput = ref<HTMLInputElement>()
const uploading = ref(false)

const getAvatarUrl = (url: string) => {
  if (url.startsWith('http')) {
    return url
  }
  return `/api${url}`
}

const handleClick = () => {
  fileInput.value?.click()
}

const handleFileChange = async (event: Event) => {
  const target = event.target as HTMLInputElement
  const file = target.files?.[0]
  
  if (!file) return
  
  // 验证文件类型
  const validTypes = ['image/jpeg', 'image/jpg', 'image/png', 'image/gif', 'image/webp']
  if (!validTypes.includes(file.type)) {
    ElMessage.error('仅支持 jpg、jpeg、png、gif、webp 格式的图片')
    return
  }
  
  // 验证文件大小 (5MB)
  const maxSize = 5 * 1024 * 1024
  if (file.size > maxSize) {
    ElMessage.error('文件大小不能超过 5MB')
    return
  }
  
  try {
    uploading.value = true
    const response = await uploadAvatar(file)
    const url = typeof response === 'string' ? response : response as unknown as string
    emit('update:modelValue', url)
    emit('success', url)
    ElMessage.success('头像上传成功')
  } catch (error: any) {
    ElMessage.error(error.message || '上传失败')
  } finally {
    uploading.value = false
    // 清空 input 以便可以重复选择同一文件
    if (fileInput.value) {
      fileInput.value.value = ''
    }
  }
}
</script>

<style scoped lang="scss">
.avatar-upload {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.avatar-preview {
  width: 120px;
  height: 120px;
  border-radius: 50%;
  overflow: hidden;
  cursor: pointer;
  position: relative;
  background-color: #f0f2f5;
  border: 2px dashed #d9d9d9;
  transition: all 0.3s;
  
  &:hover {
    border-color: #409eff;
    
    .avatar-overlay {
      opacity: 1;
    }
  }
  
  img {
    width: 100%;
    height: 100%;
    object-fit: cover;
  }
}

.avatar-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #fb923c, #ea580c);
  
  .avatar-letter {
    font-size: 48px;
    font-weight: bold;
    color: white;
  }
}

.avatar-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: opacity 0.3s;
  
  .el-icon {
    font-size: 32px;
    color: #fff;
  }
}

.upload-tip {
  margin-top: 12px;
  font-size: 12px;
  color: #999;
}
</style>
