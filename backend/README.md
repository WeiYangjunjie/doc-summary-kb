# doc-summary-kb-backend

Spring Boot 3.2 + Java 17 + MyBatis-Plus + MySQL 8 + MiniMax M3 后端。
详细契约参见 `../docs/design/CONTRACT.md`。

## 目录结构

```
backend/
├── pom.xml
├── README.md
└── src/
    ├── main/
    │   ├── java/com/example/dockb/
    │   │   ├── DocKbApplication.java
    │   │   ├── common/          # Result, ResultCode, BizException, GlobalExceptionHandler, PageResult
    │   │   ├── config/          # AppProperties, M3Properties, CorsConfig, MybatisPlusConfig,
    │   │   │                    # MybatisAutoFillHandler, AsyncConfig, RestClientConfig
    │   │   ├── controller/      # DocumentController, QaController, HealthController
    │   │   ├── service/ + impl/ # DocumentService(Impl), QaService(Impl), M3Service(Impl)
    │   │   ├── mapper/          # DocumentMapper, DocumentChunkMapper, QaHistoryMapper
    │   │   ├── entity/          # Document, DocumentChunk, QaHistory
    │   │   ├── dto/             # QaAskRequest
    │   │   ├── vo/              # DocumentVO, SearchResponseVO, SearchHitVO, CitationVO,
    │   │   │                    # QaAnswerVO, QaHistoryVO, HealthVO
    │   │   ├── client/          # M3Client, DefaultM3Client, M3Exception, dto/*
    │   │   └── util/            # TextExtractor, TextChunker, SnippetUtil
    │   └── resources/
    │       ├── application.yml
    │       └── db/schema.sql    # 拷贝自 docs/design/SCHEMA.sql
    └── test/java/com/example/dockb/
        └── DocumentControllerIT.java
```

## 本地启动

### 0. 准备

* JDK 17
* Maven 3.9+
* MySQL 8.0+（运行 `src/main/resources/db/schema.sql` 初始化）
* （可选）MiniMax M3 API Key：环境变量 `MiniMax_API_KEY` 或 `application.yml`

### 1. 初始化数据库

```bash
mysql -u root -p < src/main/resources/db/schema.sql
```

如已存在 `doc_kb` 库，脚本使用 `CREATE TABLE IF NOT EXISTS`，不会覆盖。

### 2. 配置

编辑 `src/main/resources/application.yml`：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/doc_kb?...
    username: root
    password: root
MiniMax:
  api-key: REPLACE_WITH_YOUR_KEY  # 或设置环境变量 MiniMax_API_KEY
```

启动时若 `api-key` 仍是占位符，会打印 WARN；可只测上传/列表。

### 3. 编译 / 打包

```bash
mvn -q -DskipTests package
```

产物：`target/doc-summary-kb-backend.jar`

### 4. 运行

```bash
mvn spring-boot:run
# 或
java -jar target/doc-summary-kb-backend.jar
```

端口默认 8080。文件上传目录：`<user.dir>/uploads/`。

## 测试

```bash
mvn test
```

至少包含：

* `DocumentControllerIT`：MockMvc 测试 `/api/health`、`/api/documents`、`/api/documents/search`。
  不依赖真实 M3 与 MySQL（Mock 掉 `M3Client`/`DocumentService`）。
* `m3ClientMockWorks`：M3Client mock 自检。

> 若环境无 Maven，可直接用 `javac` 语法自检（仅做语法层面）：
> ```bash
> javac --release 17 -d /tmp/dockb-out -cp "your-deps.jar" \
>   $(find src/main/java -name '*.java')
> ```

## API 速览

| Method | Path                          | 说明                 |
|--------|-------------------------------|---------------------|
| POST   | /api/documents/upload         | 上传（multipart）    |
| GET    | /api/documents                | 列表（page/size/keyword/category） |
| GET    | /api/documents/{id}           | 详情                 |
| DELETE | /api/documents/{id}           | 删除（含文件+chunks）|
| GET    | /api/documents/{id}/file      | 在线预览             |
| GET    | /api/documents/categories     | 分类聚合             |
| GET    | /api/documents/search?q=...&topK=5 | 检索           |
| POST   | /api/qa/ask                   | 问答                 |
| GET    | /api/qa/history               | 问答历史             |
| GET    | /api/health                   | 健康检查             |

## 设计要点

* **异步**：上传后立即返回 `id`，摘要/分类/标签通过 `@Async("docKbExecutor")` 异步执行，状态机 `pending→processing→done|failed`。
* **重排降级**：M3 不可达时退化为 TF 词频打分；问答降级时直接拼接 top2 上下文作为答案。
* **M3 协议**：OpenAI 兼容 `/chat/completions`，超时 10s/60s，失败 1 次重试。
* **文件解析**：PDFBox (pdf) + Apache POI (docx) + 纯文本 (txt/md)。
* **错误响应**：所有异常翻译为 `Result{code,message,data}`，HTTP 200/400/500。

## CORS

`http://localhost:5173` 与 `http://127.0.0.1:5173` 已默认放行；可通过
`application.yml` 的 `app.cors.allowed-origins` 追加。