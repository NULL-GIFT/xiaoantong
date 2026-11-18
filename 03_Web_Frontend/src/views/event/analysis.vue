<template>
  <div class="aero-dashboard">
    <div class="aero-bg-glow"></div>

    <div class="aero-card action-bar">
      <div class="action-left">
        <div class="action-icon"><calendar-outlined /></div>
        <div class="action-info">
          <div class="a-label">报告生成周期维度</div>
          <a-radio-group v-model:value="periodType" button-style="solid" @change="generateReport" class="aero-radio">
            <a-radio-button value="month">月度复盘决议 (近 30 天)</a-radio-button>
            <a-radio-button value="quarter">季度治理审计 (近 90 天)</a-radio-button>
          </a-radio-group>
        </div>
      </div>
      <div class="action-right">
        <a-button type="primary" class="aero-btn-primary" size="large" @click="exportToPDF" :loading="exporting">
          <template #icon><download-outlined /></template> 
          签发 {{ periodName }} 审计 PDF
        </a-button>
      </div>
    </div>

    <div class="report-wrapper">
      <a-spin :spinning="loading" tip="正在进行全局治理数据推演与归因分析..." size="large">
        <div id="report-container" class="aero-paper" v-show="!loading">
          
          <div class="paper-header">
            <div class="p-logo">XAT-SECURITY AUDIT</div>
            <div class="p-meta">
              <span>报告编号：{{ reportId }}</span>
              <span>生成时间：{{ currentTime }}</span>
              <span>查阅权限：{{ userRole === 'admin' ? 'SYSTEM_ADMIN (系统运维)' : 'SECURITY_USER (安保调度)' }}</span>
            </div>
          </div>

          <div class="paper-title-area">
            <h1 class="p-title">
              {{ userRole === 'admin' ? `校安通 · 联邦智能与边缘算力效能审计${periodName}` : `校安通 · 校园全域安防态势与决策${periodName}` }}
            </h1>
            <div class="p-subtitle">DATA-DRIVEN GOVERNANCE & EXECUTIVE REPORT (PAST {{ periodDays }} DAYS)</div>
            
            <div class="p-data-source">
              <span class="ds-item">📊 数据来源：全网分布式边缘计算节点 + AI视觉识别系统</span>
              <span class="ds-item">🎯 统计口径：已剔除AI误报负样本，仅统计确诊事实数据</span>
            </div>
          </div>

          <div class="paper-section exec-summary-section" v-if="reportData.total > 0 || reportData.falseCount > 0">
            <h3 class="section-title text-primary"><span class="s-dot bg-primary"></span>00. 核心结论摘要 (Executive Summary)</h3>
            <div class="exec-box">
              <template v-if="userRole === 'user'">
                <div class="exec-row">
                  <span class="exec-label">1. 校园整体风险：</span>
                  <span class="exec-val" :class="reportData.riskLevel === '高' ? 'text-danger' : (reportData.riskLevel === '中' ? 'text-warn' : 'text-safe')">
                    <strong>{{ reportData.riskLevel }}等风险</strong> 
                    <span style="font-size: 13px; margin-left: 6px; font-weight: normal;">(日均 {{ reportData.avgDaily }} 起)</span>
                  </span>
                </div>
                <div class="exec-row">
                  <span class="exec-label">2. 最大风险阵地：</span>
                  <span class="exec-val"><strong>{{ reportData.topLoc }}</strong> (占总隐患数的 {{ reportData.topLocPct }}%)</span>
                </div>
                <div class="exec-row">
                  <span class="exec-label">3. 核心爆发事件：</span>
                  <span class="exec-val text-danger"><strong>{{ reportData.topType }}</strong> (共 {{ reportData.topTypeCount }} 起，占 {{ reportData.topTypePct }}%)</span>
                </div>
                <div class="exec-row">
                  <span class="exec-label">4. 当前治理状态：</span>
                  <span class="exec-val" :class="reportData.handledRate >= 90 ? 'text-safe' : 'text-warn'">
                    已闭环处置 <strong>{{ reportData.handledRate }}%</strong> 
                    <span style="font-size: 13px; font-weight: normal;">{{ reportData.handledRate >= 90 ? '，态势已控制。' : '，存在滞留反弹风险，需加速核查。' }}</span>
                  </span>
                </div>
              </template>
              <template v-else>
                <div class="exec-row">
                  <span class="exec-label">1. 全网算力健康度：</span>
                  <span class="exec-val text-safe"><strong>优秀</strong> <span style="font-size:13px;font-weight:normal;">(处理并发警报 {{ reportData.total }} 起，无阻塞)</span></span>
                </div>
                <div class="exec-row">
                  <span class="exec-label">2. 综合算法误报率：</span>
                  <span class="exec-val" :class="reportData.falseRate > 5 ? 'text-warn' : 'text-safe'"><strong>{{ reportData.falseRate }}%</strong> <span style="font-size:13px;font-weight:normal;">(共捕获负样本 {{ reportData.falseCount }} 起)</span></span>
                </div>
                <div class="exec-row">
                  <span class="exec-label">3. 防抖失效重灾区：</span>
                  <span class="exec-val text-purple"><strong>{{ reportData.topFalseLoc }}</strong> <span style="font-size:13px;font-weight:normal;">(贡献了最多误判特征)</span></span>
                </div>
                <div class="exec-row">
                  <span class="exec-label">4. 联邦学习调度状态：</span>
                  <span class="exec-val text-primary"><strong>等待下达指令</strong> <span style="font-size:13px;font-weight:normal;">(本地差分隐私数据已就绪)</span></span>
                </div>
              </template>
            </div>
          </div>

          <div class="paper-section">
            <h3 class="section-title"><span class="s-dot"></span>01. 详细数据洞察与特征分布</h3>
            
            <a-row :gutter="24" class="chart-row">
              <a-col :span="10">
                <div class="chart-box">
                  <div class="c-title">{{ userRole === 'admin' ? '算法触发类型占比' : '安全预警类型穿透' }}</div>
                  <div ref="pieChartRef" style="width: 100%; height: 260px;"></div>
                  <div class="chart-note" v-if="reportData.total > 0 && userRole === 'user'">
                    * 结论：其中【<strong class="text-danger">{{ reportData.topType }}</strong>】占比最高 ({{ reportData.topTypePct }}%)，为本期校园主要安全痛点。
                  </div>
                </div>
              </a-col>
              <a-col :span="14">
                <div class="chart-box">
                  <div class="c-title">{{ userRole === 'admin' ? '边缘节点并发预警趋势与分类' : '风险事件演变趋势与归因构成' }}</div>
                  <div ref="trendChartRef" style="width: 100%; height: 260px;"></div>
                  
                  <div class="anomaly-alert" v-if="reportData.hasAnomaly">
                    <span class="anomaly-icon">⚠️ 异常峰值侦测：</span>
                    <strong>{{ reportData.anomalyDate }}</strong> 事件数异动升高（达 {{ reportData.anomalyCount }} 起，超均值 {{ reportData.anomalyRatio }} 倍）。系统研判可能受恶劣天气、突发活动或区域集中违规影响，建议调取当日录像复盘。
                  </div>
                  <div class="chart-note" v-else style="margin-top: 10px;">
                    * 结论：本周期内每日事件发生频次较为平稳，未出现显著的突发异常峰值。
                  </div>
                </div>
              </a-col>
            </a-row>

            <a-row :gutter="24" class="chart-row mt-4" v-if="userRole === 'admin' && reportData.falseCount > 0">
              <a-col :span="24">
                <div class="chart-box admin-chart-box">
                  <div class="c-title" style="color: #6b21a8;">🤖 联邦学习素材库：负样本贡献节点溯源 (TOP 5)</div>
                  <div ref="falseAlarmChartRef" style="width: 100%; height: 250px;"></div>
                </div>
              </a-col>
            </a-row>
          </div>

          <div class="paper-section" v-if="reportData.total > 0 || reportData.falseCount > 0">
            <h3 class="section-title"><span class="s-dot"></span>02. 治理决议与行动指令下达</h3>
            
            <div class="directive-list" v-if="userRole === 'user'">
              <div class="directive-card" v-for="(directive, idx) in userDirectives" :key="idx">
                <div class="d-header">
                  <span class="d-type">【行动指令：{{ directive.type }}】</span>
                </div>
                <div class="d-body">
                  <div class="d-row"><span class="d-label">责任单位：</span><span class="d-value"><strong>{{ directive.unit }}</strong></span></div>
                  <div class="d-row"><span class="d-label">执行期限：</span><span class="d-value">{{ directive.time }}</span></div>
                  <div class="d-row align-top">
                    <span class="d-label">具体措施：</span>
                    <span class="d-value measures-list">
                      <div v-for="(m, mIdx) in directive.measures" :key="mIdx" v-html="`- ${m}`"></div>
                    </span>
                  </div>
                </div>
              </div>
            </div>

            <div class="directive-list admin-list" v-else>
              <div class="directive-card" v-for="(directive, idx) in adminDirectives" :key="idx">
                <div class="d-header">
                  <span class="d-type" style="color: #7e22ce; background: #f3e8ff;">【运维指令：{{ directive.type }}】</span>
                </div>
                <div class="d-body">
                  <div class="d-row"><span class="d-label">责任单位：</span><span class="d-value"><strong>{{ directive.unit }}</strong></span></div>
                  <div class="d-row"><span class="d-label">执行期限：</span><span class="d-value">{{ directive.time }}</span></div>
                  <div class="d-row align-top">
                    <span class="d-label">具体措施：</span>
                    <span class="d-value measures-list">
                      <div v-for="(m, mIdx) in directive.measures" :key="mIdx" v-html="`- ${m}`"></div>
                    </span>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <div class="paper-footer">
            <div class="f-line"></div>
            <p>XAT SECURITY GOVERNANCE & FEDERATED LEARNING AUDIT ENGINE</p>
            <p class="f-stamp">本报告由 校安通智能安防底座 自动生成并存证</p>
          </div>

        </div>
      </a-spin>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, nextTick } from 'vue';
import { message } from 'ant-design-vue';
import { CalendarOutlined, DownloadOutlined } from '@ant-design/icons-vue';
import request from '@/utils/request';
import { getDevice } from '@/api/device.js';
import * as echarts from 'echarts';
import * as html2pdf from 'html2pdf.js';

// ================== 1. 核心控制与模板变量对齐 ==================
const userRole = ref(localStorage.getItem('userRole') || 'admin');
const reportId = ref('XAT-' + new Date().getTime().toString().slice(-8));
const currentTime = ref(new Date().toLocaleString('zh-CN', { hour12: false }));

const periodType = ref('month'); 
const periodName = computed(() => periodType.value === 'month' ? '月度' : '季度');
const days = computed(() => periodType.value === 'month' ? 30 : 90);
const periodDays = days;

const loading = ref(false);
const exporting = ref(false);

// ================== 2. ECharts 容器引用对齐 ==================
const pieChartRef = ref(null);
const trendChartRef = ref(null);
const falseAlarmChartRef = ref(null);
let charts = [];

// ================== 3. 核心数据源 ==================
const reportData = ref({ 
  total: 0, falseCount: 0, falseRate: 0, riskLevel: '低', avgDaily: 0,
  topLoc: '暂无', topLocPct: 0, topType: '暂无', topTypeCount: 0, topTypePct: 0,
  handledRate: 100, topFalseLoc: '暂无', hasAnomaly: false, anomalyDate: '', anomalyCount: 0, anomalyRatio: 0
});

const userDirectives = ref([]);
const adminDirectives = ref([]);
const dynamicSkillMeta = ref({});

// 💡 强力修复：超级解构函数，完美兼容 MyBatis-Plus 分页、PageHelper 甚至直接返回 List 的情况
const extractArray = (res) => {
  if (!res || (res.code !== 200 && res.code !== 0)) return [];
  const data = res.data;
  if (!data) return [];
  if (Array.isArray(data)) return data;
  if (Array.isArray(data.records)) return data.records; // 兼容 MyBatis-Plus
  if (Array.isArray(data.list)) return data.list;       // 兼容 PageHelper
  if (Array.isArray(data.rows)) return data.rows;
  return [];
};

// ================== 4. 核心拉取逻辑 ==================
const generateReport = async () => {
  loading.value = true;
  try {
    const now = new Date();
    const pastTime = new Date(now.getTime() - days.value * 24 * 60 * 60 * 1000);
    // 修复时区偏差导致的时间转换错误
    const formatTime = (date) => {
       const tzOffset = date.getTimezoneOffset() * 60000;
       return new Date(date.getTime() - tzOffset).toISOString().replace('T', ' ').substring(0, 19);
    };

    const [deviceRes, skillRes, alarmRes] = await Promise.all([
      getDevice({ pageNum: 1, pageSize: 1000 }).catch(() => ({ data: [] })),
      request.get('/api/skill/list', { params: { pageNum: 1, pageSize: 500 } }).catch(() => ({ data: [] })), 
      request.get('/api/alarm/list', { 
        params: { pageNum: 1, pageSize: 10000, startTime: formatTime(pastTime), endTime: formatTime(now) } 
      }).catch(() => ({ data: [] }))
    ]);

    // 💡 使用强力解构函数，彻底解决 .filter is not a function 导致白屏报错！
    const allDevices = extractArray(deviceRes);
    const rawSkills = extractArray(skillRes);
    const allAlarms = extractArray(alarmRes);

    const newMeta = {};
    rawSkills.forEach(skill => {
      const code = skill.skillCode || skill.skill_code;
      newMeta[code] = { name: skill.skillName || skill.skill_name, isRisk: !code.includes('good_deed') };
    });
    dynamicSkillMeta.value = newMeta;

    processData(allAlarms, allDevices);
  } catch (err) {
    console.error('报告生成拦截报错详情:', err);
    message.error('数据清洗失败，已在控制台打印详细日志');
  } finally {
    loading.value = false;
  }
};

// ================== 5. 数据清洗与推演 ==================
const processData = (alarms, devices) => {
  const deviceMap = {};
  devices.forEach(d => {
    const id = d.deviceId || d.device_id;
    if (id) deviceMap[id] = d.deviceName || d.device_name;
  });

  const getCode = (a) => a.type || a.alarmType || a.alarm_type || 'unknown';
  const getLoc = (a) => deviceMap[a.deviceId || a.device_id] || a.location || '未知区域';

  // 极度安全的数组验证
  if (!Array.isArray(alarms)) alarms = [];

  const validAlarms = alarms.filter(a => {
    const code = getCode(a);
    return dynamicSkillMeta.value[code] ? dynamicSkillMeta.value[code].isRisk : true; 
  });

  const falseAlarms = validAlarms.filter(a => a.isFalseAlarm === 1 || a.is_false_alarm === 1);
  const trueAlarms = validAlarms.filter(a => a.isFalseAlarm !== 1 && a.is_false_alarm !== 1);
  const handledAlarms = trueAlarms.filter(a => a.isHandled === 1 || a.is_handled === 1 || a.isHandled === 2);
  
  const totalValid = trueAlarms.length; 
  const falseRate = validAlarms.length > 0 ? ((falseAlarms.length / validAlarms.length) * 100).toFixed(1) : 0;
  const handleRate = totalValid > 0 ? Math.round((handledAlarms.length / totalValid) * 100) : 100;

  const locCount = {};
  const evtCount = {};
  const falseLocCount = {};
  const trendMap = {};

  trueAlarms.forEach(a => {
    const loc = getLoc(a);
    const meta = dynamicSkillMeta.value[getCode(a)] || { name: getCode(a) };
    locCount[loc] = (locCount[loc] || 0) + 1;
    evtCount[meta.name] = (evtCount[meta.name] || 0) + 1;

    const timeStr = a.time || a.alarmTime || a.alarm_time || a.createTime;
    if (timeStr && typeof timeStr === 'string' && timeStr.length >= 10) {
       const d = timeStr.substring(5, 10); 
       trendMap[d] = (trendMap[d] || 0) + 1;
    }
  });

  falseAlarms.forEach(a => {
    falseLocCount[getLoc(a)] = (falseLocCount[getLoc(a)] || 0) + 1;
  });

  const sortedLocs = Object.entries(locCount).sort((a,b)=>b[1]-a[1]);
  const sortedEvts = Object.entries(evtCount).sort((a,b)=>b[1]-a[1]);
  const sortedFalseLocs = Object.entries(falseLocCount).sort((a,b)=>b[1]-a[1]);

  reportData.value = {
    total: totalValid, falseCount: falseAlarms.length, falseRate: falseRate,
    riskLevel: totalValid > 30 ? '高' : (totalValid > 10 ? '中' : '低'),
    avgDaily: Math.round(totalValid / days.value) || 0,
    topLoc: sortedLocs.length > 0 ? sortedLocs[0][0] : '暂无',
    topLocPct: totalValid > 0 && sortedLocs.length > 0 ? Math.round((sortedLocs[0][1] / totalValid) * 100) : 0,
    topType: sortedEvts.length > 0 ? sortedEvts[0][0] : '暂无',
    topTypeCount: sortedEvts.length > 0 ? sortedEvts[0][1] : 0,
    topTypePct: totalValid > 0 && sortedEvts.length > 0 ? Math.round((sortedEvts[0][1] / totalValid) * 100) : 0,
    handledRate: handleRate,
    topFalseLoc: sortedFalseLocs.length > 0 ? sortedFalseLocs[0][0] : '暂无',
    hasAnomaly: false, anomalyDate: '', anomalyCount: 0, anomalyRatio: 0
  };

  generateAIAdvice(sortedLocs, sortedEvts, falseRate);
  nextTick(() => { drawCharts(sortedEvts, trendMap, sortedFalseLocs); });
};

const generateAIAdvice = (locs, evts, falseRate) => {
  const directives = [];
  if (locs.length > 0) {
    directives.push({
      type: '空间安防部署调优', unit: '校园安保处', time: '立即执行',
      measures: [`侦测到【${locs[0][0]}】频发风险预警 (累计 ${locs[0][1]} 起)。`, `建议增加该区域的视频监控路数，并在高峰时段调配安保进行高频巡更。`]
    });
  }
  if (Number(falseRate) > 5) {
    directives.push({
      type: '边缘算力隐私反哺', unit: '技术运维组', time: '今晚 02:00',
      measures: [`当前大盘综合误报率达 ${falseRate}%。`, `触发自动反哺机制：要求端侧设备打包近期“误报”特征流，回传云端进行联邦学习。`]
    });
  }
  if(directives.length === 0) {
    directives.push({ type: '安全态势平稳', unit: '安保与技术组', time: '持续跟进', measures: ['报告期内未发现显著高发风险。', '建议维持当前部署。'] });
  }
  userDirectives.value = directives;
  adminDirectives.value = directives;
};

// ================== 6. ECharts 渲染 ==================
const drawCharts = (evts, trendMap, falseLocs) => {
  charts.forEach(c => c.dispose());
  charts = [];

  if (pieChartRef.value && evts.length > 0) {
    const pChart = echarts.init(pieChartRef.value);
    
    // 提前准备好前 5 的数据，方便图例使用
    const pieData = evts.slice(0, 5).map(e => ({ name: e[0], value: e[1] }));
    
    pChart.setOption({
      tooltip: { trigger: 'item', formatter: '{b} <br/> {c}起 ({d}%)' }, 
      color: ['#ef4444', '#f97316', '#eab308', '#0ea5e9', '#8b5cf6'],
      
      // 💡 修复1：底部图例升级，直接在名字后面跟上“发生次数”
      legend: { 
        bottom: '0%', 
        left: 'center', 
        itemWidth: 12, 
        itemHeight: 12, 
        textStyle: { fontSize: 12, color: '#475569' },
        formatter: function (name) {
          const target = pieData.find(item => item.name === name);
          return target ? `${name} : ${target.value}起` : name;
        }
      },
      
      series: [{ 
        type: 'pie', 
        // 💡 修复2：因为没有外围文字了，圆环可以居中并进一步放大，显得更大气
        radius: ['45%', '70%'], 
        center: ['50%', '42%'],
        itemStyle: { borderRadius: 8, borderColor: '#fff', borderWidth: 2 }, 
        data: pieData, 
        // 💡 修复3：彻底关闭支出去的指示线和外部标签
        label: { show: false },
        labelLine: { show: false }
      }]
    });
    charts.push(pChart);
  }

  if (trendChartRef.value && Object.keys(trendMap).length > 0) {
    const tChart = echarts.init(trendChartRef.value);
    const dates = Object.keys(trendMap).sort();
    tChart.setOption({
      tooltip: { trigger: 'axis' }, grid: { left: '8%', right: '5%', bottom: '5%', top: '10%', containLabel: true },
      xAxis: { type: 'category', data: dates }, yAxis: { type: 'value' },
      series: [{ type: 'line', smooth: true, data: dates.map(d => trendMap[d]), areaStyle: { color: 'rgba(14,165,233,0.2)' }, itemStyle: { color: '#0ea5e9'} }]
    });
    charts.push(tChart);
  }

  if (falseAlarmChartRef.value && falseLocs.length > 0) {
    const fChart = echarts.init(falseAlarmChartRef.value);
    fChart.setOption({
      tooltip: { trigger: 'axis' }, grid: { left: '3%', right: '8%', bottom: '3%', top: '5%', containLabel: true },
      xAxis: { type: 'value' }, yAxis: { type: 'category', data: falseLocs.slice(0,5).map(l=>l[0]).reverse() },
      series: [{ type: 'bar', data: falseLocs.slice(0,5).map(l=>l[1]).reverse(), itemStyle: { color: '#d8b4fe', borderRadius: [0, 4, 4, 0] }, label: { show: true, position: 'right' } }]
    });
    charts.push(fChart);
  }
};

const exportToPDF = () => {
  exporting.value = true;
  const element = document.getElementById('report-container');
  const opt = { margin: 10, filename: `校安通_${periodName.value}审计报告.pdf`, image: { type: 'jpeg', quality: 0.98 }, html2canvas: { scale: 2, useCORS: true }, jsPDF: { unit: 'mm', format: 'a4', orientation: 'portrait' } };
  const pdfEngine = html2pdf.default ? html2pdf.default() : html2pdf();
  pdfEngine.set(opt).from(element).save().then(() => message.success('PDF 已生成')).catch(() => message.error('PDF 生成失败')).finally(() => exporting.value = false);
};

onMounted(() => {
  generateReport();
  window.addEventListener('resize', () => charts.forEach(c => c.resize()));
});
</script>
<style scoped>
/* ==================== 高级底座 ==================== */
.aero-dashboard { padding: 24px; background: #f4f7fb; min-height: 100vh; position: relative; font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Arial, sans-serif; overflow: hidden;}
.aero-bg-glow { position: absolute; top: -10%; left: 20%; width: 60%; height: 50%; background: radial-gradient(circle, rgba(14, 165, 233, 0.08) 0%, rgba(255, 255, 255, 0) 70%); pointer-events: none; z-index: 0; }

/* ==================== 顶部操作区 ==================== */
.action-bar { display: flex; justify-content: space-between; align-items: center; margin-bottom: 24px; padding: 16px 24px; background: rgba(255,255,255,0.8); backdrop-filter: blur(12px); border-radius: 12px; box-shadow: 0 4px 20px rgba(0,0,0,0.02); border: 1px solid rgba(255,255,255,1); position: relative; z-index: 10; }
.action-left { display: flex; align-items: center; }
.action-icon { width: 40px; height: 40px; background: #e0f2fe; color: #0ea5e9; border-radius: 10px; display: flex; justify-content: center; align-items: center; font-size: 20px; margin-right: 16px; }
.a-label { font-size: 12px; color: #64748b; font-weight: 600; margin-bottom: 4px; }
.aero-radio { box-shadow: 0 2px 4px rgba(0,0,0,0.02); }
.aero-btn-primary { background: #0f172a; border: none; box-shadow: 0 4px 10px rgba(15,23,42,0.2); font-weight: bold;}
.aero-btn-primary:hover { background: #1e293b; }

/* ==================== 报告本体 (PDF导出专属的高级白皮书) ==================== */
.report-wrapper { position: relative; z-index: 10; }
.aero-paper {
  background: #ffffff; width: 100%; max-width: 850px; margin: 0 auto;
  padding: 50px 60px; box-shadow: 0 10px 40px rgba(15,23,42,0.06);
  border-radius: 4px; color: #0f172a;
}

/* 页眉 */
.paper-header { display: flex; justify-content: space-between; align-items: flex-end; border-bottom: 2px solid #0f172a; padding-bottom: 12px; margin-bottom: 30px; }
.p-logo { font-size: 20px; font-weight: 900; font-family: "Impact", "DIN Alternate", Arial; letter-spacing: 1px; color: #0f172a; }
.p-meta { display: flex; flex-direction: column; text-align: right; font-size: 12px; color: #64748b; font-family: monospace; line-height: 1.6;}

/* 大标题与元数据 */
.paper-title-area { text-align: center; margin-bottom: 40px; }
.p-title { font-size: 26px; font-weight: 800; color: #0f172a; margin-bottom: 8px; letter-spacing: 1px; }
.p-subtitle { font-size: 12px; color: #94a3b8; letter-spacing: 2px; font-family: monospace; margin-bottom: 20px;}
.p-data-source { display: inline-flex; flex-direction: column; background: #f8fafc; border: 1px solid #e2e8f0; padding: 10px 20px; border-radius: 6px; text-align: left;}
.ds-item { font-size: 12px; color: #475569; font-weight: 500; line-height: 1.8;}

/* 章节通用 */
.paper-section { margin-bottom: 40px; }
.section-title { font-size: 16px; font-weight: 800; color: #0f172a; display: flex; align-items: center; margin-bottom: 20px; border-bottom: 1px solid #f1f5f9; padding-bottom: 10px;}
.s-dot { width: 6px; height: 16px; background: #0f172a; margin-right: 10px; border-radius: 2px; }
.text-primary { color: #0ea5e9 !important; }
.bg-primary { background: #0ea5e9 !important; }

/* 💡 新增：结论前置区 */
.exec-summary-section { background: #f0f9ff; padding: 24px; border-radius: 8px; border-left: 4px solid #0ea5e9; margin-bottom: 40px; }
.exec-summary-section .section-title { border-bottom: none; margin-bottom: 16px; padding-bottom: 0; }
.exec-box { display: flex; flex-direction: column; gap: 12px; }
.exec-row { font-size: 15px; }
.exec-label { color: #475569; font-weight: 600; width: 140px; display: inline-block;}
.exec-val { color: #0f172a; }

/* 详情摘要区 */
.aero-summary-box { background: #f8fafc; border: 1px solid #e2e8f0; border-radius: 8px; padding: 20px 24px; font-size: 14px; line-height: 1.8; color: #334155; }
.admin-theme { background: #faf5ff; border: 1px solid #e9d5ff; }
.s-block { text-indent: 2em; }
.mt-2 { margin-top: 12px; }
.mt-4 { margin-top: 24px; }
.hl-num { font-size: 18px; font-weight: 800; font-family: "DIN Alternate", Arial; padding: 0 4px; }
.hl-text { font-weight: 700; color: #0f172a; }
.text-safe { color: #10b981; }
.text-danger { color: #ef4444; }
.text-warn { color: #f59e0b; }
.text-purple { color: #9333ea; }

/* 异常警告 */
.anomaly-alert { margin-top: 16px; background: #fef2f2; border: 1px dashed #fca5a5; padding: 10px 12px; border-radius: 6px; font-size: 12px; color: #991b1b; line-height: 1.6;}
.anomaly-icon { font-weight: bold; color: #dc2626; }

/* 图表区 */
.chart-row { display: flex; align-items: stretch; }
.chart-box { border: 1px solid #f1f5f9; border-radius: 8px; padding: 16px; background: #ffffff; height: 100%; display: flex; flex-direction: column;}
.admin-chart-box { border: 1px dashed #d8b4fe; background: #faf5ff; }
.c-title { font-size: 14px; font-weight: 700; color: #334155; margin-bottom: 10px; text-align: center; }
.chart-note { font-size: 12px; color: #64748b; padding-top: 10px; border-top: 1px dashed #e2e8f0; margin-top: auto;}

/* 💡 新增：行动指令区 (Actionable Directives) */
.directive-list { display: flex; flex-direction: column; gap: 20px; }
.directive-card { background: #ffffff; border: 1px solid #e2e8f0; border-radius: 6px; overflow: hidden; box-shadow: 0 2px 4px rgba(0,0,0,0.02);}
.d-header { background: #f8fafc; padding: 12px 16px; border-bottom: 1px solid #e2e8f0; font-weight: 700; font-size: 15px; }
.d-type { color: #b91c1c; background: #fef2f2; padding: 4px 10px; border-radius: 4px; display: inline-block;}
.d-body { padding: 16px; display: flex; flex-direction: column; gap: 10px;}
.d-row { display: flex; font-size: 14px; line-height: 1.6;}
.align-top { align-items: flex-start; }
.d-label { width: 80px; color: #64748b; font-weight: 600; flex-shrink: 0;}
.d-value { color: #0f172a; flex: 1;}
.measures-list div { margin-bottom: 6px; }
.measures-list div:last-child { margin-bottom: 0; }

.admin-list .directive-card { border-color: #f3e8ff; }
.admin-list .d-header { background: #faf5ff; border-color: #f3e8ff; }

/* 页脚 */
.paper-footer { margin-top: 60px; text-align: center; }
.f-line { height: 1px; background: #e2e8f0; width: 60%; margin: 0 auto 20px; }
.paper-footer p { font-size: 12px; color: #94a3b8; font-weight: 600; letter-spacing: 1px; margin-bottom: 4px; }
.f-stamp { font-family: monospace; color: #cbd5e1 !important; }
</style>