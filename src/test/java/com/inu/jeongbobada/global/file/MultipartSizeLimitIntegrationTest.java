package com.inu.jeongbobada.global.file;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

// spring.servlet.multipart.max-file-size(10MB)를 넘는 파일은 컨트롤러에 닿기 전에 Spring이 막는다.
// 그때 500이 아니라 413 FILE_TOO_LARGE로 나가는지 확인한다.
// MockMvc는 크기 한도를 적용하지 않아서, 실제 Tomcat을 띄워 진짜 multipart 요청을 보낸다.
// 업로드 API가 아직 없으므로 공개 API(로그인)에 보낸다. multipart 해석은 어떤 컨트롤러로 가든 그 전에 일어난다.
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class MultipartSizeLimitIntegrationTest {

    private static final String BOUNDARY = "jeongbobada-test-boundary";

    @LocalServerPort
    private int port;

    @Test
    void 한도를_넘는_파일은_413_FILE_TOO_LARGE() throws Exception {
        // 한도보다 100KB만 크게: 남은 본문이 작아야 Tomcat이 연결을 끊지 않고 응답을 끝까지 보낸다
        byte[] file = new byte[10 * 1024 * 1024 + 100 * 1024];

        HttpResponse<String> response = postMultipart("/api/auth/login", file);

        assertThat(response.statusCode()).isEqualTo(413);
        assertThat(response.body()).contains("\"code\":\"FILE_TOO_LARGE\"");
    }

    private HttpResponse<String> postMultipart(String path, byte[] file) throws Exception {
        ByteArrayOutputStream body = new ByteArrayOutputStream();
        body.write(("--" + BOUNDARY + "\r\n"
            + "Content-Disposition: form-data; name=\"file\"; filename=\"big.pdf\"\r\n"
            + "Content-Type: application/pdf\r\n\r\n").getBytes(StandardCharsets.UTF_8));
        body.write(file);
        body.write(("\r\n--" + BOUNDARY + "--\r\n").getBytes(StandardCharsets.UTF_8));

        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
            .header("Content-Type", "multipart/form-data; boundary=" + BOUNDARY)
            .POST(HttpRequest.BodyPublishers.ofByteArray(body.toByteArray()))
            .build();
        try (HttpClient client = HttpClient.newHttpClient()) {
            return client.send(request, HttpResponse.BodyHandlers.ofString());
        }
    }
}
