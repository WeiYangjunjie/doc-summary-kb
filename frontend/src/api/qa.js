import request from './request'

/**
 * 提问
 * @param {{question:string, topK?:number}} body
 */
export function askQuestion(body) {
  return request.post('/qa/ask', body)
}

/**
 * 分页查询问答历史
 * @param {{page?:number, size?:number}} params
 */
export function listQaHistory(params = {}) {
  return request.get('/qa/history', { params })
}