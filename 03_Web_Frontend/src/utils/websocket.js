import { notification } from 'ant-design-vue';
import { useAlarmStore } from '@/stores/alarmStore'; 
import { h } from 'vue'; 

let websock = null;
let reconnectTimer = null;
let isReconnecting = false;
let wsUrlGlobal = '';

// 重连机制
const reconnect = () => {
  if (isReconnecting) return;
  isReconnecting = true;
  reconnectTimer = setTimeout(() => {
    console.log("正在尝试重新连接 WebSocket...");
    initWebSocket(wsUrlGlobal);
    isReconnecting = false;
  }, 3000); 
};

export const initWebSocket = (wsUrl) => {
  wsUrlGlobal = wsUrl;
  websock = new WebSocket(wsUrl);

  websock.onopen = function () {
    console.log("WebSocket连接成功");
    if (reconnectTimer) clearTimeout(reconnectTimer);
  };

  websock.onerror = function () {
    console.log("WebSocket连接发生错误");
    reconnect(); 
  };

  websock.onclose = function () {
    console.log("WebSocket连接已关闭");
    reconnect(); 
  };

  websock.onmessage = function (e) {
    try {
      const data = JSON.parse(e.data);
      
      const deviceId = data.device_id || data.deviceId;
      const alarmType = data.type || data.alarmType;
      
      if (!deviceId || !alarmType) {
        console.log("收到非报警的提示消息:", data);
        return; 
      }

      // 1. 默默把数据存入 Store，右侧的实时报警列表会自动更新
      const alarmStore = useAlarmStore();
      alarmStore.addAlarm(data);
      
      // 2. 🌟 告别硬编码：读取 .env 配置的真实后端地址拼接图片
      const imgPath = data.img_path || data.imgPath;
      const baseUrl = import.meta.env.VITE_APP_BASE_API || '';
      const imageUrl = imgPath ? (imgPath.startsWith('http') ? imgPath : `${baseUrl}${imgPath}`) : '';

      // 3. 🌟 修复弹窗逻辑：判断当前所在页面是不是监控页
      const currentPath = window.location.pathname + window.location.hash;
      
      // 逻辑取反：只有【不在】监控页（/monitor）的时候，才弹窗！在监控页就不要弹窗打扰看视频了！
      if (!currentPath.includes('/monitor')) {
        
        // 动态构建弹窗内容
        const descriptionNodes = [
          h('p', `发现 ${alarmType} 异常！`) // 直接使用后端传来的中文，不再写死三元表达式
        ];
        
        // 如果有图片才渲染图片节点
        if (imageUrl) {
          descriptionNodes.push(h('img', { src: imageUrl, style: 'width: 100%; border-radius: 4px; margin-top: 10px;' }));
        }

        notification.error({
          message: `🚨 紧急报警: ${data.location || '监控区域'}`, // 直接用后端传的中文位置
          description: h('div', descriptionNodes),
          duration: 5, // 5秒后自动消失
        });
      }
      
      // 删除了你代码里原先重复在外面写的那第二个 notification.error
      
    } catch (err) {
      console.log("收到非JSON格式的WebSocket消息:", e.data);
    }
  };
}


// ====== 下面是专门用于前端独立测试的代码 ======
export const testSimulateAlarm = () => {
  console.log("模拟边缘盒子发送了一条报警数据...");
  
  // 模拟一份后端发来的标准中文数据（告别英文硬编码）
  const mockData = {
    device_id: "box_001",
    type: "火灾报警",
    timestamp: Date.now(), 
    location: "主教学楼大厅",
    img_base64: "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAEhQGAhKmMIQAAAABJRU5ErkJggg==" 
  };

  const alarmStore = useAlarmStore();
  alarmStore.addAlarm(mockData);

  const imageUrl = mockData.img_base64;
  
  notification.error({
    message: `🚨 紧急报警: ${mockData.location}`,
    description: h('div', [
      h('p', `发现 ${mockData.type} 异常！`),
      h('img', { src: imageUrl, style: 'width: 100%; border-radius: 4px; margin-top: 10px;' })
    ]),
    duration: 3, 
  });
};