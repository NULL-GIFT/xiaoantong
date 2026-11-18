import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useAlarmStore = defineStore('alarm', () => {
  const alarmList = ref([])

  // 🚨 核心修复：停止在这里篡改字段名！直接存入原始数据，让 index.vue 的翻译官去处理！
  const addAlarm = (newAlarm) => {
    alarmList.value.unshift(newAlarm)
  }

  // 🌟 接收历史记录打底，同样直接存入
  const initAlarms = (historyList) => {
    alarmList.value = historyList;
  }

  return { alarmList, addAlarm, initAlarms }
})