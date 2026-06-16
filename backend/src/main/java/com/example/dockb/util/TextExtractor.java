package com.example.dockb.util;

import com.example.dockb.common.BizException;
import com.example.dockb.common.ResultCode;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

/**
 * 纯文本提取器：支持 pdf / txt / md / docx。
 */
@Slf4j
public final class TextExtractor {

    public static final Set<String> SUPPORTED_TYPES = Set.of("pdf", "txt", "md", "docx");

    private TextExtractor() {
    }

    /**
     * 抽取文件纯文本。
     *
     * @param fileType 扩展名（小写，不含点）
     */
    public static String extract(Path file, String fileType) {
        if (fileType == null) {
            throw new BizException(ResultCode.FILE_TYPE_INVALID);
        }
        String type = fileType.toLowerCase();
        if (!SUPPORTED_TYPES.contains(type)) {
            throw new BizException(ResultCode.FILE_TYPE_INVALID, "不支持的文件类型: " + fileType);
        }
        try {
            return switch (type) {
                case "pdf" -> extractPdf(file);
                case "docx" -> extractDocx(file);
                case "md", "txt" -> extractPlain(file);
                default -> throw new BizException(ResultCode.FILE_TYPE_INVALID, type);
            };
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            log.error("[TextExtractor] failed for {}: {}", file, e.getMessage(), e);
            throw new BizException(ResultCode.FILE_READ_FAILED, "文件读取失败: " + e.getMessage());
        }
    }

    private static String extractPdf(Path file) throws IOException {
        try (PDDocument doc = PDDocument.load(file.toFile())) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(doc);
        }
    }

    private static String extractDocx(Path file) throws IOException {
        try (InputStream in = Files.newInputStream(file);
             XWPFDocument doc = new XWPFDocument(in);
             XWPFWordExtractor extractor = new XWPFWordExtractor(doc)) {
            return extractor.getText();
        }
    }

    private static String extractPlain(Path file) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            char[] buf = new char[8192];
            int n;
            while ((n = reader.read(buf)) > 0) {
                sb.append(buf, 0, n);
            }
        }
        return sb.toString();
    }
}