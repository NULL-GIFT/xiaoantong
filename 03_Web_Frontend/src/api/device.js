import request from '../utils/request'

export function getDevice(params) {
  return request({
    url: '/api/device/list', // 🌟 必须加上 /api
    method: 'get',
    params 
  })
}

export function addDevices(data) {
    return request({
        url:'/api/device/add', // 🌟 必须加上 /api
        method: 'post',
        data
    })
}