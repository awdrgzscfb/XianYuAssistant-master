<script setup lang="ts">
import { ref } from 'vue';
import { useRouter } from 'vue-router';
import { useDashboard } from './useDashboard';

const router = useRouter();
const { loading, stats, loadStatistics } = useDashboard();

// 组件挂载时加载数据
loadStatistics();

// 当前激活的步骤
const activeStep = ref(0);

// 快速开始步骤
const quickStartSteps = [
  {
    title: '1. 添加闲鱼账号',
    icon: '👤',
    description: '通过扫码登录添加您的闲鱼账号',
    route: '/accounts',
    details: [
      '点击左侧菜单"闲鱼账号"',
      '点击"扫码登录"按钮',
      '使用闲鱼APP扫描二维码',
      '等待登录成功，账号自动添加'
    ]
  },
  {
    title: '2. 启动WebSocket连接',
    icon: '🔗',
    description: '建立与闲鱼服务器的实时连接',
    route: '/connection',
    details: [
      '进入"连接管理"页面',
      '选择要连接的账号',
      '点击"启动连接"按钮',
      '等待连接成功，开始监听消息'
    ]
  },
  {
    title: '3. 同步商品信息',
    icon: '📦',
    description: '获取您的闲鱼商品列表',
    route: '/goods',
    details: [
      '进入"商品管理"页面',
      '选择已连接的账号',
      '点击"刷新商品"按钮',
      '等待商品同步完成'
    ]
  },
  {
    title: '4. 配置自动化功能',
    icon: '🤖',
    description: '设置自动发货和自动回复',
    route: '/auto-delivery',
    details: [
      '在商品列表中找到目标商品',
      '开启"自动发货"或"自动回复"',
      '配置发货内容或回复规则',
      '保存配置，自动化开始工作'
    ]
  }
];

// 功能特性
const features = [
  {
    icon: '👥',
    title: '多账号管理',
    description: '支持同时管理多个闲鱼账号，轻松切换',
    color: '#409eff'
  },
  {
    icon: '🚀',
    title: '自动发货',
    description: '买家付款后自动发送发货信息，节省时间',
    color: '#67c23a'
  },
  {
    icon: '💬',
    title: '自动回复',
    description: '智能匹配关键词，自动回复买家消息',
    color: '#e6a23c'
  },
  {
    icon: '📊',
    title: '数据统计',
    description: '实时查看商品、订单、消息等数据统计',
    color: '#f56c6c'
  },
  {
    icon: '🔄',
    title: 'Token自动刷新',
    description: '智能维护登录状态，无需频繁重新登录',
    color: '#909399'
  },
  {
    icon: '📜',
    title: '操作日志',
    description: '详细记录所有操作，方便追踪和排查',
    color: '#606266'
  }
];

// 常见问题
const faqs = [
  {
    question: '如何获取Cookie？',
    answer: '在连接管理页面，点击Cookie部分的"❓"帮助按钮，查看详细的获取步骤和示例图片。'
  },
  {
    question: 'WebSocket连接失败怎么办？',
    answer: '1. 检查Cookie是否有效；2. 尝试刷新Token；3. 如果提示需要滑块验证，访问 https://www.goofish.com/im 完成验证后手动更新Cookie和Token。'
  },
  {
    question: '自动发货什么时候触发？',
    answer: '当买家付款后，系统会自动检测到"已付款待发货"消息，并根据配置自动发送发货信息。'
  },
  {
    question: 'Token过期了怎么办？',
    answer: '系统会自动刷新Token（1.5-2.5小时刷新一次），也可以在连接管理页面手动刷新。'
  }
];

// 跳转到指定页面
const navigateTo = (route: string) => {
  router.push(route);
};
</script>

<template>
  <div class="dashboard-page">
    <!-- 统计卡片 -->
    <el-row :gutter="20" class="stats-row" v-loading="loading">
      <el-col :span="8">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <div class="stat-icon account-icon">👤</div>
            <div class="stat-info">
              <div class="stat-value">{{ stats.accountCount }}</div>
              <div class="stat-label">闲鱼账号</div>
            </div>
          </div>
        </el-card>
      </el-col>
      
      <el-col :span="8">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <div class="stat-icon goods-icon">📦</div>
            <div class="stat-info">
              <div class="stat-value">{{ stats.goodsCount }}</div>
              <div class="stat-label">商品总数</div>
            </div>
          </div>
        </el-card>
      </el-col>
      
      <el-col :span="8">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <div class="stat-icon online-icon">🔥</div>
            <div class="stat-info">
              <div class="stat-value">{{ stats.onlineGoodsCount }}</div>
              <div class="stat-label">在售商品</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 快速开始指南 -->
    <el-card class="guide-card" shadow="never">
      <template #header>
        <div class="card-header">
          <span class="card-title">🚀 快速开始指南</span>
          <span class="card-subtitle">4步完成系统配置，开启自动化之旅</span>
        </div>
      </template>
      
      <el-steps :active="activeStep" align-center class="steps-container">
        <el-step 
          v-for="(step, index) in quickStartSteps" 
          :key="index"
          :title="step.title"
          :icon="step.icon"
        />
      </el-steps>

      <div class="steps-content">
        <el-row :gutter="20">
          <el-col 
            :span="6" 
            v-for="(step, index) in quickStartSteps" 
            :key="index"
          >
            <el-card 
              shadow="hover" 
              class="step-card"
              :class="{ active: activeStep === index }"
              @click="activeStep = index"
            >
              <div class="step-icon">{{ step.icon }}</div>
              <h3 class="step-title">{{ step.title }}</h3>
              <p class="step-description">{{ step.description }}</p>
              <ul class="step-details">
                <li v-for="(detail, idx) in step.details" :key="idx">
                  {{ detail }}
                </li>
              </ul>
              <el-button 
                type="primary" 
                size="small" 
                @click.stop="navigateTo(step.route)"
                class="step-button"
              >
                前往操作 →
              </el-button>
            </el-card>
          </el-col>
        </el-row>
      </div>
    </el-card>

    <!-- 功能特性 -->
    <el-card class="features-card" shadow="never">
      <template #header>
        <div class="card-header">
          <span class="card-title">✨ 功能特性</span>
          <span class="card-subtitle">强大的自动化功能，提升您的工作效率</span>
        </div>
      </template>
      
      <el-row :gutter="20">
        <el-col :span="8" v-for="(feature, index) in features" :key="index">
          <el-card shadow="hover" class="feature-card">
            <div class="feature-icon" :style="{ color: feature.color }">
              {{ feature.icon }}
            </div>
            <h3 class="feature-title">{{ feature.title }}</h3>
            <p class="feature-description">{{ feature.description }}</p>
          </el-card>
        </el-col>
      </el-row>
    </el-card>

    <!-- 常见问题 -->
    <el-card class="faq-card" shadow="never">
      <template #header>
        <div class="card-header">
          <span class="card-title">❓ 常见问题</span>
          <span class="card-subtitle">快速解答您的疑问</span>
        </div>
      </template>
      
      <el-collapse accordion>
        <el-collapse-item 
          v-for="(faq, index) in faqs" 
          :key="index"
          :name="index"
        >
          <template #title>
            <span class="faq-question">{{ faq.question }}</span>
          </template>
          <div class="faq-answer">{{ faq.answer }}</div>
        </el-collapse-item>
      </el-collapse>
    </el-card>

    <!-- 底部提示 -->
    <el-card class="tips-card" shadow="never">
      <el-alert
        title="💡 温馨提示"
        type="info"
        :closable="false"
      >
        <ul class="tips-list">
          <li>系统会自动刷新Token，保持登录状态，无需频繁重新登录</li>
          <li>建议不要频繁启动/断开连接，避免触发人机验证</li>
          <li>自动发货和自动回复需要先启动WebSocket连接才能生效</li>
          <li>所有操作都会记录在"操作日志"中，方便追踪和排查问题</li>
        </ul>
      </el-alert>
    </el-card>
  </div>
</template>

<style scoped src="./dashboard.css"></style>
