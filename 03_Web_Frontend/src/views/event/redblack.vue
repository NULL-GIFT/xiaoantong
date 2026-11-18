<template>
  <div class="governance-dashboard">
    <div class="bg-grid"></div>
    <div class="bg-gradient-mesh"></div>

    <div class="header-section">
      <div class="title-area">
        <h2 class="main-title">校园安全治理与态势感知舱</h2>
        <div class="sub-title">SECURITY GOVERNANCE & TREND ANALYSIS DASHBOARD</div>
      </div>
      <div class="filter-area">
        <div class="glass-radio">
          <span :class="{ active: timeRange === '30' }" @click="changeTime('30')">近 1 个月</span>
          <span :class="{ active: timeRange === '90' }" @click="changeTime('90')">近 1 季度</span>
        </div>
      </div>
    </div>

    <a-spin :spinning="loading" tip="正在进行环比数据推演..." size="large">
      
      <div class="kpi-row">
        <div class="kpi-card">
          <div class="kpi-title">期间安全事件总数</div>
          <div class="kpi-body">
            <div class="kpi-val">{{ globalMetrics.total }}</div>
            <div class="kpi-trend" :class="globalMetrics.totalTrend <= 0 ? 'trend-down' : 'trend-up'">
              <span class="t-icon">{{ globalMetrics.totalTrend >= 0 ? '↑' : '↓' }}</span>
              {{ Math.abs(globalMetrics.totalTrend) }}% <span>{{ compareText }}</span>
            </div>
          </div>
          <div class="kpi-sparkline" ref="spark1Ref"></div>
        </div>

        <div class="kpi-card">
          <div class="kpi-title">重点高风险区域数</div>
          <div class="kpi-body">
            <div class="kpi-val">{{ globalMetrics.riskZones }}</div>
            <div class="kpi-trend" :class="globalMetrics.zoneTrend <= 0 ? 'trend-down' : 'trend-up'">
              <span class="t-icon">{{ globalMetrics.zoneTrend >= 0 ? '↑' : '↓' }}</span>
              {{ Math.abs(globalMetrics.zoneTrend) }}% <span>{{ compareText }}</span>
            </div>
          </div>
          <div class="kpi-sparkline" ref="spark2Ref"></div>
        </div>

        <div class="kpi-card">
          <div class="kpi-title">校园综合安全指数 (0-100)</div>
          <div class="kpi-body">
            <div class="kpi-val text-safe">{{ globalMetrics.safetyIndex }}</div>
            <div class="kpi-trend" :class="globalMetrics.indexTrend >= 0 ? 'trend-down' : 'trend-up'">
              <span class="t-icon">{{ globalMetrics.indexTrend >= 0 ? '↑' : '↓' }}</span>
              {{ Math.abs(globalMetrics.indexTrend) }} 分 <span>较{{ periodText }}{{ globalMetrics.indexTrend >= 0 ? '提升' : '下降' }}</span>
            </div>
          </div>
          <div class="kpi-sparkline" ref="spark3Ref"></div>
        </div>
      </div>

      <a-row :gutter="24" class="main-content-row">
        
        <a-col :span="7" class="col-panel">
          
          <div class="glass-panel flex-auto">
            <div class="panel-head">
              <div class="h-title"><span class="h-dot dot-danger"></span> 重点高风险区域 (TOP 5)</div>
            </div>
            <div class="panel-body list-wrapper">
              
              <a-popover placement="right" overlayClassName="high-end-popover" v-for="(zone, index) in blackZones" :key="index">
                <template #content>
                  <div class="popover-box">
                    <div class="pop-header">
                      综合风险评估: 
                      <strong :class="zone.riskLevel === '高危' ? 'text-red' : 'text-orange'">
                        {{ zone.riskScore }}分 ({{ zone.riskLevel }})
                      </strong>
                    </div>
                    <div class="pop-divider"></div>
                    <div class="pop-list">
                      <div v-for="(d, i) in zone.details" :key="i" class="pop-item">
                        <span class="pop-dot" :style="{background: d.color}"></span>
                        <span class="pop-name">{{ d.name }}</span>
                        <span class="pop-val">{{ d.count }}起 ({{ d.percent }}%)</span>
                      </div>
                    </div>
                  </div>
                </template>
                
                <div class="data-item">
                  <div class="item-main">
                    <div class="item-name"><span class="rank-num">{{ index + 1 }}</span> {{ zone.name }}</div>
                    <div class="item-val-box">
                      <span class="i-val">{{ zone.current }}起</span>
                      <span class="i-trend" :class="zone.trend >= 0 ? 't-red' : 't-green'">
                        {{ zone.trend >= 0 ? '↑' : '↓' }}{{ Math.abs(zone.trend) }}%
                      </span>
                    </div>
                  </div>
                  <div class="item-sub">
                    <div class="composition-bar">
                      <div v-for="(d, i) in zone.details" :key="i" class="c-segment" :style="{ width: d.percent + '%', background: d.color }"></div>
                    </div>
                  </div>
                </div>
              </a-popover>
              
              <a-empty v-if="!blackZones.length" description="无高风险区域" />
            </div>
          </div>

          <div class="glass-panel mt-20 flex-auto">
            <div class="panel-head">
              <div class="h-title"><span class="h-dot dot-warn"></span> 高发频次事件 (TOP 5)</div>
            </div>
            <div class="panel-body list-wrapper">
              
              <a-popover placement="right" overlayClassName="high-end-popover" v-for="(evt, index) in blackEvents" :key="index">
                <template #content>
                  <div class="popover-box" style="width: 250px;">
                    <div class="pop-header">📍 空间分布追踪</div>
                    <div class="pop-divider"></div>
                    <div class="pop-ring-container">
                      <div class="css-donut" :style="getRingStyle(evt.locs)">
                        <div class="donut-hole"></div>
                      </div>
                      <div class="pop-list ring-legend">
                        <div v-for="(loc, i) in evt.locs" :key="i" class="pop-item">
                          <span class="pop-dot" :style="{background: loc.color}"></span>
                          <span class="pop-name">{{ loc.name }}</span>
                          <span class="pop-val">{{ loc.percent }}%</span>
                        </div>
                      </div>
                    </div>
                  </div>
                </template>

                <div class="data-item simplify">
                  <div class="item-main">
                    <div class="item-name"><span class="event-icon" :style="{background: evt.color}"></span> {{ evt.name }}</div>
                    <div class="item-val-box">
                      <span class="i-val">{{ evt.current }}起</span>
                      <span class="i-trend" :class="evt.trend >= 0 ? 't-red' : 't-green'">
                        {{ evt.trend >= 0 ? '↑' : '↓' }}{{ Math.abs(evt.trend) }}%
                      </span>
                    </div>
                  </div>
                </div>
              </a-popover>

              <a-empty v-if="!blackEvents.length" description="无异常事件" />
            </div>
          </div>

        </a-col>

        <a-col :span="10" class="col-panel">
          <div class="glass-panel full-height center-board">
            <div class="panel-head">
              <div class="h-title"><span class="h-dot dot-primary"></span>事件演变趋势图</div>
            </div>
            <div class="panel-body" style="padding-top: 10px;">
              <div ref="mainChartRef" style="width: 100%; height: 320px;"></div>
            </div>
            
            <div class="governance-logs mt-20">
              <div class="log-title">实时治理拦截链路</div>
              <div class="log-window">
                <div class="log-track" :style="{ transform: `translateY(-${logOffset}px)` }">
                  <div v-for="(log, i) in liveLogs" :key="i" class="g-log-item">
                    <div class="gl-time">{{ log.time }}</div>
                    <div class="gl-loc">{{ log.loc }}</div>
                    <div class="gl-action">触发规则</div>
                    <div class="gl-event" :style="{ color: log.color, border: `1px solid ${log.color}40`, background: `${log.color}10` }">{{ log.event }}</div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </a-col>

        <a-col :span="7" class="col-panel">
          
          <div class="glass-panel flex-auto">
            <div class="panel-head">
              <div class="h-title"><span class="h-dot dot-safe"></span> 区域治理成效榜 (降幅 TOP 5)</div>
            </div>
            <div class="panel-body list-wrapper">
              <div v-for="(zone, index) in redZones" :key="index" class="data-item highlight-green">
                <div class="item-main">
                  <div class="item-name"><span class="rank-num safe-rank">{{ index + 1 }}</span> {{ zone.name }}</div>
                  <div class="item-val-box">
                    <span class="i-trend t-green down-big">风险骤降 ↓{{ Math.abs(zone.trend) }}%</span>
                  </div>
                </div>
                <div class="item-sub text-gray">
                  上期 {{ zone.prev }} 起 → 本期控制在 <strong>{{ zone.current }}</strong> 起
                </div>
              </div>
              <a-empty v-if="!redZones.length" description="近期暂无明显下降区域" />
            </div>
          </div>

          <div class="glass-panel mt-20 flex-auto">
            <div class="panel-head">
              <div class="h-title"><span class="h-dot dot-gold"></span> 持续安全区域监控</div>
            </div>
            <div class="panel-body list-wrapper">
              <div v-for="(zone, index) in safeZones" :key="index" class="data-item safe-shield">
                <div class="item-main">
                  <div class="item-name"><span class="shield-icon">🛡️</span> {{ zone.name }}</div>
                  <div class="item-val-box">
                    <div class="safe-days">连续 <strong>{{ zone.days }}</strong> 天</div>
                  </div>
                </div>
                <div class="item-sub text-gray">设备在线率 100% | 运行状态稳定</div>
              </div>
              <a-empty v-if="!safeZones.length" description="无绝对安全区域" />
            </div>
          </div>

        </a-col>

      </a-row>
    </a-spin>
  </div>
</template>

<script setup>
import * as echarts from 'echarts';
import request from '@/utils/request';
import { getDevice } from '@/api/device.js';
import { ref, onMounted, onUnmounted, nextTick, computed } from 'vue';

const loading = ref(false);
const timeRange = ref('30');

const compareText = computed(() => timeRange.value === '7' ? '环比上周' : '环比上月');
const periodText = computed(() => timeRange.value === '7' ? '上周' : '上月');

// 数据源
const globalMetrics = ref({ total: 0, totalTrend: 0, riskZones: 0, zoneTrend: 0, safetyIndex: 100, indexTrend: 0 });
const blackZones = ref([]);
const blackEvents = ref([]);
const redZones = ref([]);
const safeZones = ref([]);
const liveLogs = ref([]);

const logOffset = ref(0);
let logTimer = null;

// ECharts 引用
const spark1Ref = ref(null);
const spark2Ref = ref(null);
const spark3Ref = ref(null);
const mainChartRef = ref(null);
let charts = [];

// 准备一个高颜值的动态色卡，给新上架的算法自动发颜色
const colorPalette = ['#ef4444', '#f97316', '#eab308', '#8b5cf6', '#0ea5e9', '#10b981', '#f43f5e', '#3b82f6', '#06b6d4'];
const locColors = ['#3b82f6', '#8b5cf6', '#f59e0b', '#10b981', '#f43f5e'];

const changeTime = (val) => { timeRange.value = val; fetchDashboardData(); };





// =========================================================================
// 🎨 核心修复：语义化智能色卡与权重判定（彻底消灭灰色，颜色符合人类直觉）
// =========================================================================
const dynamicSkillMeta = ref({});

const getSemanticMeta = (code, name) => {
  const lowerCode = (code || '').toLowerCase();
  const nameStr = name || lowerCode || '';
  
  // 1. 判定是否为“好人好事”或“日常行为”（非风险）
  const isGood = nameStr.includes('好') || nameStr.includes('签到') || nameStr.includes('打卡') || nameStr.includes('关灯') || nameStr.includes('正常') || lowerCode.includes('good');
  
  let color = '#3b82f6'; // 兜底：科技蓝
  let weight = isGood ? 0 : 5; // 权重

  // 2. 危险事件语义化赋色（固定颜色，绝不乱变）
  if (lowerCode.includes('fire') || nameStr.includes('火')) { color = '#ef4444'; weight = 10; } // 红色：火灾
  else if (lowerCode.includes('smoke') || nameStr.includes('烟')) { color = '#f97316'; weight = 8; } // 橙色：抽烟
  else if (lowerCode.includes('fall') || nameStr.includes('跌倒') || nameStr.includes('摔')) { color = '#f59e0b'; weight = 8; } // 黄色：跌倒
  else if (lowerCode.includes('viol') || nameStr.includes('暴力') || nameStr.includes('打架')) { color = '#8b5cf6'; weight = 9; } // 紫色：暴力
  else if (lowerCode.includes('occupy') || nameStr.includes('占')) { color = '#0ea5e9'; weight = 5; } // 亮蓝：消防通道占用
  
  // 3. 友好事件语义化赋色（给那些原本是灰色的日常行为上鲜艳的颜色）
  else if (nameStr.includes('垃圾') || nameStr.includes('卫生')) { color = '#10b981'; } // 翠绿：环保
  else if (nameStr.includes('灯') || nameStr.includes('电')) { color = '#facc15'; } // 明黄：节电
  else if (nameStr.includes('签到') || nameStr.includes('打卡')) { color = '#06b6d4'; } // 青色：考勤
  else if (nameStr.includes('读') || nameStr.includes('书')) { color = '#6366f1'; } // 靛蓝：学习
  else if (isGood) { color = '#14b8a6'; } // 水滴绿：兜底友好行为
  
  return { isRisk: !isGood, color, weight };
};

// 安全获取元数据：如果字典没配，当场智能推演颜色
const getSafeMeta = (code) => {
  if (dynamicSkillMeta.value[code]) return dynamicSkillMeta.value[code];
  const sem = getSemanticMeta(code, code);
  return { name: code, color: sem.color, weight: sem.weight, isRisk: sem.isRisk };
};

const fetchDashboardData = async () => {
  loading.value = true;
  try {
    const now = new Date();
    const days = parseInt(timeRange.value);
    const past2x = new Date(now.getTime() - days * 2 * 24 * 60 * 60 * 1000);
    const formatTime = (date) => date.toISOString().replace('T', ' ').substring(0, 19);
    
    const [deviceRes, skillRes, alarmRes] = await Promise.all([
      getDevice({ pageNum: 1, pageSize: 1000 }).catch(() => ({ data: [] })),
      request.get('/api/skill/list', { params: { pageNum: 1, pageSize: 500 } }).catch(() => ({ data: [] })), 
      request.get('/api/alarm/list', { 
        params: { pageNum: 1, pageSize: 10000, startTime: formatTime(past2x), endTime: formatTime(now), isFalseAlarm: 0 } 
      }).catch(() => ({ data: [] }))
    ]);

    const allDevices = (deviceRes.code === 200 || deviceRes.code === 0) ? (deviceRes.data?.list || deviceRes.data || []) : [];
    const rawSkills = (skillRes.code === 200 || skillRes.code === 0) ? (skillRes.data?.list || skillRes.data || []) : [];
    const allAlarms = (alarmRes.code === 200 || alarmRes.code === 0) ? (alarmRes.data?.list || alarmRes.data || []) : [];

    // 组装智能字典
    const newMeta = {};
    rawSkills.forEach((skill) => {
      const code = skill.skillCode || skill.skill_code;
      const name = skill.skillName || skill.skill_name;
      const semantic = getSemanticMeta(code, name);
      
      newMeta[code] = {
        name: name,
        color: semantic.color,
        isRisk: semantic.isRisk,
        weight: semantic.weight
      };
    });
    dynamicSkillMeta.value = newMeta;

    processData(allAlarms, allDevices, days);
  } catch (err) {
    console.error("加载大屏数据失败:", err);
  } finally {
    loading.value = false;
  }
};

const processData = (alarms, devices, days) => {
  const now = new Date();
  const past1x = new Date(now.getTime() - days * 24 * 60 * 60 * 1000);
  
  const deviceMap = {};
  devices.forEach(d => {
    const id = d.deviceId || d.device_id;
    const name = d.deviceName || d.device_name;
    if (id && name) deviceMap[id] = name;
  });

  const getCode = (a) => a.type || a.alarmType || a.alarm_type || 'unknown';
  const getTime = (a) => a.time || a.alarmTime || a.alarm_time || a.createTime || a.create_time;
  const getLoc = (a) => {
    const did = a.deviceId || a.device_id;
    return deviceMap[did] || a.location || '未知区域';
  };

  const riskAlarms = alarms.filter(a => {
    return getSafeMeta(getCode(a)).isRisk; 
  });

  const currAlarms = riskAlarms.filter(a => new Date(getTime(a)) >= past1x);
  const prevAlarms = riskAlarms.filter(a => new Date(getTime(a)) < past1x);

  const calcTrend = (curr, prev) => prev === 0 ? (curr > 0 ? 100 : 0) : Math.round(((curr - prev) / prev) * 100);

  const getZoneCount = (arr) => new Set(arr.map(a => getLoc(a))).size;
  const currZonesCount = getZoneCount(currAlarms);
  const prevZonesCount = getZoneCount(prevAlarms);
  
  const calcSafeIndex = (evts, zns) => Math.max(10, Math.min(100, Math.round(100 - (evts * 0.5 + zns * 2))));
  const currIndex = calcSafeIndex(currAlarms.length, currZonesCount);
  const prevIndex = calcSafeIndex(prevAlarms.length, prevZonesCount);

  globalMetrics.value = {
    total: currAlarms.length, totalTrend: calcTrend(currAlarms.length, prevAlarms.length),
    riskZones: currZonesCount, zoneTrend: calcTrend(currZonesCount, prevZonesCount),
    safetyIndex: currIndex, indexTrend: currIndex - prevIndex
  };

  const currZoneMap = {}; const prevZoneMap = {};
  const currEvtMap = {}; const prevEvtMap = {};

  currAlarms.forEach(a => {
    const loc = getLoc(a);
    const meta = getSafeMeta(getCode(a));
    const mName = meta.name;
    const mColor = meta.color;
    
    if (!currZoneMap[loc]) currZoneMap[loc] = { total: 0, weightScore: 0, details: {} };
    currZoneMap[loc].total++;
    currZoneMap[loc].weightScore += meta.weight; 
    if (!currZoneMap[loc].details[mName]) currZoneMap[loc].details[mName] = { count: 0, color: mColor };
    currZoneMap[loc].details[mName].count++;

    if (!currEvtMap[mName]) currEvtMap[mName] = { total: 0, color: mColor, locDistribution: {} };
    currEvtMap[mName].total++;
    currEvtMap[mName].locDistribution[loc] = (currEvtMap[mName].locDistribution[loc] || 0) + 1;
  });

  prevAlarms.forEach(a => {
    const loc = getLoc(a);
    const meta = getSafeMeta(getCode(a));
    if (!prevZoneMap[loc]) prevZoneMap[loc] = 0;
    prevZoneMap[loc]++;
    if (!prevEvtMap[meta.name]) prevEvtMap[meta.name] = 0;
    prevEvtMap[meta.name]++;
  });

  let maxWeight = 1;
  Object.values(currZoneMap).forEach(d => { if (d.weightScore > maxWeight) maxWeight = d.weightScore; });

  blackZones.value = Object.entries(currZoneMap)
    .map(([name, data]) => {
      const prev = prevZoneMap[name] || 0;
      const score = Math.min(100, Math.round((data.weightScore / maxWeight) * 60 + 40)); 
      const level = score >= 80 ? '高危' : '中危';
      const details = Object.entries(data.details).map(([dName, dVal]) => ({
        name: dName, count: dVal.count, color: dVal.color,
        percent: Math.max(Math.round((dVal.count / data.total) * 100), 2)
      })).sort((a,b)=>b.count - a.count);
      return { name, current: data.total, prev, trend: calcTrend(data.total, prev), details, riskScore: score, riskLevel: level };
    })
    .sort((a,b)=>b.current - a.current).slice(0, 5);

  blackEvents.value = Object.entries(currEvtMap)
    .map(([name, data]) => {
      const locList = Object.entries(data.locDistribution)
        .map(([lName, count], idx) => ({ 
          name: lName, count: count, 
          percent: Math.round((count / data.total) * 100),
          color: locColors[idx % locColors.length]
        })).sort((a,b)=>b.count - a.count).slice(0, 3);
      
      return {
        name, current: data.total, prev: prevEvtMap[name] || 0, trend: calcTrend(data.total, prevEvtMap[name] || 0), 
        color: data.color, locs: locList
      }
    })
    .sort((a,b)=>b.current - a.current).slice(0, 5);

  const governanceArr = [];
  Object.keys(prevZoneMap).forEach(loc => {
    const prev = prevZoneMap[loc];
    if (prev > 0) {
      const curr = currZoneMap[loc] ? currZoneMap[loc].total : 0;
      if (curr < prev) {
        governanceArr.push({ name: loc, current: curr, prev: prev, trend: calcTrend(curr, prev) });
      }
    }
  });
  redZones.value = governanceArr.sort((a,b)=> a.trend - b.trend).slice(0, 5);

  const badDeviceIds = new Set(currAlarms.map(a => a.deviceId || a.device_id).filter(Boolean));
  safeZones.value = devices
    .filter(d => (d.status === 1 || d.workStatus === '在线') && !badDeviceIds.has(d.deviceId))
    .map(d => {
      let dCount = days;
      if (d.createTime || d.create_time) {
        const createDate = new Date(d.createTime || d.create_time);
        const diffDays = Math.floor(Math.abs(now - createDate) / (1000 * 60 * 60 * 24));
        if (diffDays < dCount) dCount = diffDays;
      }
      return { name: d.deviceName || d.deviceId, days: dCount };
    }).sort((a,b)=>b.days - a.days).slice(0, 5);

  liveLogs.value = alarms.filter(a => new Date(getTime(a)) >= past1x).slice(0, 30).map(a => {
    const meta = getSafeMeta(getCode(a));
    return {
      time: new Date(getTime(a)).toLocaleTimeString('zh-CN', { hour12: false }),
      loc: getLoc(a),
      event: meta.name, color: meta.color
    };
  });

  startBarrage();

  nextTick(() => {
    drawSparks();
    drawStackedTrend(alarms.filter(a => new Date(getTime(a)) >= past1x), days); 
  });
};

const drawStackedTrend = (alarms, days) => {
  if (!mainChartRef.value) return;
  const chart = echarts.init(mainChartRef.value);
  
  const dateList = [];
  const now = new Date();
  for(let i = days - 1; i >= 0; i--) {
    const d = new Date(now.getTime() - i * 24 * 60 * 60 * 1000);
    dateList.push(`${d.getMonth()+1}/${d.getDate()}`);
  }

  const activeTypes = {};
  alarms.forEach(a => {
    const code = a.type || a.alarmType || a.alarm_type || 'unknown';
    const meta = getSafeMeta(code);
    if (!activeTypes[meta.name]) {
      activeTypes[meta.name] = { color: meta.color, data: new Array(days).fill(0) };
    }
  });

  alarms.forEach(a => {
    const timeStr = a.time || a.alarmTime || a.alarm_time || a.createTime || a.create_time;
    if (!timeStr) return;
    const d = new Date(timeStr.replace('T', ' ').replace(/-/g, '/'));
    if (!isNaN(d.getTime())) {
      const dateKey = `${d.getMonth()+1}/${d.getDate()}`;
      const dateIdx = dateList.indexOf(dateKey);
      if (dateIdx !== -1) {
        const code = a.type || a.alarmType || a.alarm_type || 'unknown';
        const meta = getSafeMeta(code);
        activeTypes[meta.name].data[dateIdx]++;
      }
    }
  });

  const series = Object.keys(activeTypes).map(typeName => ({
    name: typeName, type: 'line', stack: 'Total', smooth: 0.4, showSymbol: false,
    itemStyle: { color: activeTypes[typeName].color },
    lineStyle: { width: 1, color: '#fff' },
    areaStyle: { 
      opacity: 0.8, 
      color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
        { offset: 0, color: activeTypes[typeName].color },
        { offset: 1, color: activeTypes[typeName].color + '30' } 
      ]) 
    },
    data: activeTypes[typeName].data
  }));

  chart.setOption({
    tooltip: { 
      trigger: 'axis', backgroundColor: 'rgba(255,255,255,0.95)', borderColor: '#e2e8f0', padding: 12,
      textStyle: { color: '#0f172a' },
      formatter: function (params) {
        let total = 0; let detailsHtml = '';
        let sortedParams = [...params].sort((a,b) => b.value - a.value); 
        sortedParams.forEach(p => {
          if(p.value > 0) {
            total += p.value;
            detailsHtml += `<div style="display:flex;justify-content:space-between;font-size:12px;margin-bottom:4px;">
                <span style="display:flex;align-items:center;">
                  <span style="display:inline-block;width:8px;height:8px;border-radius:50%;background:${p.color};margin-right:6px;"></span>
                  ${p.seriesName}</span><span style="font-weight:bold;margin-left:20px;">${p.value} 起</span></div>`;
          }
        });
        return `<div style="margin-bottom:8px;padding-bottom:8px;border-bottom:1px solid #e2e8f0;">
            <div style="color:#64748b;font-size:13px;margin-bottom:4px;">${params[0].axisValue}</div>
            <div style="font-weight:bold;font-size:14px;">当日事件总计：<span style="color:#0f172a;font-size:16px;">${total}</span> 起</div>
          </div>` + (total > 0 ? detailsHtml : '<div style="color:#10b981;font-size:12px;">无记录</div>');
      }
    },
    legend: { data: Object.keys(activeTypes), top: 10, icon: 'circle', textStyle: { color: '#475569', fontSize: 12 } },
    grid: { left: '2%', right: '3%', bottom: '15%', top: '15%', containLabel: true },
    xAxis: { 
      type: 'category', boundaryGap: false, data: dateList, 
      axisLine: { show: false }, axisTick: { show: false }, 
      axisLabel: { color: '#94a3b8', margin: 12, interval: timeRange.value === '90' ? 6 : 'auto' } 
    },
    yAxis: { type: 'value', splitLine: { lineStyle: { type: 'dashed', color: '#f1f5f9' } }, axisLabel: { color: '#94a3b8' } },
    dataZoom: [
      { type: 'inside', start: timeRange.value === '90' ? 70 : 0, end: 100 },
      { type: 'slider', height: 12, bottom: 10, borderColor: 'transparent', backgroundColor: '#f1f5f9', fillerColor: 'rgba(14, 165, 233, 0.2)', handleSize: '100%', showDetail: false }
    ],
    series: series.length > 0 ? series : [{ type:'line', data:[] }]
  }, true); 
  charts.push(chart);
};






const getRingStyle = (locs) => {
  if (!locs || locs.length === 0) return {};
  let gradient = ''; let start = 0;
  locs.forEach(loc => {
      let end = start + loc.percent;
      gradient += `${loc.color} ${start}% ${end}%, `;
      start = end;
  });
  return { background: `conic-gradient(${gradient.slice(0, -2)})` };
};

const startBarrage = () => {
  if (logTimer) clearInterval(logTimer);
  logTimer = setInterval(() => {
    if (liveLogs.value.length > 4) {
      logOffset.value += 38;
      if (logOffset.value > 38 * 10) {
        logOffset.value = 0;
        liveLogs.value.push(liveLogs.value.shift());
      }
    }
  }, 2500);
};

const clearCharts = () => { charts.forEach(c => c.dispose()); charts = []; };

const drawSparks = () => {
  clearCharts();
  const makeSpark = (el, color, data) => {
    if (!el) return;
    const c = echarts.init(el);
    c.setOption({
      grid: { left: 0, right: 0, top: 5, bottom: 0 },
      xAxis: { type: 'category', show: false, data: [1,2,3,4,5,6,7] },
      yAxis: { type: 'value', show: false, min: 'dataMin' },
      series: [{ type: 'line', smooth: true, symbol: 'none', lineStyle: { width: 3, color }, data }]
    });
    charts.push(c);
  };
  makeSpark(spark1Ref.value, globalMetrics.value.totalTrend >= 0 ? '#ef4444' : '#10b981', [10,12,15,13,20,18,22]);
  makeSpark(spark2Ref.value, globalMetrics.value.zoneTrend >= 0 ? '#f97316' : '#10b981', [2,3,3,4,3,5,4]);
  makeSpark(spark3Ref.value, globalMetrics.value.indexTrend <= 0 ? '#ef4444' : '#10b981', [80,85,82,88,90,92,95]);
};



window.addEventListener('resize', () => charts.forEach(c => c.resize()));
onMounted(() => fetchDashboardData());
onUnmounted(() => { if (logTimer) clearInterval(logTimer); charts.forEach(c => c.dispose()); });
</script>

<style scoped>
/* ==================== 铂金科技白底座 (保留你的原版) ==================== */
.governance-dashboard {
  position: relative; padding: 24px; min-height: calc(100vh - 64px);
  background: #f8fafc; color: #0f172a; overflow: hidden;
  font-family: -apple-system, BlinkMacSystemFont, "SF Pro Text", "Segoe UI", Roboto, Helvetica, Arial, sans-serif;
}
.bg-grid { position: absolute; top: 0; left: 0; right: 0; bottom: 0; background-image: linear-gradient(#e2e8f0 1px, transparent 1px), linear-gradient(90deg, #e2e8f0 1px, transparent 1px); background-size: 32px 32px; opacity: 0.4; pointer-events: none; }
.bg-gradient-mesh { position: absolute; top: -20%; left: 10%; width: 80%; height: 80%; background: radial-gradient(ellipse at center, rgba(59, 130, 246, 0.08) 0%, rgba(248, 250, 252, 0) 70%); pointer-events: none; }

/* ==================== 顶部 Header ==================== */
.header-section { position: relative; z-index: 10; display: flex; justify-content: space-between; align-items: center; margin-bottom: 24px; }
.main-title { font-size: 26px; font-weight: 800; color: #0f172a; margin: 0; letter-spacing: 0.5px; }
.sub-title { font-size: 11px; color: #64748b; letter-spacing: 2px; margin-top: 4px; font-weight: 600; text-transform: uppercase;}
.glass-radio { display: flex; background: rgba(255,255,255,0.9); border: 1px solid #e2e8f0; border-radius: 8px; padding: 4px; box-shadow: 0 2px 4px rgba(15,23,42,0.02); }
.glass-radio span { padding: 6px 18px; font-size: 13px; font-weight: 600; color: #64748b; cursor: pointer; border-radius: 6px; transition: 0.2s; }
.glass-radio span.active { background: #ffffff; color: #0f172a; box-shadow: 0 1px 3px rgba(0,0,0,0.1); }

/* ==================== 核心 KPI 指标卡 ==================== */
.kpi-row { position: relative; z-index: 10; display: flex; gap: 24px; margin-bottom: 24px; }
.kpi-card { flex: 1; background: rgba(255,255,255,0.85); backdrop-filter: blur(20px); border: 1px solid rgba(255,255,255,1); border-radius: 16px; padding: 20px 24px; box-shadow: 0 10px 25px -5px rgba(15,23,42,0.05), 0 8px 10px -6px rgba(15,23,42,0.01); display: flex; flex-direction: column; position: relative; overflow: hidden;}
.kpi-title { font-size: 14px; font-weight: 600; color: #475569; margin-bottom: 12px; }
.kpi-body { display: flex; align-items: baseline; gap: 16px; z-index: 2; }
.kpi-val { font-size: 36px; font-weight: 900; color: #0f172a; font-family: "DIN Alternate", "Helvetica Neue", Arial; line-height: 1; }
.kpi-trend { font-size: 13px; font-weight: 700; display: flex; align-items: center; gap: 4px; padding: 4px 8px; border-radius: 6px; }
.kpi-trend span { font-size: 11px; font-weight: 500; color: #94a3b8; margin-left: 2px;}
.trend-up { color: #ef4444; background: #fef2f2; }
.trend-down { color: #10b981; background: #ecfdf5; }
.text-safe { color: #059669; }
.kpi-sparkline { position: absolute; bottom: 10px; right: 20px; width: 100px; height: 40px; z-index: 1; opacity: 0.6; }

/* ==================== 毛玻璃面板通用 ==================== */
.main-content-row { position: relative; z-index: 10; align-items: stretch; }
.col-panel { display: flex; flex-direction: column; }
.flex-auto { flex: 1; display: flex; flex-direction: column; }
.full-height { height: 100%; }
.mt-20 { margin-top: 24px; }
.glass-panel { background: rgba(255,255,255,0.7); backdrop-filter: blur(24px); border: 1px solid #ffffff; border-radius: 16px; padding: 20px; box-shadow: 0 4px 6px -1px rgba(0,0,0,0.02), 0 2px 4px -2px rgba(0,0,0,0.02); }
.panel-head { margin-bottom: 16px; }
.h-title { font-size: 16px; font-weight: 700; color: #1e293b; display: flex; align-items: center; }
.h-dot { width: 10px; height: 10px; border-radius: 50%; margin-right: 10px; box-shadow: inset 0 0 0 2px #fff; }
.dot-danger { background: #ef4444; box-shadow: 0 0 0 2px #fef2f2, inset 0 0 0 2px #fff; }
.dot-warn { background: #f59e0b; box-shadow: 0 0 0 2px #fffbeb, inset 0 0 0 2px #fff; }
.dot-primary { background: #3b82f6; box-shadow: 0 0 0 2px #eff6ff, inset 0 0 0 2px #fff; }
.dot-safe { background: #10b981; box-shadow: 0 0 0 2px #ecfdf5, inset 0 0 0 2px #fff; }
.dot-gold { background: #eab308; box-shadow: 0 0 0 2px #fef9c3, inset 0 0 0 2px #fff; }
.panel-body { flex: 1; display: flex; flex-direction: column; }

/* ==================== 统一列表样式 ==================== */
.list-wrapper { gap: 12px; }
.data-item { background: #ffffff; border: 1px solid #f1f5f9; border-radius: 10px; padding: 12px 16px; transition: 0.2s; box-shadow: 0 1px 2px rgba(15,23,42,0.01);}
.data-item:hover { box-shadow: 0 4px 12px rgba(15,23,42,0.05); transform: translateY(-1px); cursor: pointer;}
.item-main { display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px; }
.simplify .item-main { margin-bottom: 0; }
.item-name { font-size: 14px; font-weight: 600; color: #334155; display: flex; align-items: center; }
.rank-num { width: 22px; height: 22px; border-radius: 6px; background: #e2e8f0; color: #64748b; display: flex; justify-content: center; align-items: center; font-size: 12px; font-weight: 800; margin-right: 12px; }
.data-item:nth-child(1) .rank-num { background: #fee2e2; color: #ef4444; }
.data-item:nth-child(2) .rank-num { background: #ffedd5; color: #f97316; }
.data-item:nth-child(3) .rank-num { background: #fef3c7; color: #f59e0b; }
.safe-rank { background: #d1fae5 !important; color: #059669 !important; }
.item-val-box { display: flex; align-items: baseline; gap: 12px; }
.i-val { font-size: 16px; font-weight: 800; color: #0f172a; font-family: "DIN Alternate", Arial;}
.i-trend { font-size: 12px; font-weight: 700; padding: 2px 6px; border-radius: 4px; }
.t-red { color: #ef4444; background: #fef2f2; }
.t-green { color: #10b981; background: #ecfdf5; }
.text-red { color: #ef4444; }
.text-orange { color: #f59e0b; }

/* 细分成分条 */
.item-sub { padding-left: 34px; }
.composition-bar { height: 6px; background: #f1f5f9; border-radius: 3px; display: flex; overflow: hidden; }
.c-segment { height: 100%; transition: width 0.5s; border-right: 1px solid #fff; }
.c-segment:last-child { border-right: none; }
.event-icon { width: 8px; height: 8px; border-radius: 50%; margin-right: 12px; }

/* ==================== 治理大盘与拦截链路 ==================== */
.center-board { padding: 24px; }
.governance-logs { background: #f8fafc; border: 1px solid #e2e8f0; border-radius: 12px; padding: 16px; }
.log-title { font-size: 13px; font-weight: 700; color: #475569; margin-bottom: 12px; }
.log-window { height: 160px; overflow: hidden; position: relative; mask-image: linear-gradient(to bottom, transparent, black 15%, black 85%, transparent); -webkit-mask-image: linear-gradient(to bottom, transparent, black 15%, black 85%, transparent); }
.log-track { transition: transform 0.4s cubic-bezier(0.25, 1, 0.5, 1); }
.g-log-item { display: flex; align-items: center; font-size: 13px; padding: 10px; margin-bottom: 6px; background: #ffffff; border: 1px solid #f1f5f9; border-radius: 8px; box-shadow: 0 1px 2px rgba(0,0,0,0.01);}
.gl-time { color: #94a3b8; font-family: monospace; width: 75px; font-size: 12px;}
.gl-loc { flex: 1; font-weight: 600; color: #334155; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; padding-right: 10px;}
.gl-action { color: #64748b; font-size: 12px; margin-right: 12px;}
.gl-event { font-weight: 700; font-size: 12px; padding: 2px 8px; border-radius: 6px; }

/* ==================== 右侧特化样式 ==================== */
.highlight-green { border: 1px solid #a7f3d0; background: linear-gradient(to right, #f0fdf4, #ffffff); }
.down-big { background: transparent; font-size: 14px; }
.text-gray { font-size: 12px; color: #64748b; margin-top: 6px; }
.safe-shield { border: 1px solid #fde68a; background: linear-gradient(to right, #fffbeb, #ffffff); }
.shield-icon { font-size: 18px; margin-right: 10px; filter: drop-shadow(0 2px 4px rgba(245,158,11,0.3)); }
.safe-days { font-size: 13px; color: #b45309; }
.safe-days strong { font-size: 20px; font-weight: 900; color: #d97706; font-family: "DIN Alternate", Arial; margin: 0 4px; }

/* ==================== 💡 新增：高级气泡卡片(Popover)统一样式 ==================== */
</style>
<style>
.high-end-popover .ant-popover-inner {
  border-radius: 12px;
  box-shadow: 0 10px 30px rgba(15,23,42,0.08);
  padding: 16px;
  border: 1px solid #e2e8f0;
}
.popover-box { min-width: 200px; }
.pop-header { font-size: 14px; font-weight: 600; color: #0f172a; display: flex; justify-content: space-between;}
.pop-divider { width: 100%; height: 1px; background: #f1f5f9; margin: 10px 0; }
.pop-list { display: flex; flex-direction: column; gap: 8px; }
.pop-item { display: flex; align-items: center; font-size: 12px; color: #475569; }
.pop-dot { width: 8px; height: 8px; border-radius: 50%; margin-right: 8px; }
.pop-name { flex: 1; font-weight: 500; }
.pop-val { font-weight: 700; color: #0f172a; }

/* 纯 CSS 极速迷你环形图 */
.pop-ring-container { display: flex; align-items: center; }
.css-donut { 
  width: 70px; height: 70px; border-radius: 50%; 
  display: flex; justify-content: center; align-items: center; 
  margin-right: 16px; box-shadow: 0 2px 8px rgba(0,0,0,0.05);
}
.donut-hole { width: 44px; height: 44px; background: #fff; border-radius: 50%; }
.ring-legend { flex: 1; }
</style>