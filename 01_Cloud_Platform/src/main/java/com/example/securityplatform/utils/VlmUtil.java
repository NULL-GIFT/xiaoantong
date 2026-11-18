package com.example.securityplatform.utils;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class VlmUtil {

    @Value("${ai.qwen.api-key}")
    private String apiKey;

    @Value("${ai.qwen.model}")
    private String modelName;

    @Value("${ai.qwen.url}")
    private String apiUrl;

    /**
     * 呼叫大模型进行视觉二次验证
     * @param base64Img 边缘端传来的 base64 图片数据
     * @return true(真火) / false(误报或干扰)
     */
    public boolean verifyFire(String base64Img) {
        System.out.println("🤖 [大模型海关] 正在将疑似图片发往云端大模型 (Qwen-VL) 审核...");

        // 1. 组装符合 OpenAI 规范的 Base64 字符串
        String imagePayload = base64Img;
        if (!base64Img.startsWith("data:image")) {
            imagePayload = "data:image/jpeg;base64," + base64Img;
        }

        // 2. 构建极其克制的 Prompt 提示词（不要让大模型说废话）
        String prompt = "你是一个专业的工业级消防安全专家。请判断这张图片中是否包含真实的火焰。" +
                "如果判定为真火，请仅输出 'TRUE'；如果是晚霞、灯光、反光、红色塑料等干扰项，请仅输出 'FALSE'。绝对不要解释。";

        // 3. 构建 JSON 请求体 (Fastjson)
        JSONObject requestBody = new JSONObject();
        requestBody.put("model", modelName);

        // 组装 messages 数组
        JSONArray messages = new JSONArray();
        JSONObject message = new JSONObject();
        message.put("role", "user");

        // 组装 content 数组 (包含文字和图片)
        JSONArray contentArray = new JSONArray();

        JSONObject textObj = new JSONObject();
        textObj.put("type", "text");
        textObj.put("text", prompt);

        JSONObject imageObj = new JSONObject();
        imageObj.put("type", "image_url");
        JSONObject urlObj = new JSONObject();
        urlObj.put("url", imagePayload);
        imageObj.put("image_url", urlObj);

        contentArray.add(textObj);
        contentArray.add(imageObj);
        message.put("content", contentArray);
        messages.add(message);

        requestBody.put("messages", messages);

        // 4. 发起 HTTP POST 请求
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + apiKey);

        HttpEntity<String> entity = new HttpEntity<>(requestBody.toJSONString(), headers);

        try {
            // 调用接口
            String response = restTemplate.postForObject(apiUrl, entity, String.class);
            JSONObject resJson = JSONObject.parseObject(response);

            // 解析大模型的回答
            String answer = resJson.getJSONArray("choices")
                    .getJSONObject(0)
                    .getJSONObject("message")
                    .getString("content").trim().toUpperCase();

            System.out.println("🤖 [大模型结论]: " + answer);

            // 只要大模型没有明确说是 TRUE，我们一律按 FALSE (误报) 处理，宁缺毋滥！
            return answer.contains("TRUE");

        } catch (Exception e) {
            System.err.println("❌ 大模型接口调用失败: " + e.getMessage());
            // 如果网络断了或者超时，保守起见默认放行（返回 true），避免因为大模型挂了导致真火没报警
            return true;
        }
    }
}