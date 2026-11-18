<template>
  <div class="fui-dashboard">
    <div class="fui-bg-grid"></div>
    <div class="fui-bg-glow"></div>

    <header class="fui-header">
      <div class="header-left">
        <div class="time-box DIN">{{ currentTime }}</div>
      </div>
      <div class="header-center">
        <h1>校安通 · 数据大屏</h1>
        <p>Campus Safety Hub · Data Dashboard</p>
      </div>
      <div class="header-right">
        <div class="status-box">SYSTEM CORE: <span class="text-cyan blink">ONLINE</span></div>
      </div>
    </header>

    <div class="fui-main">
      <div class="fui-side-panel">
        
        <div class="fui-panel">
          <div class="panel-title"><span class="arrows">>>>></span> 全域感知算力大盘 <span class="arrows-right"><<<<</span></div>
          <div class="asset-grid">
            <div class="asset-item">
              <span class="label">监控摄像总数</span>
              <span class="val DIN text-cyan">{{ stats.totalDevices }}</span>
            </div>
            <div class="asset-item">
              <span class="label">当前在线设备</span>
              <span class="val DIN text-safe">{{ stats.onlineDevices }}</span>
            </div>
            <div class="asset-item">
              <span class="label">启用AI技能数</span>
              <span class="val DIN text-orange">{{ stats.activeSkills }}</span>
            </div>
            <div class="asset-item">
              <span class="label">累计发现事件</span>
              <span class="val DIN" style="color:#b37feb">{{ stats.historyTotal }}</span>
            </div>
          </div>
        </div>

        <div class="fui-panel flex-1">
          <div class="panel-title"><span class="arrows">>>>></span> 近7日拦截与发现态势 <span class="arrows-right"><<<<</span></div>
          <div class="chart-box" ref="trendChartRef"></div>
        </div>

        <div class="fui-panel flex-1">
          <div class="panel-title"><span class="arrows">>>>></span> 校园安全事件类型分布 <span class="arrows-right"><<<<</span></div>
          <div class="chart-box" ref="radarChartRef"></div>
        </div>

      </div>

      <div class="fui-center-panel">
        <div class="center-top-stats">
          <div class="stat-block">
            <div class="label">今日智能拦截 (黑榜)</div>
            <div class="val DIN text-orange">{{ stats.todayBad }}</div>
          </div>
          <div class="stat-block">
            <div class="label">今日正向行为发现 (红榜)</div>
            <div class="val DIN text-safe">+{{ stats.todayGood }}</div>
          </div>
        </div>

        <div class="stage-3d-container">
          <div class="orbit-system">
            
            <div class="giant-core">
              <div class="core-glow"></div>
              <div class="core-sphere"></div>
              <div class="core-data">
                <span class="DIN text-cyan">AI BRAIN</span>
                <p class="DIN">{{ stats.todayBad + stats.todayGood }}</p>
              </div>
            </div>

            <div class="orbit-ring">
              <div 
                v-for="(node, index) in realVideoNodes" 
                :key="node.deviceId || index"
                class="orbit-node"
                :style="{ '--angle': `${(360 / realVideoNodes.length) * index}deg` }"
              >
                <div class="hologram-card counter-spin" @click="expandVideo(node, index)">
                  <div class="card-glass">
                    <div class="holo-header">
                      <span class="h-name">{{ node.deviceName || '边缘节点' }}</span>
                      <span class="h-status">LIVE</span>
                    </div>
                    <div class="holo-video">
                      <img :src="getSurveillanceImageUrl(node, index)" @error="handleImgError" />
                      <div class="scan-line"></div>
                    </div>
                    <i class="angle angle-tl"></i><i class="angle angle-tr"></i>
                    <i class="angle angle-bl"></i><i class="angle angle-br"></i>
                  </div>
                </div>
              </div>
            </div>
            
            <div class="orbit-ring-inner"></div>

          </div>
        </div>
      </div>

      <div class="fui-side-panel">
        
        <div class="fui-panel flex-1">
          <div class="panel-title"><span class="arrows">>>>></span> 行为画像红黑榜 <span class="arrows-right"><<<<</span></div>
          <div class="chart-box" ref="redBlackChartRef"></div>
        </div>

        <div class="fui-panel flex-1-5">
          <div class="panel-title"><span class="arrows">>>>></span> 实时监控告警日志 <span class="arrows-right"><<<<</span></div>
          <div class="log-window">
            <div class="log-track" :style="{ transform: `translateY(-${logOffset}px)` }">
              <div v-for="(log, i) in liveLogs" :key="i" class="log-item">
                <div class="log-time DIN">{{ log.time }}</div>
                <div class="log-loc">{{ log.loc }}</div>
                <div class="log-type" :style="{ color: log.color, border: `1px solid ${log.color}` }">
                  {{ log.icon }} {{ log.event }}
                </div>
              </div>
            </div>
          </div>
        </div>

      </div>
    </div>

    <a-modal v-model:open="videoModalVisible" :title="(currentVideo?.deviceName || '边缘感知节点') + ' - 实时串流'" :footer="null" width="800px" centered destroyOnClose :wrapClassName="'fui-modal'">
      <div class="expanded-video-container">
        
        <img 
          v-if="getStreamUrl(currentVideo)" 
          :src="getStreamUrl(currentVideo) + '&t=' + modalOpenTime" 
          class="real-video-stream-large" 
          alt="LIVE STREAM"
          @error="handleImgError"
        />
        
        <div v-else class="real-video-stream-large video-placeholder" style="background: #050b14; display: flex; flex-direction: column; justify-content: center; align-items: center; width: 100%; height: 100%;">
          <p class="DIN" style="color: #ff4d4f; font-size: 28px; letter-spacing: 2px; font-weight: bold; margin-bottom: 10px;">⚠️ SIGNAL LOST</p>
          <span class="DIN text-cyan" style="font-size: 16px;">设备离线或边缘端未推流</span>
        </div>

        <div class="signal-text DIN">🔴 FEDERATED INFERENCING</div>
      </div>
      <div class="video-stats mt-4">
        <span><strong>节点 IP:</strong> <span class="text-cyan">{{ currentVideo?.deviceIp || '192.168.1.10X' }}</span></span>
        <span><strong>节点 SN:</strong> <span class="text-cyan">{{ currentVideo?.deviceId || 'BOX_00X' }}</span></span>
        <span class="text-safe"><strong>AI 延迟:</strong> 12ms</span>
      </div>
    </a-modal>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onUnmounted, nextTick } from 'vue';
import * as echarts from 'echarts';
import request from '@/utils/request';
import { getDevice } from '@/api/device.js';

// ================== 核心控制与状态 ==================
const currentTime = ref('');
let clockTimer = null;

const stats = reactive({
  totalDevices: 0, onlineDevices: 0, activeSkills: 0, historyTotal: 0,
  todayGood: 0, todayBad: 0
});

const realVideoNodes = ref([]);
const liveLogs = ref([]);
const dynamicSkillMeta = ref({}); 

let charts = [];
const radarChartRef = ref(null);
const redBlackChartRef = ref(null);
const trendChartRef = ref(null);

const videoModalVisible = ref(false);
const currentVideo = ref(null);
const currentVideoIndex = ref(0);

const updateTime = () => { currentTime.value = new Date().toLocaleString('zh-CN', { hour12: false }); };


const modalOpenTime = ref(0);

const expandVideo = (node, index) => { 
  currentVideo.value = node; 
  currentVideoIndex.value = index; 
  modalOpenTime.value = new Date().getTime(); // 每次点击都生成新时间戳
  videoModalVisible.value = true; 
};

// 读取 .env 中的视频地址
const videoFeedUrl = import.meta.env.VITE_APP_VIDEO_URL;

// ================= 🌟 真实视频流接入 (大屏专属版) =================

// 1. 处理中心弹窗的超清视频流
const getStreamUrl = (device) => {
  // 大屏里传进来的是 device 实体对象，直接做防空判断
  if (!device) return '';

  // 提取设备唯一标识 "box_001"
  const deviceId = device.deviceId || device.device_id;
  if (!deviceId) return '';

  // 🚨 强制使用 .env 中的正确物理机 IP (192.168.192.195)
  const baseUrl = import.meta.env.VITE_APP_VIDEO_URL; 
  
  // 返回基础拉流地址 (时间戳已经在 HTML 里用 &t= 完美拼接了)
  return `${baseUrl}?device_id=${deviceId}`;
};

// 2. 处理外围 3D 轨道上悬浮的节点小视频流
const getSurveillanceImageUrl = (node, index) => {
  if (!node) return '';
  
  const deviceId = node.deviceId || node.device_id;
  const baseUrl = import.meta.env.VITE_APP_VIDEO_URL;
  
  // 🚨 废弃旧版写死的“主教学楼大厅”限制！
  // 现在让 3D 轨道上所有的在线节点，都自动通过自己的 device_id 去拉取真实的视频流！
  if (deviceId) {
    return `${baseUrl}?device_id=${deviceId}&small=${index}`; 
  }
  
  // 兜底：如果设备没有 ID，才显示丢失信号
  return "data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='300' height='200'%3E%3Crect width='300' height='200' fill='%23050b14'/%3E%3Ctext x='150' y='100' fill='%23ff4d4f' font-size='24' font-weight='bold' text-anchor='middle'%3ESIGNAL LOST%3C/text%3E%3C/svg%3E";
};



const handleImgError = (e) => { 
  e.target.src = "data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='300' height='200'%3E%3Crect width='300' height='200' fill='%23050b14'/%3E%3Ctext x='150' y='100' fill='%23ff4d4f' font-size='24' font-weight='bold' text-anchor='middle'%3EOFFLINE%3C/text%3E%3C/svg%3E"; 
};


// 💡 强力修复：超级解构函数，防止后端分页格式变动导致白屏
const extractArray = (res) => {
  if (!res || (res.code !== 200 && res.code !== 0)) return [];
  const data = res.data;
  if (!data) return [];
  if (Array.isArray(data)) return data;
  if (Array.isArray(data.records)) return data.records; 
  if (Array.isArray(data.list)) return data.list;       
  if (Array.isArray(data.rows)) return data.rows;
  return [];
};

// ================== 🌟 超级兜底字典 (防后端掉链子) ==================
const fallbackDict = {
  'fire': '烟火检测',
  'smoke': '违规吸烟',
  'fall': '人员跌倒',
  'perimeter': '翻越围墙',
  'good_deed_trash': '捡拾垃圾',
  'good_deed_bike': '扶起车辆',
  'good_deed_help': '帮扶同伴'
};

// ================== 核心数据拉取 ==================
const fetchDashboardData = async () => {
  try {
    const now = new Date();
    const past7Days = new Date(now.getTime() - 7 * 24 * 60 * 60 * 1000);
    const formatTime = (date) => {
       const tzOffset = date.getTimezoneOffset() * 60000;
       return new Date(date.getTime() - tzOffset).toISOString().replace('T', ' ').substring(0, 19);
    };

    const [deviceRes, skillRes, alarmRes] = await Promise.all([
      getDevice({ pageNum: 1, pageSize: 100 }),
      request.get('/api/skill/list', { params: { pageNum: 1, pageSize: 500 } }).catch(() => ({ data: [] })),
      request.get('/api/alarm/list', { 
        params: { pageNum: 1, pageSize: 5000, startTime: formatTime(past7Days), endTime: formatTime(now), isFalseAlarm: 0 } 
      })
    ]);

    const allDevices = extractArray(deviceRes);
    const rawSkills = extractArray(skillRes);
    const allAlarms = extractArray(alarmRes);
    
    stats.totalDevices = allDevices.length;
    stats.onlineDevices = allDevices.filter(d => d.status === 1 || d.workStatus === '在线').length;
    stats.activeSkills = rawSkills.length || 6; // 兜底6个技能
    realVideoNodes.value = allDevices.filter(d => d.status === 1 || d.workStatus === '在线').slice(0, 5);

    // 💡 拨乱反正：不再写死任何中文名称，只做 UI（颜色、图标）的点缀！
    const newMeta = {};
    rawSkills.forEach(skill => {
      const code = skill.skillCode || skill.skill_code;
      const isGood = code.includes('good_deed');
      
      let icon = isGood ? '✨' : '⚠️';
      let color = isGood ? '#00f0ff' : '#ff4d4f'; 
      
      if(code.includes('fire')) { icon = '🔥'; color = '#ff4d4f'; }
      else if(code.includes('fall')) { icon = '⚠️'; color = '#ff9900'; } 
      else if(code.includes('perimeter')) { icon = '🚧'; color = '#ff4d4f'; } 
      else if(code.includes('violence')) { icon = '💢'; color = '#ff4d4f'; }
      else if(code.includes('smoke')) { icon = '💨'; color = '#fa8c16'; }
      else if(code.includes('occupy')) { icon = '🚗'; color = '#fa8c16'; }
      else if(code.includes('trash')) { icon = '♻️'; color = '#52c41a'; } 
      else if(code.includes('bike')) { icon = '🚲'; color = '#00f0ff'; }  

      newMeta[code] = { 
        // 🌟 核心：名字百分之百从数据库读取！
        name: skill.skillName || skill.skill_name || code, 
        icon, 
        color, 
        isGood 
      };
    });
    dynamicSkillMeta.value = newMeta;

    processData(allAlarms, allDevices);
  } catch (error) {
    console.error("加载大屏真实数据失败:", error);
    processData([], []); 
  }
};

// ================== 数据推演与加工 ==================
const processData = (alarms, allDevices) => {
  stats.historyTotal = alarms.length;
  const todayStr = new Date().toISOString().substring(0, 10);
  const todayAlarms = alarms.filter(a => (a.time || a.alarm_time || a.alarmTime || '').startsWith(todayStr));
  
  stats.todayGood = 0; stats.todayBad = 0;
  todayAlarms.forEach(a => {
    const code = a.alarm_type || a.alarmType || a.type;
    const meta = dynamicSkillMeta.value[code] || { isGood: code.includes('good_deed') };
    if(meta.isGood) stats.todayGood++; else stats.todayBad++;
  });

  const deviceNameMap = {};
  allDevices.forEach(d => {
    const id = d.deviceId || d.device_id;
    if (id) deviceNameMap[id] = d.deviceName || d.device_name || '未命名节点';
  });

  const sortedAlarms = [...alarms].sort((a, b) => {
    const timeA = new Date(a.alarm_time || a.alarmTime || a.time || 0).getTime();
    const timeB = new Date(b.alarm_time || b.alarmTime || b.time || 0).getTime();
    return timeB - timeA; 
  });

  liveLogs.value = sortedAlarms.slice(0, 30).map(a => {
    const code = a.alarm_type || a.alarmType || a.type;
    // 💡 修复：终极拦截！如果 dynamicSkillMeta 里也没有，硬用 fallbackDict 兜底！
    const meta = dynamicSkillMeta.value[code] || { 
      name: fallbackDict[code] || code, 
      icon: code.includes('good_deed') ? '✨' : '⚠️', 
      color: code.includes('good_deed') ? '#00f0ff' : '#ff4d4f' 
    };
    
    const targetId = a.device_id || a.deviceId;
    const realLocation = (targetId && deviceNameMap[targetId]) 
                          ? deviceNameMap[targetId] 
                          : (a.location || a.device_name || a.deviceName || '感知节点');

    return {
      time: new Date(a.alarm_time || a.alarmTime || a.time || new Date()).toLocaleTimeString('zh-CN', { hour12: false }),
      loc: realLocation, 
      event: meta.name, // 这里绝对是纯正中文了
      icon: meta.icon, color: meta.color
    };
  });
  startBarrage();

  const trendData = { dates: [], good: [], bad: [] };
  for(let i = 6; i >= 0; i--) {
    const d = new Date(new Date().getTime() - i * 24 * 60 * 60 * 1000);
    trendData.dates.push(`${d.getMonth()+1}/${d.getDate()}`);
    trendData.good.push(0); trendData.bad.push(0);
  }

  const redBlackData = { good: {}, bad: {} };
  const allEventsCount = {};

  alarms.forEach(a => {
    const code = a.alarm_type || a.alarmType || a.type;
    // 💡 修复：同上，给雷达图和红黑榜的统计也加上死兜底！
    const meta = dynamicSkillMeta.value[code] || { 
      name: fallbackDict[code] || code, 
      isGood: code.includes('good_deed') 
    };
    
    if (meta.isGood) redBlackData.good[meta.name] = (redBlackData.good[meta.name] || 0) + 1;
    else redBlackData.bad[meta.name] = (redBlackData.bad[meta.name] || 0) + 1;

    allEventsCount[meta.name] = (allEventsCount[meta.name] || 0) + 1;

    const timeStr = a.alarm_time || a.alarmTime || a.time || '';
    if (timeStr && typeof timeStr === 'string' && timeStr.length >= 10) {
      const dKey = `${timeStr.substring(5, 7).replace(/^0/,'')}/${timeStr.substring(8, 10).replace(/^0/,'')}`;
      const idx = trendData.dates.indexOf(dKey);
      if(idx !== -1) {
        if(meta.isGood) trendData.good[idx]++; else trendData.bad[idx]++;
      }
    }
  });

  nextTick(() => {
    drawRadarChart(redBlackData, allEventsCount);
    drawRedBlackChart(redBlackData);
    drawTrendChart(trendData);
  });
};

// ================== ECharts 渲染 ==================
const drawRadarChart = (data, allEventsCount) => {
  if(!radarChartRef.value) return;
  const chart = echarts.init(radarChartRef.value);
  
  let sortedEvents = Object.entries(allEventsCount).sort((a,b) => b[1] - a[1]).map(e => e[0]);
  if(sortedEvents.length === 0) sortedEvents = ['安全巡检', '环境监测', '行为分析', '设备状态', '人员通行', '车辆管控']; 
  while(sortedEvents.length < 3) sortedEvents.push('其他特征'); 
  
  const topEvents = sortedEvents.slice(0, 6);
  const maxVal = Math.max(...topEvents.map(name => (data.good[name] || data.bad[name] || 0))) || 10;
  const indicator = topEvents.map(name => ({ name, max: maxVal * 1.2 }));
  const vals = topEvents.map(name => (data.good[name] || data.bad[name] || 0));

  chart.setOption({
    tooltip: {
      appendToBody: true, 
      backgroundColor: 'rgba(0,0,0,0.8)', 
      textStyle: { color: '#fff' }
    },
    radar: {
      // 💡 修复 2：将雷达图缩小至 55%，防止外圈文字被挤没！
      radius: '55%', 
      center: ['50%', '55%'], // 将整体稍微往下挪一点点居中
      nameGap: 10, // 文字离雷达图的距离
      indicator: indicator, 
      shape: 'polygon', 
      splitNumber: 4,
      axisName: { color: '#00f0ff', fontSize: 11, fontWeight: 'bold' },
      splitLine: { lineStyle: { color: ['rgba(0,240,255,0.1)', 'rgba(0,240,255,0.2)', 'rgba(0,240,255,0.4)', 'rgba(0,240,255,0.6)'].reverse() } },
      splitArea: { show: false }, 
      axisLine: { lineStyle: { color: 'rgba(0,240,255,0.3)' } }
    },
    series: [{
      name: '算法触发频次', type: 'radar', data: [{ value: vals, name: '感知数据' }],
      symbol: 'circle', symbolSize: 6, itemStyle: { color: '#00f0ff' },
      areaStyle: { color: 'rgba(0,240,255,0.3)' },
      lineStyle: { color: '#00f0ff', width: 2, shadowBlur: 10, shadowColor: '#00f0ff' }
    }]
  });
  charts.push(chart);
};



const drawRedBlackChart = (data) => {
  if(!redBlackChartRef.value) return;
  const chart = echarts.init(redBlackChartRef.value);
  const badNames = Object.keys(data.bad); const badVals = Object.values(data.bad);
  const goodNames = Object.keys(data.good); const goodVals = Object.values(data.good);

  chart.setOption({
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' }, backgroundColor: 'rgba(0,0,0,0.8)', textStyle: { color: '#fff' }, formatter: (p) => `${p[0].name}: ${Math.abs(p[0].value)} 起` },
    grid: { left: '3%', right: '3%', top: '5%', bottom: '5%', containLabel: true },
    xAxis: { type: 'value', show: false },
    yAxis: { type: 'category', data: [...badNames, ...goodNames], axisLine: { show: false }, axisTick: { show: false }, axisLabel: { color: '#fff', fontSize: 11, fontWeight: 'bold' } },
    series: [{
      type: 'bar', barWidth: 12, itemStyle: { borderRadius: 4 },
      label: { show: true, position: 'inside', formatter: (p) => Math.abs(p.value), color: '#fff', fontSize: 10 },
      data: [
        ...badVals.map(v => ({ value: -v, itemStyle: { color: '#ff4d4f', shadowBlur: 5, shadowColor: '#ff4d4f' } })),
        ...goodVals.map(v => ({ value: v, itemStyle: { color: '#52c41a', shadowBlur: 5, shadowColor: '#52c41a' } }))
      ]
    }]
  });
  charts.push(chart);
};

const drawTrendChart = (trendData) => {
  if(!trendChartRef.value) return;
  const chart = echarts.init(trendChartRef.value);
  chart.setOption({
    tooltip: { trigger: 'axis', backgroundColor: 'rgba(0,0,0,0.8)', textStyle: { color: '#fff' } },
    grid: { left: '3%', right: '5%', top: '15%', bottom: '0%', containLabel: true },
    xAxis: { type: 'category', boundaryGap: false, data: trendData.dates, axisLabel: { color: '#8892a0', fontSize: 10 }, axisLine: { lineStyle: { color: 'rgba(0,240,255,0.3)' } } },
    yAxis: { type: 'value', splitLine: { lineStyle: { type: 'dashed', color: 'rgba(0,240,255,0.1)' } }, axisLabel: { show: false } },
    series: [
      { name: '违规黑榜', type: 'line', smooth: true, symbol: 'none', itemStyle: { color: '#ff4d4f' }, lineStyle: { width: 2, shadowColor: '#ff4d4f', shadowBlur: 5 }, data: trendData.bad },
      { name: '正向红榜', type: 'line', smooth: true, symbol: 'none', itemStyle: { color: '#52c41a' }, lineStyle: { width: 2, shadowColor: '#52c41a', shadowBlur: 5 }, data: trendData.good }
    ]
  });
  charts.push(chart);
};

// ================== 弹幕控制 ==================
const logOffset = ref(0);
let logTimer = null;
const startBarrage = () => {
  if (logTimer) clearInterval(logTimer);
  logTimer = setInterval(() => {
    if (liveLogs.value.length > 5) {
      logOffset.value += 38; 
      if (logOffset.value > 38 * 15) { logOffset.value = 0; liveLogs.value.push(liveLogs.value.shift()); }
    }
  }, 2000);
};

onMounted(() => {
  updateTime(); clockTimer = setInterval(updateTime, 1000);
  fetchDashboardData();
  window.addEventListener('resize', () => charts.forEach(c => c.resize()));
});

onUnmounted(() => { clearInterval(clockTimer); clearInterval(logTimer); charts.forEach(c => c.dispose()); });
</script>
<style scoped>
/* ================= 1. 【核心修复】强制禁止滚动 ================= */
.fui-dashboard {
  /* 严格锁定高度，兼容大多数管理后台框架的头部高度。如果还有滚动条，把 64px 改成 84px 或更大 */
  height: calc(100vh - 64px); 
  width: 100%; 
  box-sizing: border-box;
  /* background: #01050a; color: #fff; font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif; */
  background-image: url('../../assets/images/53bg.png'); 
  background-size: cover;
  background-position: center;
  position: relative; display: flex; flex-direction: column; 
  overflow: hidden; /* 绝对禁止内部溢出 */
}

.fui-bg-grid { position: absolute; top: 0; left: 0; width: 100%; height: 100%; background: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='80' height='80' viewBox='0 0 80 80'%3E%3Cpath d='M 80 0 L 0 0 0 80' fill='none' stroke='rgba(0,240,255,0.03)' stroke-width='1'/%3E%3C/svg%3E"); pointer-events: none;}
.fui-bg-glow { position: absolute; top: 20%; left: 30%; width: 40%; height: 60%; background: radial-gradient(ellipse at center, rgba(0, 240, 255, 0.15) 0%, rgba(0, 0, 0, 0) 70%); pointer-events: none; }

.DIN { font-family: 'DIN Alternate', monospace; }
.text-cyan { color: #00f0ff !important; text-shadow: 0 0 8px rgba(0,240,255,0.6); }
.text-orange { color: #ffb400 !important; text-shadow: 0 0 8px rgba(255,180,0,0.6); }
.text-safe { color: #52c41a !important; text-shadow: 0 0 8px rgba(82,196,26,0.6); }

/* ================= 2. 科幻 Header ================= */
.fui-header { height: 80px; flex-shrink: 0; display: flex; justify-content: space-between; align-items: flex-start; padding: 10px 40px; position: relative; z-index: 10; background: url('/images/header_bg.png') no-repeat center top; background-size: cover; border-bottom: 1px solid rgba(0,240,255,0.15); box-shadow: 0 10px 30px rgba(0,240,255,0.05);}
.header-left, .header-right { width: 25%; padding-top: 15px; }
.time-box { font-size: 26px; color: #00f0ff; letter-spacing: 2px; }
.status-box { text-align: right; font-size: 14px; font-weight: bold; color: #8892a0; letter-spacing: 1px;}
.header-center { width: 50%; text-align: center; }
.header-center h1 { margin: 0; font-size: 32px; font-weight: 900; color: #fff; letter-spacing: 4px; text-shadow: 0 0 15px rgba(0,240,255,0.8); }
.header-center p { margin: 2px 0 0; font-size: 12px; color: #00f0ff; letter-spacing: 6px; opacity: 0.7; }

/* ================= 3. 两侧面板约束 (解决高度爆表的关键) ================= */
.fui-main { 
  flex: 1; min-height: 0; /* Flex 核心定律，阻止被子元素撑爆 */
  display: flex; padding: 20px 30px; gap: 30px; position: relative; z-index: 5; 
  overflow: hidden;
}
.fui-side-panel { 
  width: 340px; height: 100%; display: flex; flex-direction: column; gap: 20px; z-index: 10; 
}
.fui-center-panel { 
  flex: 1; min-width: 0; position: relative; display: flex; flex-direction: column; align-items: center; 
}

/* 所有的弹性盒子面板必须加上 min-height: 0 */
.fui-panel { 
  background: rgba(3, 11, 24, 0.7); border: 1px solid rgba(0, 240, 255, 0.2); 
  box-shadow: 0 8px 32px rgba(0,0,0,0.5), inset 0 0 20px rgba(0, 240, 255, 0.05); 
  padding: 15px; border-radius: 2px; position: relative;
  display: flex; flex-direction: column; overflow: hidden;
}
.flex-1 { flex: 1; min-height: 0; }
.flex-1-5 { flex: 1.5; min-height: 0; }

.fui-panel::before { content: ''; position: absolute; top: -1px; left: -1px; width: 15px; height: 15px; border-top: 2px solid #00f0ff; border-left: 2px solid #00f0ff; }
.fui-panel::after { content: ''; position: absolute; bottom: -1px; right: -1px; width: 15px; height: 15px; border-bottom: 2px solid #00f0ff; border-right: 2px solid #00f0ff; }

.panel-title { font-size: 15px; font-weight: bold; color: #fff; margin-bottom: 15px; flex-shrink: 0; display: flex; align-items: center; justify-content: center; text-shadow: 0 0 5px rgba(0,240,255,0.5);}
.arrows { color: #00f0ff; margin-right: 8px; letter-spacing: -2px; opacity: 0.8;}
.arrows-right { color: #00f0ff; margin-left: 8px; letter-spacing: -2px; opacity: 0.8;}

.asset-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 10px; }
.asset-item { background: linear-gradient(180deg, rgba(0,240,255,0.1) 0%, rgba(0,240,255,0) 100%); border: 1px solid rgba(0,240,255,0.15); padding: 10px; border-radius: 4px; text-align: center; display: flex; flex-direction: column; border-top: 2px solid rgba(0,240,255,0.5);}
.asset-item .label { font-size: 12px; color: #a0aab5; margin-bottom: 5px; }
.asset-item .val { font-size: 24px; font-weight: bold; }

/* 图表容器去掉了高度写死，现在依靠 flex 自动收缩 */
.chart-box { flex: 1; min-height: 0; width: 100%; }

.log-window { flex: 1; min-height: 0; overflow: hidden; position: relative; mask-image: linear-gradient(to bottom, transparent, black 5%, black 95%, transparent); }
.log-track { transition: transform 0.4s cubic-bezier(0.25, 1, 0.5, 1); }
.log-item { display: flex; align-items: center; padding: 8px 0; border-bottom: 1px dashed rgba(0,240,255,0.1); font-size: 13px; }
.log-time { color: #00f0ff; width: 70px; font-size: 12px; opacity: 0.8;}
.log-loc { flex: 1; color: #d1d5db; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; margin-right: 10px;}
.log-type { padding: 2px 6px; border-radius: 2px; font-weight: bold; font-size: 11px; background: rgba(0,0,0,0.5); }

/* ================= 4. 中心 3D 枢纽保持原汁原味的大 ================= */
.center-top-stats { display: flex; gap: 100px; margin-top: 5px; z-index: 30; flex-shrink: 0;}
.stat-block { text-align: center; }
.stat-block .label { font-size: 16px; color: #a0aab5; margin-bottom: 5px; letter-spacing: 1px;}
.stat-block .val { font-size: 50px; font-weight: 900; line-height: 1; }

.stage-3d-container { 
  flex: 1; min-height: 0; width: 100%; position: relative; 
  perspective: 1500px; 
  display: flex; justify-content: center; align-items: center;
}

.orbit-system { position: relative; transform-style: preserve-3d; transform: rotateX(65deg); }

.giant-core {
  position: absolute; top: 50%; left: 50%;
  transform: translate(-50%, -50%) rotateX(-65deg);
  width: 400px; height: 400px; /* 放大球体容器 */
  z-index: 50;
  display: flex; justify-content: center; align-items: center;
}
.core-glow { 
  position: absolute; width: 150%; height: 150%; 
  background: radial-gradient(circle, rgba(0,240,255,0.3) 0%, transparent 60%); 
  animation: pulse 3s infinite; 
}
.core-sphere { 
  position: absolute; width: 380px; height: 380px; /* 放大核心球 */
  border-radius: 50%; 
  background-color: #011122; 
  /* 💡 修复路径：使用 Vite 的 @ 别名，并修正后缀为 .png */
  background-image: url('@/assets/images/53earth.png'); 
  background-size: cover; background-position: center;
  box-shadow: inset -30px -30px 50px rgba(0,0,0,0.9), inset 0 0 40px #00f0ff, 0 0 50px rgba(0,240,255,0.5);
  animation: earth-spin 20s linear infinite;
}

/* 放大外圈轨道 */
.orbit-ring {
  position: absolute; top: 50%; left: 50%;
  width: 800px; height: 800px; margin: -400px 0 0 -400px; /* 放大到 800px */
  border: 1px dashed rgba(0,240,255,0.4); border-radius: 50%;
  transform-style: preserve-3d; animation: ring-spin 25s linear infinite;
}

/* 放大内圈轨道 */
.orbit-ring-inner {
  position: absolute; top: 50%; left: 50%;
  width: 550px; height: 550px; margin: -275px 0 0 -275px; /* 放大到 550px */
  border: 2px solid rgba(0,240,255,0.1); border-radius: 50%;
  box-shadow: 0 0 30px rgba(0,240,255,0.2) inset; pointer-events: none;
}

/* 适配放大后的轨道半径 */
.orbit-node {
  position: absolute; top: 50%; left: 50%; transform-style: preserve-3d;
  transform: rotateZ(var(--angle)) translateY(-400px); /* 跟着外圈轨道变大 */
}

.hologram-card {
  position: absolute; left: -70px; top: -45px; 
  width: 140px; height: 90px; cursor: pointer; transform-style: preserve-3d;
  animation: counter-spin-z 25s linear infinite; transition: filter 0.3s;
}
.hologram-card:hover { filter: drop-shadow(0 0 20px #00f0ff) brightness(1.2); z-index: 100;}

.card-glass { width: 100%; height: 100%; background: rgba(1, 8, 20, 0.85); border: 1px solid rgba(0,240,255,0.4); box-shadow: inset 0 0 15px rgba(0,240,255,0.3); padding: 4px; display: flex; flex-direction: column; position: relative;}
.holo-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 4px; border-bottom: 1px solid rgba(0,240,255,0.2); padding-bottom: 2px;}
.h-name { font-size: 10px; color: #fff; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; max-width: 90px;}
.h-status { font-size: 10px; color: #ff4d4f; font-weight: bold; animation: blink 1s infinite; }
.holo-video { flex: 1; background: #000; position: relative; overflow: hidden; }
.holo-video img { width: 100%; height: 100%; object-fit: cover; opacity: 0.9; filter: contrast(1.2); }
.scan-line { position: absolute; top: 0; left: 0; width: 100%; height: 2px; background: rgba(0,240,255,0.8); box-shadow: 0 0 10px #00f0ff; animation: scan-down 2s linear infinite; }

.angle { position: absolute; width: 6px; height: 6px; border-color: #00f0ff; border-style: solid; border-width: 0; }
.angle-tl { top: -1px; left: -1px; border-top-width: 2px; border-left-width: 2px; }
.angle-tr { top: -1px; right: -1px; border-top-width: 2px; border-right-width: 2px; }
.angle-bl { bottom: -1px; left: -1px; border-bottom-width: 2px; border-left-width: 2px; }
.angle-br { bottom: -1px; right: -1px; border-bottom-width: 2px; border-right-width: 2px; }

@keyframes ring-spin { from { transform: rotateZ(0deg); } to { transform: rotateZ(360deg); } }
@keyframes counter-spin-z {
  0% { transform: rotateZ(calc(-1 * var(--angle))) rotateX(-65deg); }
  100% { transform: rotateZ(calc(-1 * var(--angle) - 360deg)) rotateX(-65deg); }
}
@keyframes earth-spin { to { background-position: 200% center; } }
@keyframes pulse { 0%, 100% { opacity: 0.4; } 50% { opacity: 0.8; transform: scale(1.05);} }
@keyframes blink { 0%, 100% { opacity: 1; } 50% { opacity: 0; } }
@keyframes scan-down { 0% { top: -10%; } 100% { top: 110%; } }

/* ================= 5. 暗黑模态框 ================= */
:deep(.fui-modal .ant-modal-content) { background: rgba(5, 15, 30, 0.95); border: 1px solid #00f0ff; box-shadow: 0 0 40px rgba(0,240,255,0.3); backdrop-filter: blur(10px);}
:deep(.fui-modal .ant-modal-header) { background: transparent; border-bottom: 1px solid rgba(0,240,255,0.2); }
:deep(.fui-modal .ant-modal-title) { color: #fff; font-size: 18px;}
:deep(.fui-modal .ant-modal-close-x) { color: #00f0ff; }
.expanded-video-container { width: 100%; height: 450px; background: #000; position: relative; display: flex; justify-content: center; align-items: center; border: 1px solid rgba(0,240,255,0.4);}
.real-video-stream-large { 
  width: 100%; 
  height: 100%; 
  object-fit: cover; /* 让视频铺满不留黑边，或者用 contain 保持比例 */
  background: #000;
}
.signal-text { position: absolute; top: 15px; right: 15px; color: #ff4d4f; font-size: 13px; font-weight: 900; background: rgba(0,0,0,0.8); padding: 6px 12px; border-radius: 4px; border: 1px solid #ff4d4f; animation: blink 2s infinite;}
.video-stats { display: flex; justify-content: space-around; background: rgba(0,240,255,0.05); padding: 15px; border: 1px solid rgba(0,240,255,0.2);}
</style>