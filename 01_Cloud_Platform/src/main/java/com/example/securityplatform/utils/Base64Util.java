package com.example.securityplatform.utils;

import org.springframework.stereotype.Component;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.UUID;

@Component
public class Base64Util {

    public static String base64ToImage(String base64Str, String saveDir) {
        System.out.println("=== Base64图片保存开始 ===");

        try {
            // 1. 检查Base64字符串
            System.out.println("Base64字符串长度: " + (base64Str != null ? base64Str.length() : 0));
            System.out.println("Base64前50字符: " + (base64Str != null ? base64Str.substring(0, Math.min(50, base64Str.length())) : "null"));

            // 2. 创建目录
            File dir = new File(saveDir);
            System.out.println("目标目录: " + dir.getAbsolutePath());
            System.out.println("目录是否存在: " + dir.exists());

            if (!dir.exists()) {
                boolean created = dir.mkdirs();
                System.out.println("创建目录结果: " + (created ? "成功" : "失败"));
            }

            // 3. 生成唯一文件名（避免覆盖）
            String fileName = System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8) + ".jpg";
            File imageFile = new File(dir, fileName);
            System.out.println("生成文件名: " + fileName);
            System.out.println("完整路径: " + imageFile.getAbsolutePath());

            // 4. 解码并保存
            // 检查Base64是否以data:image开头
            if (base64Str.startsWith("data:image")) {
                System.out.println("⚠️  Base64包含data:前缀，需要去除");
                // 去除前缀
                base64Str = base64Str.substring(base64Str.indexOf(",") + 1);
                System.out.println("处理后长度: " + base64Str.length());
            }

            byte[] imageBytes = Base64.getDecoder().decode(base64Str);
            System.out.println("解码后字节数: " + imageBytes.length);

            try (FileOutputStream fos = new FileOutputStream(imageFile)) {
                fos.write(imageBytes);
            }

            System.out.println("✅ 图片保存成功: " + fileName);
            System.out.println("文件大小: " + imageFile.length() + " 字节");

            return fileName;

        } catch (Exception e) {
            System.err.println("❌ 图片保存失败: " + e.getClass().getName() + " - " + e.getMessage());
            e.printStackTrace();
            return null;
        } finally {
            System.out.println("=== Base64图片保存结束 ===\n");
        }
    }
}