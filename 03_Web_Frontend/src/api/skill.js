import request from '../utils/request'

export function getSkills(params) {
  return request({
    url: '/api/skill/list', // 🌟 必须加上 /api
    method: 'get',
    params
  })
}

export function addSkills(data) {
    return request({
        url:'/api/skill/add', // 🌟 必须加上 /api
        method: 'post',
        data
    })
}